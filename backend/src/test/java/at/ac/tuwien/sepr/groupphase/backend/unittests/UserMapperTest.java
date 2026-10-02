package at.ac.tuwien.sepr.groupphase.backend.unittests;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.UserRegisterDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.UserMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the mapping logic between user entities and DTOs.
 * Verifies that MapStruct correctly transforms data across different layers.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserMapperTest implements TestData {

    @Autowired
    private UserMapper userMapper;

    /**
     * Verifies that a UserRegisterDto is correctly mapped to an ApplicationUser entity.
     * Checks if all relevant registration fields are preserved.
     */
    @Test
    public void givenNothing_whenMapUserRegisterDtoToEntity_thenEntityHasAllProperties() {
        UserRegisterDto userRegisterDto = new UserRegisterDto(
            USER_REGISTER_EMAIL,
            USER_REGISTER_PASSWORD,
            USER_REGISTER_FIRST_NAME,
            USER_REGISTER_LAST_NAME
        );

        ApplicationUser entity = userMapper.userRegisterDtoToApplicationUser(userRegisterDto);

        assertAll(
            () -> assertEquals(USER_REGISTER_EMAIL, entity.getEmail()),
            () -> assertEquals(USER_REGISTER_FIRST_NAME, entity.getFirstName()),
            () -> assertEquals(USER_REGISTER_LAST_NAME, entity.getLastName()),
            () -> assertEquals(USER_REGISTER_PASSWORD, entity.getPassword())
        );
    }

}