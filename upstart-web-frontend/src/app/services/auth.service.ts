import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { API_BASE_URL } from '../config';

// User roles as defined in the backend
export enum UserRole {
  SUPER_ADMIN = 'SUPER_ADMIN',
  VENDOR_OWNER = 'VENDOR_OWNER',
  DELIVERY_OWNER = 'DELIVERY_OWNER',
  CLIENT = 'CLIENT',
  DRIVER = 'DRIVER'
}

export interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role: UserRole;
  isEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role?: UserRole;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = `${API_BASE_URL}/auth`;
  private readonly userBaseUrl = `${API_BASE_URL}/users`;
  private readonly tokenKey = 'authToken';
  private readonly refreshTokenKey = 'refreshToken';
  private readonly userKey = 'currentUser';
  
  private currentUserSubject = new BehaviorSubject<User | null>(this.getCurrentUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();
  
  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient) {
    // Check token validity on service initialization
    this.checkTokenValidity();
  }

  // Authentication methods
  authenticateUser(credentials: LoginRequest): Observable<LoginResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, credentials, { headers })
      .pipe(
        tap(response => {
          this.setAuthData(response.accessToken, response.refreshToken, response.user);
        })
      );
  }

  // Alias for backward compatibility
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.authenticateUser(credentials);
  }

  refreshToken(): Observable<LoginResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<LoginResponse>(`${this.baseUrl}/refresh`, { refreshToken }).pipe(
      tap(response => {
        this.setAuthData(response.accessToken, response.refreshToken, response.user);
      })
    );
  }

  logoutUser(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => {
        this.clearAuthData();
      })
    );
  }

  // Alias for backward compatibility
  logout(): void {
    this.clearLocalSession();
    this.logoutUser().subscribe({ error: () => undefined });
  }

  // Registration - creates a CLIENT account via the public users endpoint.
  register(userData: RegisterRequest): Observable<User> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.post<User>(`${this.userBaseUrl}/register`, userData, { headers });
  }

  // Clears local session data without calling the server.
  // Safe to use when the token is missing/expired (avoids interceptor recursion).
  clearLocalSession(): void {
    this.clearAuthData();
  }

  getCurrentUserFromAPI(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/me`);
  }

  // Token management
  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  hasValidToken(): boolean {
    const token = this.getToken();
    if (!token) return false;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Math.floor(Date.now() / 1000);
      return payload.exp > currentTime;
    } catch {
      return false;
    }
  }

  // Role-based access control
  hasRole(role: UserRole): boolean {
    const user = this.getCurrentUser();
    return user?.role === role;
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const user = this.getCurrentUser();
    return user ? roles.includes(user.role) : false;
  }

  canCreateOrder(): boolean {
    return this.hasAnyRole([UserRole.VENDOR_OWNER, UserRole.CLIENT]);
  }

  canViewAllOrders(): boolean {
    return this.hasRole(UserRole.SUPER_ADMIN);
  }

  canUpdateOrderStatus(): boolean {
    return this.hasAnyRole([UserRole.DELIVERY_OWNER, UserRole.DRIVER]);
  }

  canAssignDeliveryCompany(): boolean {
    return this.hasRole(UserRole.VENDOR_OWNER);
  }

  canRateOrder(): boolean {
    return this.hasRole(UserRole.CLIENT);
  }

  canCancelOrder(): boolean {
    return this.hasAnyRole([UserRole.VENDOR_OWNER, UserRole.CLIENT]);
  }

  canUpdateOrder(): boolean {
    return this.hasAnyRole([UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER]);
  }

  canDeleteOrder(): boolean {
    return this.hasRole(UserRole.SUPER_ADMIN);
  }

  // UI permission helpers
  canAccessDriversPage(): boolean {
    return this.hasAnyRole([UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER]);
  }

  canAccessVendorCompaniesPage(): boolean {
    return this.hasAnyRole([UserRole.SUPER_ADMIN, UserRole.VENDOR_OWNER]);
  }

  canAccessDeliveryCompaniesPage(): boolean {
    return this.hasAnyRole([UserRole.SUPER_ADMIN, UserRole.DELIVERY_OWNER]);
  }

  canAccessAdminPages(): boolean {
    return this.hasRole(UserRole.SUPER_ADMIN);
  }

  // HTTP headers helper
  getAuthHeaders(): HttpHeaders {
    const token = this.getToken();
    if (!token || !this.hasValidToken()) {
      console.error('No valid token available for API request');
      throw new Error('Authentication required');
    }
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  // Private helper methods
  private setAuthData(token: string, refreshToken: string, user: User): void {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.refreshTokenKey, refreshToken);
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);
  }

  private clearAuthData(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    localStorage.removeItem(this.userKey);
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
  }

  private getCurrentUserFromStorage(): User | null {
    const userJson = localStorage.getItem(this.userKey);
    if (userJson) {
      try {
        return JSON.parse(userJson);
      } catch {
        return null;
      }
    }
    return null;
  }

  private checkTokenValidity(): void {
    if (!this.hasValidToken()) {
      this.clearAuthData();
    }
  }

  // User profile methods
  updateProfile(userData: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/profile`, userData).pipe(
      tap(user => {
        localStorage.setItem(this.userKey, JSON.stringify(user));
        this.currentUserSubject.next(user);
      })
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/change-password`, {
      currentPassword,
      newPassword
    });
  }

  // Password reset
  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/reset-password`, {
      token,
      newPassword
    });
  }

  // Account verification
  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/verify-email`, { token });
  }

  resendVerificationEmail(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/resend-verification`, {});
  }
}