import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {News, NewsPage} from '../dtos/news';
import {Observable} from 'rxjs';
import {Globals} from '../global/globals';

@Injectable({
  providedIn: 'root'
})
/**
 * Service for news API requests.
 */
export class NewsService {

  private newsBaseUri: string = this.globals.backendUri + '/news';

  constructor(private httpClient: HttpClient, private globals: Globals) {
  }

  /**
   * Loads all messages from the backend
   */
  getNews(): Observable<News[]> {
    return this.httpClient.get<News[]>(this.newsBaseUri);
  }

  /**
   * Loads specific news entry from the backend
   *
   * @param id of news entry to load
   */
  getNewsById(id: number): Observable<News> {
    return this.httpClient.get<News>(this.newsBaseUri + '/' + id);
  }

  /**
   * Persists news to the backend
   *
   * @param news to persist
   */
  createNews(news: News, imageFile: File | null): Observable<News> {
    const formData = new FormData();
    formData.append('news', new Blob([JSON.stringify(news)], {type: 'application/json'}));
    if (imageFile) {
      formData.append('image', imageFile);
    }
    return this.httpClient.post<News>(this.newsBaseUri, formData);
  }

  /**
   * Get a list of unread news items for the user.
   *
   * @returns list of unread news items
   */
  getUnreadNews(page: number, size: number): Observable<NewsPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.httpClient.get<NewsPage>(`${this.newsBaseUri}/unread`, {params});
  }


  /**
   * Get a list of read news items for the user.
   *
   * @returns list of read news items
   */
  getReadNews(page: number, size: number): Observable<NewsPage> {
    const params = new HttpParams()
      .set('page', page)
      .set('size', size);
    return this.httpClient.get<NewsPage>(`${this.newsBaseUri}/read`, {params});
  }

}
