import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';

@Injectable({
  providedIn: 'root'
})
export class PdfTicketService {

  private ticketBaseUri: string = this.globals.backendUri + '/tickets';

  constructor(private httpClient: HttpClient, private globals: Globals) {}

  getTicketPdf(ticketId: number): Observable<Blob> {
    return this.httpClient.get(`${this.ticketBaseUri}/${ticketId}/pdf`, {
      responseType: 'blob'
    });
  }
}
