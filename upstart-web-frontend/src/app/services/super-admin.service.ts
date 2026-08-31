import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// SuperAdmin Interface
export interface SuperAdmin {
  id?: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  enabled: boolean;
  verified: boolean;
  lastSystemAccess?: string;
  systemPermissions?: string[];
  securityLevel: number;
  createdAt?: string;
  updatedAt?: string;
  profilePicture?: string;
  department?: string;
  position?: string;
  accessLevel?: 'FULL' | 'LIMITED' | 'READ_ONLY';
  isLocked?: boolean;
  lockReason?: string;
  notes?: string;
}

// Paginated Response Interface
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
}

// SuperAdmin Search Parameters
export interface SuperAdminSearchParams {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  enabled?: boolean;
  verified?: boolean;
  department?: string;
  position?: string;
  minSecurityLevel?: number;
  maxSecurityLevel?: number;
  lastAccessAfter?: string;
  lastAccessBefore?: string;
  createdAfter?: string;
  createdBefore?: string;
}

// SuperAdmin Statistics Interface
export interface SuperAdminStats {
  total: number;
  active: number;
  verified: number;
  activeAndVerified: number;
  highSecurityLevel: number;
  recentlyActive: number;
  totalSystemAccess: number;
  averageSecurityLevel: number;
}

@Injectable({
  providedIn: 'root'
})
export class SuperAdminService {
  private baseUrl = `${API_BASE_URL}/super-admin`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  // CRUD Operations
  createSuperAdmin(superAdmin: SuperAdmin): Observable<SuperAdmin> {
    return this.http.post<SuperAdmin>(this.baseUrl, superAdmin);
  }

  getSuperAdminById(id: number): Observable<SuperAdmin> {
    return this.http.get<SuperAdmin>(`${this.baseUrl}/${id}`);
  }

  getSuperAdminByUsername(username: string): Observable<SuperAdmin> {
    return this.http.get<SuperAdmin>(`${this.baseUrl}/username/${username}`);
  }

  getSuperAdminByEmail(email: string): Observable<SuperAdmin> {
    return this.http.get<SuperAdmin>(`${this.baseUrl}/email/${email}`);
  }

  getSuperAdminByUsernameOrEmail(usernameOrEmail: string): Observable<SuperAdmin> {
    return this.http.get<SuperAdmin>(`${this.baseUrl}/find/${usernameOrEmail}`);
  }

  searchSuperAdmins(searchParams: SuperAdminSearchParams): Observable<PaginatedResponse<SuperAdmin>> {
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

    return this.http.get<PaginatedResponse<SuperAdmin>>(`${this.baseUrl}/search`, {
      params
    });
  }

  getAllSuperAdmins(page: number = 0, size: number = 20): Observable<PaginatedResponse<SuperAdmin>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PaginatedResponse<SuperAdmin>>(this.baseUrl, {
      params
    });
  }

  getActiveAdminsSince(since: string): Observable<SuperAdmin[]> {
    const params = new HttpParams().set('since', since);
    return this.http.get<SuperAdmin[]>(`${this.baseUrl}/active-since`, {
      params
    });
  }

  getAdminsByMinimumSecurityLevel(minLevel: number): Observable<SuperAdmin[]> {
    return this.http.get<SuperAdmin[]>(`${this.baseUrl}/security-level/${minLevel}`);
  }

  updateSuperAdmin(id: number, superAdmin: SuperAdmin): Observable<SuperAdmin> {
    return this.http.put<SuperAdmin>(`${this.baseUrl}/${id}`, superAdmin);
  }

  updateLastSystemAccess(id: number): Observable<SuperAdmin> {
    return this.http.patch<SuperAdmin>(`${this.baseUrl}/${id}/last-access`, null);
  }

  updateSystemPermissions(id: number, permissions: string[]): Observable<SuperAdmin> {
    const body = { permissions };
    return this.http.patch<SuperAdmin>(`${this.baseUrl}/${id}/permissions`, body);
  }

  updateSecurityLevel(id: number, securityLevel: number): Observable<SuperAdmin> {
    const body = { securityLevel };
    return this.http.patch<SuperAdmin>(`${this.baseUrl}/${id}/security-level`, body);
  }

  disableSuperAdmin(id: number): Observable<SuperAdmin> {
    return this.http.patch<SuperAdmin>(`${this.baseUrl}/${id}/disable`, null);
  }

  deleteSuperAdmin(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // Statistics
  getSuperAdminStatistics(): Observable<SuperAdminStats> {
    return this.http.get<SuperAdminStats>(`${this.baseUrl}/statistics`);
  }

  countActiveSuperAdmins(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/active`);
  }

  countTotalSuperAdmins(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/total`);
  }

  // Error handling helper
  private handleError(error: any): Observable<never> {
    console.error('SuperAdmin service error:', error);
    throw error;
  }
}