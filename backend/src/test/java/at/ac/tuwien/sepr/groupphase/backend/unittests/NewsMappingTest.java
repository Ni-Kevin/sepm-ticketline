package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.DetailedNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SimpleNewsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.NewsMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NewsMappingTest implements TestData {

    private final News news = News.NewsBuilder.aNews()
        .withId(ID)
        .withTitle(TEST_NEWS_TITLE)
        .withSummary(TEST_NEWS_SUMMARY)
        .withText(TEST_NEWS_TEXT)
        .withPublishedAt(TEST_NEWS_PUBLISHED_AT)
        .build();
    @Autowired
    private NewsMapper newsMapper;

    @Test
    public void givenNothing_whenMapDetailedNewsDtoToEntity_thenEntityHasAllProperties() {
        DetailedNewsDto detailedNewsDto = newsMapper.newsToDetailedNewsDto(news);
        assertAll(
            () -> assertEquals(ID, detailedNewsDto.getId()),
            () -> assertEquals(TEST_NEWS_TITLE, detailedNewsDto.getTitle()),
            () -> assertEquals(TEST_NEWS_SUMMARY, detailedNewsDto.getSummary()),
            () -> assertEquals(TEST_NEWS_TEXT, detailedNewsDto.getText()),
            () -> assertEquals(TEST_NEWS_PUBLISHED_AT, detailedNewsDto.getPublishedAt())
        );
    }

    @Test
    public void givenNothing_whenMapListWithTwoNewsEntitiesToSimpleDto_thenGetListWithSizeTwoAndAllProperties() {
        List<News> newsEntries = new ArrayList<>();
        newsEntries.add(news);
        newsEntries.add(news);

        List<SimpleNewsDto> simpleNewsDtos = newsMapper.newsToSimpleNewsDto(newsEntries);
        assertEquals(2, simpleNewsDtos.size());
        SimpleNewsDto simpleNewsDto = simpleNewsDtos.get(0);
        assertAll(
            () -> assertEquals(ID, simpleNewsDto.getId()),
            () -> assertEquals(TEST_NEWS_TITLE, simpleNewsDto.getTitle()),
            () -> assertEquals(TEST_NEWS_SUMMARY, simpleNewsDto.getSummary()),
            () -> assertEquals(TEST_NEWS_PUBLISHED_AT, simpleNewsDto.getPublishedAt())
        );
    }


}
