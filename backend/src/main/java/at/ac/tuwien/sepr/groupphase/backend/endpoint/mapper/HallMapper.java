package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface HallMapper {

    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    HallDetailDto hallToHallDetailDto(Hall hall);

    List<HallDetailDto> hallToHallDetailDto(List<Hall> hall);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "sectors", ignore = true)
    @Mapping(target = "areas", ignore = true)
    Hall hallCreateDtoToHall(HallCreateDto hallCreateDto);
}
