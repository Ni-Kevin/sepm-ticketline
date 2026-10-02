import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {Globals} from '../global/globals';
import {CheckoutSummarySeatholdDto, CheckoutSummaryTicketDto} from '../dtos/checkout-summary.dto';
import {
  PurchaseRequestWithSeatholdIdsDto,
  OrderDto,
  PurchaseRequestWithTicketIdsDto
} from '../dtos/purchase-request.dto';
import {ReservationRequestDto} from '../dtos/reservation-request.dto';
import {ReservationResponseDto} from '../dtos/reservation-response.dto';
import {PerformanceHallLayoutDto, SeatStatusDto} from '../dtos/performance-layout.dto';
import {MyTicketDto} from "../dtos/my-tickets";

@Injectable({
  providedIn: 'root'
})
/**
 * Service for booking and purchase API requests.
 */
export class BookingService {

  private bookingBaseUri: string = this.globals.backendUri + '/bookings';
  private holdsBaseUri: string = this.globals.backendUri + '/holds';
  private performanceBaseUri: string = this.globals.backendUri + '/performances';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  /**
   * Get checkout summary for a specific performance.
   *
   * @param performanceId performance id
   * @returns checkout summary response
   */
  getCheckoutSummaryFromSeathold(performanceId: number): Observable<CheckoutSummarySeatholdDto> {
    return this.httpClient.get<CheckoutSummarySeatholdDto>(`${this.holdsBaseUri}/${performanceId}`);
  }

  getCheckoutSummaryFromTickets(performanceId: number, ticketIds: number[]): Observable<CheckoutSummaryTicketDto> {
    const params = new HttpParams().set('ticketIds', ticketIds.join(','));
    return this.httpClient.get<CheckoutSummaryTicketDto>(
      `${this.bookingBaseUri}/checkout/${performanceId}`,
      {params}
    );
  }


  /**
   * Purchase held seats.
   *
   * @param request purchase request payload
   * @returns created order response
   */
  purchaseTicketsWithSeatholdIds(request: PurchaseRequestWithSeatholdIdsDto): Observable<OrderDto> {
    return this.httpClient.post<OrderDto>(`${this.bookingBaseUri}/purchases`, request);
  }

  purchaseTicketsWithTicketIds(request: PurchaseRequestWithTicketIdsDto): Observable<OrderDto> {
    return this.httpClient.post<OrderDto>(`${this.bookingBaseUri}/purchases-from-reservation`, request);
  }

  /**
   * Reserve held seats.
   *
   * @param request reservation request payload
   * @returns reservation response
   */
  reserveTickets(request: ReservationRequestDto): Observable<ReservationResponseDto> {
    return this.httpClient.post<ReservationResponseDto>(`${this.bookingBaseUri}/reservations`, request);
  }

  /**
   * Get the hall layout for a specific performance.
   *
   * @param performanceId performance id
   * @returns hall layout with sectors and dimensions
   */
  getHallLayout(performanceId: number): Observable<PerformanceHallLayoutDto> {
    return this.httpClient.get<PerformanceHallLayoutDto>(`${this.performanceBaseUri}/${performanceId}/layout`);
  }

  /**
   * Get the status of all seats in a specific sector for a performance.
   *
   * @param performanceId performance id
   * @param sectorId sector id
   * @returns list of seat statuses
   */
  getSeatsForSector(performanceId: number, sectorId: number): Observable<SeatStatusDto[]> {
    return this.httpClient.get<SeatStatusDto[]>(`${this.performanceBaseUri}/${performanceId}/sectors/${sectorId}/seats`);
  }

  /**
   * Create holds for one or more seats.
   *
   * @param performanceId performance id
   * @param request hold request payload
   * @returns list of created hold IDs
   */
  createHolds(performanceId: number, request: { items: any[] }): Observable<number[]> {
    return this.httpClient.post<number[]>(`${this.holdsBaseUri}/${performanceId}`, request);
  }

  /**
   * Cancel all holds for a specific performance.
   *
   * @param performanceId performance id
   * @returns observable of void
   */
  cancelCheckout(performanceId: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.holdsBaseUri}/cancel-all/${performanceId}`);
  }

  /**
   *  Cancel a reserved ticket by its ID.
   *
   * @param ticketIds  IDs of the tickets to cancel
   * @return          observable of void
   */
  cancelReservedTicket(ticketIds: number[]): Observable<void> {
    return this.httpClient.delete<void>(`${this.bookingBaseUri}/tickets/cancel`, {
      body: {ticketIds}
    });
  }

  purchaseReservedTickets(request: PurchaseRequestWithSeatholdIdsDto): Observable<OrderDto> {
    return this.httpClient.post<OrderDto>(`${this.bookingBaseUri}/purchases/from-reservations`, request);
  }

  /**
   * Cancel one or more purchased tickets from the same purchase.
   *
   * @param ticketIds IDs of tickets to cancel
   * @return observable of void
   */
  cancelPurchasedTickets(ticketIds: number[]): Observable<void> {
    return this.httpClient.post<void>(`${this.bookingBaseUri}/purchases/cancel`, {ticketIds});
  }


  /**
   *  Get all tickets for the current user, including past and future tickets.
   *
   *  @return   list of tickets for the current user
   */
  getAllTickets(): Observable<MyTicketDto[]> {
    return this.httpClient.get<MyTicketDto[]>(`${this.bookingBaseUri}/my-tickets`);
  }
}
