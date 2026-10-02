export  type TicketStatus = 'RESERVED' | 'PURCHASED' | 'CANCELLED';

export interface MyTicketDto {
  orderId: number;
  ticketId: number;
  ticketStatus: TicketStatus;
  previousStatus?: TicketStatus;
  performanceId: number;
  finalTicketPrice: number;
  performanceTitle: string;
  performanceDate: string;
  venueName: string;
  hallName: string;
  sector: string;
  seat: string;
  invoiceId?: number;
}
