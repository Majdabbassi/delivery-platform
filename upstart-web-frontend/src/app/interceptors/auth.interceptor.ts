import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  constructor(private authService: AuthService, private router: Router) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (this.isPublicEndpoint(req.url)) {
      return next.handle(req);
    }

    const token = this.authService.getToken();

    if (!token || token.trim() === '') {
      this.handleMissingToken(req.url);
      return throwError(() => new Error('Authentication required - no valid token'));
    }

    const authReq = req.clone({ setHeaders: this.buildHeaders(req, token) });

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 && !req.url.includes('/auth/refresh')) {
          return this.handle401(authReq, next);
        }
        return throwError(() => error);
      })
    );
  }

  private handle401(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.authService.getRefreshToken()) {
      this.handleMissingToken(req.url);
      return throwError(() => new Error('Authentication required - refresh token missing'));
    }

    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(this.authService.getToken());
          return next.handle(req.clone({ setHeaders: this.buildHeaders(req, this.authService.getToken() || '') }));
        }),
        catchError((error) => {
          this.isRefreshing = false;
          this.authService.clearLocalSession();
          const currentUrl = this.router.url;
          if (!currentUrl.startsWith('/login')) {
            this.router.navigate(['/login']);
          }
          return throwError(() => error);
        })
      );
    }

    return this.refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(token => next.handle(req.clone({ setHeaders: this.buildHeaders(req, token || '') })))
    );
  }

  private handleMissingToken(url: string): void {
    this.authService.clearLocalSession();
    const currentUrl = this.router.url;
    if (!currentUrl.startsWith('/login')) {
      this.router.navigate(['/login']);
    }
  }

  private buildHeaders(req: HttpRequest<any>, token: string): { [key: string]: string } {
    const headers: { [key: string]: string } = {
      'Authorization': `Bearer ${token}`
    };

    if (!req.headers.has('Content-Type') && !(req.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    return headers;
  }

  private isPublicEndpoint(url: string): boolean {
    const publicEndpoints = [
      '/auth/login',
      '/auth/refresh',
      '/users/register'
    ];

    return publicEndpoints.some(endpoint => url.includes(endpoint));
  }
}