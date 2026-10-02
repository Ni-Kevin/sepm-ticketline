import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Performance} from '../../dtos/performance';
import {PerformanceService} from '../../services/performance.service';
import {ArtistService} from "../../services/artist.service";
import {EVENT_GENRES} from '../../constants/event-genres';

@Component({
  selector: 'app-performances',
  templateUrl: './performances.component.html',
  styleUrls: ['./performances.component.scss'],
  standalone: false
})
export class PerformancesComponent implements OnInit {
  allPerformances: Performance[] = [];
  performances: Performance[] = [];
  errorMessage = '';
  searchQuery = '';
  genre = '';
  genres = EVENT_GENRES;
  advancedFiltersOpen = false;
  startTimeFrom = '';
  endTimeTo = '';
  startPriceMin: number | null = null;
  startPriceMax: number | null = null;
  hallName = '';
  currentPage = 1;
  pageSize = 10;
  artistId: number | null = null;
  artistName: string | null = null;

  constructor(private performanceService: PerformanceService,
              private route: ActivatedRoute,
              private artistService: ArtistService,
              private router: Router) {
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('artistId');
    this.artistId = idParam ? Number(idParam) : null;

    if (this.artistId) {
      this.artistService.getArtistById(this.artistId).subscribe({
        next: artist => this.artistName = artist.artistName,
        error: () => this.artistName = null
      });
    }

    this.loadPerformances();
  }

  private loadPerformances(): void {
    this.errorMessage = '';

    const request = this.artistId
      ? this.performanceService.getPerformancesByArtist(this.artistId)
      : this.performanceService.getPerformances();

    request.subscribe({
      next: performances => {
        this.allPerformances = performances;
        this.applyFilters();
      },
      error: () => {
        this.errorMessage = 'Could not load performances';
      }
    });
  }

  onSearchQueryChanged(query: string): void {
    this.searchQuery = query;
  }

  onSearchTriggered(): void {
    this.applyFilters();
  }

  get totalPages(): number {
    return Math.ceil(this.performances.length / this.pageSize);
  }

  get paginatedPerformances(): Performance[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.performances.slice(startIndex, startIndex + this.pageSize);
  }

  onPageChanged(page: number): void {
    this.currentPage = page;
  }

  toggleAdvancedFilters(): void {
    this.advancedFiltersOpen = !this.advancedFiltersOpen;
  }

  resetAdvancedFilters(): void {
    this.genre = '';
    this.startTimeFrom = '';
    this.endTimeTo = '';
    this.startPriceMin = null;
    this.startPriceMax = null;
    this.hallName = '';
    this.applyFilters();
  }

  private applyFilters(): void {
    this.currentPage = 1;
    const query = this.normalize(this.searchQuery);
    const genre = this.normalize(this.genre);
    const hallName = this.normalize(this.hallName);

    this.performances = this.allPerformances.filter(performance => {
      if (this.shouldHidePastPerformances() && this.hasPerformanceStarted(performance)) {
        return false;
      }

      if (query && !this.normalize(performance.performanceName).includes(query)) {
        return false;
      }

      if (genre && this.normalize(performance.genre) !== genre) {
        return false;
      }

      if (!this.isDateInRange(performance.startTime, this.startTimeFrom, '')) {
        return false;
      }

      if (!this.isDateInRange(performance.endTime, '', this.endTimeTo)) {
        return false;
      }

      if (this.startPriceMin !== null && performance.startPrice < this.startPriceMin) {
        return false;
      }

      if (this.startPriceMax !== null && performance.startPrice > this.startPriceMax) {
        return false;
      }

      if (hallName && !this.normalize(performance.hallName).includes(hallName)) {
        return false;
      }

      return true;
    });
  }

  private shouldHidePastPerformances(): boolean {
    return !this.startTimeFrom && !this.endTimeTo;
  }

  private hasPerformanceStarted(performance: Performance): boolean {
    const startTime = new Date(performance.startTime).getTime();
    return Number.isFinite(startTime) && startTime < Date.now();
  }

  private isDateInRange(value: string | undefined, from: string, to: string): boolean {
    if (!from && !to) {
      return true;
    }

    const time = value ? new Date(value).getTime() : NaN;
    if (!Number.isFinite(time)) {
      return false;
    }

    const fromTime = from ? new Date(from).getTime() : NaN;
    if (Number.isFinite(fromTime) && time < fromTime) {
      return false;
    }

    const toTime = to ? new Date(to).getTime() : NaN;
    return !Number.isFinite(toTime) || time <= toTime;
  }

  private normalize(value: string | undefined): string {
    return (value ?? '').trim().toLowerCase();
  }

  isPerformanceStarted(performance: Performance): boolean {
    const startTime = new Date(performance.startTime).getTime();
    return Number.isFinite(startTime) && startTime <= Date.now();
  }

  selectSeats(performance: Performance): void {
    if (this.isPerformanceStarted(performance)) {
      return;
    }

    if (performance.id) {
      this.router.navigate(['/seat-selection', performance.id]);
    }
  }
}
