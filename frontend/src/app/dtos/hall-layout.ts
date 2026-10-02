export interface SeatLayout {
  id?: number;
  rowNumber: number;
  seatNumber: number;
  positionX: number;
  positionY: number;
}

export interface SectorLayout {
  id?: number;
  name: string;
  type: 'SEATING' | 'STANDING';
  color: string;
  capacity?: number | null;
  seats: SeatLayout[];
}

export interface HallAreaLayout {
  id?: number;
  positionX: number;
  positionY: number;
  width: number;
  length: number;
  type: 'SEATING' | 'STANDING' | 'STAGE';
  sectorId?: number | null;
  sectorName?: string | null;
}

export interface HallLayout {
  hallId: number;
  name: string;
  width: number;
  length: number;
  venueId: number;
  layoutLocked: boolean;
  sectors: SectorLayout[];
  areas: HallAreaLayout[];
}
