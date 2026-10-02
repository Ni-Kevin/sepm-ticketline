import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';
import {Event, EventPage} from '../dtos/event';

@Injectable({
  providedIn: 'root'
})
/**
 * Service for event API requests.
 */
export class EventService {

  private eventBaseUri: string = this.globals.backendUri + '/events';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  /**
   * Create a new event.
   *
   * @param event event payload
   * @param imageFile optional image file
   * @returns created event response
   */
  createEvent(event: Event, imageFile: File | null): Observable<Event> {
    const formData = new FormData();
    formData.append('event', new Blob([JSON.stringify(event)], {type: 'application/json'}));
    if (imageFile) {
      formData.append('image', imageFile);
    }
    return this.httpClient.post<Event>(this.eventBaseUri, formData);
  }

  getEventById(id: number): Observable<Event> {
    return this.httpClient.get<Event>(`${this.eventBaseUri}/${id}`);
  }

  /**
   * Load all events.
   *
   * @returns list of events
   */
  getEvents(
    query?: string,
    genre?: string,
    dateFrom?: string,
    dateTo?: string,
    hallName?: string,
    venueStreet?: string,
    venueCity?: string,
    venueZipCode?: string,
    startPriceMin?: number | null,
    startPriceMax?: number | null,
    page?: number,
    size?: number
  ): Observable<EventPage> {
    let params = new HttpParams();
    const normalizedQuery = query?.trim() ?? '';
    const normalizedGenre = genre?.trim() ?? '';
    const normalizedDateFrom = dateFrom?.trim() ?? '';
    const normalizedDateTo = dateTo?.trim() ?? '';
    const normalizedHallName = hallName?.trim() ?? '';
    const normalizedVenueStreet = venueStreet?.trim() ?? '';
    const normalizedVenueCity = venueCity?.trim() ?? '';
    const normalizedVenueZipCode = venueZipCode?.trim() ?? '';
    if (normalizedQuery) {
      params = params.set('q', normalizedQuery);
    }
    if (normalizedGenre) {
      params = params.set('genre', normalizedGenre);
    }
    if (normalizedDateFrom) {
      params = params.set('dateFrom', normalizedDateFrom);
    }
    if (normalizedDateTo) {
      params = params.set('dateTo', normalizedDateTo);
    }
    if (normalizedHallName) {
      params = params.set('hallName', normalizedHallName);
    }
    if (normalizedVenueStreet) {
      params = params.set('venueStreet', normalizedVenueStreet);
    }
    if (normalizedVenueCity) {
      params = params.set('venueCity', normalizedVenueCity);
    }
    if (normalizedVenueZipCode) {
      params = params.set('venueZipCode', normalizedVenueZipCode);
    }
    if (startPriceMin != null && Number.isFinite(startPriceMin) && startPriceMin >= 0) {
      params = params.set('startPriceMin', startPriceMin);
    }
    if (startPriceMax != null && Number.isFinite(startPriceMax) && startPriceMax >= 0) {
      params = params.set('startPriceMax', startPriceMax);
    }
    if (page != null) {
      params = params.set('page', page);
    }
    if (size != null) {
      params = params.set('size', size);
    }
    return this.httpClient.get<EventPage>(this.eventBaseUri, {params});
  }
}
