package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for order persistence operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o "
        + "LEFT JOIN FETCH o.tickets t "
        + "LEFT JOIN FETCH t.performance p "
        + "LEFT JOIN FETCH p.event LEFT JOIN FETCH t.sector "
        + "LEFT JOIN FETCH t.seat LEFT JOIN FETCH o.user "
        + "WHERE o.id = (SELECT t2.order.id FROM Ticket t2 WHERE t2.id = :ticketId)")
    Optional<Order> findByTicketIdWithTickets(@Param("ticketId") Long ticketId);
}
