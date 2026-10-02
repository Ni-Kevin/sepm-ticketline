package at.ac.tuwien.sepr.groupphase.backend.endpoint;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.HallLayoutMapper;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.HallMapper;
import at.ac.tuwien.sepr.groupphase.backend.service.HallService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/halls")
public class HallEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final HallService hallService;
    private final HallMapper hallMapper;
    private final HallLayoutMapper hallLayoutMapper;

    public HallEndpoint(HallService hallService, HallMapper hallMapper, HallLayoutMapper hallLayoutMapper) {
        this.hallService = hallService;
        this.hallMapper = hallMapper;
        this.hallLayoutMapper = hallLayoutMapper;
    }

    @Secured("ROLE_ADMIN")
    @GetMapping
    @Operation(summary = "Get all halls", security = @SecurityRequirement(name = "apiKey"))
    public List<HallDetailDto> findAll(@RequestParam(name = "venueId", required = false) Long venueId) {
        LOGGER.info("GET /api/v1/halls?venueId={}", venueId);
        return hallMapper.hallToHallDetailDto(hallService.findAll(venueId)).stream()
            .map(this::withLockStatus)
            .toList();
    }

    @Secured("ROLE_ADMIN")
    @GetMapping(value = "/{id}")
    @Operation(summary = "Get hall by id", security = @SecurityRequirement(name = "apiKey"))
    public HallDetailDto findOne(@PathVariable(name = "id") Long id) {
        LOGGER.info("GET /api/v1/halls/{}", id);
        return withLockStatus(hallMapper.hallToHallDetailDto(hallService.findOne(id)));
    }

    @Secured("ROLE_ADMIN")
    @GetMapping(value = "/{id}/layout")
    @Operation(summary = "Get hall layout by hall id", security = @SecurityRequirement(name = "apiKey"))
    public HallLayoutDto findLayout(@PathVariable(name = "id") Long id) {
        LOGGER.info("GET /api/v1/halls/{}/layout", id);
        HallLayoutDto dto = hallLayoutMapper.hallToHallLayoutDto(hallService.findLayout(id));
        dto.setLayoutLocked(hallService.isLayoutLocked(id));
        return dto;
    }

    @Secured("ROLE_ADMIN")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    @Operation(summary = "Create a hall", security = @SecurityRequirement(name = "apiKey"))
    public HallDetailDto create(@Valid @RequestBody HallCreateDto hallCreateDto) throws ValidationException {
        LOGGER.info("POST /api/v1/halls body: {}", hallCreateDto);
        return withLockStatus(hallMapper.hallToHallDetailDto(hallService.create(
            hallMapper.hallCreateDtoToHall(hallCreateDto),
            hallCreateDto.getVenueId()
        )));
    }

    @Secured("ROLE_ADMIN")
    @PutMapping(value = "/{id}")
    @Operation(summary = "Update a hall", security = @SecurityRequirement(name = "apiKey"))
    public HallDetailDto update(@PathVariable(name = "id") Long id, @Valid @RequestBody HallCreateDto hallCreateDto) throws ValidationException {
        LOGGER.info("PUT /api/v1/halls/{} body: {}", id, hallCreateDto);
        return withLockStatus(hallMapper.hallToHallDetailDto(hallService.update(
            id,
            hallMapper.hallCreateDtoToHall(hallCreateDto),
            hallCreateDto.getVenueId()
        )));
    }

    @Secured("ROLE_ADMIN")
    @PutMapping(value = "/{id}/layout")
    @Operation(summary = "Update a hall layout", security = @SecurityRequirement(name = "apiKey"))
    public HallLayoutDto updateLayout(@PathVariable(name = "id") Long id, @Valid @RequestBody HallLayoutDto hallLayoutDto) throws ValidationException {
        LOGGER.info("PUT /api/v1/halls/{}/layout body: {}", id, hallLayoutDto);
        HallLayoutDto dto = hallLayoutMapper.hallToHallLayoutDto(hallService.updateLayout(
            id,
            hallLayoutMapper.sectorLayoutDtoToSector(hallLayoutDto.getSectors()),
            mapAreasWithSectorReferences(hallLayoutDto)
        ));
        dto.setLayoutLocked(hallService.isLayoutLocked(id));
        return dto;
    }

    private List<at.ac.tuwien.sepr.groupphase.backend.entity.HallArea> mapAreasWithSectorReferences(HallLayoutDto hallLayoutDto) {
        List<at.ac.tuwien.sepr.groupphase.backend.entity.HallArea> areas = hallLayoutMapper.hallAreaLayoutDtoToHallArea(hallLayoutDto.getAreas());
        var mappedSectors = hallLayoutMapper.sectorLayoutDtoToSector(hallLayoutDto.getSectors());
        var sectorsById = mappedSectors.stream()
            .filter(sector -> sector.getId() != null)
            .collect(java.util.stream.Collectors.toMap(at.ac.tuwien.sepr.groupphase.backend.entity.Sector::getId, java.util.function.Function.identity()));
        var sectorsByName = mappedSectors.stream()
            .filter(sector -> sector.getName() != null)
            .collect(java.util.stream.Collectors.toMap(sector -> sector.getName().toLowerCase(), java.util.function.Function.identity(), (left, right) -> left));

        for (int i = 0; i < areas.size(); i++) {
            Long sectorId = hallLayoutDto.getAreas().get(i).getSectorId();
            if (sectorId != null) {
                areas.get(i).setSector(sectorsById.get(sectorId));
            } else if (hallLayoutDto.getAreas().get(i).getSectorName() != null) {
                areas.get(i).setSector(sectorsByName.get(hallLayoutDto.getAreas().get(i).getSectorName().toLowerCase()));
            }
        }
        return areas;
    }

    private HallDetailDto withLockStatus(HallDetailDto dto) {
        dto.setLayoutLocked(hallService.isLayoutLocked(dto.getId()));
        dto.setDimensionsLocked(hallService.isDimensionsLocked(dto.getId()));
        dto.setDimensionsLockReason(hallService.getDimensionsLockReason(dto.getId()));
        return dto;
    }
}
