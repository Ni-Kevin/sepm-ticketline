package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import java.util.List;

public class PerformanceHallLayoutDto {
    private String hallName;
    private Integer hallWidth;
    private Integer hallLength;
    private String venueName;
    private String venueStreet;
    private String venueCity;
    private String venueZip;
    private String venueCountry;
    private List<PerformanceSectorLayoutDto> sectors;

    public PerformanceHallLayoutDto() {
    }

    public PerformanceHallLayoutDto(String hallName, Integer hallWidth, Integer hallLength,
                                    String venueName, String venueStreet, String venueCity,
                                    String venueZip, String venueCountry, List<PerformanceSectorLayoutDto> sectors) {
        this.hallName = hallName;
        this.hallWidth = hallWidth;
        this.hallLength = hallLength;
        this.venueName = venueName;
        this.venueStreet = venueStreet;
        this.venueCity = venueCity;
        this.venueZip = venueZip;
        this.venueCountry = venueCountry;
        this.sectors = sectors;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public Integer getHallWidth() {
        return hallWidth;
    }

    public void setHallWidth(Integer hallWidth) {
        this.hallWidth = hallWidth;
    }

    public Integer getHallLength() {
        return hallLength;
    }

    public void setHallLength(Integer hallLength) {
        this.hallLength = hallLength;
    }

    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getVenueStreet() {
        return venueStreet;
    }

    public void setVenueStreet(String venueStreet) {
        this.venueStreet = venueStreet;
    }

    public String getVenueCity() {
        return venueCity;
    }

    public void setVenueCity(String venueCity) {
        this.venueCity = venueCity;
    }

    public String getVenueZip() {
        return venueZip;
    }

    public void setVenueZip(String venueZip) {
        this.venueZip = venueZip;
    }

    public String getVenueCountry() {
        return venueCountry;
    }

    public void setVenueCountry(String venueCountry) {
        this.venueCountry = venueCountry;
    }

    public List<PerformanceSectorLayoutDto> getSectors() {
        return sectors;
    }

    public void setSectors(List<PerformanceSectorLayoutDto> sectors) {
        this.sectors = sectors;
    }
}
