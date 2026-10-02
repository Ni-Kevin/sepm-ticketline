package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.VenueCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.VenueDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class VenueEndpointTest implements TestData {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private VenueRepository venueRepository;
    @Autowired
    private JsonMapper jsonMapper;
    @Autowired
    private JwtTokenizer jwtTokenizer;
    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    void setUp() {
        venueRepository.deleteAll();
    }

    @Test
    void givenValidVenue_whenCreateFindAllFindOneUpdate_thenWorks() throws Exception {
        VenueCreateDto create = venueDto("Venue A");
        VenueDetailDto created = jsonMapper.readValue(postVenue(create).getResponse().getContentAsString(), VenueDetailDto.class);
        assertEquals(1, venueRepository.count());
        assertEquals("Austria", created.getCountry());

        MockHttpServletResponse listResponse = getVenues().getResponse();
        List<VenueDetailDto> venues = List.of(jsonMapper.readValue(listResponse.getContentAsString(), VenueDetailDto[].class));
        assertEquals(1, venues.size());

        MockHttpServletResponse findResponse = getVenue(created.getId()).getResponse();
        assertEquals(created.getName(), jsonMapper.readValue(findResponse.getContentAsString(), VenueDetailDto.class).getName());

        VenueDetailDto updated = jsonMapper.readValue(putVenue(created.getId(), venueDto("Venue B")).getResponse().getContentAsString(), VenueDetailDto.class);
        assertEquals("Venue B", updated.getName());
    }

    @Test
    void givenInvalidVenue_whenPost_then400() throws Exception {
        VenueCreateDto create = new VenueCreateDto();
        MockHttpServletResponse response = postVenue(create).getResponse();
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    void givenInvalidZipCode_whenPost_then400() throws Exception {
        VenueCreateDto create = venueDto("Venue A");
        create.setZipCode("101");

        MockHttpServletResponse response = postVenue(create).getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    void givenMissingVenue_whenUpdate_then404() throws Exception {
        MockHttpServletResponse response = putVenue(999L, venueDto("Venue B")).getResponse();
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    private org.springframework.test.web.servlet.MvcResult postVenue(Object body) throws Exception {
        return mockMvc.perform(post("/api/v1/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private org.springframework.test.web.servlet.MvcResult putVenue(Long id, Object body) throws Exception {
        return mockMvc.perform(put("/api/v1/venues/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private org.springframework.test.web.servlet.MvcResult getVenues() throws Exception {
        return mockMvc.perform(get("/api/v1/venues")
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private org.springframework.test.web.servlet.MvcResult getVenue(Long id) throws Exception {
        return mockMvc.perform(get("/api/v1/venues/" + id)
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private VenueCreateDto venueDto(String name) {
        VenueCreateDto dto = new VenueCreateDto();
        dto.setName(name);
        dto.setStreet("Street");
        dto.setCity("City");
        dto.setCountry("Germany");
        dto.setZipCode("1000");
        return dto;
    }
}
