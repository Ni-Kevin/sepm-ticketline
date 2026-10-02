package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.DetailedNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.NewsInquiryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.NewsPageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SimpleNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.NewsMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsReadState;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsReadStateRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class NewsEndpointTest implements TestData {

    private static final byte[] TEST_IMAGE = "news-image".getBytes(StandardCharsets.UTF_8);

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
    private UserRepository userRepository;

    @Autowired
    private NewsReadStateRepository newsReadStateRepository;


    private ApplicationUser testUser;

    private News news = News.NewsBuilder.aNews()
        .withTitle(TEST_NEWS_TITLE)
        .withSummary(TEST_NEWS_SUMMARY)
        .withText(TEST_NEWS_TEXT)
        .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
        .withImage(TEST_IMAGE)
        .build();

    @BeforeEach
    public void beforeEach() {
        newsRepository.deleteAll();
        news = News.NewsBuilder.aNews()
            .withTitle(TEST_NEWS_TITLE)
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
            .withImage(TEST_IMAGE)
            .build();

        userRepository.deleteAll();
        testUser = new ApplicationUser();
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail(ADMIN_USER);
        testUser.setPassword("Password123!");
        testUser.setRole(UserRole.ROLE_ADMIN);
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    public void afterEach() {
        newsRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void givenNothing_whenFindAll_thenEmptyList() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

        List<SimpleNewsDto> simpleNewsDtos = Arrays.asList(jsonMapper.readValue(response.getContentAsString(),
            SimpleNewsDto[].class));

        assertEquals(0, simpleNewsDtos.size());
    }

    @Test
    public void givenOneNewsEntry_whenFindAll_thenListWithSizeOneAndNewsWithAllPropertiesExceptText()
        throws Exception {
        newsRepository.save(news);

        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

        List<SimpleNewsDto> simpleNewsDtos = Arrays.asList(jsonMapper.readValue(response.getContentAsString(),
            SimpleNewsDto[].class));

        assertEquals(1, simpleNewsDtos.size());
        SimpleNewsDto simpleNewsDto = simpleNewsDtos.get(0);
        assertAll(
            () -> assertEquals(news.getId(), simpleNewsDto.getId()),
            () -> assertEquals(TEST_NEWS_TITLE, simpleNewsDto.getTitle()),
            () -> assertEquals(TEST_NEWS_SUMMARY, simpleNewsDto.getSummary()),
            () -> assertEquals(TEST_NEWS_PUBLISHED_AT, simpleNewsDto.getPublishedAt())
        );
    }

    @Test
    public void givenOneNewsEntry_whenFindById_thenNewsWithAllProperties() throws Exception {
        newsRepository.save(news);

        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI + "/{id}", news.getId())
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType())
        );

        DetailedNewsDto detailedNewsDto = jsonMapper.readValue(response.getContentAsString(), DetailedNewsDto.class);
        assertEquals(news, newsMapper.detailedNewsDtoToNews(detailedNewsDto));
    }

    @Test
    public void givenOneNewsEntry_whenFindByNonExistingId_then404() throws Exception {
        newsRepository.save(news);

        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI + "/{id}", -1)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void givenNothing_whenPostWithoutPublishedAt_thenNewsWithAllSetPropertiesPlusIdAndPublishedDate() throws Exception {
        news.setPublishedAt(null);
        news.setImage(null);
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
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());

        DetailedNewsDto newsResponse = jsonMapper.readValue(response.getContentAsString(), DetailedNewsDto.class);

        assertNotNull(newsResponse.getId());
        assertNotNull(newsResponse.getPublishedAt());
        assertArrayEquals(TEST_IMAGE, newsResponse.getImage());
        assertTrue(isNow(newsResponse.getPublishedAt()));
        newsResponse.setId(null);
        newsResponse.setPublishedAt(null);
        newsResponse.setImage(null);
        assertEquals(news, newsMapper.detailedNewsDtoToNews(newsResponse));
    }

    @Test
    public void givenNothing_whenPostWithExplicitPublishedAtAndNoText_thenNewsUsesProvidedPublishedAt() throws Exception {
        news.setText(null);
        news.setImage(null);
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

        DetailedNewsDto newsResponse = jsonMapper.readValue(response.getContentAsString(), DetailedNewsDto.class);
        assertAll(
            () -> assertEquals(TEST_NEWS_PUBLISHED_AT, newsResponse.getPublishedAt()),
            () -> assertNull(newsResponse.getText()),
            () -> assertArrayEquals(TEST_IMAGE, newsResponse.getImage())
        );
    }

    @Test
    public void givenNothing_whenPostWithoutImage_then201AndNewsWithoutImage() throws Exception {
        NewsInquiryDto newsInquiryDto = newsMapper.newsToNewsInquiryDto(news);
        MockMultipartFile newsPart = newsPart(newsInquiryDto);

        MvcResult mvcResult = this.mockMvc.perform(multipart(NEWS_BASE_URI)
                .file(newsPart)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        DetailedNewsDto newsResponse = jsonMapper.readValue(response.getContentAsString(), DetailedNewsDto.class);
        assertNull(newsResponse.getImage());
    }

    @Test
    public void givenNewsWithInvalidImageType_whenPost_then400() throws Exception {
        NewsInquiryDto newsInquiryDto = newsMapper.newsToNewsInquiryDto(news);
        MockMultipartFile newsPart = newsPart(newsInquiryDto);
        MockMultipartFile invalidImagePart = new MockMultipartFile(
            "image",
            "news.gif",
            MediaType.IMAGE_GIF_VALUE,
            TEST_IMAGE
        );

        MvcResult mvcResult = this.mockMvc.perform(multipart(NEWS_BASE_URI)
            .file(newsPart)
            .file(invalidImagePart)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () -> assertTrue(response.getContentAsString().contains("Only PNG, JPEG and WebP images are allowed"))
        );
    }

    @Test
    public void givenNothing_whenPostInvalid_then400() throws Exception {
        news.setTitle(" ");
        news.setSummary(null);
        news.setImage(null);
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

        assertAll(
            () -> assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus()),
            () -> {
                String content = response.getContentAsString();
                content = content.substring(content.indexOf('[') + 1, content.indexOf(']'));
                String[] errors = content.split(",");
                assertEquals(2, errors.length);
            }
        );
    }

    @Test
    public void givenNews_whenGetUnread_thenReturnsNewsList() throws Exception {
        newsRepository.save(news);

        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI + "/unread")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        NewsPageDto result = jsonMapper.readValue(response.getContentAsString(), NewsPageDto.class);
        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals(TEST_NEWS_TITLE, result.getContent().get(0).getTitle()),
            () -> assertEquals(TEST_NEWS_SUMMARY, result.getContent().get(0).getSummary()),
            () -> assertEquals(1, result.getTotalPages()),
            () -> assertEquals(1, result.getTotalElements())
        );
    }

    @Test
    public void givenNoNews_whenGetUnread_thenReturnsEmptyList() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI + "/unread")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        NewsPageDto result = jsonMapper.readValue(response.getContentAsString(), NewsPageDto.class);
        assertEquals(0, result.getContent().size());
    }

    @Test
    public void givenNoToken_whenGetUnread_then403() throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI + "/unread"))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();
        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }


    @Test
    public void openingNewsDetails_shouldCreateNewsReadState() throws Exception {
        News news = News.NewsBuilder.aNews()
            .withTitle("Detail Test")
            .withSummary("Kurz")
            .withText("Das ist der ganz lange Text für US 7.2")
            .withPublishedAt(LocalDateTime.now())
            .build();
        news = newsRepository.save(news);

        mockMvc.perform(get(NEWS_BASE_URI + "/{id}", news.getId())
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES))
            .contentType(MediaType.APPLICATION_JSON));

        List<NewsReadState> readStates = newsReadStateRepository.findAll();
        assertThat(readStates).hasSize(1);
        assertThat(readStates.get(0).getNews().getId()).isEqualTo(news.getId());
        Long userId = readStates.get(0).getUser().getId();
        ApplicationUser savedUser = userRepository.findById(userId).orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo(ADMIN_USER);
    }


    @Test
    public void openingNewsDetailsTwice_shouldCreateOnlyOneNewsReadState() throws Exception {
        News news = News.NewsBuilder.aNews()
            .withTitle("Duplicate Test")
            .withSummary("Kurz")
            .withText("Inhalt")
            .withPublishedAt(LocalDateTime.now())
            .build();
        news = newsRepository.save(news);


        mockMvc.perform(get(NEWS_BASE_URI + "/{id}", news.getId())
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES))
            .contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(get(NEWS_BASE_URI + "/{id}", news.getId())
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES))
            .contentType(MediaType.APPLICATION_JSON));

        List<NewsReadState> readStates = newsReadStateRepository.findAll();
        assertThat(readStates).hasSize(1);
    }


    @Test
    public void givenOneReadNewsEntry_whenGetReadNews_thenReturnsListWithThisNews() throws Exception {
        News savedNews = News.NewsBuilder.aNews()
            .withTitle(TEST_NEWS_TITLE)
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(LocalDateTime.now())
            .build();
        savedNews = newsRepository.save(savedNews);

        NewsReadState readState = new NewsReadState();
        readState.setNews(savedNews);
        readState.setUser(testUser);
        readState.setReadAt(LocalDateTime.now());
        newsReadStateRepository.save(readState);

        MvcResult mvcResult = this.mockMvc.perform(get(NEWS_BASE_URI + "/read")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        NewsPageDto result = jsonMapper.readValue(response.getContentAsString(), NewsPageDto.class);

        assertAll(
            () -> assertEquals(1, result.getContent().size()),
            () -> assertEquals(TEST_NEWS_TITLE, result.getContent().get(0).getTitle()),
            () -> assertEquals(TEST_NEWS_SUMMARY, result.getContent().get(0).getSummary()),
            () -> assertEquals(1, result.getTotalPages()),
            () -> assertEquals(1, result.getTotalElements())
        );
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
            TEST_IMAGE
        );
    }

    private boolean isNow(LocalDateTime date) {
        LocalDateTime today = LocalDateTime.now();
        return date.getYear() == today.getYear() && date.getDayOfYear() == today.getDayOfYear()
            && date.getHour() == today.getHour();
    }
}
