import {HallAreaLayout, HallLayout, SeatLayout, SectorLayout} from '../../dtos/hall-layout';

export type AreaType = HallAreaLayout['type'];
export type SectorType = SectorLayout['type'];
export type AreaSnapshot = {x: number; y: number; width: number; length: number};
export type SeatPosition = {positionX: number; positionY: number};

export function getDefaultAreaColor(type: AreaType): string {
  switch (type) {
    case 'STAGE':
      return '#d97706';
    case 'STANDING':
      return '#0f766e';
    case 'SEATING':
    default:
      return '#2563eb';
  }
}

export function getDefaultSectorColor(type: SectorType): string {
  return type === 'STANDING' ? '#0f766e' : '#2563eb';
}

export function clampPosition(value: number, max: number): number {
  return Math.max(0, Math.min(Number(value) || 0, Math.max(0, max)));
}

function ensureMinimum(value: number): number {
  return Math.max(1, Number(value) || 1);
}

export function clampSize(value: number, max: number): number {
  return Math.max(1, Math.min(ensureMinimum(value), Math.max(1, max)));
}

export function normalizeCapacity(capacity: number | null | undefined): number {
  return Math.max(1, Number(capacity) || 1);
}

export function createAreaSnapshot(area: HallAreaLayout): AreaSnapshot {
  return {
    x: area.positionX,
    y: area.positionY,
    width: area.width,
    length: area.length
  };
}

export function normalizeArea(area: HallAreaLayout, layout: HallLayout): HallAreaLayout {
  area.width = clampSize(area.width, layout.width);
  area.length = clampSize(area.length, layout.length);
  area.positionX = clampPosition(area.positionX, layout.width - area.width);
  area.positionY = clampPosition(area.positionY, layout.length - area.length);
  area.width = clampSize(area.width, layout.width - area.positionX);
  area.length = clampSize(area.length, layout.length - area.positionY);
  return area;
}

export function getSeatPosition(area: HallAreaLayout, gridX: number, gridY: number): SeatPosition {
  return {
    positionX: area.positionX + gridX,
    positionY: area.positionY + gridY
  };
}

export function getSeatKey({positionX, positionY}: SeatPosition): string {
  return `${positionX}-${positionY}`;
}

export function findSeatAtPosition(seats: SeatLayout[], position: SeatPosition): SeatLayout | undefined {
  return seats.find(seat => seat.positionX === position.positionX && seat.positionY === position.positionY);
}

export function isSeatInsideArea(seat: SeatLayout, area: HallAreaLayout): boolean {
  return seat.positionX >= area.positionX
    && seat.positionX < area.positionX + area.width
    && seat.positionY >= area.positionY
    && seat.positionY < area.positionY + area.length;
}

export function renumberSeats(seats: SeatLayout[]): SeatLayout[] {
  const sorted = [...seats].sort((left, right) => left.positionY - right.positionY || left.positionX - right.positionX);
  const rowNumbers = new Map<number, number>();
  let nextRow = 1;
  const seatCountsByRow = new Map<number, number>();

  return sorted.map(seat => {
    let rowNumber = rowNumbers.get(seat.positionY);
    if (!rowNumber) {
      rowNumber = nextRow++;
      rowNumbers.set(seat.positionY, rowNumber);
    }

    const seatNumber = (seatCountsByRow.get(rowNumber) ?? 0) + 1;
    seatCountsByRow.set(rowNumber, seatNumber);

    return {
      ...seat,
      rowNumber,
      seatNumber
    };
  });
}

export function reconcileSeatsForArea(area: HallAreaLayout, seats: SeatLayout[], previousSnapshot?: AreaSnapshot): SeatLayout[] {
  let nextSeats = [...seats];

  if (previousSnapshot) {
    const deltaX = area.positionX - previousSnapshot.x;
    const deltaY = area.positionY - previousSnapshot.y;
    if (deltaX !== 0 || deltaY !== 0) {
      nextSeats = nextSeats.map(seat => ({...seat, positionX: seat.positionX + deltaX, positionY: seat.positionY + deltaY}));
    }
  }

  return renumberSeats(nextSeats.filter(seat => isSeatInsideArea(seat, area)));
}

export function buildFullSeatGrid(area: HallAreaLayout): SeatLayout[] {
  const seats: SeatLayout[] = [];
  for (let y = 0; y < area.length; y++) {
    for (let x = 0; x < area.width; x++) {
      seats.push({
        positionX: area.positionX + x,
        positionY: area.positionY + y,
        rowNumber: 0,
        seatNumber: 0
      });
    }
  }
  return renumberSeats(seats);
}

export function generateSectorName(type: SectorType, sectors: SectorLayout[]): string {
  const prefix = type === 'SEATING' ? 'Seating Sector' : 'Standing Sector';
  const names = new Set(sectors.map(sector => sector.name.toLowerCase()));
  let index = 1;
  while (names.has(`${prefix} ${index}`.toLowerCase())) {
    index++;
  }
  return `${prefix} ${index}`;
}

export function createSectorForArea(area: HallAreaLayout, sectors: SectorLayout[]): SectorLayout {
  const type = area.type as SectorType;
  return {
    name: generateSectorName(type, sectors),
    type,
    color: getDefaultSectorColor(type),
    capacity: type === 'STANDING' ? 1 : null,
    seats: []
  };
}

export function getSectorForArea(layout: HallLayout, area?: HallAreaLayout): SectorLayout | undefined {
  if (!area || area.type === 'STAGE') {
    return undefined;
  }

  if (area.sectorId != null) {
    const byId = layout.sectors.find(sector => sector.id === area.sectorId);
    if (byId) {
      return byId;
    }
  }

  if (area.sectorName) {
    return layout.sectors.find(sector => sector.name.toLowerCase() === area.sectorName?.toLowerCase());
  }

  return undefined;
}

export function overlaps(left: HallAreaLayout, right: HallAreaLayout): boolean {
  return left.positionX < right.positionX + right.width
    && left.positionX + left.width > right.positionX
    && left.positionY < right.positionY + right.length
    && left.positionY + left.length > right.positionY;
}

export function canPlaceArea(layout: HallLayout, area: HallAreaLayout, ignoredArea?: HallAreaLayout): boolean {
  return !layout.areas.some(existing => existing !== ignoredArea && overlaps(existing, area));
}

export function normalizeLoadedLayout(layout: HallLayout): HallLayout {
  return {
    ...layout,
    sectors: (layout.sectors ?? []).map(sector => ({
      ...sector,
      seats: renumberSeats(sector.seats ?? []),
      capacity: sector.type === 'STANDING' ? normalizeCapacity(sector.capacity) : null,
      color: sector.color || getDefaultSectorColor(sector.type)
    })),
    areas: (layout.areas ?? []).map(area => ({
      ...area,
      sectorId: area.type === 'STAGE' ? null : area.sectorId ?? null,
      sectorName: area.type === 'STAGE' ? null : area.sectorName ?? null
    }))
  };
}

export function buildLayoutPayload(layout: HallLayout): HallLayout {
  const sectors = layout.sectors.map(sector => ({
    ...sector,
    name: sector.name.trim(),
    color: sector.color || getDefaultSectorColor(sector.type),
    capacity: sector.type === 'STANDING' ? normalizeCapacity(sector.capacity) : null,
    seats: sector.type === 'SEATING' ? renumberSeats(sector.seats ?? []) : []
  }));

  const payloadLayout = {...layout, sectors};
  const areas = layout.areas.map(area => {
    const normalizedArea: HallAreaLayout = {
      ...area,
      sectorId: null,
      sectorName: null
    };

    normalizeArea(normalizedArea, payloadLayout);

    if (normalizedArea.type !== 'STAGE') {
      const sector = getSectorForArea(payloadLayout, area);
      normalizedArea.sectorName = sector?.name ?? null;
      normalizedArea.sectorId = sector?.id ?? null;
    }

    return normalizedArea;
  });

  return {...payloadLayout, areas};
}
