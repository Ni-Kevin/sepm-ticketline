import {Component, OnInit} from '@angular/core';
import {BookingService} from '../../services/booking.service';
import {MyTicketDto, TicketStatus} from "../../dtos/my-tickets";
import {PdfTicketService} from "../../services/pdf.ticket.service";
import {PdfInvoiceService} from "../../services/pdf.invoice.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-my-tickets',
  templateUrl: './my-tickets.component.html',
  styleUrls: ['./my-tickets.component.scss'],
  standalone: false
})
export class MyTicketsComponent implements OnInit {
  tickets: MyTicketDto[] = [];
  successMessage = '';
  errorMessage = '';
  showPast = false;
  selectedTicketToCancel: MyTicketDto | null = null;
  relatedTicketsForPerformance: MyTicketDto[] = [];
  selectedReservedTicketIds: number[] = [];
  selectedTicketsToPurchase: MyTicketDto[] = [];
  selectedPurchasedTicketIds: number[] = [];
  expandedBookings = new Set<number>();
  expandedPastBookings = new Set<number>();
  upcomingCurrentPage = 1;
  pastCurrentPage = 1;
  pageSize = 10;

  previousTicketStatuses = new Map<number, TicketStatus>();
  singleTicketForModal: MyTicketDto[] = [];
  isSingleCancel = false;


  constructor(private bookingService: BookingService,
              private pdfTicketService: PdfTicketService,
              private pdfInvoiceService: PdfInvoiceService,
              private router: Router) {
  }

  get upcomingTickets() {
    const now = new Date();
    return this.tickets.filter(ticket => ticket.ticketStatus !== 'CANCELLED'
      && new Date(ticket.performanceDate) >= now
    );
  }

  get pastTickets() {
    const now = new Date();
    return this.tickets.filter(ticket => ticket.ticketStatus === 'CANCELLED'
      || new Date(ticket.performanceDate) < now
    );
  }

  ngOnInit(): void {
    this.bookingService.getAllTickets().subscribe({
      next: (data) => {
        this.tickets = data;
        this.upcomingCurrentPage = 1;
        this.pastCurrentPage = 1;
      },
      error: () => {
        this.errorMessage = 'Could not load your tickets.';
      }
    });
  }


  onCancelTicket(ticket: MyTicketDto): void {
    this.isSingleCancel = false;
    this.selectedTicketToCancel = ticket;
    this.selectedReservedTicketIds = ticket.ticketStatus === 'RESERVED'
      ? this.tickets
        .filter(t => t.orderId === ticket.orderId && t.ticketStatus === 'RESERVED')
        .map(t => t.ticketId)
      : [];
    this.selectedPurchasedTicketIds = ticket.ticketStatus === 'PURCHASED'
      ? this.tickets
        .filter(t => t.orderId === ticket.orderId && t.ticketStatus === 'PURCHASED')
        .map(t => t.ticketId)
      : [];
  }

  onCancelSingleTicket(ticket: MyTicketDto): void {
    this.isSingleCancel = true;
    this.selectedTicketToCancel = ticket;
    this.singleTicketForModal = [ticket];
    this.selectedReservedTicketIds = ticket.ticketStatus === 'RESERVED' ? [ticket.ticketId] : [];
    this.selectedPurchasedTicketIds = ticket.ticketStatus === 'PURCHASED' ? [ticket.ticketId] : [];
  }

  get cancellablePurchasedTickets(): MyTicketDto[] {
    if (!this.selectedTicketToCancel?.orderId) return [];
    return this.tickets.filter(ticket => ticket.ticketStatus === 'PURCHASED'
      && ticket.orderId === this.selectedTicketToCancel?.orderId
      && new Date(ticket.performanceDate) >= new Date());
  }

  get selectedCancellationPurchasedTickets(): MyTicketDto[] {
    const source = this.isSingleCancel ? this.singleTicketForModal : this.cancellablePurchasedTickets;
    return source.filter(ticket => this.selectedPurchasedTicketIds.includes(ticket.ticketId));
  }

  get selectedCancellationTotal(): number {
    return this.selectedCancellationPurchasedTickets
      .map(ticket => ticket.finalTicketPrice ?? 0)
      .reduce((sum, price) => sum + price, 0);
  }

  isPurchasedTicketSelected(ticketId: number): boolean {
    return this.selectedPurchasedTicketIds.includes(ticketId);
  }

  togglePurchasedTicket(ticketId: number, checked: boolean): void {
    if (checked) {
      this.selectedPurchasedTicketIds = [...new Set([...this.selectedPurchasedTicketIds, ticketId])];
    } else {
      this.selectedPurchasedTicketIds = this.selectedPurchasedTicketIds.filter(id => id !== ticketId);
    }
  }

  get cancellableReservedTickets(): MyTicketDto[] {
    if (!this.selectedTicketToCancel?.orderId) return [];
    return this.tickets.filter(ticket => ticket.ticketStatus === 'RESERVED'
      && ticket.orderId === this.selectedTicketToCancel?.orderId
      && new Date(ticket.performanceDate) >= new Date());
  }

  get selectedCancellationReservedTickets(): MyTicketDto[] {
    return this.cancellableReservedTickets.filter(ticket => this.selectedReservedTicketIds.includes(ticket.ticketId));
  }

  isReservedTicketSelected(ticketId: number): boolean {
    return this.selectedReservedTicketIds.includes(ticketId);
  }

  toggleReservedTicket(ticketId: number, checked: boolean): void {
    if (checked) {
      this.selectedReservedTicketIds = [...new Set([...this.selectedReservedTicketIds, ticketId])];
    } else {
      this.selectedReservedTicketIds = this.selectedReservedTicketIds.filter(id => id !== ticketId);
    }
  }

  onBuyTicket(ticket: MyTicketDto): void {
    this.relatedTicketsForPerformance = this.tickets.filter(
      t => t.performanceId === ticket.performanceId && t.ticketStatus === 'RESERVED' && t.ticketId !== ticket.ticketId
    );
    this.relatedTicketsForPerformance.unshift(ticket);
    this.selectedTicketsToPurchase = [...this.relatedTicketsForPerformance];
  }

  onBuySingleTicket(ticket: MyTicketDto): void {
    this.relatedTicketsForPerformance = [ticket];
    this.selectedTicketsToPurchase = [ticket];
  }

  toggleBooking(orderId: number): void {
    if (this.expandedBookings.has(orderId)) {
      this.expandedBookings.delete(orderId);
    } else {
      this.expandedBookings.add(orderId);
    }
  }

  isBookingExpanded(orderId: number): boolean {
    return this.expandedBookings.has(orderId);
  }

  togglePastBooking(orderId: number): void {
    if (this.expandedPastBookings.has(orderId)) {
      this.expandedPastBookings.delete(orderId);
    } else {
      this.expandedPastBookings.add(orderId);
    }
  }

  isPastBookingExpanded(orderId: number): boolean {
    return this.expandedPastBookings.has(orderId);
  }

  get groupedBookings() {
    const groups = new Map<number, MyTicketDto[]>();

    for (const ticket of this.upcomingTickets) {
      const key = ticket.orderId;
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key)!.push(ticket);
    }

    return Array.from(groups.entries()).map(([orderId, tickets]) => ({
      orderId,
      performanceTitle: tickets[0].performanceTitle,
      performanceDate: tickets[0].performanceDate,
      venueName: tickets[0].venueName,
      hallName: tickets[0].hallName,
      ticketCount: tickets.length,
      totalPrice: tickets.reduce((sum, t) => sum + (t.finalTicketPrice ?? 0), 0),
      tickets
    }));
  }


  get groupedPastBookings() {
    const groups = new Map<number, MyTicketDto[]>();

    for (const ticket of this.pastTickets) {
      const key = ticket.orderId;
      if (!groups.has(key)) {
        groups.set(key, []);
      }
      groups.get(key)!.push(ticket);
    }

    return Array.from(groups.entries()).map(([orderId, tickets]) => ({
      orderId,
      performanceTitle: tickets[0].performanceTitle,
      performanceDate: tickets[0].performanceDate,
      venueName: tickets[0].venueName,
      hallName: tickets[0].hallName,
      ticketCount: tickets.length,
      totalPrice: tickets.reduce((sum, t) => sum + (t.finalTicketPrice ?? 0), 0),
      dominantStatus: tickets.some(t => t.ticketStatus === 'RESERVED') ? 'RESERVED'
        : tickets.some(t => t.ticketStatus === 'PURCHASED') ? 'PURCHASED'
          : 'CANCELLED' as TicketStatus,
      tickets
    }));
  }

  get upcomingTotalPages(): number {
    return Math.ceil(this.groupedBookings.length / this.pageSize);
  }

  get pastTotalPages(): number {
    return Math.ceil(this.groupedPastBookings.length / this.pageSize);
  }

  get paginatedGroupedBookings() {
    const startIndex = (this.upcomingCurrentPage - 1) * this.pageSize;
    return this.groupedBookings.slice(startIndex, startIndex + this.pageSize);
  }

  get paginatedGroupedPastBookings() {
    const startIndex = (this.pastCurrentPage - 1) * this.pageSize;
    return this.groupedPastBookings.slice(startIndex, startIndex + this.pageSize);
  }

  onUpcomingPageChanged(page: number): void {
    this.upcomingCurrentPage = page;
  }

  onPastPageChanged(page: number): void {
    this.pastCurrentPage = page;
  }

  togglePastTickets(): void {
    this.showPast = !this.showPast;
    this.pastCurrentPage = 1;
  }

  goToCheckoutWithTicketIds(): void {
    if (this.selectedTicketsToPurchase.length === 0) return;

    const ticketIds = this.selectedTicketsToPurchase.map(t => t.ticketId);
    const performanceId = this.selectedTicketsToPurchase[0].performanceId;
    this.router.navigate(['/checkout/purchase-from-reservation', performanceId], {
      queryParams: {ticketIds: ticketIds.join(',')}
    });
  }

  toggleTicketSelection(ticket: MyTicketDto): void {
    const index = this.selectedTicketsToPurchase.indexOf(ticket);
    if (index > -1) {
      this.selectedTicketsToPurchase.splice(index, 1);
    } else {
      this.selectedTicketsToPurchase.push(ticket);
    }
  }

  selectAllTickets(): void {
    this.selectedTicketsToPurchase = [...this.relatedTicketsForPerformance];
  }

  clearAllTickets(): void {
    this.selectedTicketsToPurchase = [];
  }

  selectAllReservedTickets(): void {
    this.selectedReservedTicketIds = this.cancellableReservedTickets.map(t => t.ticketId);
  }

  clearAllReservedTickets(): void {
    this.selectedReservedTicketIds = [];
  }

  selectAllPurchasedTickets(): void {
    this.selectedPurchasedTicketIds = this.cancellablePurchasedTickets.map(t => t.ticketId);
  }

  clearAllPurchasedTickets(): void {
    this.selectedPurchasedTicketIds = [];
  }

  onConfirmCancelReservedTicket(): void {
    if (this.selectedReservedTicketIds.length === 0) return;

    const cancelledIds = [...this.selectedReservedTicketIds];
    cancelledIds.forEach(id => {
      const ticket = this.tickets.find(t => t.ticketId === id);
      if (ticket) {
        this.previousTicketStatuses.set(id, ticket.ticketStatus as TicketStatus);
      }
    });

    this.bookingService.cancelReservedTicket(cancelledIds).subscribe({
      next: () => {
        this.successMessage = 'Your reservations have been cancelled.';
        this.errorMessage = '';
        this.tickets = this.tickets.map(ticket => cancelledIds.includes(ticket.ticketId)
          ? {...ticket, ticketStatus: 'CANCELLED'}
          : ticket);
        this.selectedReservedTicketIds = [];
        this.selectedTicketToCancel = null;
      },
      error: (error) => {
        this.errorMessage = error?.error?.detail ?? error?.error?.message ?? 'Could not cancel the selected tickets.';
        this.successMessage = '';
      }
    });
  }

  onConfirmCancelPurchasedTickets(): void {
    if (this.selectedPurchasedTicketIds.length === 0) return;

    const cancelledIds = [...this.selectedPurchasedTicketIds];
    cancelledIds.forEach(id => {
      const ticket = this.tickets.find(t => t.ticketId === id);
      if (ticket) {
        this.previousTicketStatuses.set(id, ticket.ticketStatus as TicketStatus);
      }
    });
    this.bookingService.cancelPurchasedTickets(cancelledIds).subscribe({
      next: () => {
        this.successMessage = 'Your selected tickets have been cancelled.';
        this.errorMessage = '';
        this.tickets = this.tickets.map(ticket => cancelledIds.includes(ticket.ticketId)
          ? {...ticket, ticketStatus: 'CANCELLED'}
          : ticket);
        this.selectedPurchasedTicketIds = [];
        this.selectedTicketToCancel = null;
      },
      error: (error) => {
        this.errorMessage = error?.error?.detail ?? error?.error?.message ?? 'Could not cancel the selected tickets.';
        this.successMessage = '';
      }
    });
  }

  wasTicketPurchasedBefore(ticket: MyTicketDto): boolean {
    return ticket.invoiceId != null;
  }

  downloadTicketPdf(ticketId: number): void {
    this.pdfTicketService.getTicketPdf(ticketId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ticket-${ticketId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage = 'Could not download ticket PDF.';
      }
    });
  }

  downloadInvoicePdf(ticketId: number): void {
    this.pdfInvoiceService.getInvoicePdf(ticketId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `invoice-${ticketId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage = 'Could not download invoice PDF.';
      }
    });
  }

  downloadCancellationInvoicePdf(ticketId: number): void {
    this.pdfInvoiceService.getCancellationInvoicePdf(ticketId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `cancellation-invoice-${ticketId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage = 'Could not download cancellation invoice.';
      }
    });
  }
}
