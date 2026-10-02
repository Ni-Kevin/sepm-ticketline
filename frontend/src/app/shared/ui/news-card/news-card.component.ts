import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-news-card',
  templateUrl: './news-card.component.html',
  styleUrls: ['./news-card.component.scss'],
  standalone: false
})
export class NewsCardComponent {
  @Input() title!: string;
  @Input() publishedAt!: Date;
  @Input() summary?: string;
  @Input() image?: string | null;
}
