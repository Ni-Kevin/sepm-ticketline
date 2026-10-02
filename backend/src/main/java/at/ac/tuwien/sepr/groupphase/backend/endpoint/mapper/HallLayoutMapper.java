package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallAreaLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SectorLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface HallLayoutMapper {

    @Mapping(target = "sectorId", source = "sector.id")
    @Mapping(target = "sectorName", source = "sector.name")
    HallAreaLayoutDto hallAreaToHallAreaLayoutDto(HallArea hallArea);

    List<HallAreaLayoutDto> hallAreaToHallAreaLayoutDto(List<HallArea> hallAreas);

    SeatLayoutDto seatToSeatLayoutDto(Seat seat);

    List<SeatLayoutDto> seatToSeatLayoutDto(List<Seat> seats);

    SectorLayoutDto sectorToSectorLayoutDto(Sector sector);

    List<SectorLayoutDto> sectorToSectorLayoutDto(List<Sector> sectors);

    HallArea hallAreaLayoutDtoToHallArea(HallAreaLayoutDto hallAreaLayoutDto);

    List<HallArea> hallAreaLayoutDtoToHallArea(List<HallAreaLayoutDto> hallAreaLayoutDtos);

    Seat seatLayoutDtoToSeat(SeatLayoutDto seatLayoutDto);

    List<Seat> seatLayoutDtoToSeat(List<SeatLayoutDto> seatLayoutDtos);

    @Mapping(target = "hall", ignore = true)
    Sector sectorLayoutDtoToSector(SectorLayoutDto sectorLayoutDto);

    List<Sector> sectorLayoutDtoToSector(List<SectorLayoutDto> sectorLayoutDtos);

    default HallLayoutDto hallToHallLayoutDto(Hall hall) {
        HallLayoutDto dto = new HallLayoutDto();
        dto.setHallId(hall.getId());
        dto.setName(hall.getName());
        dto.setWidth(hall.getWidth());
        dto.setLength(hall.getLength());
        dto.setVenueId(hall.getVenue().getId());
        dto.setSectors(sectorToSectorLayoutDto(hall.getSectors()));
        dto.setAreas(hallAreaToHallAreaLayoutDto(hall.getAreas()));
        return dto;
    }
}
