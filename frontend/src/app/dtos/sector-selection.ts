export interface SectorSelection {
  sectorId: number;
  name: string;
  type: 'SEATING' | 'STANDING';
  seatIds?: number[];
  quantity?: number;
}
