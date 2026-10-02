import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';
import {EventMonthlySales} from '../dtos/event-sales-stat';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  getMonthlyEventSales(year?: number, month?: number, genres?: string[]): Observable<EventMonthlySales> {
    let params = new HttpParams();
    if (year != null) {
      params = params.set('year', year);
    }
    if (month != null) {
      params = params.set('month', month);
    }
    if (genres && genres.length > 0) {
      genres.forEach(genre => {
        params = params.append('genres', genre);
      });
    }

    return this.httpClient.get<EventMonthlySales>(`${this.globals.backendUri + '/statistics'}/event-sales`, {params});
  }
}
