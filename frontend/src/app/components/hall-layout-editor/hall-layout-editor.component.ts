import {Component, HostListener, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {HallAreaLayout, HallLayout, SectorLayout} from '../../dtos/hall-layout';
import {HallService} from '../../services/hall.service';
import {
  AreaSnapshot,
  AreaType,
  buildLayoutPayload,
  buildFullSeatGrid,
  canPlaceArea,
  clampPosition,
  createAreaSnapshot,
  createSectorForArea,
  findSeatAtPosition,
  generateSectorName,
  getDefaultAreaColor,
  getDefaultSectorColor,
  getSeatKey,
  getSeatPosition,
  getSectorForArea,
  normalizeLoadedLayout,
  normalizeArea,
  normalizeCapacity,
  reconcileSeatsForArea,
  renumberSeats,
  SectorType
} from './hall-layout-editor.utils';

type AreaInteraction = {
  mode: 'move' | 'resize';
  area: HallAreaLayout;
  startMouseX: number;
  startMouseY: number;
  startArea: AreaSnapshot;
};

@Component({
  selector: 'app-hall-layout-editor',
  templateUrl: './hall-layout-editor.component.html',
  styleUrls: ['./hall-layout-editor.component.scss'],
  standalone: false
})
export class HallLayoutEditorComponent implements OnInit {

  layout?: HallLayout;
  saving = false;
  successMessage = '';
  errorMessages: string[] = [];

  scale = 10;
  canvasZoom = 1;
  seatGridZoom = 1;
  selectedArea?: HallAreaLayout;
  areaInteraction?: AreaInteraction;

  private selectedAreaSnapshot?: AreaSnapshot;

  constructor(private route: ActivatedRoute,
              private hallService: HallService) {
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadLayout(Number(id));
    }
  }

  get selectedSector(): SectorLayout | undefined {
    return this.getSectorForArea(this.selectedArea);
  }

  get selectedSeatCount(): number {
    return this.selectedSector?.type === 'SEATING' ? this.selectedSector.seats.length : 0;
  }

  get isLayoutLocked(): boolean {
    return this.layout?.layoutLocked ?? false;
  }

  get seatGridColumns(): number[] {
    return this.selectedArea?.type === 'SEATING'
      ? Array.from({length: this.selectedArea.width}, (_, index) => index)
      : [];
  }

  get effectiveScale(): number {
    return this.scale * this.canvasZoom;
  }

  get seatGridTemplateColumns(): string {
    return `repeat(${this.seatGridColumns.length}, minmax(${this.getSeatCellSize()}px, 1fr))`;
  }

  get seatGridRows(): number[] {
    return this.selectedArea?.type === 'SEATING'
      ? Array.from({length: this.selectedArea.length}, (_, index) => index)
      : [];
  }

  get occupiedSeatKeys(): Set<string> {
    return new Set((this.selectedSector?.seats ?? []).map(seat => getSeatKey(seat)));
  }

  get seatTitles(): Record<string, string> {
    return Object.fromEntries(
      (this.selectedSector?.seats ?? []).map(seat => [getSeatKey(seat), `Row ${seat.rowNumber}, Seat ${seat.seatNumber}`])
    );
  }

  zoomCanvasIn(): void {
    this.canvasZoom = Math.min(2.5, Number((this.canvasZoom + 0.1).toFixed(2)));
  }

  zoomCanvasOut(): void {
    this.canvasZoom = Math.max(0.2, Number((this.canvasZoom - 0.1).toFixed(2)));
  }

  resetCanvasZoom(): void {
    this.canvasZoom = 1;
  }

  zoomSeatGridIn(): void {
    this.seatGridZoom = Math.min(2.5, Number((this.seatGridZoom + 0.1).toFixed(2)));
  }

  zoomSeatGridOut(): void {
    this.seatGridZoom = Math.max(0.6, Number((this.seatGridZoom - 0.1).toFixed(2)));
  }

  resetSeatGridZoom(): void {
    this.seatGridZoom = 1;
  }

  onCanvasClick(event: MouseEvent): void {
    if (!this.layout || this.isLayoutLocked) {
      return;
    }

    if ((event.target as HTMLElement).closest('.hall-area')) {
      return;
    }

    const canvas = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const defaultSize = 5;
    const area: HallAreaLayout = {
      positionX: clampPosition(Math.round((event.clientX - canvas.left) / this.scale), this.layout.width - defaultSize),
      positionY: clampPosition(Math.round((event.clientY - canvas.top) / this.scale), this.layout.length - defaultSize),
      width: defaultSize,
      length: defaultSize,
      type: 'STANDING',
      sectorId: null,
      sectorName: null
    };

    if (area.type !== 'STAGE') {
      const sector = createSectorForArea(area, this.layout.sectors);
      this.linkAreaToSector(area, sector);
      if (canPlaceArea(this.layout, area)) {
        this.layout.sectors.push(sector);
        this.layout.areas.push(area);
        this.selectArea(area);
      }
      return;
    }

    if (canPlaceArea(this.layout, area)) {
      this.layout.areas.push(area);
      this.selectArea(area);
    }
  }

  selectArea(area: HallAreaLayout, event?: MouseEvent): void {
    event?.stopPropagation();
    this.selectedArea = area;
    this.captureSelectedAreaSnapshot();
    this.reconcileSelectedSectorSeats();
    this.resetSeatGridZoom();
  }

  startAreaDrag(area: HallAreaLayout, event: MouseEvent): void {
    if (this.isLayoutLocked) {
      return;
    }

    if (this.isResizeHandle(event.target)) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.selectArea(area);
    this.areaInteraction = {
      mode: 'move',
      area,
      startMouseX: event.clientX,
      startMouseY: event.clientY,
      startArea: createAreaSnapshot(area)
    };
  }

  startAreaResize(area: HallAreaLayout, event: MouseEvent): void {
    if (this.isLayoutLocked) {
      return;
    }

    event.preventDefault();
    event.stopPropagation();
    this.selectArea(area);
    this.areaInteraction = {
      mode: 'resize',
      area,
      startMouseX: event.clientX,
      startMouseY: event.clientY,
      startArea: createAreaSnapshot(area)
    };
  }

  @HostListener('document:mousemove', ['$event'])
  onDocumentMouseMove(event: MouseEvent): void {
    if (!this.layout || !this.areaInteraction) {
      return;
    }

    event.preventDefault();
    const interaction = this.areaInteraction;
    const deltaX = Math.round((event.clientX - interaction.startMouseX) / this.effectiveScale);
    const deltaY = Math.round((event.clientY - interaction.startMouseY) / this.effectiveScale);
    const previous = createAreaSnapshot(interaction.area);

    if (interaction.mode === 'move') {
      interaction.area.positionX = interaction.startArea.x + deltaX;
      interaction.area.positionY = interaction.startArea.y + deltaY;
    } else {
      interaction.area.width = interaction.startArea.width + deltaX;
      interaction.area.length = interaction.startArea.length + deltaY;
    }

    normalizeArea(interaction.area, this.layout);
    if (!canPlaceArea(this.layout, interaction.area, interaction.area)) {
      interaction.area.positionX = previous.x;
      interaction.area.positionY = previous.y;
      interaction.area.width = previous.width;
      interaction.area.length = previous.length;
      return;
    }
    this.reconcileSelectedSectorSeats(previous);
    this.captureSelectedAreaSnapshot();
  }

  @HostListener('document:mouseup')
  onDocumentMouseUp(): void {
    this.areaInteraction = undefined;
  }

  deleteSelectedArea(): void {
    if (!this.layout || !this.selectedArea || this.isLayoutLocked) {
      return;
    }

    const linkedSector = this.getSectorForArea(this.selectedArea);
    this.layout.areas = this.layout.areas.filter(area => area !== this.selectedArea);
    if (linkedSector) {
      this.layout.sectors = this.layout.sectors.filter(sector => sector !== linkedSector);
    }
    this.selectedArea = undefined;
    this.selectedAreaSnapshot = undefined;
  }

  updateSelectedAreaType(type: AreaType): void {
    if (!this.layout || !this.selectedArea || this.isLayoutLocked) {
      return;
    }

    const previousSector = this.getSectorForArea(this.selectedArea);
    this.selectedArea.type = type;

    if (type === 'STAGE') {
      if (previousSector) {
        this.layout.sectors = this.layout.sectors.filter(sector => sector !== previousSector);
      }
      this.clearAreaSectorLink(this.selectedArea);
      this.captureSelectedAreaSnapshot();
      return;
    }

    if (!previousSector) {
      const sector = createSectorForArea(this.selectedArea, this.layout.sectors);
      this.layout.sectors.push(sector);
      this.linkAreaToSector(this.selectedArea, sector);
      if (sector.type === 'SEATING') {
        sector.seats = buildFullSeatGrid(this.selectedArea);
      }
    } else {
      previousSector.type = type as SectorType;
      previousSector.color = getDefaultSectorColor(previousSector.type);
      previousSector.capacity = previousSector.type === 'STANDING' ? normalizeCapacity(previousSector.capacity) : null;
      previousSector.seats = [];
      this.linkAreaToSector(this.selectedArea, previousSector);
      if (previousSector.type === 'SEATING') {
        previousSector.seats = buildFullSeatGrid(this.selectedArea);
      }
    }

    this.captureSelectedAreaSnapshot();
    this.reconcileSelectedSectorSeats();
  }

  updateSelectedSectorName(name: string): void {
    if (this.isLayoutLocked) {
      return;
    }

    const sector = this.selectedSector;
    if (!sector || !this.selectedArea) {
      return;
    }

    sector.name = name;
    this.selectedArea.sectorName = name;
  }

  normalizeSelectedSector(): void {
    if (this.isLayoutLocked) {
      return;
    }

    const sector = this.selectedSector;
    if (!sector || !this.selectedArea) {
      return;
    }
    
    sector.name = sector.name.trim();
    if (!sector.name) {
      sector.name = generateSectorName(sector.type, this.layout?.sectors ?? []);
    }
    sector.color = sector.color || getDefaultSectorColor(sector.type);
    sector.capacity = sector.type === 'STANDING' ? normalizeCapacity(sector.capacity) : null;
    this.linkAreaToSector(this.selectedArea, sector);
    this.reconcileSelectedSectorSeats();
  }

  normalizeSelectedArea(): void {
    if (!this.layout || !this.selectedArea) {
      return;
    }

    const previous = createAreaSnapshot(this.selectedArea);
    normalizeArea(this.selectedArea, this.layout);
    if (!canPlaceArea(this.layout, this.selectedArea, this.selectedArea)) {
      this.selectedArea.positionX = previous.x;
      this.selectedArea.positionY = previous.y;
      this.selectedArea.width = previous.width;
      this.selectedArea.length = previous.length;
      return;
    }
    this.reconcileSelectedSectorSeats(previous);
    this.captureSelectedAreaSnapshot();
  }

  toggleSeat(gridX: number, gridY: number): void {
    if (this.isLayoutLocked) {
      return;
    }

    const seatingContext = this.getSelectedSeatingContext();
    if (!seatingContext) {
      return;
    }

    const {area, sector} = seatingContext;

    const seatPosition = getSeatPosition(area, gridX, gridY);
    const existingSeat = findSeatAtPosition(sector.seats, seatPosition);
    sector.seats = existingSeat
      ? renumberSeats(sector.seats.filter(seat => seat !== existingSeat))
      : renumberSeats([...sector.seats, {...seatPosition, rowNumber: 0, seatNumber: 0}]);
  }

  getSeatCellSize(): number {
    if (!this.selectedArea || this.selectedArea.type !== 'SEATING') {
      return 24;
    }

    const maxDimension = Math.max(this.selectedArea.width, this.selectedArea.length, 1);
    const base = Math.max(18, Math.min(40, Math.floor(520 / maxDimension)));
    return Math.round(base * this.seatGridZoom);
  }

  getSelectedAreaMaxWidth(): number {
    if (!this.layout || !this.selectedArea) {
      return 1;
    }
    return Math.max(1, this.layout.width - this.selectedArea.positionX);
  }

  getSelectedAreaMaxLength(): number {
    if (!this.layout || !this.selectedArea) {
      return 1;
    }
    return Math.max(1, this.layout.length - this.selectedArea.positionY);
  }

  getAreaColor(area: HallAreaLayout): string {
    return this.getSectorForArea(area)?.color || getDefaultAreaColor(area.type);
  }

  saveLayout(): void {
    if (!this.layout || this.isLayoutLocked) {
      return;
    }

    const payload = buildLayoutPayload(this.layout);

    this.saving = true;
    this.successMessage = '';
    this.errorMessages = [];

    this.hallService.updateLayout(payload.hallId, payload).subscribe({
      next: layout => {
        this.applyLoadedLayout(layout);
        this.calculateScale();
        this.selectedArea = undefined;
        this.selectedAreaSnapshot = undefined;
        this.saving = false;
        this.successMessage = 'Layout saved.';
      },
      error: error => {
        this.saving = false;
        this.errorMessages = this.extractErrorMessages(error);
      }
    });
  }

  private loadLayout(id: number): void {
    this.hallService.getLayout(id).subscribe({
      next: layout => {
        this.applyLoadedLayout(layout);
        this.calculateScale();
      },
      error: error => {
        this.errorMessages = this.extractErrorMessages(error);
      }
    });
  }

  private calculateScale(): void {
    if (!this.layout) {
      return;
    }

    const maxWidth = 1280;
    const maxHeight = 880;
    if (this.layout.width > 0 && this.layout.length > 0) {
      const widthScale = maxWidth / this.layout.width;
      const heightScale = maxHeight / this.layout.length;
      this.scale = Math.max(3, Math.min(12, Math.floor(Math.min(widthScale, heightScale))));
    }
  }

  private extractErrorMessages(error: any): string[] {
    if (error.error?.errors) {
      return error.error.errors;
    }
    if (error.error?.message) {
      return [error.error.message];
    }
    return [error.error ?? 'Could not load hall layout.'];
  }

  private applyLoadedLayout(layout: HallLayout): void {
    this.layout = normalizeLoadedLayout(layout);
  }

  private linkAreaToSector(area: HallAreaLayout, sector: SectorLayout): void {
    area.sectorName = sector.name;
    area.sectorId = sector.id ?? null;
  }

  private clearAreaSectorLink(area: HallAreaLayout): void {
    area.sectorId = null;
    area.sectorName = null;
  }

  private getSectorForArea(area?: HallAreaLayout): SectorLayout | undefined {
    return this.layout ? getSectorForArea(this.layout, area) : undefined;
  }

  private getSelectedSeatingContext(): {area: HallAreaLayout; sector: SectorLayout} | null {
    const area = this.selectedArea;
    const sector = this.selectedSector;

    if (!area || area.type !== 'SEATING' || !sector) {
      return null;
    }

    return {area, sector};
  }

  private captureSelectedAreaSnapshot(): void {
    if (!this.selectedArea) {
      this.selectedAreaSnapshot = undefined;
      return;
    }

    this.selectedAreaSnapshot = createAreaSnapshot(this.selectedArea);
  }

  private reconcileSelectedSectorSeats(previousSnapshot: AreaSnapshot | undefined = this.selectedAreaSnapshot): void {
    const seatingContext = this.getSelectedSeatingContext();
    if (!seatingContext) {
      return;
    }

    const {area, sector} = seatingContext;
    sector.seats = reconcileSeatsForArea(area, sector.seats, previousSnapshot);
  }

  private isResizeHandle(target: EventTarget | null): boolean {
    return target instanceof HTMLElement && !!target.closest('.hall-area-resize-handle');
  }
}
