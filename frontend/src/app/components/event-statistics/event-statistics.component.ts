import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {EventMonthlySales} from '../../dtos/event-sales-stat';
import {StatisticsService} from '../../services/statistics.service';
import {AuthService} from '../../services/auth.service';
import {EVENT_GENRES} from '../../constants/event-genres';

@Component({
  selector: 'app-event-statistics',
  templateUrl: './event-statistics.component.html',
  styleUrls: ['./event-statistics.component.scss'],
  standalone: false
})
export class EventStatisticsComponent implements OnInit {
  stats: EventMonthlySales | null = null;
  errorMessage = '';
  selectedMonth = this.currentMonthInput();
  selectedGenre = '';
  genres = EVENT_GENRES;

  constructor(private statisticsService: StatisticsService,
              private router: Router,
              private authService: AuthService) {
  }

  ngOnInit(): void {
    this.loadStats();
  }

  get maxSales(): number {
    return Math.max(1, ...(this.stats?.topOverall.map(entry => entry.ticketsSold) ?? [1]));
  }

  goToEvent(eventId: number): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    this.router.navigate(['/events', eventId, 'performances']);
  }

  barWidth(value: number, max: number): string {
    const width = Math.max(8, Math.round((value / Math.max(1, max)) * 100));
    return `${width}%`;
  }

  loadStats(): void {
    this.errorMessage = '';

    const [yearStr, monthStr] = this.selectedMonth.split('-');
    const year = Number(yearStr);
    const month = Number(monthStr);

    const genre = this.selectedGenre.trim();
    const genres = genre ? [genre] : [];

    this.statisticsService.getMonthlyEventSales(year, month, genres).subscribe({
      next: data => {
        this.stats = data;
      },
      error: () => {
        this.errorMessage = 'Could not load monthly event statistics.';
      }
    });
  }

  private currentMonthInput(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }
}
