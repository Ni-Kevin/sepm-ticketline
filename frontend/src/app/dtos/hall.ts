export interface Hall {
  id?: number;
  name: string;
  width: number;
  length: number;
  venueId: number;
  venueName?: string;
  layoutLocked?: boolean;
  dimensionsLocked?: boolean;
  dimensionsLockReason?: 'PERFORMANCE' | 'AREAS' | null;
}
