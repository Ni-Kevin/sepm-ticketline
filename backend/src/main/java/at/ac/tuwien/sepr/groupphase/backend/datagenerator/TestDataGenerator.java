package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Event;
import at.ac.tuwien.sepr.groupphase.backend.entity.Hall;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallArea;
import at.ac.tuwien.sepr.groupphase.backend.entity.HallAreaType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.OrderStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.PaymentMethod;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.PerformanceSectorPrice;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.Seat;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.SectorType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.TicketStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.Venue;
import at.ac.tuwien.sepr.groupphase.backend.enums.UserRole;
import at.ac.tuwien.sepr.groupphase.backend.repository.ArtistRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.EventRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallAreaRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.HallRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository; // IMPORT HINZUFÜGT
import at.ac.tuwien.sepr.groupphase.backend.repository.OrderRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.VenueRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("generateData")
@DependsOn("userDataGenerator")
public class TestDataGenerator {

    private static final String FIXED_COUNTRY = "Austria";

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final VenueRepository venueRepository;
    private final HallRepository hallRepository;
    private final SectorRepository sectorRepository;
    private final SeatRepository seatRepository;
    private final ArtistRepository artistRepository;
    private final EventRepository eventRepository;
    private final PerformanceRepository performanceRepository;
    private final HallAreaRepository hallAreaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final InvoiceRepository invoiceRepository; // FIELD HINZUFÜGT

    public TestDataGenerator(VenueRepository venueRepository,
                             HallRepository hallRepository,
                             SectorRepository sectorRepository,
                             SeatRepository seatRepository,
                             ArtistRepository artistRepository,
                             EventRepository eventRepository,
                             PerformanceRepository performanceRepository,
                             HallAreaRepository hallAreaRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             TicketRepository ticketRepository,
                             OrderRepository orderRepository,
                             ReservationRepository reservationRepository,
                             InvoiceRepository invoiceRepository) { // INJECTION HINZUFÜGT

        this.venueRepository = venueRepository;
        this.hallRepository = hallRepository;
        this.sectorRepository = sectorRepository;
        this.seatRepository = seatRepository;
        this.artistRepository = artistRepository;
        this.eventRepository = eventRepository;
        this.performanceRepository = performanceRepository;
        this.hallAreaRepository = hallAreaRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.reservationRepository = reservationRepository;
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @PostConstruct
    @Transactional
    public void generateTestData() {
        if (venueRepository.count() > 0) {
            LOGGER.debug("Test data already exists. Skipping generation.");
            return;
        }

        generateUsers();

        Venue opera = createVenue("Wiener Staatsoper", "Opernring 2", "Wien", "1010");
        Venue stadthalle = createVenue("Wiener Stadthalle", "Roland-Rainer-Platz 1", "Wien", "1150");
        Venue gasometer = createVenue("Gasometer Planet.tt", "Guglgasse 12", "Wien", "1110");
        venueRepository.saveAll(List.of(opera, stadthalle, gasometer));

        Hall operaMain = createHall("Opera", 100, 90, opera);
        hallRepository.save(operaMain);
        createHallArea((operaMain.getWidth() - 60) / 2, 0, 60, 10, HallAreaType.STAGE, operaMain, null);

        Hall halleD = createHall("Halle D", 150, 150, stadthalle);
        Hall halleE = createHall("Halle E", 80, 100, stadthalle);
        hallRepository.saveAll(List.of(halleD, halleE));
        createHallArea((halleD.getWidth() - 60) / 2, 0, 60, 20, HallAreaType.STAGE, halleD, null);
        createHallArea((halleE.getWidth() - 60) / 2, 0, 60, 20, HallAreaType.STAGE, halleE, null);

        Hall gasometerB = createHall("Halle B", 100, 120, gasometer);
        hallRepository.save(gasometerB);
        createHallArea((gasometerB.getWidth() - 60) / 2, 0, 60, 25, HallAreaType.STAGE, gasometerB, null);


        List<Sector> operaSectors = new ArrayList<>();
        operaSectors.add(createSectorWithSeatGrid(operaMain, "Orchesterparterre 1", SectorType.SEATING, "#2563EB",  30, 10, 15, 15));
        operaSectors.add(createSectorWithSeatGrid(operaMain, "Mittelloge - Rang 1", SectorType.SEATING, "#2563EB",  30, 10, 15, 30));
        operaSectors.add(createSectorWithSeatGrid(operaMain, "Balkon 1", SectorType.SEATING, "#2563EB", 30,  20, 15, 45));
        operaSectors.add(createSectorWithSeatGrid(operaMain, "Orchesterparterre 2", SectorType.SEATING, "#2563EB",  30, 10, 55, 15));
        operaSectors.add(createSectorWithSeatGrid(operaMain, "Mittelloge - Rang 2", SectorType.SEATING, "#2563EB",  30, 10, 55, 30));
        operaSectors.add(createSectorWithSeatGrid(operaMain, "Balkon 2", SectorType.SEATING, "#2563EB", 30,  20, 55, 45));

        List<Sector> stadthalledSectors = new ArrayList<>();
        stadthalledSectors.add(createStandingSector(halleD, "Front of Stage 1", SectorType.STANDING, "#0F766E", 500, 50, 50, 15, 30));
        stadthalledSectors.add(createStandingSector(halleD, "Front of Stage 2", SectorType.STANDING, "#0F766E", 500, 50, 50, 85, 30));
        stadthalledSectors.add(createSectorWithSeatGrid(halleD, "Seating Sector 1", SectorType.SEATING, "#2563EB", 50, 50, 15, 100));
        stadthalledSectors.add(createSectorWithSeatGrid(halleD, "Seating Sector 2", SectorType.SEATING, "#2563EB", 50, 50, 85, 100));

        List<Sector> stadthalleeSectors = new ArrayList<>();
        stadthalleeSectors.add(createStandingSector(halleE, "Front of Stage 1", SectorType.STANDING, "#0F766E", 300, 30, 30, 5, 30));
        stadthalleeSectors.add(createStandingSector(halleE, "Front of Stage 2", SectorType.STANDING, "#0F766E", 300, 30, 30, 45, 30));
        stadthalleeSectors.add(createSectorWithSeatGrid(halleE, "Seating Sector 1", SectorType.SEATING, "#2563EB",  30, 30, 5, 70));
        stadthalleeSectors.add(createSectorWithSeatGrid(halleE, "Seating Sector 2", SectorType.SEATING, "#2563EB", 30, 30, 45, 70));

        List<Sector> gasometerSectors = new ArrayList<>();
        gasometerSectors.add(createSectorWithSeatGrid(gasometerB, "Seating 1", SectorType.SEATING, "#2563EB", 40, 20, 5, 30));
        gasometerSectors.add(createSectorWithSeatGrid(gasometerB, "Seating 2", SectorType.SEATING, "#2563EB", 40, 20, 55, 30));

        gasometerSectors.add(createSectorWithSeatGrid(gasometerB, "Seating 3", SectorType.SEATING, "#2563EB", 40, 20, 5, 55));
        gasometerSectors.add(createSectorWithSeatGrid(gasometerB, "Seating 4", SectorType.SEATING, "#2563EB", 40, 20, 55, 55));

        gasometerSectors.add(createSectorWithSeatGrid(gasometerB, "Seating 5", SectorType.SEATING, "#2563EB", 40, 20, 5, 90));
        gasometerSectors.add(createSectorWithSeatGrid(gasometerB, "Seating 6", SectorType.SEATING, "#2563EB", 40, 20, 55, 90));


        Artist billieEilish = createArtist("Billie", "Eilish", "Billie Eilish");
        Artist theWeeknd = createArtist("Abel", "Tesfaye", "The Weeknd");
        Artist fredAgain = createArtist("Fred", "Gibson", "Fred again..");
        Artist rammstein = createArtist("Till", "Lindemann", "Rammstein");
        Artist seilerSpeer = createArtist("Christopher", "Seiler", "Seiler und Speer");
        Artist viennaPhil = createArtist("Wiener", "Philharmoniker", "Wiener Philharmoniker");
        artistRepository.saveAll(List.of(billieEilish, theWeeknd, fredAgain, rammstein, seilerSpeer, viennaPhil));

        LocalDateTime baseTime = LocalDateTime.now().withHour(19).withMinute(30).withSecond(0).withNano(0);


        Event e1 = createEvent("Classic Gala Night", "Opera", "Klassische Meisterwerke modern inszeniert.", "event1.jpg");
        eventRepository.save(e1);
        final Performance perfOpera = createPerformanceSeries(e1, "Wiener Philharmoniker - Opening Concert", baseTime.plusDays(2), operaMain, operaSectors, List.of(viennaPhil), new BigDecimal("140.00"));
        createPerformanceSeries(e1, "Wiener Philharmoniker - Matinee", baseTime.plusDays(3).withHour(14), operaMain, operaSectors, List.of(viennaPhil), new BigDecimal("95.00"));

        Event e2 = createEvent("The After Hours Til Dawn Tour", "Concert", "Die spektakuläre Stadion- und Arena-Show live in Wien.", "event2.jpeg");
        eventRepository.save(e2);
        final Performance perfPop = createPerformanceSeries(e2, "The Weeknd - Live (Day 1)", baseTime.plusDays(5), halleD, stadthalledSectors, List.of(theWeeknd), new BigDecimal("95.00"));
        createPerformanceSeries(e2, "The Weeknd - Live (Day 2)", baseTime.plusDays(6), halleD, stadthalledSectors, List.of(theWeeknd), new BigDecimal("95.00"));

        Event e3 = createEvent("Europe Stadium Tour", "Concert", "Die spektakulärste Pyro-Show der Welt.", "event3.jpeg");
        eventRepository.save(e3);
        final Performance perfRock = createPerformanceSeries(e3, "Rammstein Arena Experience", baseTime.plusDays(12), halleD, stadthalledSectors, List.of(rammstein), new BigDecimal("110.00"));

        Event e4 = createEvent("London to Vienna electronic", "Concert", "Intime Club-Vibes und packende Beats.", "event4.jpeg");
        eventRepository.save(e4);
        createPerformanceSeries(e4, "Fred again.. Solo Set", baseTime.plusDays(4).withHour(22).withMinute(0), gasometerB, gasometerSectors, List.of(fredAgain), new BigDecimal("49.00"));

        Event e5 = createEvent("Austropop Open Air & Indoor", "Concert", "Ehrlicher Rock und Humor aus Österreich.", "event5.jpeg");
        eventRepository.save(e5);

        createPerformanceSeries(e5, "Seiler und Speer - Akustisch", baseTime.plusDays(1), halleE, stadthalleeSectors, List.of(seilerSpeer), new BigDecimal("45.00"));

        seatRepository.flush();
        generateTicketData(perfOpera, perfPop, perfRock, operaSectors.get(0), stadthalledSectors.get(0), stadthalledSectors.get(2));

        LOGGER.info("Successfully generated extensive test database state with realistic venues and modern artists!");
    }

    private void generateUsers() {
        String encodedPassword = passwordEncoder.encode("Passwort1!");
        List<ApplicationUser> usersToSave = new ArrayList<>();

        usersToSave.add(createTestUser("maxmustermann@email.com", encodedPassword, "Max", "Mustermann", UserRole.ROLE_ADMIN, false));
        usersToSave.add(createTestUser("manager@email.com", encodedPassword, "Sarah", "Conner", UserRole.ROLE_ADMIN, false));

        usersToSave.add(createTestUser("user@email.com", encodedPassword, "User", "User", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("john.doe@gmail.com", encodedPassword, "John", "Doe", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("anna.schmidt@gmx.at", encodedPassword, "Anna", "Schmidt", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("david.alaba@football.com", encodedPassword, "David", "Alaba", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("lisa.mueller@yahoo.com", encodedPassword, "Lisa", "Müller", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("michael.jordan@bulls.com", encodedPassword, "Michael", "Jordan", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("emma.watson@magic.com", encodedPassword, "Emma", "Watson", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("thomas.brezina@knickerbocker.at", encodedPassword, "Thomas", "Brezina", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("julia.roberts@hollywood.com", encodedPassword, "Julia", "Roberts", UserRole.ROLE_USER, false));
        usersToSave.add(createTestUser("felix.austria@tuwien.ac.at", encodedPassword, "Felix", "Austria", UserRole.ROLE_USER, false));


        usersToSave.add(createTestUser("locked.user@email.com", encodedPassword, "Gesperrter", "User", UserRole.ROLE_USER, true));
        usersToSave.add(createTestUser("longname@email.com", encodedPassword, "Hubertus-Maximilian", "Graf-von-Grafenegg-Schoenbrunn", UserRole.ROLE_USER, true));

        userRepository.saveAll(usersToSave);
    }

    private void generateTicketData(Performance perfOpera, Performance perfPop, Performance perfRock, Sector operaSeatSec, Sector standingSec, Sector stadthalleSeatSec) {
        ApplicationUser admin = userRepository.findByEmail("admin@email.com").orElse(null);
        ApplicationUser john = userRepository.findByEmail("john.doe@gmail.com").orElse(null);
        ApplicationUser anna = userRepository.findByEmail("anna.schmidt@gmx.at").orElse(null);
        ApplicationUser david = userRepository.findByEmail("david.alaba@football.com").orElse(null);

        if (admin == null || john == null || anna == null || david == null) {
            return;
        }

        List<Seat> operaSeats = operaSeatSec.getSeats();

        Order adminOrder = createOrder(admin, new BigDecimal("504.00"), OrderStatus.PURCHASED, PaymentMethod.CREDIT_CARD);
        createTicket(perfOpera, operaSeatSec, operaSeats.get(0), adminOrder, null, new BigDecimal("168.00"), TicketStatus.PURCHASED, "QR_admin_1");
        createTicket(perfOpera, operaSeatSec, operaSeats.get(1), adminOrder, null, new BigDecimal("168.00"), TicketStatus.PURCHASED, "QR_admin_2");
        createTicket(perfOpera, operaSeatSec, operaSeats.get(2), adminOrder, null, new BigDecimal("168.00"), TicketStatus.PURCHASED, "QR_admin_3");

        Reservation johnRes = createReservation(john, perfPop.getStartTime().minusMinutes(30));
        createTicket(perfPop, standingSec, null, null, johnRes, new BigDecimal("95.00"), TicketStatus.RESERVED, null);
        createTicket(perfPop, standingSec, null, null, johnRes, new BigDecimal("95.00"), TicketStatus.RESERVED, null);


        Order annaOrder = createOrder(anna, new BigDecimal("190.00"), OrderStatus.PURCHASED, PaymentMethod.PAYPAL);
        createTicket(perfPop, standingSec, null, annaOrder, null, new BigDecimal("95.00"), TicketStatus.PURCHASED, "QR_ANNA_P1");
        createTicket(perfPop, standingSec, null, annaOrder, null, new BigDecimal("95.00"), TicketStatus.PURCHASED, "QR_ANNA_P2");

        Reservation annaRes = createReservation(anna, perfOpera.getStartTime().minusMinutes(30));
        createTicket(perfOpera, operaSeatSec, operaSeats.get(3), null, annaRes, new BigDecimal("168.00"), TicketStatus.RESERVED, null);

        List<Seat> stadthalleSeats = stadthalleSeatSec.getSeats();

        Order davidOrder = createOrder(david, new BigDecimal("264.00"), OrderStatus.PURCHASED, PaymentMethod.CREDIT_CARD);
        createTicket(perfRock, stadthalleSeatSec, stadthalleSeats.get(0), davidOrder, null, new BigDecimal("132.00"), TicketStatus.PURCHASED, "QR_DAVID_1");
        createTicket(perfRock, stadthalleSeatSec, stadthalleSeats.get(1), davidOrder, null, new BigDecimal("132.00"), TicketStatus.PURCHASED, "QR_DAVID_2");
    }


    private Order createOrder(ApplicationUser user, BigDecimal totalPrice, OrderStatus status, PaymentMethod paymentMethod) {
        Order order = new Order();
        order.setUser(user);
        order.setPurchaseDate(LocalDateTime.now());
        order.setTotalPrice(totalPrice);
        order.setStatus(status);
        order.setPaymentMethod(paymentMethod);
        Order savedOrder = orderRepository.save(order);

        createInvoice(savedOrder);

        return savedOrder;
    }

    private void createInvoice(Order order) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + order.getId() + "-" + (System.currentTimeMillis() % 10000));
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(order.getTotalPrice());
        invoice.setOrder(order);
        invoiceRepository.save(invoice);
    }

    private Reservation createReservation(ApplicationUser user, LocalDateTime reservedUntil) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setReservedUntil(reservedUntil);
        return reservationRepository.save(reservation);
    }

    private void createTicket(Performance perf, Sector sec, Seat seat, Order order, Reservation reservation, BigDecimal price, TicketStatus status, String qrCode) {
        Ticket ticket = new Ticket();
        ticket.setPerformance(perf);
        ticket.setSector(sec);
        ticket.setSeat(seat);
        ticket.setOrder(order);
        ticket.setReservation(reservation);
        ticket.setFinalPrice(price);
        ticket.setStatus(status);
        ticket.setQrCode(qrCode);
        ticketRepository.save(ticket);
    }

    private ApplicationUser createTestUser(String email, String password, String firstName, String lastName, UserRole role, boolean isLocked) {
        ApplicationUser user = new ApplicationUser();
        user.setEmail(email);
        user.setPassword(password);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        user.setRole(role);
        user.setLocked(isLocked);

        return user;
    }

    private byte[] loadImageAsBytes(String imageName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("testdata-event-images/" + imageName)) {
            if (is == null) {
                LOGGER.warn("Image {} not found!", imageName);
                return null;
            }
            return is.readAllBytes();
        } catch (IOException e) {
            LOGGER.error("Error loading image {}", imageName, e);
            return null;
        }
    }

    private Performance createPerformanceSeries(Event event, String name, LocalDateTime start, Hall hall, List<Sector> sectors, List<Artist> artists, BigDecimal basePrice) {
        Performance performance = new Performance();
        performance.setPerformanceName(name);
        performance.setStartTime(start);
        performance.setEndTime(start.plusHours(2).plusMinutes(30));
        performance.setStartPrice(basePrice);
        performance.setHall(hall);
        performance.setEvent(event);
        performance.setGenre(event.getGenre());
        performance.getArtists().addAll(artists);

        for (Sector sector : sectors) {
            PerformanceSectorPrice psp = new PerformanceSectorPrice();
            psp.setSectorId(sector.getId());

            BigDecimal finalSectorPrice = sector.getType() == SectorType.SEATING
                ? basePrice.multiply(new BigDecimal("1.20")).setScale(2, RoundingMode.HALF_UP)
                : basePrice;

            psp.setPrice(finalSectorPrice);
            psp.setPerformance(performance);
            psp.setSectorName(sector.getName());
            psp.setSectorType(String.valueOf(sector.getType()));
            performance.getSectorPrices().add(psp);
        }

        return performanceRepository.save(performance);
    }

    private Sector createSectorWithSeatGrid(Hall hall, String name, SectorType type, String color, int sectorWidth, int sectorLength, int startX, int startY) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setType(type);
        sector.setColor(color);
        sector.setHall(hall);
        sectorRepository.save(sector);

        createHallArea(startX, startY, sectorWidth, sectorLength, HallAreaType.SEATING, hall, sector);

        List<Seat> seatsToSave = new ArrayList<>();
        for (int row = 1; row <= sectorLength; row++) {
            for (int column = 1; column <= sectorWidth; column++) {
                Seat seat = new Seat();
                seat.setRowNumber(row);
                seat.setSeatNumber(column);
                seat.setPositionX(startX + column - 1);
                seat.setPositionY(startY + row - 1);
                seat.setSector(sector);
                seatsToSave.add(seat);
            }
        }
        seatRepository.saveAll(seatsToSave);
        sector.setSeats(seatsToSave);
        sectorRepository.save(sector);

        return sector;
    }

    private Sector createStandingSector(Hall hall, String name, SectorType type, String color, Integer capacity, int sectorWidth, int sectorLength, int startX, int startY) {
        Sector sector = new Sector();
        sector.setName(name);
        sector.setType(type);
        sector.setColor(color);
        sector.setCapacity(capacity);
        sector.setHall(hall);
        sectorRepository.save(sector);

        createHallArea(startX, startY, sectorWidth, sectorLength, HallAreaType.STANDING, hall, sector);
        return sector;
    }

    private Venue createVenue(String name, String street, String city, String zipCode) {
        Venue venue = new Venue();
        venue.setName(name);
        venue.setStreet(street);
        venue.setCity(city);
        venue.setCountry(FIXED_COUNTRY);
        venue.setZipCode(zipCode);
        return venue;
    }

    private Hall createHall(String name, Integer width, Integer length, Venue venue) {
        Hall hall = new Hall();
        hall.setName(name);
        hall.setWidth(width);
        hall.setLength(length);
        hall.setVenue(venue);
        return hall;
    }

    private void createHallArea(Integer x, Integer y, Integer width, Integer length, HallAreaType type, Hall hall, Sector sector) {
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

    private Artist createArtist(String firstName, String lastName, String artistName) {
        Artist artist = new Artist();
        artist.setFirstName(firstName);
        artist.setLastName(lastName);
        artist.setArtistName(artistName);
        return artist;
    }

    private Event createEvent(String title, String genre, String description, String imageFileName) {
        Event event = new Event();
        event.setTitle(title);
        event.setGenre(genre);
        event.setDescription(description);
        LocalDateTime now = LocalDateTime.now();
        event.setStartTime(now);
        event.setEndTime(now.plusDays(14));
        byte[] imageBytes = loadImageAsBytes(imageFileName);
        event.setImage(imageBytes);
        return event;
    }
}
