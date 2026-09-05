import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// DriverPerson Interface
export interface DriverPerson {
  id?: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  nationalId?: string;
  dateOfBirth?: string;
  address?: string;
  emergencyContact?: string;
  licenseNumber?: string;
  licenseExpiryDate?: string;
  vehicleType?: string;
  vehicleModel?: string;
  vehiclePlateNumber?: string;
  isAvailable: boolean;
  isVerified: boolean;
  rating?: number;
  totalDeliveries?: number;
  totalEarnings?: number;
  lastDeliveryDate?: string;
  joinDate?: string;
  profilePicture?: string;
  bankAccountInfo?: string;
  insuranceNumber?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}


export interface DriverPersonSearchParams {
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  nationalId?: string;
  licenseNumber?: string;
  vehicleType?: string;
  vehiclePlateNumber?: string;
  isAvailable?: boolean;
  isVerified?: boolean;
  minRating?: number;
  createdAfter?: string;
  createdBefore?: string;
  address?: string;
  emergencyContact?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DriverPersonService {
  private readonly baseUrl = `${API_BASE_URL}/driver-persons`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  private handleError = (error: any): Observable<never> => {
    console.error('Driver person service error:', error);
    throw error;
  }

  // Delivery-owner scoped driver management
  getMyCompanyDrivers(): Observable<DriverPerson[]> {
    return this.http.get<DriverPerson[]>(`${this.baseUrl}/my/company`).pipe(
      catchError(this.handleError)
    );
  }

  addDriverToMyCompany(driverPerson: DriverPerson): Observable<DriverPerson> {
    return this.http.post<DriverPerson>(`${this.baseUrl}/my/company`, driverPerson).pipe(
      catchError(this.handleError)
    );
  }

  updateCompanyDriverAvailability(id: number, isAvailable: boolean): Observable<DriverPerson> {
    const params = new HttpParams().set('isAvailable', isAvailable.toString());
    return this.http.patch<DriverPerson>(`${this.baseUrl}/my/company/${id}/availability`, null, { params }).pipe(
      catchError(this.handleError)
    );
  }

  removeDriverFromMyCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/my/company/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  // Create operations
  createDriverPerson(driverPerson: DriverPerson): Observable<DriverPerson> {
    return this.http.post<DriverPerson>(this.baseUrl, driverPerson).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getDriverPersonById(id: number): Observable<DriverPerson> {
    return this.http.get<DriverPerson>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllDriverPersons(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<DriverPerson>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<DriverPerson>>(this.baseUrl, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchDriverPersons(searchParams: DriverPersonSearchParams): Observable<PaginatedResponse<DriverPerson>> {
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

    return this.http.get<PaginatedResponse<DriverPerson>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateDriverPerson(id: number, driverPerson: DriverPerson): Observable<DriverPerson> {
    return this.http.put<DriverPerson>(`${this.baseUrl}/${id}`, driverPerson).pipe(
      catchError(this.handleError)
    );
  }

  updateAvailabilityStatus(id: number, isAvailable: boolean): Observable<DriverPerson> {
    const params = new HttpParams().set('isAvailable', isAvailable.toString());
    return this.http.patch<DriverPerson>(`${this.baseUrl}/${id}/availability`, null, { params }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerificationStatus(id: number, isVerified: boolean): Observable<DriverPerson> {
    const params = new HttpParams().set('isVerified', isVerified.toString());
    return this.http.patch<DriverPerson>(`${this.baseUrl}/${id}/verification`, null, { params }).pipe(
      catchError(this.handleError)
    );
  }

  getCurrentDriver(): Observable<DriverPerson> {
    return this.http.get<DriverPerson>(`${this.baseUrl}/me`).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteDriverPerson(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  hardDeleteDriverPerson(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/hard`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  countActiveDriverPersons(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  countAvailableDriverPersons(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/available`).pipe(
      catchError(this.handleError)
    );
  }

  countVerifiedDriverPersons(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/verified`).pipe(
      catchError(this.handleError)
    );
  }

  getAverageRating(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/average-rating`).pipe(
      catchError(this.handleError)
    );
  }

  getAverageEarnings(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/average-earnings`).pipe(
      catchError(this.handleError)
    );
  }

  getTotalDeliveries(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/total-deliveries`).pipe(
      catchError(this.handleError)
    );
  }
}