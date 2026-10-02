import {Component, EventEmitter, Input, OnChanges, Output, SimpleChanges} from '@angular/core';
import {HallAreaLayout, HallLayout, SeatLayout} from '../../dtos/hall-layout';

@Component({
  selector: 'app-hall-layout-preview',
  templateUrl: './hall-layout-preview.component.html',
  styleUrls: ['./hall-layout-preview.component.scss'],
  standalone: false
})
export class HallLayoutPreviewComponent implements OnChanges {
  @Input() layout: HallLayout | null = null;
  @Input() selectedArea?: HallAreaLayout;
  @Input() interactive = false;
  @Input() selectable = false;
  @Input() showZoomControls = true;
  @Input() baseScale = 10;
  @Input() externalScale = 10;
  @Input() fixedHeightPx = 520;
  @Input() stageDefaultColor = '#334155';
  @Input() standingDefaultColor = '#7c3aed';
  @Input() seatingDefaultColor = '#2563eb';
  @Input() defaultCanvasZoom = 1;

  @Output() canvasClicked = new EventEmitter<MouseEvent>();
  @Output() areaSelected = new EventEmitter<{area: HallAreaLayout; event: MouseEvent}>();
  @Output() areaDragStarted = new EventEmitter<{area: HallAreaLayout; event: MouseEvent}>();
  @Output() areaResizeStarted = new EventEmitter<{area: HallAreaLayout; event: MouseEvent}>();

  canvasZoom = 1;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['defaultCanvasZoom']) {
      this.canvasZoom = this.clampZoom(this.defaultCanvasZoom);
    }
  }

  get renderScale(): number {
    return this.showZoomControls ? this.baseScale * this.canvasZoom : this.externalScale;
  }

  zoomCanvasIn(): void {
    this.canvasZoom = this.clampZoom(Number((this.canvasZoom + 0.1).toFixed(2)));
  }

  zoomCanvasOut(): void {
    this.canvasZoom = this.clampZoom(Number((this.canvasZoom - 0.1).toFixed(2)));
  }

  resetCanvasZoom(): void {
    this.canvasZoom = this.clampZoom(this.defaultCanvasZoom);
  }

  onCanvasClick(event: MouseEvent): void {
    this.canvasClicked.emit(event);
  }

  onAreaSelect(area: HallAreaLayout, event: MouseEvent): void {
    this.areaSelected.emit({area, event});
  }

  onAreaDragStart(area: HallAreaLayout, event: MouseEvent): void {
    this.areaDragStarted.emit({area, event});
  }

  onAreaResizeStart(area: HallAreaLayout, event: MouseEvent): void {
    event.stopPropagation();
    this.areaResizeStarted.emit({area, event});
  }

  getAreaColor(area: HallAreaLayout): string {
    if (area.type === 'STAGE') {
      return '#334155';
    }
    return this.getSectorForArea(area)?.color || this.getDefaultAreaColor(area.type);
  }

  getAreaLabel(area: HallAreaLayout): string {
    if (area.type === 'STAGE') {
      return 'Stage';
    }
    return this.getSectorForArea(area)?.name?.trim() || this.getAreaTypeLabel(area.type);
  }

  getAreaBadge(area: HallAreaLayout): string {
    if (area.type === 'STAGE') {
      return 'Stage';
    }
    return this.isAreaUnassigned(area) ? 'Missing sector' : this.getAreaTypeLabel(area.type);
  }

  getAreaSeats(area: HallAreaLayout): SeatLayout[] {
    return this.getSectorForArea(area)?.seats ?? [];
  }

  isAreaUnassigned(area: HallAreaLayout): boolean {
    return area.type !== 'STAGE' && !this.getSectorForArea(area);
  }

  private getAreaTypeLabel(type: HallAreaLayout['type']): string {
    switch (type) {
      case 'STAGE':
        return 'Stage';
      case 'STANDING':
        return 'Standing';
      case 'SEATING':
      default:
        return 'Seating';
    }
  }

  private getSectorForArea(area?: HallAreaLayout): HallLayout['sectors'][number] | undefined {
    if (!this.layout || !area) {
      return undefined;
    }

    if (area.sectorId != null) {
      const byId = this.layout.sectors.find(sector => sector.id === area.sectorId);
      if (byId) {
        return byId;
      }
    }

    if (area.sectorName) {
      return this.layout.sectors.find(sector => sector.name.toLowerCase() === area.sectorName?.toLowerCase());
    }

    return undefined;
  }

  private getDefaultAreaColor(type: HallAreaLayout['type']): string {
    switch (type) {
      case 'STAGE':
        return this.stageDefaultColor;
      case 'STANDING':
        return this.standingDefaultColor;
      case 'SEATING':
      default:
        return this.seatingDefaultColor;
    }
  }

  private clampZoom(zoom: number): number {
    return Math.min(2.5, Math.max(0.2, Number(zoom) || 1));
  }
}
