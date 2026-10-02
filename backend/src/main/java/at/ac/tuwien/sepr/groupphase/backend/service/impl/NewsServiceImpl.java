package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import at.ac.tuwien.sepr.groupphase.backend.entity.NewsReadState;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsReadStateRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.NewsService;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import jakarta.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class NewsServiceImpl implements NewsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final NewsRepository newsRepository;
    private final NewsReadStateRepository newsReadStateRepository;
    private final UserService userService;

    public NewsServiceImpl(NewsRepository newsRepository, NewsReadStateRepository newsReadStateRepository, UserService userService) {
        this.newsRepository = newsRepository;
        this.newsReadStateRepository = newsReadStateRepository;
        this.userService = userService;
    }

    @Override
    public List<News> findAll() {
        LOGGER.debug("Find all news");
        return newsRepository.findAllByOrderByPublishedAtDesc();
    }

    @Override
    public News findOne(Long id) {
        LOGGER.debug("Find news with id {}", id);
        Optional<News> news = newsRepository.findById(id);
        if (news.isPresent()) {
            return news.get();
        } else {
            throw new NotFoundException(String.format("Could not find news with id %s", id));
        }
    }

    @Override
    public News publishNews(News news) {
        LOGGER.debug("Publish new news entry {}", news);
        if (news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }
        return newsRepository.save(news);
    }

    @Override
    public Page<News> getUnreadNews(Long userId, int page, int size) {
        return newsRepository.findUnreadNewsByUserId(
            userId,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))
        );
    }


    @Override
    public Page<News> getReadNews(Long userId, int page, int size) {
        return newsRepository.findReadNewsByUserId(
            userId,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))
        );
    }


    @Override
    @Transactional
    public void markNewsAsRead(Long newsId, String email) {
        LOGGER.debug("Mark News {} as read after clicking on the detail button {}", newsId, email);

        News news = findOne(newsId);

        ApplicationUser user = userService.findApplicationUserByEmail(email);

        boolean alreadyMarked = newsReadStateRepository.existsByUserAndNews(user, news);


        if (!alreadyMarked) {
            NewsReadState newsReadState = new NewsReadState();
            newsReadState.setUser(user);
            newsReadState.setNews(news);
            newsReadState.setReadAt(LocalDateTime.now());

            newsReadStateRepository.save(newsReadState);
        }

    }

}
