import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';
import {Artist} from '../dtos/artist';

@Injectable({
  providedIn: 'root'
})
/**
 * Service for artist API requests.
 */
export class ArtistService {

  private artistBaseUri: string = this.globals.backendUri + '/artists';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  /**
   * Create a new artist.
   *
   * @param artist artist payload
   * @returns created artist response
   */
  createArtist(artist: Artist): Observable<Artist> {
    return this.httpClient.post<Artist>(this.artistBaseUri, artist);
  }

  /**
   * Search artists by query string.
   *
   * @param query search term
   * @returns matching artists
   */
  searchArtists(query: string): Observable<Artist[]> {
    return this.httpClient.get<Artist[]>(`${this.artistBaseUri}?query=${encodeURIComponent(query)}`);
  }

  getArtistById(id: number): Observable<Artist> {
    return this.httpClient.get<Artist>(`${this.artistBaseUri}/${id}`);
  }
}
