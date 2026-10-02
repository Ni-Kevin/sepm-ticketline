package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserUpdateDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
    /**
     * Maps a UserRegisterDto to an ApplicationUser entity.
     *
     * @param userRegisterDto the DTO containing registration data
     * @return the mapped ApplicationUser entity
     */
    ApplicationUser userRegisterDtoToApplicationUser(UserRegisterDto userRegisterDto);

    /**
     * Maps an ApplicationUser entity to a UserUpdateDto.
     * This is used to return the updated data to the frontend.
     *
     * @param user the entity from the database
     * @return the DTO for the response
     */
    UserUpdateDto applicationUserToUserUpdateDto(ApplicationUser user);

    ApplicationUser userCreateDtoToApplicationUser(UserCreateDto userCreateDto);

    /**
     * Maps a UserUpdateDto to an ApplicationUser entity.
     * * @param userUpdateDto the DTO with updated data
     *
     * @return a new ApplicationUser entity
     */
    ApplicationUser userUpdateDtoToApplicationUser(UserUpdateDto userUpdateDto);

}
