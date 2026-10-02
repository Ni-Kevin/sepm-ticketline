import {Component, OnInit} from '@angular/core';
import {AuthService} from '../../services/auth.service';
import {Router} from '@angular/router';
import {StatisticsService} from '../../services/statistics.service';
import {NewsService} from '../../services/news.service';
import {EventMonthlySales} from '../../dtos/event-sales-stat';
import {News} from '../../dtos/news';
import {EVENT_GENRES} from '../../constants/event-genres';

@Component({
    selector: 'app-home',
    templateUrl: './home.component.html',
    styleUrls: ['./home.component.scss'],
    standalone: false
})
export class HomeComponent implements OnInit {
  stats: EventMonthlySales | null = null;
  latestNews: News[] = [];
  topEventsInfoMessage = '';
  newsInfoMessage = '';
  selectedMonth = this.currentMonthInput();
  selectedGenre = '';
  genres = EVENT_GENRES;

  constructor(
    public authService: AuthService,
    private statisticsService: StatisticsService,
    private newsService: NewsService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.loadTopEvents();

    if (!this.authService.isLoggedIn()) {
      this.newsInfoMessage = 'Log in to see the latest news.';
      return;
    }

    this.loadLatestNews();
  }

  get maxSales(): number {
    return Math.max(1, ...(this.stats?.topOverall.map(entry => entry.ticketsSold) ?? [1]));
  }

  onMonthChange(): void {
    this.loadTopEvents();
  }

  onGenreChange(): void {
    this.loadTopEvents();
  }

  goToEvent(eventId: number): void {
    this.router.navigate(['/events', eventId, 'performances']);
  }

  goToNews(newsId?: number): void {
    if (!this.authService.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }

    if (newsId == null) {
      this.router.navigate(['/news']);
      return;
    }

    this.router.navigate(['/news', newsId]);
  }

  imageSrc(news: News): string | null {
    return news.image ? 'data:image/*;base64,' + news.image : null;
  }

  formatPublishedAt(publishedAt?: string): string {
    if (!publishedAt) {
      return '';
    }

    const date = new Date(publishedAt);
    if (Number.isNaN(date.getTime())) {
      return publishedAt;
    }

    return date.toLocaleDateString('de-AT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  barWidth(value: number, max: number): string {
    const width = Math.max(8, Math.round((value / Math.max(1, max)) * 100));
    return `${width}%`;
  }

  private loadTopEvents(): void {
    const [yearStr, monthStr] = this.selectedMonth.split('-');
    const year = Number(yearStr);
    const month = Number(monthStr);
    const genre = this.selectedGenre.trim();
    const genres = genre ? [genre] : [];

    this.statisticsService.getMonthlyEventSales(year, month, genres).subscribe({
      next: stats => {
        this.stats = stats;
        this.topEventsInfoMessage = '';
      },
      error: () => {
        this.stats = null;
        this.topEventsInfoMessage = 'Top 10 events are currently unavailable.';
      }
    });
  }

  private loadLatestNews(): void {
    this.newsService.getNews().subscribe({
      next: newsEntries => {
        this.latestNews = [...newsEntries]
          .sort((a, b) => this.newsTimestamp(b) - this.newsTimestamp(a))
          .slice(0, 5);
      },
      error: () => {
        this.newsInfoMessage = 'News are currently unavailable.';
      }
    });
  }

  private newsTimestamp(news: News): number {
    return news.publishedAt ? new Date(news.publishedAt).getTime() : 0;
  }

  private currentMonthInput(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

}
