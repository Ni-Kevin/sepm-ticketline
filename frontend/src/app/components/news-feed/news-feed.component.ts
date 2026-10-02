import {Component, OnInit, TemplateRef} from "@angular/core";
import {AuthService} from '../../services/auth.service';
import {News} from "../../dtos/news";
import {NewsService} from "../../services/news.service";

@Component({
  selector: 'app-news-feed',
  templateUrl: './news-feed.component.html',
  styleUrl: './news-feed.component.scss',
  standalone: false
})
export class NewsFeedComponent implements OnInit {

  error = false;
  errorMessage = '';
  newsEntries: News[] = [];
  readNewsEntries: News[] = [];
  showRead = false;
  unreadCurrentPage = 1;
  readCurrentPage = 1;
  pageSize = 10;
  unreadTotalPageCount = 0;
  readTotalPageCount = 0;

  constructor(public authService: AuthService,
              private newsService: NewsService) {
  }
  ngOnInit() {
    this.loadUnreadNews();
  }


  toggleShowRead() {
    this.showRead = !this.showRead;
    this.readCurrentPage = 1;
    if (this.showRead) {
      this.loadReadNews();
    }
  }

  isAdmin(): boolean {
    return this.authService.getUserRole() === 'ADMIN';
  }


  imageSrc(news: News): string | null {
    return news.image ? 'data:image/*;base64,' + news.image : null;
  }

  formatPublishedAt(publishedAt?: string): string {
    if (!publishedAt) {
      return '';
    }

    const [datePart, timePart = ''] = publishedAt.split('T');
    const [year, month, day] = datePart.split('-');
    const [hours = '00', minutes = '00'] = timePart.split(':');

    if (!year || !month || !day) {
      return publishedAt;
    }

    return `${day}.${month}.${year} ${hours}:${minutes}`;
  }

  vanishError() {
    this.error = false;
  }

  get unreadTotalPages(): number {
    return this.unreadTotalPageCount;
  }

  get readTotalPages(): number {
    return this.readTotalPageCount;
  }

  onUnreadPageChanged(page: number): void {
    this.unreadCurrentPage = page;
    this.loadUnreadNews(false);
  }

  onReadPageChanged(page: number): void {
    this.readCurrentPage = page;
    this.loadReadNews(false);
  }

  private loadUnreadNews(resetPage = true) {
    if (resetPage) {
      this.unreadCurrentPage = 1;
    }

    this.newsService.getUnreadNews(this.unreadCurrentPage - 1, this.pageSize).subscribe({
      next: newsPage => {
        this.newsEntries = newsPage.content;
        this.unreadTotalPageCount = newsPage.totalPages;
      },
      error: error => {
        this.defaultServiceErrorHandling(error);
      }
    });
  }


  private loadReadNews(resetPage = true) {
    if (resetPage) {
      this.readCurrentPage = 1;
    }

    this.newsService.getReadNews(this.readCurrentPage - 1, this.pageSize).subscribe({
      next: newsPage => {
        this.readNewsEntries = newsPage.content;
        this.readTotalPageCount = newsPage.totalPages;
      },
      error: error => {
        this.defaultServiceErrorHandling(error);
      }
    });
  }

  private defaultServiceErrorHandling(error: any) {
    this.error = true;
    if (typeof error.error === 'object') {
      this.errorMessage = error.error.error;
    } else {
      this.errorMessage = error.error;
    }
  }

}
