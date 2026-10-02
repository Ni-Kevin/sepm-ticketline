package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ArtistDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.ArtistMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.lang.invoke.MethodHandles;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;


/**
 * REST endpoint for artist operations.
 */
@RestController
@RequestMapping(value = "/api/v1/artists")
public class ArtistEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ArtistService artistService;
    private final ArtistMapper artistMapper;

    public ArtistEndpoint(ArtistService artistService, ArtistMapper artistMapper) {
        this.artistService = artistService;
        this.artistMapper = artistMapper;
    }

    /**
     * Create a new artist.
     *
     * @param artistCreateDto artist payload
     * @return persisted artist
     */
    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a new artist", security = @SecurityRequirement(name = "apiKey"))
    public ArtistDetailDto create(@Valid @RequestBody ArtistCreateDto artistCreateDto) {
        LOGGER.info("POST /api/v1/artists body: {}", artistCreateDto);
        return artistMapper.artistToArtistDetailDto(
            artistService.createArtist(artistMapper.artistCreateDtoToArtist(artistCreateDto)));
    }

    /**
     * Search artists by query string.
     *
     * @param query optional query term
     * @return matching artists
     */
    @PermitAll
    @GetMapping
    @Operation(summary = "Search artists")
    public List<ArtistDetailDto> search(@RequestParam(name = "query", required = false) String query) {
        LOGGER.info("GET /api/v1/artists?query={}", query);
        return artistService.searchArtists(query).stream().map(artistMapper::artistToArtistDetailDto).toList();
    }

    /**
     * Get artist by id.
     *
     * @param id artist id
     * @return artist detail
     */
    @PermitAll
    @GetMapping(value = "/{id}")
    @Operation(summary = "Get artist by id")
    public ArtistDetailDto getById(@PathVariable(name = "id") Long id) {
        LOGGER.info("GET /api/v1/artists/{}", id);
        return artistMapper.artistToArtistDetailDto(artistService.getArtistById(id));
    }
}
