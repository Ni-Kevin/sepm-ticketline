package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.DetailedNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.NewsInquiryDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.NewsPageDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SimpleNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.NewsMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.service.NewsService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * REST endpoint for news-related operations.
 */
@RestController
@RequestMapping(value = "/api/v1/news")
public class NewsEndpoint {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        MediaType.IMAGE_JPEG_VALUE,
        MediaType.IMAGE_PNG_VALUE,
        "image/webp"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final NewsService newsService;
    private final NewsMapper newsMapper;
    private final UserService userService;

    @Autowired
    public NewsEndpoint(NewsService newsService, NewsMapper newsMapper, UserService userService) {
        this.newsService = newsService;
        this.newsMapper = newsMapper;
        this.userService = userService;
    }

    @Secured("ROLE_USER")
    @GetMapping
    @Operation(summary = "Get list of news entries without details", security = @SecurityRequirement(name = "apiKey"))
    public List<SimpleNewsDto> findAll() {
        LOGGER.info("GET /api/v1/news");
        return newsMapper.newsToSimpleNewsDto(newsService.findAll());
    }

    @Secured("ROLE_USER")
    @GetMapping(value = "/{id}")
    @Operation(summary = "Get detailed information about a specific news entry and mark it as read", security = @SecurityRequirement(name = "apiKey"))
    public DetailedNewsDto find(@PathVariable(name = "id") Long id, Authentication auth) {
        LOGGER.info("GET /api/v1/news/{} for user: {}", id, auth.getName());

        newsService.markNewsAsRead(id, auth.getName());

        return newsMapper.newsToDetailedNewsDto(newsService.findOne(id));
    }

    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Publish a news entry", security = @SecurityRequirement(name = "apiKey"))
    public DetailedNewsDto create(
        @Valid @RequestPart("news") NewsInquiryDto newsDto,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        LOGGER.info("POST /api/v1/news body: {}", newsDto);
        try {
            var news = newsMapper.newsInquiryDtoToNews(newsDto);
            if (image != null && !image.isEmpty()) {
                if (!ALLOWED_IMAGE_TYPES.contains(image.getContentType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PNG, JPEG and WebP images are allowed");
                }
                news.setImage(image.getBytes());
            }
            return newsMapper.newsToDetailedNewsDto(newsService.publishNews(news));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read image file");
        }
    }

    /**
     * Returns a list of all news that the authenticated user has not read yet.
     *
     * @param auth authentication of the current user
     * @return a list of unread news for the user
     */
    @GetMapping("/unread")
    @Secured("ROLE_USER")
    public NewsPageDto getUnreadNews(
        Authentication auth,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        LOGGER.info("GET /api/v1/news/unread for user: {}", auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        var newsPage = newsService.getUnreadNews(user.getId(), page, size);
        return new NewsPageDto(
            newsMapper.newsToSimpleNewsDto(newsPage.getContent()),
            newsPage.getTotalPages(),
            newsPage.getTotalElements()
        );
    }


    /**
     * Returns a list of all news that the authenticated user has already read.
     *
     * @param auth authentication of the current user
     * @return a list of read news for the user
     */
    @GetMapping("/read")
    @Secured("ROLE_USER")
    public NewsPageDto getReadNews(
        Authentication auth,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        LOGGER.info("GET /api/v1/news/read for user: {}", auth.getName());

        ApplicationUser user = userService.findApplicationUserByEmail(auth.getName());
        var newsPage = newsService.getReadNews(user.getId(), page, size);
        return new NewsPageDto(
            newsMapper.newsToSimpleNewsDto(newsPage.getContent()),
            newsPage.getTotalPages(),
            newsPage.getTotalElements()
        );
    }

}
