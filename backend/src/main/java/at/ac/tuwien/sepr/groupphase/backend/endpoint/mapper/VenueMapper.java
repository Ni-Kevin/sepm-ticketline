package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.VenueCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.VenueDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface VenueMapper {

    VenueDetailDto venueToVenueDetailDto(Venue venue);

    List<VenueDetailDto> venueToVenueDetailDto(List<Venue> venue);

    Venue venueCreateDtoToVenue(VenueCreateDto venueCreateDto);
}
