export interface PerformanceSectorPrice {
  sectorId: number;
  sectorName: string;
  sectorType: 'SEATING' | 'STANDING';
  price: number;
}

export interface Performance {
  id?: number;
  startTime: string;
  endTime?: string;
  durationHours?: number;
  durationMinutes?: number;
  performanceName: string;
  genre?: string;
  startPrice: number;
  hallId: number;
  hallName?: string;
  artistIds: number[];
  artistNames?: string[];
  sectorPrices?: PerformanceSectorPrice[];
}
