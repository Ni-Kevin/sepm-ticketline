package at.ac.tuwien.sepr.groupphase.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "hall_area", uniqueConstraints = {
    @UniqueConstraint(name = "uk_hall_area_hall_position", columnNames = {"hall_id", "x_coordinate", "y_coordinate"})
})
public class HallArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "x_coordinate", nullable = false)
    private Integer positionX;

    @Column(name = "y_coordinate", nullable = false)
    private Integer positionY;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer length;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HallAreaType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public HallAreaType getType() {
        return type;
    }

    public void setType(HallAreaType type) {
        this.type = type;
    }

    public Hall getHall() {
        return hall;
    }

    public void setHall(Hall hall) {
        this.hall = hall;
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
        if (!(o instanceof HallArea hallArea)) {
            return false;
        }
        return Objects.equals(id, hallArea.id)
            && Objects.equals(positionX, hallArea.positionX)
            && Objects.equals(positionY, hallArea.positionY)
            && Objects.equals(width, hallArea.width)
            && Objects.equals(length, hallArea.length)
            && type == hallArea.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, positionX, positionY, width, length, type);
    }

    @Override
    public String toString() {
        return "HallArea{"
            + "id=" + id
            + ", positionX=" + positionX
            + ", positionY=" + positionY
            + ", width=" + width
            + ", length=" + length
            + ", type=" + type
            + '}';
    }
}
