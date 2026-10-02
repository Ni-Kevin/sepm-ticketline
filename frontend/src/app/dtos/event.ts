export interface Event {
  id?: number;
  title: string;
  genre: string;
  description?: string;
  startTime?: string;
  endTime?: string;
  image?: string;
  performanceIds?: number[];
}

export interface EventPage {
  content: Event[];
  totalPages: number;
  totalElements: number;
}
