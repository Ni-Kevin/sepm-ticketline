package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    /**
     * Find all news entries ordered by published at date (descending).
     *
     * @return ordered list of all news entries
     */
    List<News> findAllByOrderByPublishedAtDesc();

    /**
     * Find all news entries that have not been read by the user with the given ID.
     *
     * @param userId the ID of the user for whom to find unread news entries
     * @return a list of unread news entries for the specified user
     */
    @Query(
        value = "SELECT n FROM News n WHERE n NOT IN (SELECT r.news FROM NewsReadState r WHERE r.user.id = :userId) ORDER BY n.publishedAt DESC",
        countQuery = "SELECT COUNT(n) FROM News n WHERE n NOT IN (SELECT r.news FROM NewsReadState r WHERE r.user.id = :userId)"
    )
    Page<News> findUnreadNewsByUserId(@Param("userId") Long userId, Pageable pageable);


    /**
     * Find all news entries that have already been read by the user with the given ID.
     *
     * @param userId the ID of the user for whom to find read news entries
     * @return a list of read news entries for the specified user
     */
    @Query(
        value = "SELECT n FROM News n WHERE n IN (SELECT r.news FROM NewsReadState r WHERE r.user.id = :userId) ORDER BY n.publishedAt DESC",
        countQuery = "SELECT COUNT(n) FROM News n WHERE n IN (SELECT r.news FROM NewsReadState r WHERE r.user.id = :userId)"
    )
    Page<News> findReadNewsByUserId(@Param("userId") Long userId, Pageable pageable);
}
