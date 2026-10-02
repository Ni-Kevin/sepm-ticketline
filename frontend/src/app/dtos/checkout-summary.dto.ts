export interface HoldItemDto {
  holdId: number;
  sectorName: string;
  seatName: string | null;
  quantity: number;
  price: number;
}

export interface CheckoutSummarySeatholdDto {
  items: HoldItemDto[];
  totalAmount: number;
  earliestExpiration: string | null;
  artists: string[];
  eventName: string;
  performanceName: string;
  performanceStartTime: string;
  hallName: string;
  venueName: string;
}

export interface TicketItemDto {
  ticketId: number;
  sectorName: string;
  seatName: string | null;
  quantity: number;
  price: number;
}

export interface CheckoutSummaryTicketDto {
  items: TicketItemDto[];
  totalAmount: number;
  earliestExpiration: string | null;
  artists: string[];
  eventName: string;
  performanceName: string;
  performanceStartTime: string;
  hallName: string;
  venueName: string;
}
