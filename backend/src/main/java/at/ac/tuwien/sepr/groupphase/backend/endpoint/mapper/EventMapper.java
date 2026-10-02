package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.EventDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting between event entities and DTOs.
 */
@Mapper
public interface EventMapper {

    /**
     * Map create DTO to entity.
     *
     * @param eventCreateDto source DTO
     * @return mapped entity
     */
    @Mapping(target = "id", ignore = true)
    Event eventCreateDtoToEvent(EventCreateDto eventCreateDto);

    /**
     * Map entity to detail DTO.
     *
     * @param event source entity
     * @return mapped detail DTO
     */
    @Mapping(target = "performanceIds", expression = "java(mapPerformanceIds(event))")
    EventDetailDto eventToEventDetailDto(Event event);

    default List<Long> mapPerformanceIds(Event event) {
        if (event == null || event.getPerformances() == null) {
            return List.of();
        }
        return event.getPerformances().stream()
            .map(Performance::getId)
            .filter(id -> id != null)
            .sorted(Comparator.naturalOrder())
            .toList();
    }
}
