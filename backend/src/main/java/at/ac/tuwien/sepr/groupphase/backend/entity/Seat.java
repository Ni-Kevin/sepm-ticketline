package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(name = "seat", uniqueConstraints = {
    @UniqueConstraint(name = "uk_seat_sector_row_number", columnNames = {"sector_id", "row_number", "seat_number"})
})
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(name = "x_coordinate", nullable = false)
    private Integer positionX;

    @Column(name = "y_coordinate", nullable = false)
    private Integer positionY;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public void setPositionX(Integer positionX) {
        this.positionX = positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public void setPositionY(Integer positionY) {
        this.positionY = positionY;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Seat seat)) {
            return false;
        }
        return Objects.equals(id, seat.id)
            && Objects.equals(rowNumber, seat.rowNumber)
            && Objects.equals(seatNumber, seat.seatNumber)
            && Objects.equals(positionX, seat.positionX)
            && Objects.equals(positionY, seat.positionY);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, rowNumber, seatNumber, positionX, positionY);
    }

    @Override
    public String toString() {
        return "rowNumber=" + rowNumber
            + ", seatNumber=" + seatNumber;
    }
}
