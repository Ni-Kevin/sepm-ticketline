import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';
import {Hall} from '../dtos/hall';
import {HallLayout} from '../dtos/hall-layout';

@Injectable({
  providedIn: 'root'
})
export class HallService {

  private hallBaseUri: string = this.globals.backendUri + '/halls';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  getHalls(venueId?: number): Observable<Hall[]> {
    let params = new HttpParams();
    if (venueId !== undefined && venueId !== null) {
      params = params.set('venueId', venueId);
    }
    return this.httpClient.get<Hall[]>(this.hallBaseUri, {params});
  }

  getHallById(id: number): Observable<Hall> {
    return this.httpClient.get<Hall>(this.hallBaseUri + '/' + id);
  }

  createHall(hall: Hall): Observable<Hall> {
    return this.httpClient.post<Hall>(this.hallBaseUri, hall);
  }

  updateHall(id: number, hall: Hall): Observable<Hall> {
    return this.httpClient.put<Hall>(this.hallBaseUri + '/' + id, hall);
  }

  getLayout(id: number): Observable<HallLayout> {
    return this.httpClient.get<HallLayout>(this.hallBaseUri + '/' + id + '/layout');
  }

  updateLayout(id: number, layout: HallLayout): Observable<HallLayout> {
    return this.httpClient.put<HallLayout>(this.hallBaseUri + '/' + id + '/layout', layout);
  }
}
