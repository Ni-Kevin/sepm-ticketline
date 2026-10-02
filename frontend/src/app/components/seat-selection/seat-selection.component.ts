import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {BookingService} from '../../services/booking.service';
import {PerformanceHallLayoutDto, PerformanceSectorLayoutDto, SeatStatusDto} from '../../dtos/performance-layout.dto';
import {SectorSelection} from "../../dtos/sector-selection";

@Component({
  selector: 'app-seat-selection',
  templateUrl: './seat-selection.component.html',
  styleUrls: ['./seat-selection.component.scss'],
  standalone: false
})
export class SeatSelectionComponent implements OnInit {

  viewMode: 'HALL' | 'SECTOR' = 'HALL';
  performanceId!: number;
  hallLayout?: PerformanceHallLayoutDto;
  selectedSector?: PerformanceSectorLayoutDto;

  selections = new Map<number, SectorSelection>();
  sectorSeats: SeatStatusDto[] = [];

  loading = false;
  errorMessage = '';

  constructor(private route: ActivatedRoute,
              private router: Router,
              private bookingService: BookingService) {
  }

  ngOnInit() {
    this.performanceId = Number(this.route.snapshot.paramMap.get('performanceId'));
    if (isNaN(this.performanceId)) {
      this.errorMessage = 'Invalid performance ID';
      return;
    }
    this.loadHallLayout();
  }

  private loadHallLayout() {
    this.loading = true;
    this.bookingService.getHallLayout(this.performanceId).subscribe({
      next: layout => {
        this.hallLayout = layout;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load hall layout';
        this.loading = false;
      }
    });
  }

  selectSector(sector: PerformanceSectorLayoutDto) {
    if (sector.sectorId === null || sector.type === 'STAGE') {
      return;
    }
    this.selectedSector = sector;
    this.viewMode = 'SECTOR';

    if (this.selectedSector.type === 'SEATING') {
      this.loadSeatsForSector(sector.sectorId);
    }
  }

  private loadSeatsForSector(sectorId: number) {
    this.loading = true;
    this.bookingService.getSeatsForSector(this.performanceId, sectorId).subscribe({
      next: seats => {
        this.sectorSeats = seats;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Could not load seats for this sector';
        this.loading = false;
      }
    });
  }

  get gridColumns(): number {
    if (!this.sectorSeats || this.sectorSeats.length === 0) return 1;
    const minX = Math.min(...this.sectorSeats.map(s => s.positionX));
    const maxX = Math.max(...this.sectorSeats.map(s => s.positionX));
    return maxX - minX + 1;
  }

  get gridRows(): number {
    if (!this.sectorSeats || this.sectorSeats.length === 0) return 1;
    const minY = Math.min(...this.sectorSeats.map(s => s.positionY));
    const maxY = Math.max(...this.sectorSeats.map(s => s.positionY));
    return maxY - minY + 1;
  }

  getRelativeX(absoluteX: number) {
    const minX = Math.min(...this.sectorSeats.map(s => s.positionX));
    return absoluteX - minX + 1;
  }

  getRelativeY(absoluteY: number) {
    const minY = Math.min(...this.sectorSeats.map(s => s.positionY));
    return absoluteY - minY + 1;
  }

  toggleSeat(seatId: number) {
    if (!this.selectedSector || this.selectedSector.sectorId === null) return;

    const sectorId = this.selectedSector.sectorId;
    const selection = this.selections.get(sectorId);

    if (this.selectedSector.type === 'SEATING') {
      if (selection) {
        const index = selection.seatIds?.indexOf(seatId) ?? -1;
        if (index > -1) {
          selection.seatIds?.splice(index, 1);
        } else {
          selection.seatIds?.push(seatId);
        }
        if (selection.seatIds?.length === 0) {
          this.selections.delete(sectorId);
        }
      } else {
        this.selections.set(sectorId, {
          sectorId: sectorId,
          name: this.selectedSector.name,
          type: 'SEATING',
          seatIds: [seatId],
          quantity: 1
        });
      }
    }
  }

  get currentStandingQuantity(): number {
    if (!this.selectedSector || this.selectedSector.sectorId === null) return 0;
    return this.selections.get(this.selectedSector.sectorId)?.quantity ?? 0;
  }

  set currentStandingQuantity(value: number) {
    const sanitizedValue = value < 0 ? 0 : value;
    this.updateStandingQuantity(sanitizedValue);
  }

  updateStandingQuantity(quantity: number) {
    if (!this.selectedSector || this.selectedSector.sectorId === null) return;

    const sectorId = this.selectedSector.sectorId;

    if (this.selectedSector.type === 'STANDING') {
      if (quantity > 0) {
        this.selections.set(sectorId, {
          sectorId: sectorId,
          name: this.selectedSector.name,
          type: 'STANDING',
          quantity: quantity
        });
      } else {
        this.selections.delete(sectorId);
      }
    }
  }

  removeFromSector(sectorId: number) {
    this.selections.delete(sectorId);
  }

  isSeatSelected(seatId: number): boolean {
    if (!this.selectedSector || this.selectedSector.sectorId === null) return false;
    const selection = this.selections.get(this.selectedSector.sectorId);
    return selection?.seatIds?.includes(seatId) ?? false;
  }

  goBackToHall() {
    this.viewMode = 'HALL';
    this.selectedSector = undefined;
  }

  createSeatHoldAndGoToCheckout(mode: string) {
    if (this.selections.size === 0) {
      this.errorMessage = 'Please select at least one seat or ticket';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    const items: any[] = [];
    this.selections.forEach((selection) => {
      if (selection.type === 'SEATING' && selection.seatIds) {
        selection.seatIds.forEach(seatId => {
          items.push({ sectorId: selection.sectorId, seatId: seatId, quantity: 1 });
        });
      } else if (selection.type === 'STANDING' && selection.quantity) {
        items.push({ sectorId: selection.sectorId, seatId: null, quantity: selection.quantity });
      }
    });
    this.bookingService.createHolds(this.performanceId, { items }).subscribe({
      next: () => this.router.navigate([`/checkout/${mode}`, this.performanceId]),
      error: error => {
        this.loading = false;
        if (typeof error?.error === 'string') { this.errorMessage = error.error; return; }
        if (error?.error?.errors && Array.isArray(error.error.errors)) { this.errorMessage = error.error.errors.join(','); return; }
        this.errorMessage = error?.error?.message ?? error?.error?.detail ?? 'Reservation failed';
      }
    });
  }
}
