package at.ac.tuwien.sepr.groupphase.backend.endpoint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body used for creating a new artist.
 */
public class ArtistCreateDto {

    @NotBlank(message = "must not be blank")
    @Size(max = 255)
    private String firstName;

    @NotBlank(message = "must not be blank")
    @Size(max = 255)
    private String lastName;

    @NotBlank(message = "must not be blank")
    @Size(max = 255)
    private String artistName;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }
}
