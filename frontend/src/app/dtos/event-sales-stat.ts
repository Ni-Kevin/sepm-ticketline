export interface EventSalesStat {
  eventId: number;
  eventTitle: string;
  genre: string;
  ticketsSold: number;
}

export interface EventMonthlySales {
  year: number;
  month: number;
  topOverall: EventSalesStat[];
  topByGenre: Record<string, EventSalesStat[]>;
}
