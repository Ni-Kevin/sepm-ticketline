import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable, of} from 'rxjs';
import {Globals} from '../global/globals';
import {Performance} from '../dtos/performance';

@Injectable({
  providedIn: 'root'
})
/**
 * Service for performance API requests.
 */
export class PerformanceService {

  private performanceBaseUri: string = this.globals.backendUri + '/performances';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  /**
   * Create a new performance.
   *
   * @param performance performance payload
   * @returns created performance response
   */
  createPerformance(performance: Performance): Observable<Performance> {
    return this.httpClient.post<Performance>(this.performanceBaseUri, performance);
  }

  getPerformances(): Observable<Performance[]> {
    return this.httpClient.get<Performance[]>(this.performanceBaseUri);
  }

  getPerformancesByIds(ids: number[]): Observable<Performance[]> {
    if (ids.length === 0) {
      return of([]);
    }

    return this.httpClient.get<Performance[]>(this.performanceBaseUri, {
      params: {ids: ids.map(id => String(id))}
    });
  }

  /**
   * Delete a performance by id.
   *
   * @param id performance id
   * @returns empty response body
   */
  deletePerformance(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${this.performanceBaseUri}/${id}`);
  }

  getPerformancesByArtist(artistId: number): Observable<Performance[]> {
    return this.httpClient.get<Performance[]>(`${this.performanceBaseUri}/by-artist/${artistId}`);
  }
}
