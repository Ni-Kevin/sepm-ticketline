import {Component, OnInit} from '@angular/core';
import {ArtistService} from '../../services/artist.service';
import {Artist} from '../../dtos/artist';
import {Router} from '@angular/router';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-artist',
  templateUrl: './artist.component.html',
  styleUrls: ['./artist.component.scss'],
  standalone: false
})
export class ArtistComponent implements OnInit {
  query = '';
  artists: Artist[] = [];
  isLoading = false;
  hasSearched = false;
  errorMessage = '';
  currentPage = 1;
  pageSize = 10;

  constructor(
    private artistService: ArtistService,
    public authService: AuthService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.onSearch('');
  }

  onSearch(query?: string): void {
    const trimmedQuery = (query ?? this.query).trim();
    this.query = trimmedQuery;
    this.hasSearched = true;
    this.errorMessage = '';

    this.isLoading = true;
    this.artistService.searchArtists(trimmedQuery).subscribe({
      next: artists => {
        this.artists = artists;
        this.currentPage = 1;
        this.isLoading = false;
      },
      error: error => {
        this.errorMessage = error?.error?.message ?? 'Artists could not be loaded';
        this.artists = [];
        this.isLoading = false;
      }
    });
  }

  goToPerformances(artist: Artist): void {
    this.router.navigate(['/artists', artist.id, 'performances']);
  }

  get totalPages(): number {
    return Math.ceil(this.artists.length / this.pageSize);
  }

  get paginatedArtists(): Artist[] {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.artists.slice(startIndex, startIndex + this.pageSize);
  }

  onPageChanged(page: number): void {
    this.currentPage = page;
  }
}
