import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// Admin Interface
export interface Admin {
  id?: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'MODERATOR';
  enabled: boolean;
  isEnabled?: boolean;
  verified: boolean;
  lastLoginDate?: string;
  lastLoginAt?: string;
  profilePicture?: string;
  department?: string;
  position?: string;
  permissions?: string[];
  assignedModules?: string[];
  accessLevel?: 'FULL' | 'LIMITED' | 'READ_ONLY';
  isLocked?: boolean;
  lockReason?: string;
  loginAttempts?: number;
  canManageUsers?: boolean;
  canManageSystem?: boolean;
  canViewReports?: boolean;
  canManageContent?: boolean;
  sessionTimeout?: number;
  description?: string;
  emergencyContact?: string;
  address?: string;
  nationalId?: string;
  dateOfBirth?: string;
  hireDate?: string;
  salary?: number;
  isActive?: boolean;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface AdminSearchParams {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  role?: string;
  enabled?: boolean;
  verified?: boolean;
  department?: string;
  position?: string;
  nationalId?: string;
  createdAfter?: string;
  createdBefore?: string;
  hiredAfter?: string;
  hiredBefore?: string;
  bornAfter?: string;
  bornBefore?: string;
  address?: string;
  emergencyContact?: string;
  minSalary?: number;
  maxSalary?: number;
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface AdminStats {
  total: number;
  active: number;
  verified: number;
  activeAndVerified: number;
  superAdmins: number;
  admins: number;
  moderators: number;
  recentlyHired: number;
  totalAdmins: number;
  activeAdmins: number;
  lockedAdmins: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly baseUrl = `${API_BASE_URL}/admins`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  private handleError = (error: any): Observable<never> => {
    console.error('Admin service error:', error);
    throw error;
  }

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  // Create operations
  createAdmin(admin: Admin): Observable<Admin> {
    return this.http.post<Admin>(this.baseUrl, admin).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getAdminById(id: number): Observable<Admin> {
    return this.http.get<Admin>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllAdmins(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<Admin>>(this.baseUrl, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchAdmins(searchParams: AdminSearchParams): Observable<PaginatedResponse<Admin>> {
    let params = new HttpParams();
    
    Object.keys(searchParams).forEach(key => {
      const value = (searchParams as any)[key];
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value)) {
          value.forEach(v => params = params.append(key, v));
        } else {
          params = params.set(key, value.toString());
        }
      }
    });

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Quick search endpoints
  searchByName(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/search/name`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchByContact(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/search/contact`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedAdmins(page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/active-verified`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getSuperAdmins(page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/super-admins`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getAdminsByRole(role: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/role/${encodeURIComponent(role)}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getAdminsByDepartment(department: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/department/${encodeURIComponent(department)}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getRecentlyHiredAdmins(since?: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Admin>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (since) {
      params = params.set('since', since);
    }

    return this.http.get<PaginatedResponse<Admin>>(`${this.baseUrl}/recently-hired`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Lookup endpoints
  getByUsername(username: string): Observable<Admin> {
    return this.http.get<Admin>(`${this.baseUrl}/username/${encodeURIComponent(username)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByEmail(email: string): Observable<Admin> {
    return this.http.get<Admin>(`${this.baseUrl}/email/${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByNationalId(nationalId: string): Observable<Admin> {
    return this.http.get<Admin>(`${this.baseUrl}/national-id/${encodeURIComponent(nationalId)}`).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateAdmin(id: number, admin: Admin): Observable<Admin> {
    return this.http.put<Admin>(`${this.baseUrl}/${id}`, admin).pipe(
      catchError(this.handleError)
    );
  }

  updateEnabledStatus(id: number, enabled: boolean): Observable<Admin> {
    const params = new HttpParams().set('enabled', enabled.toString());
    
    return this.http.patch<Admin>(`${this.baseUrl}/${id}/enabled`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerifiedStatus(id: number, verified: boolean): Observable<Admin> {
    const params = new HttpParams().set('verified', verified.toString());
    
    return this.http.patch<Admin>(`${this.baseUrl}/${id}/verified`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateRole(id: number, role: string): Observable<Admin> {
    const params = new HttpParams().set('role', role);
    
    return this.http.patch<Admin>(`${this.baseUrl}/${id}/role`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateActiveStatus(id: number, isActive: boolean): Observable<Admin> {
    const params = new HttpParams().set('isActive', isActive.toString());
    
    return this.http.patch<Admin>(`${this.baseUrl}/${id}/active`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updatePassword(id: number, newPassword: string): Observable<{message: string}> {
    return this.http.patch<{message: string}>(`${this.baseUrl}/${id}/password`, 
      { password: newPassword }).pipe(
      catchError(this.handleError)
    );
  }

  lockAdmin(id: number, reason?: string): Observable<Admin> {
    const body = reason ? { reason } : {};
    return this.http.patch<Admin>(`${this.baseUrl}/${id}/lock`, body).pipe(
      catchError(this.handleError)
    );
  }

  unlockAdmin(id: number): Observable<Admin> {
    return this.http.patch<Admin>(`${this.baseUrl}/${id}/unlock`, null).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteAdmin(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  softDeleteAdmin(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/soft`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getAdminCounts(): Observable<AdminStats> {
    return this.http.get<AdminStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  getTotalAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/total`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  getVerifiedAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/verified`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  getSuperAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/super-admins`).pipe(
      catchError(this.handleError)
    );
  }

  getAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/admins`).pipe(
      catchError(this.handleError)
    );
  }

  getModeratorsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/moderators`).pipe(
      catchError(this.handleError)
    );
  }

  getRecentlyHiredAdminsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/recently-hired`).pipe(
      catchError(this.handleError)
    );
  }

  // Validation endpoints
  existsByUsername(username: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/username/${encodeURIComponent(username)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByEmail(email: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByNationalId(nationalId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/national-id/${encodeURIComponent(nationalId)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByUsernameAndIdNot(username: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/username/${encodeURIComponent(username)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByEmailAndIdNot(email: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(email)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByNationalIdAndIdNot(nationalId: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/national-id/${encodeURIComponent(nationalId)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }
}