import {Component, OnInit, TemplateRef} from '@angular/core';
import {NewsService} from '../../services/news.service';
import {News} from '../../dtos/news';
import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-news',
  templateUrl: './news.component.html',
  styleUrls: ['./news.component.scss'],
  standalone: false
})
export class NewsComponent implements OnInit {

  error = false;
  errorMessage = '';
  currentNews: News = {
    title: '',
    summary: ''
  };
  newsEntries: News[] = [];

  constructor(
    private newsService: NewsService,
    private authService: AuthService,
    private modalService: NgbModal
  ) {
  }

  ngOnInit() {
    this.loadNews();
  }

  isAdmin(): boolean {
    return this.authService.getUserRole() === 'ADMIN';
  }

  openExistingNewsModal(id: number | undefined, newsDetailsModal: TemplateRef<any>) {
    if (id === undefined) {
      return;
    }
    this.newsService.getNewsById(id).subscribe({
      next: res => {
        this.currentNews = res;
        this.modalService.open(newsDetailsModal, {ariaLabelledBy: 'news-details-title', size: 'lg'});
      },
      error: err => {
        this.defaultServiceErrorHandling(err);
      }
    });
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

  private loadNews() {
    this.newsService.getNews().subscribe({
      next: (newsEntries: News[]) => {
        this.newsEntries = newsEntries;
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
