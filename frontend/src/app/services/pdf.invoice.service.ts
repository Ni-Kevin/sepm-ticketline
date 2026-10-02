import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';

@Injectable({
  providedIn: 'root'
})
export class PdfInvoiceService {
  private orderBaseUri: string = this.globals.backendUri + '/orders';

  constructor(private httpClient: HttpClient, private globals: Globals) {}

  getInvoicePdf(ticketId: number): Observable<Blob> {
    return this.httpClient.get(`${this.orderBaseUri}/by-ticket/${ticketId}/invoice`, {
      responseType: 'blob'
    });
  }

  getCancellationInvoicePdf(ticketId: number): Observable<Blob> {
    return this.httpClient.get(`${this.orderBaseUri}/by-ticket/${ticketId}/cancellation-invoice`, {
      responseType: 'blob'
    });
  }
}
