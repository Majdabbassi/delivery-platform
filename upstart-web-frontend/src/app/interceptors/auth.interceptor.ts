import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService, private router: Router) {}
  
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip adding auth header for public auth endpoints
    if (this.isPublicEndpoint(req.url)) {
      return next.handle(req);
    }
    
    // Get the token from AuthService
    const token = this.authService.getToken();
    
    // Ensure token exists and is not empty for protected endpoints
    if (!token || token.trim() === '') {
      console.error('No valid token available for protected endpoint:', req.url);
      this.authService.clearLocalSession();
      const currentUrl = this.router.url;
      if (!currentUrl.startsWith('/login')) {
        this.router.navigate(['/login']);
      }
      return throwError(() => new Error('Authentication required - no valid token'));
    }
    
    // Prepare headers object
    const headers: { [key: string]: string } = {};
    
    // Always add Authorization header for protected endpoints
    headers['Authorization'] = `Bearer ${token}`;
    
    // Only set Content-Type if not already set and not a FormData request
    if (!req.headers.has('Content-Type') && !(req.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }
    
    const authReq = req.clone({ setHeaders: headers });
    
    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Token is invalid or expired, clear local auth data and redirect
          this.authService.clearLocalSession();
          const currentUrl = this.router.url;
          if (!currentUrl.startsWith('/login')) {
            this.router.navigate(['/login']);
          }
        }
        // Note: 403 (authenticated but forbidden) is NOT treated as a session
        // failure - it is rethrown so pages can show an access-denied message
        // instead of logging the user out.
        return throwError(() => error);
      })
    );
  }
  
  private isPublicEndpoint(url: string): boolean {
    const publicEndpoints = [
      '/auth/login',
      '/auth/register',
      '/auth/forgot-password',
      '/auth/reset-password',
      '/auth/verify-email',
      '/auth/refresh-token'
    ];
    
    return publicEndpoints.some(endpoint => url.includes(endpoint));
  }
}