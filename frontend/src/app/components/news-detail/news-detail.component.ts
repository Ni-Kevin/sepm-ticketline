import {Component, OnInit} from '@angular/core';
import {News} from "../../dtos/news";
import {ActivatedRoute} from "@angular/router";
import {NewsService} from "../../services/news.service";

@Component({
  selector: 'app-news-detail',
  templateUrl: './news-detail.component.html',
  styleUrl: './news-detail.component.scss',
  standalone: false
})
export class NewsDetailComponent implements OnInit{

  error = false;
  errorMessage = '';
  currentNews: News = { title: '', summary: '' };

  constructor(
    private route: ActivatedRoute,
    private newsService: NewsService
  ) {}

    ngOnInit(): void {
      const idParam = this.route.snapshot.paramMap.get('id');
      if (idParam) {
        const id = +idParam;
        this.loadNewsDetails(id);
      }
    }

  loadNewsDetails(id: number) {
    this.newsService.getNewsById(id).subscribe({
      next: res => {
        this.currentNews = res;
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
    if (!publishedAt) return '';
    const [datePart, timePart = ''] = publishedAt.split('T');
    const [year, month, day] = datePart.split('-');
    const [hours = '00', minutes = '00'] = timePart.split(':');
    if (!year || !month || !day) return publishedAt;
    return `${day}.${month}.${year} ${hours}:${minutes}`;
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
