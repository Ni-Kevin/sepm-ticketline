import {Component, OnInit} from '@angular/core';
import {AuthService} from '../../services/auth.service';
import {ActivatedRoute} from '@angular/router';
import {Router} from '@angular/router';
import {EventService} from '../../services/event.service';
import {Event} from '../../dtos/event';
import {EVENT_GENRES} from '../../constants/event-genres';

@Component({
  selector: 'app-events',
  templateUrl: './events.component.html',
  styleUrls: ['./events.component.scss'],
  standalone: false
})
export class EventsComponent implements OnInit {

  successMessage = '';
  events: Event[] = [];
  errorMessage = '';
  searchQuery = '';
  genre = '';
  genres = EVENT_GENRES;
  advancedFiltersOpen = false;
  dateFrom = '';
  dateTo = '';
  hallName = '';
  venueStreet = '';
  venueCity = '';
  venueZipCode = '';
  startPriceMin: number | null = null;
  startPriceMax: number | null = null;
  currentPage = 1;
  pageSize = 10;
  totalPageCount = 0;

  constructor(public authService: AuthService,
              private route: ActivatedRoute,
              private router: Router,
              private eventService: EventService) {
  }

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      this.successMessage = params.get('created') === '1' ? 'Event created successfully' : '';
    });
    this.loadEvents();
  }

  isAdmin(): boolean {
    return this.authService.getUserRole() === 'ADMIN';
  }

  private loadEvents(resetPage = true) {
    this.errorMessage = '';
    if (resetPage) {
      this.currentPage = 1;
    }
    this.eventService.getEvents(
      this.searchQuery,
      this.genre,
      this.dateFrom,
      this.dateTo,
      this.hallName,
      this.venueStreet,
      this.venueCity,
      this.venueZipCode,
      this.startPriceMin,
      this.startPriceMax,
      this.currentPage - 1,
      this.pageSize
    ).subscribe({
      next: eventPage => {
        this.events = eventPage.content;
        this.totalPageCount = eventPage.totalPages;
      },
      error: () => {
        this.errorMessage = 'Could not load events';
      }
    });
  }

  get totalPages(): number {
    return this.totalPageCount;
  }

  onPageChanged(page: number): void {
    this.currentPage = page;
    this.loadEvents(false);
  }

  onSearchQueryChanged(query: string): void {
    this.searchQuery = query;
  }

  onSearchTriggered(): void {
    this.loadEvents();
  }

  resetFilters(): void {
    this.genre = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.hallName = '';
    this.venueStreet = '';
    this.venueCity = '';
    this.venueZipCode = '';
    this.startPriceMin = null;
    this.startPriceMax = null;
    this.loadEvents();
  }

  toggleAdvancedFilters(): void {
    this.advancedFiltersOpen = !this.advancedFiltersOpen;
  }

  onBuyTickets(eventId: number | undefined): void {
    if (!eventId) {
      return;
    }

    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    this.router.navigate(['/events', eventId, 'performances']);
  }
}
