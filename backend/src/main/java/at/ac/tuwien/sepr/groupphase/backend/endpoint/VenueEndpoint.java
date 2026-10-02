package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.VenueCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.VenueDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.VenueMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/venues")
public class VenueEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final VenueService venueService;
    private final VenueMapper venueMapper;

    public VenueEndpoint(VenueService venueService, VenueMapper venueMapper) {
        this.venueService = venueService;
        this.venueMapper = venueMapper;
    }

    @Secured("ROLE_ADMIN")
    @GetMapping
    @Operation(summary = "Get all venues", security = @SecurityRequirement(name = "apiKey"))
    public List<VenueDetailDto> findAll() {
        LOGGER.info("GET /api/v1/venues");
        return venueMapper.venueToVenueDetailDto(venueService.findAll());
    }

    @Secured("ROLE_ADMIN")
    @GetMapping(value = "/{id}")
    @Operation(summary = "Get venue by id", security = @SecurityRequirement(name = "apiKey"))
    public VenueDetailDto findOne(@PathVariable(name = "id") Long id) {
        LOGGER.info("GET /api/v1/venues/{}", id);
        return venueMapper.venueToVenueDetailDto(venueService.findOne(id));
    }

    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a venue", security = @SecurityRequirement(name = "apiKey"))
    public VenueDetailDto create(@Valid @RequestBody VenueCreateDto venueCreateDto) {
        LOGGER.info("POST /api/v1/venues body: {}", venueCreateDto);
        return venueMapper.venueToVenueDetailDto(venueService.create(venueMapper.venueCreateDtoToVenue(venueCreateDto)));
    }

    @Secured("ROLE_ADMIN")
    @PutMapping(value = "/{id}")
    @Operation(summary = "Update a venue", security = @SecurityRequirement(name = "apiKey"))
    public VenueDetailDto update(@PathVariable(name = "id") Long id, @Valid @RequestBody VenueCreateDto venueCreateDto) {
        LOGGER.info("PUT /api/v1/venues/{} body: {}", id, venueCreateDto);
        return venueMapper.venueToVenueDetailDto(venueService.update(id, venueMapper.venueCreateDtoToVenue(venueCreateDto)));
    }
}
