import {Injectable} from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpErrorResponse } from '@angular/common/http';
import {AuthService} from '../services/auth.service';
import {Observable, throwError} from 'rxjs';
import {catchError} from 'rxjs/operators';
import {Globals} from '../global/globals';
import {Router} from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private authService: AuthService, private globals: Globals, private router: Router) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const authUri = this.globals.backendUri + '/users/authentication';
    const registerUri = this.globals.backendUri + '/users/register';
    const passwordResetRequestUri = this.globals.backendUri + '/users/password-reset/request';
    const passwordResetConfirmUri = this.globals.backendUri + '/users/password-reset/confirm';

    if (req.url === authUri || req.url === registerUri || req.url === passwordResetRequestUri || req.url === passwordResetConfirmUri) {
      return next.handle(req);
    }

    const token = this.authService.getToken();
    if (!token || !this.authService.isLoggedIn()) {
      return next.handle(req);
    }

    const authReq = req.clone({
      headers: req.headers.set('Authorization', 'Bearer ' + token)
    });

    return next.handle(authReq).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 401) {
          this.authService.logoutUser();
          window.location.href = '/#/login';
        }
        return throwError(() => err);
      })
    );
  }
}
