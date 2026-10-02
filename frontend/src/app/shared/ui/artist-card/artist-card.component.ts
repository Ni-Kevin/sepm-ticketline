import {Component, Input} from '@angular/core';
import {Artist} from '../../../dtos/artist';

@Component({
  selector: 'app-artist-card',
  templateUrl: './artist-card.component.html',
  styleUrls: ['./artist-card.component.scss'],
  standalone: false
})
export class ArtistCardComponent {
  @Input() artist!: Artist;

  displayName(): string {
    return this.artist.artistName?.trim() || `${this.artist.firstName} ${this.artist.lastName}`.trim();
  }

  fullName(): string {
    return `${this.artist.firstName} ${this.artist.lastName}`.trim();
  }

  initials(): string {
    const parts = this.displayName().split(/\s+/).filter(Boolean);
    return parts.slice(0, 2).map(part => part[0]?.toUpperCase() ?? '').join('');
  }

  showFullName(): boolean {
    return this.displayName().toLocaleLowerCase() !== this.fullName().toLocaleLowerCase();
  }
}
