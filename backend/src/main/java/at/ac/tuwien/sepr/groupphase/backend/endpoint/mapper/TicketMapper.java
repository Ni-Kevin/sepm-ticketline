package at.ac.tuwien.sepr.groupphase.backend.endpoint.mapper;

import at.ac.tuwien.sepr.groupphase.backend.endpoint.dto.MyTicketDto;
import at.ac.tuwien.sepr.groupphase.backend.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "ticketId", source = "id")
    @Mapping(target = "finalTicketPrice", source = "finalPrice")
    @Mapping(target = "ticketStatus", source = "status")
    @Mapping(target = "performanceId", source = "performance.id")
    @Mapping(target = "performanceTitle", source = "performance.performanceName")
    @Mapping(target = "performanceDate", source = "performance.startTime")
    @Mapping(target = "hallName", source = "performance.hall.name")
    @Mapping(target = "venueName", source = "performance.hall.venue.name")
    @Mapping(target = "sector", source = "sector.name")
    @Mapping(target = "seat", source = "ticket", qualifiedByName = "mapSeatToString")
    @Mapping(target = "orderId", source = "ticket", qualifiedByName = "mapOrderId")
    @Mapping(target = "invoiceId", source = "ticket", qualifiedByName = "mapInvoiceId")
    @org.mapstruct.ValueMapping(source = "USED", target = "PURCHASED")
    MyTicketDto ticketToMyTicketDto(Ticket ticket);

    List<MyTicketDto> ticketListToMyTicketDtoList(List<Ticket> tickets);


    @Named("mapOrderId")
    default Long mapOrderId(Ticket ticket) {
        if (ticket.getOrder() != null) {
            return ticket.getOrder().getId();
        }
        if (ticket.getReservation() != null) {
            return ticket.getReservation().getId();
        }
        return null;
    }

    @Named("mapInvoiceId")
    default Long mapInvoiceId(Ticket ticket) {
        if (ticket.getOrder() != null && ticket.getOrder().getInvoices() != null && !ticket.getOrder().getInvoices().isEmpty()) {
            return ticket.getOrder().getInvoices().get(0).getId();
        }
        return null;
    }

    @Named("mapSeatToString")
    default String mapSeatToString(Ticket ticket) {
        if (ticket.getSeat() == null) {
            return "Standing";
        }
        return "Row: " + ticket.getSeat().getRowNumber() + ", Seat: " + ticket.getSeat().getSeatNumber();
    }
}