import {Component, EventEmitter, Input, Output} from '@angular/core';
import {HallAreaLayout} from '../../dtos/hall-layout';

@Component({
  selector: 'app-seat-grid-editor',
  templateUrl: './seat-grid-editor.component.html',
  styleUrls: ['./seat-grid-editor.component.scss'],
  standalone: false
})
export class SeatGridEditorComponent {
  @Input() selectedArea?: HallAreaLayout;
  @Input() selectedSeatCount = 0;
  @Input() seatGridZoom = 1;
  @Input() seatGridRows: number[] = [];
  @Input() seatGridColumns: number[] = [];
  @Input() seatGridTemplateColumns = '';
  @Input() occupiedSeatKeys = new Set<string>();
  @Input() seatTitles: Record<string, string> = {};

  @Output() zoomOut = new EventEmitter<void>();
  @Output() resetZoom = new EventEmitter<void>();
  @Output() zoomIn = new EventEmitter<void>();
  @Output() seatToggled = new EventEmitter<{gridX: number; gridY: number}>();

  getSeatKey(gridX: number, gridY: number): string {
    if (!this.selectedArea) {
      return `${gridX}-${gridY}`;
    }

    return `${this.selectedArea.positionX + gridX}-${this.selectedArea.positionY + gridY}`;
  }

  isSeatPlaced(gridX: number, gridY: number): boolean {
    return this.occupiedSeatKeys.has(this.getSeatKey(gridX, gridY));
  }

  getSeatTitle(gridX: number, gridY: number): string {
    const key = this.getSeatKey(gridX, gridY);
    return this.seatTitles[key] ?? `Row ${gridY + 1}, Column ${gridX + 1}`;
  }
}
