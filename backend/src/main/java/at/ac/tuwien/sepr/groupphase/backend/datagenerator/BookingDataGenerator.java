package at.ac.tuwien.sepr.groupphase.backend.datagenerator;

import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
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
import at.ac.tuwien.sepr.groupphase.backend.repository.OrderRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("generateData")
@DependsOn({"eventDataGenerator", "userDataGenerator"})
public class BookingDataGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TARGET_ORDER_COUNT = 1000;
    private static final int TICKETS_PER_ORDER = 5;
    private static final int TARGET_RESERVATION_COUNT = 1000;
    private static final int TICKETS_PER_RESERVATION = 5;

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final PerformanceRepository performanceRepository;
    private final SectorRepository sectorRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public BookingDataGenerator(OrderRepository orderRepository,
                                ReservationRepository reservationRepository,
                                PerformanceRepository performanceRepository,
                                SectorRepository sectorRepository,
                                SeatRepository seatRepository,
                                UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.performanceRepository = performanceRepository;
        this.sectorRepository = sectorRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    @Transactional
    public void generateBookingData() {
        long existingOrders = orderRepository.count();
        long existingReservations = reservationRepository.count();

        if (existingOrders >= TARGET_ORDER_COUNT && existingReservations >= TARGET_RESERVATION_COUNT) {
            LOGGER.debug("Bookings already generated ({} orders, {} reservations). Skipping.",
                existingOrders, existingReservations);
            return;
        }

        List<Performance> performances = performanceRepository.findAllWithRelationsOrderByStartTimeAsc();
        if (performances.isEmpty()) {
            LOGGER.warn("No performances found. Cannot generate bookings.");
            return;
        }

        List<ApplicationUser> users = userRepository.findAll();
        if (users.isEmpty()) {
            LOGGER.warn("No users found. Cannot generate bookings.");
            return;
        }

        Map<Long, List<Sector>> hallSectors = new HashMap<>();
        Map<Long, List<Seat>> sectorSeats = new HashMap<>();
        Map<Long, Map<Long, Integer>> perfSectorSeatIndex = new HashMap<>();
        Map<Long, Map<Long, BigDecimal>> perfSectorPrices = new HashMap<>();

        for (Performance perf : performances) {
            Long hallId = perf.getHall().getId();
            if (!hallSectors.containsKey(hallId)) {
                List<Sector> sectors = sectorRepository.findByHallIdOrderByNameAsc(hallId);
                hallSectors.put(hallId, sectors);
                for (Sector sector : sectors) {
                    if (sector.getType() == SectorType.SEATING) {
                        List<Seat> seats = seatRepository.findBySectorIdOrderByRowNumberAscSeatNumberAsc(sector.getId());
                        sectorSeats.put(sector.getId(), seats);
                    }
                }
            }

            Map<Long, BigDecimal> priceMap = new HashMap<>();
            for (PerformanceSectorPrice psp : perf.getSectorPrices()) {
                priceMap.put(psp.getSectorId(), psp.getPrice());
            }
            perfSectorPrices.put(perf.getId(), priceMap);
        }

        int ordersToCreate = TARGET_ORDER_COUNT - (int) existingOrders;
        int reservationsToCreate = TARGET_RESERVATION_COUNT - (int) existingReservations;

        LOGGER.debug("Generating {} orders and {} reservations", ordersToCreate, reservationsToCreate);

        if (ordersToCreate > 0) {
            generateOrders(ordersToCreate, users, performances, hallSectors, sectorSeats, perfSectorSeatIndex, perfSectorPrices);
        }

        if (reservationsToCreate > 0) {
            generateReservations(reservationsToCreate, users, performances, hallSectors, sectorSeats, perfSectorSeatIndex, perfSectorPrices);
        }

        LOGGER.debug("Successfully generated booking data");
    }

    private void generateOrders(int count, List<ApplicationUser> users, List<Performance> performances,
                                Map<Long, List<Sector>> hallSectors, Map<Long, List<Seat>> sectorSeats,
                                Map<Long, Map<Long, Integer>> perfSectorSeatIndex, Map<Long, Map<Long, BigDecimal>> perfSectorPrices) {
        List<Order> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ApplicationUser user = users.get(i % users.size());
            Performance perf = performances.get(i % performances.size());
            List<Sector> sectors = hallSectors.get(perf.getHall().getId());
            if (sectors == null || sectors.isEmpty()) {
                continue;
            }

            Order order = new Order();
            order.setUser(user);
            order.setPurchaseDate(LocalDateTime.now().minusDays(count - i));
            order.setStatus(OrderStatus.PURCHASED);
            order.setPaymentMethod(PaymentMethod.CREDIT_CARD);

            BigDecimal totalPrice = BigDecimal.ZERO;
            List<Ticket> tickets = new ArrayList<>();
            for (int t = 0; t < TICKETS_PER_ORDER; t++) {
                Sector sector = sectors.get(t % sectors.size());
                Map<Long, BigDecimal> prices = perfSectorPrices.get(perf.getId());
                BigDecimal price = (prices != null) ? prices.getOrDefault(sector.getId(), perf.getStartPrice()) : perf.getStartPrice();
                totalPrice = totalPrice.add(price);

                Seat seat = null;
                if (sector.getType() == SectorType.SEATING) {
                    seat = nextSeat(perf.getId(), sector.getId(), sectorSeats, perfSectorSeatIndex);
                }

                Ticket ticket = new Ticket();
                ticket.setPerformance(perf);
                ticket.setSector(sector);
                ticket.setSeat(seat);
                ticket.setFinalPrice(price);
                ticket.setStatus(TicketStatus.PURCHASED);
                ticket.setQrCode("ORDER-" + i + "-" + t);
                ticket.setOrder(order);
                tickets.add(ticket);
            }

            order.setTickets(tickets);
            order.setTotalPrice(totalPrice);

            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber("BLK-INV-O-" + i);
            invoice.setType(InvoiceType.PURCHASED);
            invoice.setCreatedAt(LocalDateTime.now().minusDays(count - i));
            invoice.setTotalAmount(totalPrice);
            invoice.setOrder(order);
            order.setInvoices(List.of(invoice));

            batch.add(order);

            if (batch.size() >= 50) {
                orderRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            orderRepository.saveAll(batch);
        }
    }

    private void generateReservations(int count, List<ApplicationUser> users, List<Performance> performances,
                                      Map<Long, List<Sector>> hallSectors, Map<Long, List<Seat>> sectorSeats,
                                      Map<Long, Map<Long, Integer>> perfSectorSeatIndex, Map<Long, Map<Long, BigDecimal>> perfSectorPrices) {
        List<Reservation> batch = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            ApplicationUser user = users.get(i % users.size());
            Performance perf = performances.get(i % performances.size());
            List<Sector> sectors = hallSectors.get(perf.getHall().getId());
            if (sectors == null || sectors.isEmpty()) {
                continue;
            }

            Reservation reservation = new Reservation();
            reservation.setUser(user);
            reservation.setReservedUntil(perf.getStartTime().minusMinutes(30));

            List<Ticket> tickets = new ArrayList<>();
            for (int t = 0; t < TICKETS_PER_RESERVATION; t++) {
                Sector sector = sectors.get(t % sectors.size());
                Seat seat = null;
                if (sector.getType() == SectorType.SEATING) {
                    seat = nextSeat(perf.getId(), sector.getId(), sectorSeats, perfSectorSeatIndex);
                }

                BigDecimal price = perfSectorPrices.get(perf.getId()).getOrDefault(sector.getId(), perf.getStartPrice());
                Ticket ticket = new Ticket();
                ticket.setPerformance(perf);
                ticket.setSector(sector);
                ticket.setSeat(seat);
                ticket.setFinalPrice(price);
                ticket.setStatus(TicketStatus.RESERVED);
                ticket.setReservation(reservation);
                tickets.add(ticket);
            }

            reservation.setTickets(tickets);
            batch.add(reservation);

            if (batch.size() >= 50) {
                reservationRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            reservationRepository.saveAll(batch);
        }
    }

    private Seat nextSeat(Long performanceId, Long sectorId, Map<Long, List<Seat>> sectorSeats,
                          Map<Long, Map<Long, Integer>> perfSectorSeatIndex) {
        List<Seat> seats = sectorSeats.get(sectorId);
        if (seats == null || seats.isEmpty()) {
            return null;
        }

        Map<Long, Integer> seatIndexMap = perfSectorSeatIndex.computeIfAbsent(performanceId, k -> new HashMap<>());
        int index = seatIndexMap.getOrDefault(sectorId, 0);
        int actualIndex = index % seats.size();
        seatIndexMap.put(sectorId, actualIndex + 1);
        return seats.get(actualIndex);
    }
}