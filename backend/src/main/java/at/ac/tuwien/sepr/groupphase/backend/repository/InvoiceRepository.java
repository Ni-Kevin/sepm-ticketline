package at.ac.tuwien.sepr.groupphase.backend.repository;

import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findFirstByOrderIdAndTypeOrderByIdDesc(Long orderId, InvoiceType type);

    @Query("SELECT i FROM Invoice i "
        + "LEFT JOIN FETCH i.order o "
        + "LEFT JOIN FETCH o.user "
        + "LEFT JOIN FETCH o.tickets t "
        + "LEFT JOIN FETCH t.performance p "
        + "LEFT JOIN FETCH p.event "
        + "LEFT JOIN FETCH t.sector s "
        + "LEFT JOIN FETCH s.hall h "
        + "LEFT JOIN FETCH h.venue "
        + "LEFT JOIN FETCH t.seat "
        + "WHERE i.id = :id")
    Optional<Invoice> findByIdWithAllRelations(@Param("id") Long id);
}
