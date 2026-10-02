export interface PerformanceSectorLayoutDto {
  sectorId: number | null;
  name: string;
  type: string;
  color: string;
  positionX: number;
  positionY: number;
  width: number;
  length: number;
  price: number;
  capacity: number,
  soldTickets: number
}

export interface PerformanceHallLayoutDto {
  hallName: string;
  hallWidth: number;
  hallLength: number;
  venueName: string;
  venueStreet: string;
  venueCity: string;
  venueZip: string;
  venueCountry: string;
  sectors: PerformanceSectorLayoutDto[];
}

export interface SeatStatusDto {
  seatId: number;
  rowNumber: number;
  seatNumber: number;
  status: 'AVAILABLE' | 'HELD' | 'SOLD';
  positionX: number,
  positionY: number
}
