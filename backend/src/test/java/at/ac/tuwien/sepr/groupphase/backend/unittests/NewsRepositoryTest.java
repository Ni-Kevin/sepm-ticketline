package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
// This test slice annotation is used instead of @SpringBootTest to load only repository beans instead of
// the entire application context
@DataJpaTest
@ActiveProfiles("test")
public class NewsRepositoryTest implements TestData {

    @Autowired
    private NewsRepository newsRepository;

    @Test
    public void givenNothing_whenSaveNews_thenFindListWithOneElementAndFindNewsById() {
        News news = News.NewsBuilder.aNews()
            .withTitle(TEST_NEWS_TITLE)
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
            .build();

        newsRepository.save(news);

        assertAll(
            () -> assertEquals(1, newsRepository.findAll().size()),
            () -> assertNotNull(newsRepository.findById(news.getId()))
        );
    }

    @Test
    public void givenMultipleNews_whenFindAllByOrderByPublishedAtDesc_thenReturnsDescendingOrder() {
        News olderNews = News.NewsBuilder.aNews()
            .withTitle("Older")
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(LocalDateTime.of(2020, 1, 1, 10, 0))
            .build();

        News newerNews = News.NewsBuilder.aNews()
            .withTitle("Newer")
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(LocalDateTime.of(2021, 1, 1, 10, 0))
            .build();

        newsRepository.saveAll(List.of(olderNews, newerNews));

        List<News> result = newsRepository.findAllByOrderByPublishedAtDesc();

        assertAll(
            () -> assertEquals(2, result.size()),
            () -> assertEquals("Newer", result.get(0).getTitle()),
            () -> assertEquals("Older", result.get(1).getTitle())
        );
    }

    @Test
    public void givenNews_whenFindUnreadNewsByUserId_thenReturnsAllNewsForNewUser() {
        News news = News.NewsBuilder.aNews()
            .withTitle(TEST_NEWS_TITLE)
            .withSummary(TEST_NEWS_SUMMARY)
            .withText(TEST_NEWS_TEXT)
            .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
            .build();
        newsRepository.save(news);

        Page<News> result = newsRepository.findUnreadNewsByUserId(999L, PageRequest.of(0, 10));

        assertEquals(1, result.getContent().size());
        assertEquals(TEST_NEWS_TITLE, result.getContent().get(0).getTitle());
    }

    @Test
    public void givenNoNews_whenFindUnreadNewsByUserId_thenReturnsEmptyList() {
        Page<News> result = newsRepository.findUnreadNewsByUserId(999L, PageRequest.of(0, 10));
        assertEquals(0, result.getContent().size());
    }

}
