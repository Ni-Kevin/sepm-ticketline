package at.ac.tuwien.sepr.groupphase.backend.service;

import at.ac.tuwien.sepr.groupphase.backend.entity.News;

import java.util.List;
import org.springframework.data.domain.Page;

public interface NewsService {

    /**
     * Find all news entries ordered by published at date (descending).
     *
     * @return ordered list of all news entries
     */
    List<News> findAll();


    /**
     * Find a single news entry by id.
     *
     * @param id the id of the news entry
     * @return the news entry
     */
    News findOne(Long id);

    /**
     * Publish a single news entry.
     *
     * @param news to publish
     * @return published news entry
     */
    News publishNews(News news);


    /**
     * Returns a list of all news that the user has not read yet.
     *
     * @param userId - ID of the user to get the unread news for
     * @return a list of unread news for the user
     */
    Page<News> getUnreadNews(Long userId, int page, int size);


    /**
     * After a user clicked on the detail view of a news entry the news are marked as read
     * so that they are not shown to the user again.
     *
     * @param newsId the ID of the news to be marked as read for the user
     * @param email  the email of the user who read the news
     */
    void markNewsAsRead(Long newsId, String email);


    /**
     * Returns a list of all news that the user has already read.
     *
     * @param userId - ID of the user to get the read news for
     * @return a list of read news for the user
     */
    Page<News> getReadNews(Long userId, int page, int size);
}
