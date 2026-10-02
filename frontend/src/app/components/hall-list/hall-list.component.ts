import {Component, OnInit} from '@angular/core';
import {Hall} from '../../dtos/hall';
import {Venue} from '../../dtos/venue';
import {HallService} from '../../services/hall.service';
import {VenueService} from '../../services/venue.service';

@Component({
  selector: 'app-hall-list',
  templateUrl: './hall-list.component.html',
  styleUrls: ['./hall-list.component.scss'],
  standalone: false
})
export class HallListComponent implements OnInit {

  halls: Hall[] = [];
  venues: Venue[] = [];
  selectedVenueId?: number;
  error = false;
  errorMessage = '';
  currentPage = 1;
  pageSize = 15;

  constructor(private hallService: HallService,
              private venueService: VenueService) {
  }

  ngOnInit(): void {
    this.loadVenues();
    this.loadHalls();
  }

  onVenueFilterChange(value: string): void {
    this.selectedVenueId = value ? Number(value) : undefined;
    this.currentPage = 1;
    this.loadHalls();
  }

  private loadVenues(): void {
    this.venueService.getVenues().subscribe({
      next: venues => this.venues = venues,
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  private loadHalls(): void {
    this.hallService.getHalls(this.selectedVenueId).subscribe({
      next: halls => {
        this.halls = halls;
        this.currentPage = 1;
      },
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  get totalPages(): number {
    return Math.ceil(this.halls.length / this.pageSize);
  }

  get paginatedHalls(): Hall[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.halls.slice(startIndex, startIndex + this.pageSize);
  }

  onPageChanged(page: number): void {
    this.currentPage = page;
  }

  private extractErrorMessage(error: any): string {
    if (error.error?.errors) {
      return error.error.errors.join(', ');
    }
    if (error.error?.message) {
      return error.error.message;
    }
    return error.error ?? 'Could not load halls.';
  }
}
