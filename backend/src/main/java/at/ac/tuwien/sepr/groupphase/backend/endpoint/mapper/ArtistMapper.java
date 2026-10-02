package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ArtistDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting between artist entities and DTOs.
 */
@Mapper
public interface ArtistMapper {

    /**
     * Map create DTO to entity.
     *
     * @param artistCreateDto source DTO
     * @return mapped entity
     */
    @Mapping(target = "id", ignore = true)
    Artist artistCreateDtoToArtist(ArtistCreateDto artistCreateDto);

    /**
     * Map entity to detail DTO.
     *
     * @param artist source entity
     * @return mapped detail DTO
     */
    ArtistDetailDto artistToArtistDetailDto(Artist artist);
}
