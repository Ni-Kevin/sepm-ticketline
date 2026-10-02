package at.ac.tuwien.sepr.groupphase.backend.integrationtest;

import at.ac.tuwien.sepr.groupphase.backend.basetest.TestData;
import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallCreateDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallAreaLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallDetailDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.HallLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SeatLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.SectorLayoutDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallAreaType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
public class HallEndpointTest implements TestData {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HallRepository hallRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JwtTokenizer jwtTokenizer;

    @Autowired
    private SecurityProperties securityProperties;

    private Venue venueOne;
    private Venue venueTwo;

    @BeforeEach
    public void beforeEach() {
        performanceRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();

        venueOne = venueRepository.save(venue("Venue One"));
        venueTwo = venueRepository.save(venue("Venue Two"));
    }

    @Test
    public void givenValidHall_whenCreate_thenCreated() throws Exception {
        HallCreateDto dto = createHallDto("Main Hall", venueOne.getId(), 20, 30);

        MockHttpServletResponse response = performPost(dto).getResponse();

        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
        HallDetailDto hall = jsonMapper.readValue(response.getContentAsString(), HallDetailDto.class);
        assertAll(
            () -> assertNotNull(hall.getId()),
            () -> assertEquals("Main Hall", hall.getName()),
            () -> assertEquals(20, hall.getWidth()),
            () -> assertEquals(30, hall.getLength()),
            () -> assertEquals(venueOne.getId(), hall.getVenueId()),
            () -> assertEquals("Venue One", hall.getVenueName())
        );
    }

    @Test
    public void givenInvalidHallCreate_whenPost_then400AndErrors() throws Exception {
        HallCreateDto dto = new HallCreateDto();
        dto.setVenueId(venueOne.getId());

        MockHttpServletResponse response = performPost(dto).getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("errors"));
        assertEquals(0, hallRepository.count());
    }

    @Test
    public void givenNonExistingVenue_whenCreateHall_then404() throws Exception {
        MockHttpServletResponse response = performPost(createHallDto("Main Hall", 999L, 20, 30)).getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
        assertEquals(0, hallRepository.count());
    }

    @Test
    public void givenWrongRole_whenCreateHall_then403() throws Exception {
        HallCreateDto dto = createHallDto("Main Hall", venueOne.getId(), 20, 30);

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/halls")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(dto))
                .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(DEFAULT_USER, USER_ROLES)))
            .andDo(print())
            .andReturn().getResponse();

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
    }

    @Test
    public void givenCreatedHall_whenFindAll_thenContainsVenueInfo() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));

        MockHttpServletResponse response = performGet().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        List<HallDetailDto> halls = Arrays.asList(jsonMapper.readValue(response.getContentAsString(), HallDetailDto[].class));
        assertEquals(1, halls.size());
        assertAll(
            () -> assertEquals(hall.getId(), halls.get(0).getId()),
            () -> assertEquals(venueOne.getId(), halls.get(0).getVenueId()),
            () -> assertEquals(venueOne.getName(), halls.get(0).getVenueName()),
            () -> assertFalse(halls.get(0).isLayoutLocked())
        );
    }

    @Test
    public void givenCreatedHall_whenFindOne_thenReturnsDetail() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));

        MockHttpServletResponse response = performGet("/" + hall.getId()).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallDetailDto dto = jsonMapper.readValue(response.getContentAsString(), HallDetailDto.class);
        assertAll(
            () -> assertEquals(hall.getId(), dto.getId()),
            () -> assertEquals("Main Hall", dto.getName()),
            () -> assertEquals(venueOne.getId(), dto.getVenueId()),
            () -> assertFalse(dto.isLayoutLocked())
        );
    }

    @Test
    public void givenHallUsedInPerformance_whenFindOne_thenReturnsLockedFlag() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        performanceRepository.save(performance(hall, "Locked Hall Detail"));

        MockHttpServletResponse response = performGet("/" + hall.getId()).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallDetailDto dto = jsonMapper.readValue(response.getContentAsString(), HallDetailDto.class);
        assertTrue(dto.isLayoutLocked());
        assertTrue(dto.isDimensionsLocked());
    }

    @Test
    public void givenHallWithAreas_whenFindOne_thenReturnsDimensionsLockedFlag() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        performPut("/" + hall.getId() + "/layout", layout);

        MockHttpServletResponse response = performGet("/" + hall.getId()).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallDetailDto dto = jsonMapper.readValue(response.getContentAsString(), HallDetailDto.class);
        assertFalse(dto.isLayoutLocked());
        assertTrue(dto.isDimensionsLocked());
    }

    @Test
    public void givenCreatedHall_whenUpdate_thenChangesHallAndVenue() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));

        MockHttpServletResponse response = performPut("/" + hall.getId(), createHallDto("Updated Hall", venueTwo.getId(), 25, 35)).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallDetailDto dto = jsonMapper.readValue(response.getContentAsString(), HallDetailDto.class);
        assertAll(
            () -> assertEquals("Updated Hall", dto.getName()),
            () -> assertEquals(25, dto.getWidth()),
            () -> assertEquals(35, dto.getLength()),
            () -> assertEquals(venueTwo.getId(), dto.getVenueId()),
            () -> assertEquals(venueTwo.getName(), dto.getVenueName()),
            () -> assertFalse(dto.isLayoutLocked())
        );
    }

    @Test
    public void givenHallUsedInPerformance_whenUpdateDimensions_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        performanceRepository.save(performance(hall, "Locked Hall Update"));

        MockHttpServletResponse response = performPut("/" + hall.getId(), createHallDto("Updated Hall", venueTwo.getId(), 25, 35)).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("width and length can no longer be changed"));
    }

    @Test
    public void givenHallWithAreas_whenUpdateDimensions_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        performPut("/" + hall.getId() + "/layout", layout);

        MockHttpServletResponse response = performPut("/" + hall.getId(), createHallDto("Updated Hall", venueTwo.getId(), 25, 35)).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("already has configured areas"));
    }

    @Test
    public void givenHallUsedInPerformance_whenUpdateWithoutDimensionChange_then200() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        performanceRepository.save(performance(hall, "Locked Hall Update"));

        MockHttpServletResponse response = performPut("/" + hall.getId(), createHallDto("Updated Hall", venueTwo.getId(), 20, 30)).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallDetailDto dto = jsonMapper.readValue(response.getContentAsString(), HallDetailDto.class);
        assertAll(
            () -> assertEquals("Updated Hall", dto.getName()),
            () -> assertEquals(20, dto.getWidth()),
            () -> assertEquals(30, dto.getLength()),
            () -> assertTrue(dto.isLayoutLocked())
        );
    }

    @Test
    public void givenNonExistingHall_whenUpdate_then404() throws Exception {
        MockHttpServletResponse response = performPut("/9999", createHallDto("Updated Hall", venueTwo.getId(), 25, 35)).getResponse();

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatus());
    }

    @Test
    public void givenInvalidHallUpdate_whenPut_then400() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));

        MockHttpServletResponse response = performPut("/" + hall.getId(), createHallDto("Updated Hall", venueTwo.getId(), 0, -1)).getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
    }

    @Test
    public void givenDuplicateHallName_whenPostOrPut_then422() throws Exception {
        Hall existing = hallRepository.save(hall("Hall One", 20, 30, venueOne));
        hallRepository.save(hall("Dup Hall", 20, 30, venueOne));

        MockHttpServletResponse postResponse = performPost(createHallDto("dup hall", venueOne.getId(), 22, 32)).getResponse();
        MockHttpServletResponse putResponse = performPut("/" + existing.getId(), createHallDto("dup hall", venueOne.getId(), 22, 32)).getResponse();

        assertAll(
            () -> assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), postResponse.getStatus()),
            () -> assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), putResponse.getStatus())
        );
    }

    @Test
    public void givenValidLayout_whenUpdate_thenSavesStageAndSeatingAreas() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        SectorLayoutDto seating = sector("Seating A", SectorType.SEATING);
        seating.getSeats().add(seat(1, 1, 9, 11));
        seating.getSeats().add(seat(1, 2, 10, 11));
        layout.getSectors().add(seating);
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.SEATING, "Seating A"));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallLayoutDto dto = jsonMapper.readValue(response.getContentAsString(), HallLayoutDto.class);
        assertEquals(2, dto.getAreas().size());
        assertTrue(dto.getAreas().stream().anyMatch(a -> a.getType() == HallAreaType.STAGE));
        assertTrue(dto.getAreas().stream().anyMatch(a -> a.getType() == HallAreaType.SEATING && "Seating A".equals(a.getSectorName())));
        assertTrue(dto.getSectors().stream().anyMatch(s -> "Seating A".equals(s.getName()) && s.getSeats().size() == 2));
        assertFalse(dto.isLayoutLocked());
    }

    @Test
    public void givenHallUsedInPerformance_whenGetLayout_thenReturnsLockedFlag() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        performanceRepository.save(performance(hall, "Locked Layout Test"));

        MockHttpServletResponse response = performGet("/" + hall.getId() + "/layout").getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallLayoutDto dto = jsonMapper.readValue(response.getContentAsString(), HallLayoutDto.class);
        assertTrue(dto.isLayoutLocked());
    }

    @Test
    public void givenHallUsedInPerformance_whenUpdateLayout_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        performanceRepository.save(performance(hall, "Locked Layout Test"));

        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("already used in a performance"));
    }

    @Test
    public void givenExistingSeatingLayout_whenSeatRemoved_thenLayoutUpdateSucceeds() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());

        SectorLayoutDto seating = sector("Seating A", SectorType.SEATING);
        seating.getSeats().add(seat(1, 1, 9, 11));
        seating.getSeats().add(seat(1, 2, 10, 11));
        layout.getSectors().add(seating);
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.SEATING, "Seating A"));

        HallLayoutDto createdLayout = jsonMapper.readValue(
            performPut("/" + hall.getId() + "/layout", layout).getResponse().getContentAsString(),
            HallLayoutDto.class
        );

        SectorLayoutDto persistedSeating = createdLayout.getSectors().stream()
            .filter(sector -> "Seating A".equals(sector.getName()))
            .findFirst()
            .orElseThrow();
        persistedSeating.getSeats().remove(0);
        persistedSeating.getSeats().get(0).setRowNumber(1);
        persistedSeating.getSeats().get(0).setSeatNumber(1);

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", createdLayout).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallLayoutDto updatedLayout = jsonMapper.readValue(response.getContentAsString(), HallLayoutDto.class);
        SectorLayoutDto updatedSeating = updatedLayout.getSectors().stream()
            .filter(sector -> "Seating A".equals(sector.getName()))
            .findFirst()
            .orElseThrow();

        assertAll(
            () -> assertEquals(1, updatedSeating.getSeats().size()),
            () -> assertEquals(1, updatedSeating.getSeats().get(0).getRowNumber()),
            () -> assertEquals(1, updatedSeating.getSeats().get(0).getSeatNumber())
        );
    }

    @Test
    public void givenExistingSeatingLayout_whenSeatingAreaAndSectorRemoved_thenLayoutUpdateSucceeds() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());

        SectorLayoutDto seating = sector("Seating A", SectorType.SEATING);
        seating.getSeats().add(seat(1, 1, 9, 11));
        layout.getSectors().add(seating);
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.SEATING, "Seating A"));

        HallLayoutDto createdLayout = jsonMapper.readValue(
            performPut("/" + hall.getId() + "/layout", layout).getResponse().getContentAsString(),
            HallLayoutDto.class
        );

        createdLayout.setSectors(List.of());
        createdLayout.setAreas(createdLayout.getAreas().stream()
            .filter(area -> area.getType() == HallAreaType.STAGE)
            .toList());

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", createdLayout).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallLayoutDto updatedLayout = jsonMapper.readValue(response.getContentAsString(), HallLayoutDto.class);
        assertAll(
            () -> assertEquals(0, updatedLayout.getSectors().size()),
            () -> assertEquals(1, updatedLayout.getAreas().size()),
            () -> assertEquals(HallAreaType.STAGE, updatedLayout.getAreas().get(0).getType())
        );
    }

    @Test
    public void givenValidStandingLayout_whenUpdate_thenPersistsStandingSectorAndArea() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());

        SectorLayoutDto standing = sector("Standing A", SectorType.STANDING);
        standing.setCapacity(120);
        layout.getSectors().add(standing);
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.STANDING, "Standing A"));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        HallLayoutDto dto = jsonMapper.readValue(response.getContentAsString(), HallLayoutDto.class);
        assertAll(
            () -> assertTrue(dto.getSectors().stream().anyMatch(s -> "Standing A".equals(s.getName()) && s.getCapacity() == 120)),
            () -> assertTrue(dto.getAreas().stream().anyMatch(a -> a.getType() == HallAreaType.STANDING && "Standing A".equals(a.getSectorName())))
        );
    }

    @Test
    public void givenInvalidLayout_whenUpdate_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(18, 0, 5, 5, HallAreaType.STAGE));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("exceeds hall width"));
    }

    @Test
    public void givenStageAreaWithSector_whenUpdateLayout_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getSectors().add(sector("Seating A", SectorType.SEATING));
        HallAreaLayoutDto stage = area(0, 0, 6, 3, HallAreaType.STAGE);
        stage.setSectorName("Seating A");
        layout.getAreas().add(stage);

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Stage areas must not be linked to a sector"));
    }

    @Test
    public void givenLayoutWithoutStage_whenUpdate_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getSectors().add(sector("Seating A", SectorType.SEATING));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.SEATING, "Seating A"));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("at least one stage area"));
    }

    @Test
    public void givenOverlappingAreas_whenUpdateLayout_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getSectors().add(sector("Seating A", SectorType.SEATING));
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(5, 2, 5, 5, HallAreaType.SEATING, "Seating A"));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("must not overlap"));
    }

    @Test
    public void givenNegativeAreaPosition_whenUpdateLayout_then400() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(-1, 0, 6, 3, HallAreaType.STAGE));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Area x position must not be negative"));
    }

    @Test
    public void givenSeatingAreaWithoutSector_whenUpdateLayout_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.SEATING, null));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Seating areas must be linked to a seated sector"));
    }

    @Test
    public void givenStandingAreaWithoutSector_whenUpdateLayout_then422() throws Exception {
        Hall hall = hallRepository.save(hall("Main Hall", 20, 30, venueOne));
        HallLayoutDto layout = new HallLayoutDto();
        layout.setHallId(hall.getId());
        layout.setName(hall.getName());
        layout.setWidth(hall.getWidth());
        layout.setLength(hall.getLength());
        layout.setVenueId(venueOne.getId());
        layout.getAreas().add(area(0, 0, 6, 3, HallAreaType.STAGE));
        layout.getAreas().add(area(8, 10, 5, 5, HallAreaType.STANDING, null));

        MockHttpServletResponse response = performPut("/" + hall.getId() + "/layout", layout).getResponse();

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), response.getStatus());
        assertTrue(response.getContentAsString().contains("Standing areas must be linked to a standing sector"));
    }

    private MvcResult performGet() throws Exception {
        return mockMvc.perform(get("/api/v1/halls")
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private MvcResult performGet(String path) throws Exception {
        return mockMvc.perform(get("/api/v1/halls" + path)
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private MvcResult performPost(HallCreateDto dto) throws Exception {
        return mockMvc.perform(post("/api/v1/halls")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(dto))
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private MvcResult performPut(String path, Object body) throws Exception {
        return mockMvc.perform(put("/api/v1/halls" + path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(body))
            .header(securityProperties.getAuthHeader(), jwtTokenizer.getAuthToken(ADMIN_USER, ADMIN_ROLES)))
            .andDo(print())
            .andReturn();
    }

    private HallCreateDto createHallDto(String name, Long venueId, int width, int length) {
        HallCreateDto dto = new HallCreateDto();
        dto.setName(name);
        dto.setVenueId(venueId);
        dto.setWidth(width);
        dto.setLength(length);
        return dto;
    }

    private Hall hall(String name, int width, int length, Venue venue) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setWidth(width);
        hall.setLength(length);
        hall.setVenue(venue);
        return hall;
    }

    private Performance performance(Hall hall, String name) {
        Performance performance = new Performance();
        performance.setHall(hall);
        performance.setPerformanceName(name);
        performance.setStartPrice(BigDecimal.TEN);
        performance.setStartTime(LocalDateTime.now().plusDays(1));
        performance.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        return performance;
    }

    private HallAreaLayoutDto area(int x, int y, int width, int length, HallAreaType type) {
        HallAreaLayoutDto dto = new HallAreaLayoutDto();
        dto.setPositionX(x);
        dto.setPositionY(y);
        dto.setWidth(width);
        dto.setLength(length);
        dto.setType(type);
        return dto;
    }

    private HallAreaLayoutDto area(int x, int y, int width, int length, HallAreaType type, String sectorName) {
        HallAreaLayoutDto dto = area(x, y, width, length, type);
        dto.setSectorName(sectorName);
        return dto;
    }

    private SectorLayoutDto sector(String name, SectorType type) {
        SectorLayoutDto dto = new SectorLayoutDto();
        dto.setName(name);
        dto.setType(type);
        dto.setColor("#123456");
        return dto;
    }

    private SeatLayoutDto seat(int rowNumber, int seatNumber, int x, int y) {
        SeatLayoutDto dto = new SeatLayoutDto();
        dto.setRowNumber(rowNumber);
        dto.setSeatNumber(seatNumber);
        dto.setPositionX(x);
        dto.setPositionY(y);
        return dto;
    }

    private Venue venue(String name) {
        Venue venue = new Venue();
        venue.setName(name);
        venue.setStreet("Street 1");
        venue.setCity("Vienna");
        venue.setCountry("AT");
        venue.setZipCode("1000");
        return venue;
    }
}
