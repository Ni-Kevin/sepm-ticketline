package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsReadStateRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.NewsRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.UserService;
import at.ac.tuwien.sepr.groupphase.backend.service.impl.NewsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NewsReadStateRepository newsReadStateRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NewsServiceImpl newsService;

    @Test
    public void publishNewsSetsPublishedAtWhenMissing() {
        News newsToPublish = News.NewsBuilder.aNews()
            .withTitle("Breaking News")
            .withSummary("Summary")
            .withText("Text")
            .build();

        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));

        News result = newsService.publishNews(newsToPublish);

        ArgumentCaptor<News> newsCaptor = ArgumentCaptor.forClass(News.class);
        verify(newsRepository).save(newsCaptor.capture());

        assertAll(
            () -> assertNotNull(result.getPublishedAt()),
            () -> assertNotNull(newsCaptor.getValue().getPublishedAt()),
            () -> assertEquals(newsCaptor.getValue().getPublishedAt(), result.getPublishedAt())
        );
    }

    @Test
    public void publishNewsPreservesExplicitPublishedAt() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 31, 12, 0);
        News newsToPublish = News.NewsBuilder.aNews()
            .withTitle("Scheduled News")
            .withSummary("Summary")
            .withText("Text")
            .withPublishedAt(publishedAt)
            .build();

        when(newsRepository.save(any(News.class))).thenAnswer(invocation -> invocation.getArgument(0));

        News result = newsService.publishNews(newsToPublish);

        assertEquals(publishedAt, result.getPublishedAt());
        verify(newsRepository).save(newsToPublish);
    }

    @Test
    public void findOneThrowsNotFoundExceptionForMissingId() {
        when(newsRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> newsService.findOne(42L));
    }

    @Test
    public void getUnreadNewsReturnsUnreadNewsForUser() {
        News someNews = News.NewsBuilder.aNews()
            .withTitle("Test")
            .withSummary("Summary")
            .withPublishedAt(LocalDateTime.now())
            .build();
        when(newsRepository.findUnreadNewsByUserId(1L, PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(someNews)));
        Page<News> result = newsService.getUnreadNews(1L, 0, 10);
        assertEquals(1, result.getContent().size());
        verify(newsRepository).findUnreadNewsByUserId(1L, PageRequest.of(0, 10));
    }

    @Test
    public void getUnreadNewsReturnsEmptyListWhenAllRead() {
        when(newsRepository.findUnreadNewsByUserId(1L, PageRequest.of(0, 10)))
            .thenReturn(Page.empty());
        Page<News> result = newsService.getUnreadNews(1L, 0, 10);
        assertEquals(0, result.getContent().size());
    }


    @Test
    public void givenNewsNotReadYet_whenFindOne_thenSaveNewReadState() {
        Long newsId = 1L;
        String email = "admin@email.com";
        News news = News.NewsBuilder.aNews().build();
        ApplicationUser user = new ApplicationUser();

        when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));
        when(userService.findApplicationUserByEmail(email)).thenReturn(user);

        when(newsReadStateRepository.existsByUserAndNews(user, news)).thenReturn(false);

        newsService.markNewsAsRead(newsId, email);

        verify(newsReadStateRepository, times(1)).save(any());
    }

    @Test
    public void givenNewsAlreadyRead_whenMarkAsRead_thenDoNotSaveAgain() {
        Long newsId = 1L;
        String email = "admin@email.com";
        News news = News.NewsBuilder.aNews().build();
        ApplicationUser user = new ApplicationUser();

        when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));
        when(userService.findApplicationUserByEmail(email)).thenReturn(user);

        when(newsReadStateRepository.existsByUserAndNews(user, news)).thenReturn(true);

        newsService.markNewsAsRead(newsId, email);

        verify(newsReadStateRepository, times(0)).save(any());
    }

    @Test
    public void getReadNewsReturnsReadNewsForUser() {
        News someNews = News.NewsBuilder.aNews()
            .withTitle("Schon gelesene News")
            .withSummary("Summary")
            .withPublishedAt(LocalDateTime.now())
            .build();

        when(newsRepository.findReadNewsByUserId(1L, PageRequest.of(0, 10)))
            .thenReturn(new PageImpl<>(List.of(someNews)));

        Page<News> result = newsService.getReadNews(1L, 0, 10);

        assertEquals(1, result.getContent().size());
        assertEquals("Schon gelesene News", result.getContent().get(0).getTitle());
        verify(newsRepository, times(1)).findReadNewsByUserId(1L, PageRequest.of(0, 10));
    }

    @Test
    public void getReadNewsReturnsEmptyListWhenNothingReadYet() {
        when(newsRepository.findReadNewsByUserId(1L, PageRequest.of(0, 10)))
            .thenReturn(Page.empty());

        Page<News> result = newsService.getReadNews(1L, 0, 10);

        assertEquals(0, result.getContent().size());
        verify(newsRepository, times(1)).findReadNewsByUserId(1L, PageRequest.of(0, 10));
    }

}
