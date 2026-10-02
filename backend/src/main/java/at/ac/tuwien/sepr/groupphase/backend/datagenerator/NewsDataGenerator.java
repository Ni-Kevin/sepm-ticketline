package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Profile("generateData")
@Component
public class NewsDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int TARGET_NEWS_COUNT = 100;
    private static final String TEST_NEWS_TITLE = "Title";
    private static final String TEST_NEWS_SUMMARY = "Summary of the news entry";
    private static final String TEST_NEWS_TEXT = "This is the text of the news entry";

    private final NewsRepository newsRepository;

    public NewsDataGenerator(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @PostConstruct
    private void generateNews() {
        long existing = newsRepository.count();
        if (existing >= TARGET_NEWS_COUNT) {
            LOGGER.debug("News already generated ({} existing). Skipping.", existing);
            return;
        }

        int toCreate = TARGET_NEWS_COUNT - (int) existing;
        LOGGER.debug("generating {} news entries", toCreate);

        List<News> batch = new ArrayList<>();
        for (int i = (int) existing; i < TARGET_NEWS_COUNT; i++) {
            batch.add(News.NewsBuilder.aNews()
                .withTitle(TEST_NEWS_TITLE + " " + i)
                .withSummary(TEST_NEWS_SUMMARY + " " + i)
                .withText(TEST_NEWS_TEXT + " " + i)
                .withPublishedAt(LocalDateTime.now().minusMonths(i))
                .withImage(loadImageAsBytes(String.format("News%d.jpeg", (i % 3) + 1)))
                .build());

            if (batch.size() >= 50) {
                newsRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            newsRepository.saveAll(batch);
        }
        LOGGER.debug("Saved news entries up to target {}", TARGET_NEWS_COUNT);
    }

    private byte[] loadImageAsBytes(String imageName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("testdata-event-images/" + imageName)) {
            if (is == null) {
                LOGGER.warn("Image {} not found!", imageName);
                return null;
            }
            return is.readAllBytes();
        } catch (IOException e) {
            LOGGER.error("Error loading image {}", imageName, e);
            return null;
        }
    }

}
