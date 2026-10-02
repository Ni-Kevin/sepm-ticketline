package at.ac.tuwien.sepr.groupphase.backend.service.impl;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.CheckoutSummaryTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.MyTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.OrderDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithSeatholdIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.PurchaseRequestWithTicketIdsDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationRequestDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.ReservationResponseDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.TicketItemDto;
import at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper.TicketMapper;
import at.ac.tuwien.sepr.groupphase.backend.entity.ApplicationUser;
import at.ac.tuwien.sepr.groupphase.backend.entity.Artist;
import at.ac.tuwien.sepr.groupphase.backend.entity.Invoice;
import at.ac.tuwien.sepr.groupphase.backend.entity.InvoiceType;
import at.ac.tuwien.sepr.groupphase.backend.entity.Order;
import at.ac.tuwien.sepr.groupphase.backend.entity.OrderStatus;
import at.ac.tuwien.sepr.groupphase.backend.entity.PaymentMethod;
import at.ac.tuwien.sepr.groupphase.backend.entity.Performance;
import at.ac.tuwien.sepr.groupphase.backend.entity.Reservation;
import at.ac.tuwien.sepr.groupphase.backend.entity.SeatHold;
import at.ac.tuwien.sepr.groupphase.backend.entity.Sector;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import at.ac.tuwien.sepr.groupphase.backend.entity.TicketStatus;
import at.ac.tuwien.sepr.groupphase.backend.exception.NotFoundException;
import at.ac.tuwien.sepr.groupphase.backend.exception.ValidationException;
import at.ac.tuwien.sepr.groupphase.backend.repository.InvoiceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.OrderRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.PerformanceRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.ReservationRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SeatHoldRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.SectorRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.TicketRepository;
import at.ac.tuwien.sepr.groupphase.backend.repository.UserRepository;
import at.ac.tuwien.sepr.groupphase.backend.service.BookingService;
import at.ac.tuwien.sepr.groupphase.backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final PerformanceRepository performanceRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final EmailService emailService;
    private final SectorRepository sectorRepository;
    private final TicketMapper ticketMapper;

    public BookingServiceImpl(OrderRepository orderRepository,
                              TicketRepository ticketRepository,
                              SeatHoldRepository seatHoldRepository,
                              PerformanceRepository performanceRepository,
                              UserRepository userRepository,
                              InvoiceRepository invoiceRepository,
                              EmailService emailService,
                              ReservationRepository reservationRepository,
                              SectorRepository sectorRepository, TicketMapper ticketMapper) {
        this.orderRepository = orderRepository;
        this.ticketRepository = ticketRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.performanceRepository = performanceRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.emailService = emailService;
        this.reservationRepository = reservationRepository;
        this.sectorRepository = sectorRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional
    public OrderDto purchase(PurchaseRequestWithSeatholdIdsDto request, Long userId) throws ValidationException {
        LOGGER.info("Processing purchase for user {} and performance {}", userId, request.getPerformanceId());

        Performance performance = performanceRepository.findById(request.getPerformanceId())
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        validatePerformanceTimeForPurchase(performance);

        List<SeatHold> holds = validateAndCollectHolds(request.getHoldIds(), userId, request.getPerformanceId());

        int existingTickets = ticketRepository.countTicketsForUserAndPerformance(userId, request.getPerformanceId());
        int holdsQty = holds.stream().mapToInt(SeatHold::getQuantity).sum();
        if (existingTickets + holdsQty > 10) {
            throw new ValidationException("Ticket limit exceeded",
                List.of("Maximum of 10 tickets per user per performance exceeded"));
        }

        BigDecimal totalAmount = calculateTotalAmount(holds, performance);
        if (!processPayment(totalAmount, request.getPaymentMethod(), request.getPaymentDetails())) {
            throw new ValidationException("Payment processing failed", List.of("Payment was rejected by provider"));
        }


        ApplicationUser user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        Order savedOrder = createOrder(user, totalAmount, request.getPaymentMethod());
        Invoice invoice = createInvoice(savedOrder);
        savedOrder.setInvoices(List.of(invoice));

        List<Ticket> generatedTickets = generateTicketsForHolds(holds, savedOrder, performance);
        savedOrder.setTickets(generatedTickets);

        emailService.sendOrderConfirmationEmail(user.getEmail(), user.getLastName(), savedOrder);

        return new OrderDto(
            savedOrder.getId(),
            savedOrder.getPurchaseDate(),
            savedOrder.getTotalPrice(),
            savedOrder.getPaymentMethod(),
            savedOrder.getStatus().name()
        );
    }

    @Override
    @Transactional
    public ReservationResponseDto reserve(ReservationRequestDto request, Long userId) throws ValidationException {
        LOGGER.info("Processing reserve for user {} and performance {}", userId, request.getPerformanceId());

        Performance performance = performanceRepository.findById(request.getPerformanceId())
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        ApplicationUser user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        validatePerformanceTimeForReserve(performance);

        List<SeatHold> holds = validateAndCollectHolds(request.getHoldIds(), userId, request.getPerformanceId());

        int existingTickets = ticketRepository.countTicketsForUserAndPerformance(userId, request.getPerformanceId());
        int holdsQty = holds.stream().mapToInt(SeatHold::getQuantity).sum();
        if (existingTickets + holdsQty > 10) {
            throw new ValidationException("Ticket limit exceeded",
                List.of("Maximum of 10 tickets per user per performance exceeded"));
        }

        Reservation reservation = createReservation(user, performance);
        List<Ticket> generatedTickets = generateTicketsForReserve(holds, reservation, performance);
        reservation.setTickets(generatedTickets);
        emailService.sendReservationConfirmationEmail(user.getEmail(), user.getLastName(), reservation);

        return new ReservationResponseDto(
            reservation.getReservationNumber(),
            reservation.getReservedUntil()
        );
    }

    @Override
    @Transactional
    public OrderDto purchaseFromReservation(PurchaseRequestWithTicketIdsDto request, Long userId) throws ValidationException {
        LOGGER.info("Processing purchase from reservation for user {} and performance {}", userId, request.getPerformanceId());

        Performance performance = performanceRepository.findById(request.getPerformanceId())
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        validatePerformanceTimeForPurchaseAfterReserve(performance);

        List<Long> lockedIds = ticketRepository.findReservedTicketIdsWithLock(request.getTicketIds(), userId);
        if (lockedIds.size() != request.getTicketIds().size()) {
            List<Long> missingIds = request.getTicketIds().stream()
                .filter(id -> !lockedIds.contains(id))
                .toList();
            throw new ValidationException("Some tickets could not be found or do not belong to you",
                List.of("Missing ticket IDs: " + missingIds));
        }

        List<Ticket> tickets = ticketRepository.findReservedTicketsWithAllRelations(lockedIds, userId);

        for (Ticket ticket : tickets) {
            if (!ticket.getPerformance().getId().equals(request.getPerformanceId())) {
                throw new ValidationException("Ticket " + ticket.getId() + " does not belong to the selected performance",
                    List.of("Ticket " + ticket.getId() + " belongs to a different performance"));
            }
        }

        BigDecimal totalAmount = tickets.stream()
            .map(Ticket::getFinalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!processPayment(totalAmount, request.getPaymentMethod(), request.getPaymentDetails())) {
            throw new ValidationException("Payment processing failed", List.of("Payment was rejected by provider"));
        }

        ApplicationUser user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));

        Order savedOrder = createOrder(user, totalAmount, request.getPaymentMethod());
        Invoice invoice = createInvoice(savedOrder);
        savedOrder.setInvoices(List.of(invoice));

        List<Reservation> associatedReservations = tickets.stream()
            .map(Ticket::getReservation)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        for (Ticket ticket : tickets) {
            ticket.setOrder(savedOrder);
            ticket.setReservation(null);
            ticket.setStatus(TicketStatus.PURCHASED);

            ticket.setQrCode(String.format("P%d-O%d-T%d-%s",
                performance.getId(),
                savedOrder.getId(),
                ticket.getId(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 16)));

            ticketRepository.save(ticket);
        }

        for (Reservation res : associatedReservations) {
            boolean hasRemainingTickets = ticketRepository.existsByReservationId(res.getId());

            if (!hasRemainingTickets) {
                LOGGER.info("Reservation {} is now empty and will be deleted", res.getId());
                reservationRepository.delete(res);
            }
        }

        savedOrder.setTickets(tickets);
        emailService.sendOrderConfirmationEmail(user.getEmail(), user.getLastName(), savedOrder);

        return new OrderDto(
            savedOrder.getId(),
            savedOrder.getPurchaseDate(),
            savedOrder.getTotalPrice(),
            savedOrder.getPaymentMethod(),
            savedOrder.getStatus().name()
        );
    }

    @Override
    @Transactional
    public void cancelReservedTickets(List<Long> ticketIds, Long userId) throws ValidationException {
        LOGGER.info("Cancelling reserved ticket {} for user {}", ticketIds, userId);

        if (ticketIds == null || ticketIds.isEmpty()) {
            throw new ValidationException("Ticket cancellation failed", List.of("No ticket IDs provided"));
        }

        List<Ticket> allTickets = ticketRepository.findAllById(ticketIds);

        if (allTickets.size() != ticketIds.size()) {
            List<Long> foundIds = allTickets.stream().map(Ticket::getId).toList();
            List<Long> missingIds = ticketIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new NotFoundException("Some tickets not found: " + missingIds);
        }

        List<String> errors = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            if (ticket.getStatus() != TicketStatus.RESERVED) {
                errors.add("Ticket " + ticket.getId() + " is not reserved");
            }
            if (ticket.getReservation() == null || !ticket.getReservation().getUser().getId().equals(userId)) {
                errors.add("Ticket " + ticket.getId() + " does not belong to user " + userId);
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Ticket cancellation failed", errors);
        }

        List<Ticket> tickets = ticketRepository.findReservedTicketsWithAllRelations(ticketIds, userId);

        LocalDateTime now = LocalDateTime.now();

        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);

            if (ticket.getSeat() != null) {
                seatHoldRepository.findActiveHoldBySeat(
                    ticket.getSeat().getId(),
                    ticket.getPerformance().getId(),
                    now
                ).ifPresent(seatHoldRepository::delete);
            } else {
                seatHoldRepository.findActiveHoldsByUserSectorAndPerformance(
                        userId,
                        ticket.getSector().getId(),
                        ticket.getPerformance().getId(),
                        now
                    ).stream()
                    .findFirst()
                    .ifPresent(seatHoldRepository::delete);
            }

            ApplicationUser user = ticket.getReservation().getUser();
            emailService.sendCancellationEmail(user.getEmail(), user.getLastName(), ticket);
        }

        LOGGER.info("Successfully cancelled {} reserved tickets for user {}. Confirmation emails sent.", ticketIds.size(), userId);
    }

    @Override
    @Transactional
    public void cancelPurchasedTickets(List<Long> ticketIds, Long userId) throws ValidationException {
        LOGGER.info("Cancelling purchased tickets {} for user {}", ticketIds, userId);

        List<Long> distinctTicketIds = ticketIds.stream().distinct().toList();
        List<Ticket> tickets = ticketRepository.findPurchasedTicketsByIdsAndUserId(distinctTicketIds, userId);
        if (tickets.size() != distinctTicketIds.size()) {
            throw new ValidationException("Ticket cancellation failed", List.of("Only your purchased tickets can be cancelled"));
        }

        Long orderId = tickets.getFirst().getOrder().getId();
        boolean sameOrder = tickets.stream().allMatch(ticket -> ticket.getOrder().getId().equals(orderId));
        if (!sameOrder) {
            throw new ValidationException("Ticket cancellation failed", List.of("Tickets from different purchases cannot be cancelled together"));
        }

        boolean performanceAlreadyStarted = tickets.stream()
            .anyMatch(ticket -> ticket.getPerformance().getStartTime().isBefore(LocalDateTime.now()));
        if (performanceAlreadyStarted) {
            throw new ValidationException("Ticket cancellation failed", List.of("Tickets for performances that have already started cannot be cancelled"));
        }

        Order order = orderRepository.findByTicketIdWithTickets(tickets.getFirst().getId())
            .orElseThrow(() -> new NotFoundException("Order not found"));

        for (Ticket ticket : tickets) {
            ticket.setStatus(TicketStatus.CANCELLED);
            ticketRepository.save(ticket);
        }

        boolean allTicketsCancelled = order.getTickets().stream()
            .allMatch(ticket -> ticket.getStatus() == TicketStatus.CANCELLED);
        if (allTicketsCancelled) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }

        Invoice cancellationInvoice = createOrUpdateCancellationInvoice(order);
        cancellationInvoice.setOrder(order);

        List<Invoice> invoices = new ArrayList<>(order.getInvoices() != null ? order.getInvoices() : List.of());
        invoices.removeIf(i -> i.getType() == InvoiceType.CANCELLED);
        invoices.add(cancellationInvoice);
        order.setInvoices(invoices);

        emailService.sendPurchaseCancellationEmail(order.getUser().getEmail(), order.getUser().getLastName(), order, tickets);
        LOGGER.info("Successfully cancelled purchased tickets {} for user {}", distinctTicketIds, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyTicketDto> getAllTicketsForUser(Long userId) {
        List<Ticket> tickets = ticketRepository.findAllByUserIdWithRelations(userId);
        return ticketMapper.ticketListToMyTicketDtoList(tickets);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckoutSummaryTicketDto getCheckoutSummaryFromTickets(Long performanceId, List<Long> ticketIds, Long userId) throws ValidationException {
        LOGGER.info("Getting checkout summary from tickets for performance {} and tickets {} for user {}",
            performanceId, ticketIds, userId);

        List<Ticket> tickets = ticketRepository.findReservedTicketsWithAllRelations(ticketIds, userId);

        if (tickets.size() != ticketIds.size()) {
            List<Long> foundIds = tickets.stream().map(Ticket::getId).toList();
            List<Long> missingIds = ticketIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new NotFoundException("Reserved tickets not found: " + missingIds);
        }

        Performance performance = performanceRepository.findById(performanceId)
            .orElseThrow(() -> new NotFoundException("Performance not found"));

        for (Ticket ticket : tickets) {
            if (!ticket.getPerformance().getId().equals(performanceId)) {
                throw new ValidationException("Ticket " + ticket.getId()
                    + " does not belong to performance " + performanceId, List.of(""));
            }
        }

        List<TicketItemDto> items = new ArrayList<>();
        for (Ticket ticket : tickets) {
            String seatName = null;
            if (ticket.getSeat() != null) {
                seatName = String.format("Row %d, Seat %d",
                    ticket.getSeat().getRowNumber(),
                    ticket.getSeat().getSeatNumber());
            }

            TicketItemDto item = new TicketItemDto(
                ticket.getId(),
                ticket.getSector().getName(),
                seatName,
                1,
                ticket.getFinalPrice()
            );
            items.add(item);
        }

        BigDecimal totalAmount = items.stream()
            .map(TicketItemDto::getPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime earliestExpiration = performance.getStartTime().minusMinutes(30L);

        List<String> artists = performance.getArtists().stream()
            .map(Artist::getArtistName)
            .collect(Collectors.toList());

        String eventName = performance.getEvent() != null
            ? performance.getEvent().getTitle()
            : performance.getPerformanceName();

        return new CheckoutSummaryTicketDto(
            items,
            totalAmount,
            earliestExpiration,
            artists,
            eventName,
            performance.getPerformanceName(),
            performance.getStartTime(),
            performance.getHall().getName(),
            performance.getHall().getVenue().getName()
        );
    }

    private void validatePerformanceTimeForPurchase(Performance performance) throws ValidationException {
        if (performance.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Performance has already started",
                List.of("Cannot buy tickets for past events."));
        }
    }

    private void validatePerformanceTimeForReserve(Performance performance) throws ValidationException {
        if (performance.getStartTime().minusMinutes(30L).isBefore(LocalDateTime.now())) {
            throw new ValidationException("Tickets can only be reserved until 30 min before performance start",
                List.of("Cannot reserve tickets for past events."));
        }
    }

    private void validatePerformanceTimeForPurchaseAfterReserve(Performance performance) throws ValidationException {
        if (performance.getStartTime().minusMinutes(30L).isBefore(LocalDateTime.now())) {
            throw new ValidationException("Reserved tickets can only be purchased until 30 min before performance start",
                List.of("Cannot purchase tickets."));
        }
    }

    private List<SeatHold> validateAndCollectHolds(List<Long> holdIds, Long userId, Long requestPerformanceId) throws ValidationException {
        List<SeatHold> holds = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Long id : holdIds) {
            SeatHold hold = seatHoldRepository.findAndLockSeatById(id).orElse(null);
            if (hold == null) {
                errors.add("Hold " + id + " not found");
            } else if (!hold.getUser().getId().equals(userId)) {
                errors.add("Hold " + id + " belongs to another user");
            } else if (!hold.isActive() || hold.getExpiresAt().isBefore(LocalDateTime.now())) {
                errors.add("Hold " + id + " has expired");
            } else if (!hold.getPerformance().getId().equals(requestPerformanceId)) {
                errors.add("Hold " + id + " does not belong to the selected performance");
            } else {
                holds.add(hold);
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
        return holds;
    }

    private BigDecimal calculateTotalAmount(List<SeatHold> holds, Performance performance) {
        return holds.stream()
            .map(h -> calculateItemPrice(h, performance).multiply(BigDecimal.valueOf(h.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Order createOrder(ApplicationUser user, BigDecimal amount, PaymentMethod method) {
        Order order = new Order();
        order.setUser(user);
        order.setPurchaseDate(LocalDateTime.now());
        order.setTotalPrice(amount);
        order.setStatus(OrderStatus.PURCHASED);
        order.setPaymentMethod(method);
        return orderRepository.save(order);
    }

    private Reservation createReservation(ApplicationUser user, Performance performance) {
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setReservedUntil(performance.getStartTime().minusMinutes(30L));
        return reservationRepository.save(reservation);
    }

    private Invoice createInvoice(Order order) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + order.getId() + "-" + System.currentTimeMillis() % 1000);
        invoice.setType(InvoiceType.PURCHASED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(order.getTotalPrice());
        invoice.setOrder(order);
        return invoiceRepository.save(invoice);
    }

    private Invoice createOrUpdateCancellationInvoice(Order order) {
        BigDecimal cancelledTotal = BigDecimal.ZERO;
        for (Ticket ticket : order.getTickets()) {
            if (ticket.getStatus() == TicketStatus.CANCELLED) {
                cancelledTotal = cancelledTotal.add(ticket.getFinalPrice());
            }
        }

        Optional<Invoice> existing = invoiceRepository.findFirstByOrderIdAndTypeOrderByIdDesc(
            order.getId(), InvoiceType.CANCELLED);
        LOGGER.info("Found existing cancellation invoice: {}", existing.isPresent() ? existing.get().getInvoiceNumber() : "NONE");

        if (existing.isPresent()) {
            Invoice invoice = existing.get();
            invoice.setTotalAmount(cancelledTotal);
            return invoiceRepository.save(invoice);
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("CINV-" + order.getId() + "-" + System.currentTimeMillis() % 1000);
        invoice.setType(InvoiceType.CANCELLED);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setTotalAmount(cancelledTotal);
        invoice.setOrder(order);
        return invoiceRepository.save(invoice);
    }

    private List<Ticket> generateTicketsForHolds(List<SeatHold> holds, Order order, Performance performance) {
        List<Ticket> generatedTickets = new ArrayList<>();
        for (SeatHold hold : holds) {
            BigDecimal price = calculateItemPrice(hold, performance);

            // fetch sector with full hall -> venue chain while still in transaction
            Sector fullSector = sectorRepository.findByIdWithVenue(hold.getSector().getId())
                .orElse(hold.getSector());

            for (int i = 0; i < hold.getQuantity(); i++) {
                Ticket ticket = new Ticket();
                ticket.setOrder(order);
                ticket.setPerformance(performance);
                ticket.setSector(fullSector); // fully loaded
                ticket.setSeat(hold.getSeat());
                ticket.setFinalPrice(price);
                ticket.setStatus(TicketStatus.PURCHASED);

                ticket = ticketRepository.save(ticket);
                ticket.setQrCode(String.format("P%d-O%d-T%d-%s",
                    performance.getId(),
                    order.getId(),
                    ticket.getId(),
                    UUID.randomUUID().toString().replace("-", "").substring(0, 16)));
                ticket = ticketRepository.save(ticket);

                generatedTickets.add(ticket);
            }
            hold.setActive(false);
            seatHoldRepository.save(hold);
        }
        return generatedTickets;
    }

    private List<Ticket> generateTicketsForReserve(List<SeatHold> holds, Reservation reservation, Performance performance) {
        List<Ticket> generatedTickets = new ArrayList<>();
        for (SeatHold hold : holds) {
            BigDecimal price = calculateItemPrice(hold, performance);
            for (int i = 0; i < hold.getQuantity(); i++) {
                Ticket ticket = new Ticket();
                ticket.setReservation(reservation);
                ticket.setPerformance(performance);
                ticket.setSector(hold.getSector());
                ticket.setSeat(hold.getSeat());
                ticket.setFinalPrice(price);
                ticket.setStatus(TicketStatus.RESERVED);
                ticket = ticketRepository.save(ticket);
                generatedTickets.add(ticket);
            }
            hold.setActive(false);
            seatHoldRepository.save(hold);
        }

        return generatedTickets;
    }


    private BigDecimal calculateItemPrice(SeatHold hold, Performance performance) {
        if (hold.getSector() != null && hold.getPerformance() != null) {
            BigDecimal categoryPrice = hold.getPerformance().getSectorPrices().stream()
                .filter(p -> Objects.equals(hold.getSector().getId(), p.getSectorId()))
                .map(p -> p.getPrice())
                .findFirst()
                .orElse(performance.getStartPrice());

            if (categoryPrice != null) {
                return categoryPrice;
            }
        }

        return performance.getStartPrice();
    }

    private boolean processPayment(BigDecimal amount, PaymentMethod method, Map<String, String> details) {
        LOGGER.info("Processing payment of {} via {}", amount, method);

        if (details == null || details.isEmpty()) {
            LOGGER.warn("Payment failed: No payment details provided");
            return false;
        }

        return switch (method) {
            case CREDIT_CARD -> validateCreditCard(details);
            case PAYPAL, KLARNA -> validateEmail(details.get("email"));
            default -> {
                LOGGER.warn("Payment failed: Unknown payment method");
                yield false;
            }
        };
    }

    private boolean validateCreditCard(Map<String, String> details) {
        String number = details.get("cardNumber");

        if (number != null) {
            number = number.replaceAll("\\s+", "");
        }
        if (number == null || !number.matches("\\d{13,19}")) {
            return false;
        }

        String expiry = details.get("expiry");
        String cvv = details.get("cvv");

        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            return false;
        }
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            return false;
        }


        String[] parts = expiry.split("/");
        int expMonth = Integer.parseInt(parts[0]);
        int expYear = Integer.parseInt("20" + parts[1]);

        YearMonth now = YearMonth.now();
        YearMonth expiryDate = YearMonth.of(expYear, expMonth);
        if (expiryDate.isBefore(now)) {
            LOGGER.warn("Payment failed: Credit card is expired ({})", expiry);
            return false;
        }

        return true;
    }

    private boolean validateEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return email != null && email.matches(emailRegex);
    }
}
