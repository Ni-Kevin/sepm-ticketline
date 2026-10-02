import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';
import {Venue} from '../dtos/venue';

@Injectable({
  providedIn: 'root'
})
export class VenueService {

  private venueBaseUri: string = this.globals.backendUri + '/venues';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  getVenues(): Observable<Venue[]> {
    return this.httpClient.get<Venue[]>(this.venueBaseUri);
  }

  getVenueById(id: number): Observable<Venue> {
    return this.httpClient.get<Venue>(this.venueBaseUri + '/' + id);
  }

  createVenue(venue: Venue): Observable<Venue> {
    return this.httpClient.post<Venue>(this.venueBaseUri, venue);
  }

  updateVenue(id: number, venue: Venue): Observable<Venue> {
    return this.httpClient.put<Venue>(this.venueBaseUri + '/' + id, venue);
  }
}
