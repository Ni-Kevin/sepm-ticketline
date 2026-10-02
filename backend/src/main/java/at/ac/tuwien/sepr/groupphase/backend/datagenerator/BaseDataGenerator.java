package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallAreaType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallAreaRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("generateData")
@DependsOn("testDataGenerator")
public class BaseDataGenerator {
    private static final String FIXED_COUNTRY = "Austria";


    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TARGET_VENUE_COUNT = 25;
    private static final int SEAT_GRID_SIZE = 20;
    private static final int STANDING_CAPACITY = 150;
    private static final int TARGET_ARTIST_COUNT = 100;
    private static final int TARGET_USER_COUNT = 1000;

    private static final int GAP = 4;

    private final VenueRepository venueRepository;
    private final HallRepository hallRepository;
    private final SectorRepository sectorRepository;
    private final SeatRepository seatRepository;
    private final HallAreaRepository hallAreaRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BaseDataGenerator(VenueRepository venueRepository,
                             HallRepository hallRepository,
                             SectorRepository sectorRepository,
                             SeatRepository seatRepository,
                             HallAreaRepository hallAreaRepository,
                             ArtistRepository artistRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        this.venueRepository = venueRepository;
        this.hallRepository = hallRepository;
        this.sectorRepository = sectorRepository;
        this.seatRepository = seatRepository;
        this.hallAreaRepository = hallAreaRepository;
        this.artistRepository = artistRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void generateBaseData() {
        generateUsers();
        generateVenues();
        generateArtists();
    }

    private void generateUsers() {
        long existing = userRepository.count();
        if (existing >= TARGET_USER_COUNT) {
            LOGGER.debug("Users already generated ({} existing). Skipping.", existing);
            return;
        }

        int toCreate = TARGET_USER_COUNT - (int) existing;
        LOGGER.debug("Generating {} additional users", toCreate);

        String encodedPassword = passwordEncoder.encode("Passwort1!");
        List<ApplicationUser> batch = new ArrayList<>();
        for (int i = (int) existing; i < TARGET_USER_COUNT; i++) {
            batch.add(ApplicationUser.ApplicationUserBuilder.anApplicationUser()
                .withEmail("perfuser-" + i + "@test.com")
                .withPassword(encodedPassword)
                .withFirstName("PerfUser")
                .withLastName(String.valueOf(i))
                .withRole(UserRole.ROLE_USER)
                .withLocked(false)
                .build());

            if (batch.size() >= 50) {
                userRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            userRepository.saveAll(batch);
        }
        LOGGER.debug("Saved users up to target {}", TARGET_USER_COUNT);
    }

    private void generateVenues() {
        long existing = venueRepository.count();
        if (existing >= TARGET_VENUE_COUNT) {
            LOGGER.debug("Venues already generated ({} existing). Skipping.", existing);
            return;
        }

        int toCreate = TARGET_VENUE_COUNT - (int) existing;
        LOGGER.debug("Generating {} additional venues with dynamic halls", toCreate);

        for (int i = 0; i < toCreate; i++) {
            int index = (int) existing + i + 1;
            Venue venue = new Venue();
            venue.setName("Base Venue " + index);
            venue.setStreet("Street " + index);
            venue.setCity("City " + index);
            venue.setCountry(FIXED_COUNTRY);
            venue.setZipCode(String.format("%04d", 1000 + index));
            venue = venueRepository.save(venue);

            Hall hall = new Hall();
            hall.setName("Hall " + index);
            hall.setWidth(120);
            hall.setLength(100);
            hall.setVenue(venue);
            hall = hallRepository.save(hall);

            int stageWidth = hall.getWidth() - (2 * GAP);
            int stageLength = 12;
            createHallArea(GAP, 0, stageWidth, stageLength, HallAreaType.STAGE, hall, null);


            int usableWidth = hall.getWidth() - GAP;
            int sectorsPerRow = usableWidth / (SEAT_GRID_SIZE + GAP);

            if (sectorsPerRow <= 0) {
                sectorsPerRow = 1;
            }

            int totalRowWidth = (sectorsPerRow * SEAT_GRID_SIZE) + ((sectorsPerRow - 1) * GAP);
            int startX = (hall.getWidth() - totalRowWidth) / 2;

            int standingY = GAP + stageLength + GAP;
            for (int col = 0; col < sectorsPerRow; col++) {
                int currentX = startX + col * (SEAT_GRID_SIZE + GAP);
                createStandingSector(hall, "Standing " + (col + 1), STANDING_CAPACITY, currentX, standingY);
            }

            int seatingY = standingY + SEAT_GRID_SIZE + GAP;
            for (int col = 0; col < sectorsPerRow; col++) {
                int currentX = startX + col * (SEAT_GRID_SIZE + GAP);
                createSeatingSector(hall, "Seating " + (col + 1), currentX, seatingY);
            }
        }

        LOGGER.debug("Successfully generated {} venues", toCreate);
    }

    private void createSeatingSector(Hall hall, String name, int posX, int posY) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setType(SectorType.SEATING);
        sector.setColor("#2563EB");
        sector.setHall(hall);
        sector = sectorRepository.save(sector);

        createHallArea(posX, posY, SEAT_GRID_SIZE, SEAT_GRID_SIZE, HallAreaType.SEATING, hall, sector);

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= SEAT_GRID_SIZE; row++) {
            for (int col = 1; col <= SEAT_GRID_SIZE; col++) {
                Seat seat = new Seat();
                seat.setRowNumber(row);
                seat.setSeatNumber(col);
                seat.setPositionX(posX + col - 1);
                seat.setPositionY(posY + row - 1);
                seat.setSector(sector);
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
        sector.setSeats(seats);
        sectorRepository.save(sector);
    }

    private void createStandingSector(Hall hall, String name, int capacity, int posX, int posY) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setType(SectorType.STANDING);
        sector.setColor("#0F766E");
        sector.setCapacity(capacity);
        sector.setHall(hall);
        sector = sectorRepository.save(sector);

        createHallArea(posX, posY, SEAT_GRID_SIZE, SEAT_GRID_SIZE, HallAreaType.STANDING, hall, sector);
    }

    private void createHallArea(int x, int y, int width, int length, HallAreaType type, Hall hall, Sector sector) {
        HallArea area = new HallArea();
        area.setPositionX(x);
        area.setPositionY(y);
        area.setWidth(width);
        area.setLength(length);
        area.setType(type);
        area.setHall(hall);
        area.setSector(sector);
        hallAreaRepository.save(area);
    }

    private void generateArtists() {
        long existing = artistRepository.count();
        if (existing >= TARGET_ARTIST_COUNT) {
            LOGGER.debug("Artists already generated ({} existing). Skipping.", existing);
            return;
        }

        int toCreate = TARGET_ARTIST_COUNT - (int) existing;
        LOGGER.debug("Generating {} additional artists", toCreate);

        List<Artist> batch = new ArrayList<>();
        for (int i = (int) existing; i < TARGET_ARTIST_COUNT; i++) {
            Artist artist = new Artist();
            artist.setArtistName(String.format("Artist %d", i));
            artist.setFirstName(String.format("FirstName %d", i));
            artist.setLastName(String.format("LastName %d", i));
            batch.add(artist);

            if (batch.size() >= 50) {
                artistRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            artistRepository.saveAll(batch);
        }
        LOGGER.debug("Saved artists up to target {}", TARGET_ARTIST_COUNT);
    }
}
