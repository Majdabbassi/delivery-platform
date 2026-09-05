import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// Driver Interface
export interface Driver {
  id?: number;
  username: string;
  email: string;
  password?: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  role: string;
  isEnabled: boolean;
  createdAt?: string;
  updatedAt?: string;
  dateOfBirth?: string;
  address?: string;
  gender?: 'MALE' | 'FEMALE' | 'OTHER';
  nationalId?: string;
  licenseNumber?: string;
  drivingLicenseNumber?: string;
  licenseExpiryDate?: string;
  vehicleType?: string;
  vehiclePlateNumber?: string;
  vehicleModel?: string;
  vehicleYear?: number;
  isAvailable?: boolean;
  isVehicleOwned?: boolean;
  currentLocation?: string;
  totalDeliveries?: number;
  completedDeliveries?: number;
  cancelledDeliveries?: number;
  totalEarnings?: number;
  rating?: number;
  joinDate?: string;
  workingHours?: string;
  preferredWorkingAreas?: string;
  emergencyContact?: string;
  deliveryCompanyId?: number;
  experienceYears?: number;
  isVerified?: boolean;
  lastActiveDate?: string;
  insuranceNumber?: string;
  insuranceExpiryDate?: string;
  bankAccountNumber?: string;
}


export interface DriverSearchParams {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  nationalId?: string;
  enabled?: boolean;
  verified?: boolean;
  available?: boolean;
  vehicleType?: string;
  deliveryCompanyId?: number;
  minExperience?: number;
  minRating?: number;
  createdAfter?: string;
  createdBefore?: string;
  bornAfter?: string;
  bornBefore?: string;
  address?: string;
  emergencyContact?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface DriverStats {
  total: number;
  active: number;
  verified: number;
  available: number;
  activeAndVerified: number;
  experienced: number;
  withMultipleDeliveries: number;
}

@Injectable({
  providedIn: 'root'
})
export class DriverService {
  private readonly baseUrl = `${API_BASE_URL}/drivers`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  private handleError = (error: any): Observable<never> => {
    console.error('Driver service error:', error);
    throw error;
  }

  // Create operations
  createDriver(driver: Driver): Observable<Driver> {
    return this.http.post<Driver>(this.baseUrl, driver).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getDriverById(id: number): Observable<Driver> {
    return this.http.get<Driver>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllDrivers(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<Driver>>(this.baseUrl, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchDrivers(searchParams: DriverSearchParams): Observable<PaginatedResponse<Driver>> {
    let params = new HttpParams();
    
    Object.keys(searchParams).forEach(key => {
      const value = (searchParams as any)[key];
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, value.toString());
      }
    });

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Quick search endpoints
  searchByName(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/search/name`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchByContact(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/search/contact`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedDrivers(page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/active-verified`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getAvailableDrivers(page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/available`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getExperiencedDrivers(page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/experienced`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getNewDrivers(since?: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (since) {
      params = params.set('since', since);
    }

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/new`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getDriversByCompany(companyId: number, page: number = 0, size: number = 10): Observable<PaginatedResponse<Driver>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Driver>>(`${this.baseUrl}/company/${companyId}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Lookup endpoints
  getByUsername(username: string): Observable<Driver> {
    return this.http.get<Driver>(`${this.baseUrl}/username/${encodeURIComponent(username)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByEmail(email: string): Observable<Driver> {
    return this.http.get<Driver>(`${this.baseUrl}/email/${encodeURIComponent(email)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByNationalId(nationalId: string): Observable<Driver> {
    return this.http.get<Driver>(`${this.baseUrl}/national-id/${encodeURIComponent(nationalId)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByLicenseNumber(licenseNumber: string): Observable<Driver> {
    return this.http.get<Driver>(`${this.baseUrl}/license/${encodeURIComponent(licenseNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateDriver(id: number, driver: Driver): Observable<Driver> {
    return this.http.put<Driver>(`${this.baseUrl}/${id}`, driver).pipe(
      catchError(this.handleError)
    );
  }

  updateEnabledStatus(id: number, enabled: boolean): Observable<Driver> {
    const params = new HttpParams().set('enabled', enabled.toString());
    
    return this.http.patch<Driver>(`${this.baseUrl}/${id}/enabled`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerifiedStatus(id: number, verified: boolean): Observable<Driver> {
    const params = new HttpParams().set('verified', verified.toString());
    
    return this.http.patch<Driver>(`${this.baseUrl}/${id}/verified`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateAvailabilityStatus(id: number, available: boolean): Observable<Driver> {
    const params = new HttpParams().set('available', available.toString());
    
    return this.http.patch<Driver>(`${this.baseUrl}/${id}/availability`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateLocation(id: number, location: string): Observable<Driver> {
    const params = new HttpParams().set('location', location);
    
    return this.http.patch<Driver>(`${this.baseUrl}/${id}/location`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteDriver(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  softDeleteDriver(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/soft`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getDriverCounts(): Observable<DriverStats> {
    return this.http.get<DriverStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  getTotalDriversCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/total`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveDriversCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  getVerifiedDriversCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/verified`).pipe(
      catchError(this.handleError)
    );
  }

  getAvailableDriversCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/available`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedDriversCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  getExperiencedDriversCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/experienced`).pipe(
      catchError(this.handleError)
    );
  }

  getDriversWithMultipleDeliveriesCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/multiple-deliveries`).pipe(
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

  existsByLicenseNumber(licenseNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/license/${encodeURIComponent(licenseNumber)}`).pipe(
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

  existsByLicenseNumberAndIdNot(licenseNumber: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/license/${encodeURIComponent(licenseNumber)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }
}