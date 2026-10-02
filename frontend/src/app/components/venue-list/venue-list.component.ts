import {Component, OnInit} from '@angular/core';
import {Venue} from '../../dtos/venue';
import {VenueService} from '../../services/venue.service';

@Component({
  selector: 'app-venue-list',
  templateUrl: './venue-list.component.html',
  styleUrls: ['./venue-list.component.scss'],
  standalone: false
})
export class VenueListComponent implements OnInit {

  venues: Venue[] = [];
  error = false;
  errorMessage = '';
  currentPage = 1;
  pageSize = 15;

  constructor(private venueService: VenueService) {
  }

  ngOnInit(): void {
    this.loadVenues();
  }

  private loadVenues(): void {
    this.venueService.getVenues().subscribe({
      next: venues => {
        this.venues = venues;
        this.currentPage = 1;
      },
      error: error => {
        this.error = true;
        this.errorMessage = this.extractErrorMessage(error);
      }
    });
  }

  get totalPages(): number {
    return Math.ceil(this.venues.length / this.pageSize);
  }

  get paginatedVenues(): Venue[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.venues.slice(startIndex, startIndex + this.pageSize);
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
    return error.error ?? 'Could not load venues.';
  }
}
