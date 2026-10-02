package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ArtistCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ArtistDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.security.JwtTokenizer;
import java.util.Arrays;
import java.util.List;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ArtistEndpointTest implements TestData {

    private static final String ARTIST_BASE_URI = BASE_URI + "/artists";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    public void beforeEach() {
        performanceRepository.deleteAll();
        artistRepository.deleteAll();
    }

    @Test
    public void postArtistCreatesArtistWhenPayloadIsValid() throws Exception {
        ArtistCreateDto createDto = new ArtistCreateDto();
        createDto.setFirstName("Taylor");
        createDto.setLastName("Swift");
        createDto.setArtistName("TS");
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(ARTIST_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        ArtistDetailDto responseBody = jsonMapper.readValue(response.getContentAsString(), ArtistDetailDto.class);

        assertAll(
            () -> assertEquals(HttpStatus.CREATED.value(), response.getStatus()),
            () -> assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType()),
            () -> assertNotNull(responseBody.getId()),
            () -> assertEquals("Taylor", responseBody.getFirstName()),
            () -> assertEquals("Swift", responseBody.getLastName()),
            () -> assertEquals("TS", responseBody.getArtistName())
        );
    }

    @Test
    public void postArtistReturnsConflictWhenArtistNameAlreadyExists() throws Exception {
        Artist existing = new Artist();
        existing.setFirstName("John");
        existing.setLastName("Legend");
        existing.setArtistName("duplicate-name");
        artistRepository.save(existing);

        ArtistCreateDto createDto = new ArtistCreateDto();
        createDto.setFirstName("Other");
        createDto.setLastName("Singer");
        createDto.setArtistName("DUPLICATE-NAME");
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(ARTIST_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatus());
    }

    @Test
    public void postArtistReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        ArtistCreateDto createDto = new ArtistCreateDto();
        createDto.setFirstName(" ");
        createDto.setLastName(null);
        createDto.setArtistName(null);
        String body = jsonMapper.writeValueAsString(createDto);

        MvcResult mvcResult = this.mockMvc.perform(post(ARTIST_BASE_URI)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void searchArtistsReturnsSortedResultsForMatchingQuery() throws Exception {
        Artist zulu = new Artist();
        zulu.setFirstName("Anna");
        zulu.setLastName("Zulu");
        zulu.setArtistName("Zulu Stage");
        artistRepository.save(zulu);

        Artist alpha = new Artist();
        alpha.setFirstName("Aaron");
        alpha.setLastName("Alpha");
        alpha.setArtistName("alpha stage");
        artistRepository.save(alpha);

        MvcResult mvcResult = this.mockMvc.perform(get(ARTIST_BASE_URI)
            .param("query", "stage")
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        List<ArtistDetailDto> responseBody = Arrays.asList(jsonMapper.readValue(response.getContentAsString(), ArtistDetailDto[].class));

        assertAll(
            () -> assertEquals(HttpStatus.OK.value(), response.getStatus()),
            () -> assertEquals(2, responseBody.size()),
            () -> assertEquals("alpha stage", responseBody.get(0).getArtistName()),
            () -> assertEquals("Zulu Stage", responseBody.get(1).getArtistName())
        );
    }
}
