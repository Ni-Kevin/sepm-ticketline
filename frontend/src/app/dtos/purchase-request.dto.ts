export interface PurchaseRequestWithSeatholdIdsDto {
  performanceId: number;
  holdIds: number[];
  paymentMethod: string;
  paymentDetails?: { [key: string]: string };
}

export interface PurchaseRequestWithTicketIdsDto {
  performanceId: number;
  ticketIds: number[];
  paymentMethod: string;
  paymentDetails?: { [key: string]: string };
}

export interface OrderDto {
  id: number;
  purchaseDate: string;
  totalPrice: number;
  paymentMethod: string;
  status: string;
}
