package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PerformanceSectorPriceDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting between performance entities and DTOs.
 */
@Mapper
public interface PerformanceMapper {

    /**
     * Map create DTO to entity.
     *
     * @param performanceCreateDto source DTO
     * @return mapped entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "artists", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "hall", expression = "java(hallFromId(performanceCreateDto.getHallId()))")
    Performance performanceCreateDtoToPerformance(PerformanceCreateDto performanceCreateDto);

    default Hall hallFromId(Long hallId) {
        if (hallId == null) {
            return null;
        }
        Hall hall = new Hall();
        hall.setId(hallId);
        return hall;
    }

    List<PerformanceSectorPrice> performanceSectorPriceDtosToPerformanceSectorPrices(List<PerformanceSectorPriceDto> sectorPrices);

    PerformanceSectorPrice performanceSectorPriceDtoToPerformanceSectorPrice(PerformanceSectorPriceDto sectorPrice);

    @Mapping(target = "artistIds", expression = "java(performance.getArtists().stream().map(artist -> artist.getId()).toList())")
    @Mapping(target = "artistNames", expression = "java(performance.getArtists().stream().map(artist -> artist.getArtistName()).toList())")
    @Mapping(target = "hallId", expression = "java(performance.getHall() != null ? performance.getHall().getId() : null)")
    @Mapping(target = "hallName", expression = "java(performance.getHall() != null ? performance.getHall().getName() : null)")
    @Mapping(target = "genre", source = "genre")
    PerformanceDetailDto performanceToPerformanceDetailDto(Performance performance);

    List<PerformanceSectorPriceDto> performanceSectorPricesToPerformanceSectorPriceDtos(List<PerformanceSectorPrice> sectorPrices);

    PerformanceSectorPriceDto performanceSectorPriceToPerformanceSectorPriceDto(PerformanceSectorPrice sectorPrice);
}
