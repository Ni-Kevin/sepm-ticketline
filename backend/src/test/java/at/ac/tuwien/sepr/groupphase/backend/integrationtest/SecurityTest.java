package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.BackendApplication;
import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.NewsInquiryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.NewsMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Security is a cross-cutting concern, however for the sake of simplicity it is tested against the news endpoint
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SecurityTest implements TestData {

    private static final List<Class<?>> mappingAnnotations = Lists.list(
        RequestMapping.class,
        GetMapping.class,
        PostMapping.class,
        PutMapping.class,
        PatchMapping.class,
        DeleteMapping.class
    );

    private static final List<Class<?>> securityAnnotations = Lists.list(
        Secured.class,
        PreAuthorize.class,
        RolesAllowed.class,
        PermitAll.class,
        DenyAll.class,
        DeclareRoles.class
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private List<Object> components;

    private News news = News.NewsBuilder.aNews()
        .withTitle(TEST_NEWS_TITLE)
        .withSummary(TEST_NEWS_SUMMARY)
        .withText(TEST_NEWS_TEXT)
        .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
        .withImage("news-image".getBytes(StandardCharsets.UTF_8))
        .build();

    @BeforeEach
    public void beforeEach() {
        newsRepository.deleteAll();
        news = News.NewsBuilder.aNews()
            .withTitle(TEST_NEWS_TITLE)
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
            .withImage("news-image".getBytes(StandardCharsets.UTF_8))
            .build();
    }

    /**
     * This ensures every Rest Method is secured with Method Security.
     * It is very easy to forget securing one method causing a security vulnerability.
     * Feel free to remove / disable / adapt if you do not use Method Security (e.g. if you prefer Web Security to define who may perform which actions) or want to use Method Security on the service layer.
     */
    @Test
    public void ensureSecurityAnnotationPresentForEveryEndpoint() {
        List<ImmutablePair<Class<?>, Method>> notSecured = components.stream()
            .map(AopUtils::getTargetClass) // beans may be proxies, get the target class instead
            .filter(clazz -> clazz.getCanonicalName() != null && clazz.getCanonicalName().startsWith(BackendApplication.class.getPackageName())) // limit to our package
            .filter(clazz -> clazz.getAnnotation(RestController.class) != null) // limit to classes annotated with @RestController
            .flatMap(clazz -> Arrays.stream(clazz.getDeclaredMethods()).map(method -> new ImmutablePair<Class<?>, Method>(clazz, method))) // get all class -> method pairs
            .filter(pair -> Arrays.stream(pair.getRight().getAnnotations()).anyMatch(annotation -> mappingAnnotations.contains(annotation.annotationType()))) // keep only the pairs where the method has a "mapping annotation"
            .filter(pair -> Arrays.stream(pair.getRight().getAnnotations()).noneMatch(annotation -> securityAnnotations.contains(annotation.annotationType()))) // keep only the pairs where the method does not have a "security annotation"
            .toList();

        assertThat(notSecured.size())
            .as("Most rest methods should be secured. If one is really intended for public use, explicitly state that with @PermitAll. "
                + "The following are missing: \n" + notSecured.stream().map(pair -> "Class: " + pair.getLeft() + " Method: " + pair.getRight()).reduce("", (a, b) -> a + "\n" + b))
            .isZero();

    }

    @Test
    public void givenUserLoggedIn_whenFindAll_then200() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType())
        );
    }

    @Test
    public void givenNoOneLoggedIn_whenFindAll_then401() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void givenAdminLoggedIn_whenPost_then201() throws Exception {
        NewsInquiryDto newsInquiryDto = newsMapper.newsToNewsInquiryDto(news);
        MockMultipartFile newsPart = newsPart(newsInquiryDto);
        MockMultipartFile imagePart = imagePart();

        MvcResult mvcResult = this.mockMvc.perform(multipart(NEWS_BASE_URI)
            .file(newsPart)
            .file(imagePart)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
    }

    @Test
    public void givenNoOneLoggedIn_whenPost_then403() throws Exception {
        news.setPublishedAt(null);
        NewsInquiryDto newsInquiryDto = newsMapper.newsToNewsInquiryDto(news);
        MockMultipartFile newsPart = newsPart(newsInquiryDto);
        MockMultipartFile imagePart = imagePart();

        MvcResult mvcResult = this.mockMvc.perform(multipart(NEWS_BASE_URI)
            .file(newsPart)
            .file(imagePart))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void givenUserLoggedIn_whenPost_then403() throws Exception {
        news.setPublishedAt(null);
        NewsInquiryDto newsInquiryDto = newsMapper.newsToNewsInquiryDto(news);
        MockMultipartFile newsPart = newsPart(newsInquiryDto);
        MockMultipartFile imagePart = imagePart();

        MvcResult mvcResult = this.mockMvc.perform(multipart(NEWS_BASE_URI)
            .file(newsPart)
            .file(imagePart)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    private MockMultipartFile newsPart(NewsInquiryDto newsInquiryDto) throws Exception {
        return new MockMultipartFile(
            "news",
            "news",
            MediaType.APPLICATION_JSON_VALUE,
            jsonMapper.writeValueAsString(newsInquiryDto).getBytes(StandardCharsets.UTF_8)
        );
    }

    private MockMultipartFile imagePart() {
        return new MockMultipartFile(
            "image",
            "news.png",
            MediaType.IMAGE_PNG_VALUE,
            "news-image".getBytes(StandardCharsets.UTF_8)
        );
    }
}
