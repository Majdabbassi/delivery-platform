import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// DeliveryCompany Interface
export interface DeliveryCompany {
  id?: number;
  name: string;
  email: string;
  phone: string;
  address: string;
  website?: string;
  serviceType: string;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED';
  contactPerson: string;
  contractStartDate?: string;
  contractEndDate?: string;
  totalDeliveries?: number;
  totalRevenue?: number;
  lastDeliveryDate?: string;
  coverageAreas?: string[];
  vehicleTypes?: string[];
  rating?: number;
  isVerified?: boolean;
  isActive?: boolean;
  licenseNumber?: string;
  insuranceNumber?: string;
  emergencyContact?: string;
  operatingHours?: string;
  deliveryOwnerId?: number;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}


export interface DeliveryCompanySearchParams {
  name?: string;
  email?: string;
  phone?: string;
  contactPerson?: string;
  serviceType?: string;
  status?: string;
  verified?: boolean;
  minRating?: number;
  deliveryOwnerId?: number;
  coverageArea?: string;
  vehicleType?: string;
  createdAfter?: string;
  createdBefore?: string;
  contractStartAfter?: string;
  contractStartBefore?: string;
  address?: string;
  emergencyContact?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface DeliveryCompanyStats {
  total: number;
  active: number;
  inactive: number;
  verified: number;
  activeAndVerified: number;
  highRated: number;
  withMultipleVehicles: number;
  pending: number;
  totalRevenue: number;
}

@Injectable({
  providedIn: 'root'
})
export class DeliveryCompanyService {
  private readonly baseUrl = `${API_BASE_URL}/delivery-companies`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  private handleError = (error: any): Observable<never> => {
    console.error('Delivery company service error:', error);
    throw error;
  }

  private mapCompany(company: any): DeliveryCompany {
    return {
      ...company,
      name: company.companyName,
      email: company.contactEmail,
      phone: company.contactPhone,
      address: company.companyAddress,
      serviceType: company.serviceRegion || 'Standard Delivery',
      status: company.isActive ? 'ACTIVE' : 'INACTIVE',
      contactPerson: company.owner ? `${company.owner.firstName} ${company.owner.lastName}` : 'N/A',
      coverageAreas: company.managedZones ? company.managedZones.split(',').map((area: string) => area.trim()) : [],
      vehicleTypes: company.vehicleTypesSupported ? company.vehicleTypesSupported.split(',').map((type: string) => type.trim()) : [],
      licenseNumber: company.operatingLicense,
      deliveryOwnerId: company.owner?.id,
      totalDeliveries: company.totalDeliveriesManaged || 0,
      totalRevenue: company.totalRevenue || 0,
      rating: company.rating || 0,
      isVerified: company.isLicensed || false
    };
  }

  private mapPage(response: any): PaginatedResponse<DeliveryCompany> {
    return { ...response, content: (response.content || []).map((company: any) => this.mapCompany(company)) };
  }

  // Create operations
  createDeliveryCompany(company: DeliveryCompany): Observable<DeliveryCompany> {
    return this.http.post<DeliveryCompany>(this.baseUrl, company).pipe(
      catchError(this.handleError)
    );
  }

  createDeliveryCompanyWithOwner(ownerId: number, company: DeliveryCompany): Observable<DeliveryCompany> {
    return this.http.post<DeliveryCompany>(`${this.baseUrl}/with-owner/${ownerId}`, company).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getDeliveryCompanyById(id: number): Observable<DeliveryCompany> {
    return this.http.get<DeliveryCompany>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllDeliveryCompanies(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<DeliveryCompany>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<DeliveryCompany>>(this.baseUrl, {
      params
    }).pipe(
      map(response => this.mapPage(response)),
      catchError(this.handleError)
    );
  }

  searchDeliveryCompanies(searchParams: DeliveryCompanySearchParams): Observable<PaginatedResponse<DeliveryCompany>> {
    let params = new HttpParams();

    const backendParams: Record<string, any> = { ...searchParams };
    if (searchParams.name) {
      backendParams['companyName'] = searchParams.name;
      delete backendParams['name'];
      delete backendParams['email'];
      delete backendParams['phone'];
      delete backendParams['contactPerson'];
    }
    if (searchParams.status && searchParams.status !== 'all') {
      backendParams['isActive'] = searchParams.status === 'ACTIVE';
      delete backendParams['status'];
    }
    delete backendParams['verified'];

    Object.keys(backendParams).forEach(key => {
      const value = backendParams[key];
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value)) {
          value.forEach(v => params = params.append(key, v));
        } else {
          params = params.set(key, value.toString());
        }
      }
    });

    return this.http.get<PaginatedResponse<DeliveryCompany>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      map(response => this.mapPage(response)),
      catchError(this.handleError)
    );
  }

  // Quick search endpoints
  searchByNameOrServiceType(query: string): Observable<DeliveryCompany[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/search/name-service`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchByContact(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryCompany>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryCompany>>(`${this.baseUrl}/search/contact`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedDeliveryCompanies(): Observable<DeliveryCompany[]> {
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  getPopularDeliveryCompanies(): Observable<DeliveryCompany[]> {
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/popular`).pipe(
      catchError(this.handleError)
    );
  }

  getHighRatedDeliveryCompanies(): Observable<DeliveryCompany[]> {
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/high-rated`).pipe(
      catchError(this.handleError)
    );
  }

  getNewDeliveryCompanies(): Observable<DeliveryCompany[]> {
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/new`).pipe(
      catchError(this.handleError)
    );
  }

  getDeliveryCompaniesByServiceType(serviceType: string): Observable<DeliveryCompany[]> {
    const params = new HttpParams().set('serviceType', serviceType);
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/service-type`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getDeliveryCompaniesByOwner(ownerId: number): Observable<DeliveryCompany[]> {
    return this.http.get<DeliveryCompany[]>(`${this.baseUrl}/owner/${ownerId}`).pipe(
      map(companies => (companies || []).map(company => this.mapCompany(company))),
      catchError(this.handleError)
    );
  }

  // Lookup endpoints
  findByCompanyName(companyName: string): Observable<DeliveryCompany | null> {
    const params = new HttpParams().set('companyName', companyName);
    return this.http.get<DeliveryCompany | null>(`${this.baseUrl}/lookup/name`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  findByOperatingLicense(operatingLicense: string): Observable<DeliveryCompany | null> {
    const params = new HttpParams().set('operatingLicense', operatingLicense);
    return this.http.get<DeliveryCompany | null>(`${this.baseUrl}/lookup/license`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  findByContactEmail(contactEmail: string): Observable<DeliveryCompany | null> {
    const params = new HttpParams().set('contactEmail', contactEmail);
    return this.http.get<DeliveryCompany | null>(`${this.baseUrl}/lookup/email`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getByOwner(ownerId: number, page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryCompany>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryCompany>>(`${this.baseUrl}/owner/${ownerId}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getByOwnerPaginated(ownerId: number, page: number = 0, size: number = 10): Observable<PaginatedResponse<DeliveryCompany>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<DeliveryCompany>>(`${this.baseUrl}/owner/${ownerId}/paginated`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateDeliveryCompany(id: number, company: DeliveryCompany): Observable<DeliveryCompany> {
    return this.http.put<DeliveryCompany>(`${this.baseUrl}/${id}`, company).pipe(
      catchError(this.handleError)
    );
  }

  updateDeliveryCompanyDetails(id: number, company: DeliveryCompany): Observable<DeliveryCompany> {
    return this.http.put<DeliveryCompany>(`${this.baseUrl}/${id}/details`, company).pipe(
      catchError(this.handleError)
    );
  }

  updateActiveStatus(id: number, isActive: boolean): Observable<DeliveryCompany> {
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/active-status`, { isActive }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerificationStatus(id: number, isVerified: boolean): Observable<DeliveryCompany> {
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/verification-status`, { isVerified }).pipe(
      catchError(this.handleError)
    );
  }

  updateAverageRating(id: number, averageRating: number): Observable<DeliveryCompany> {
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/average-rating`, { averageRating }).pipe(
      catchError(this.handleError)
    );
  }

  updateTotalDeliveries(id: number, totalDeliveries: number): Observable<DeliveryCompany> {
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/total-deliveries`, { totalDeliveries }).pipe(
      catchError(this.handleError)
    );
  }

  updateTotalRevenue(id: number, totalRevenue: number): Observable<DeliveryCompany> {
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/total-revenue`, { totalRevenue }).pipe(
      catchError(this.handleError)
    );
  }

  updateCommissionRate(id: number, commissionRate: number): Observable<DeliveryCompany> {
    const body = { commissionRate };
    
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/commission-rate`, body).pipe(
      catchError(this.handleError)
    );
  }

  updateMaxDrivers(id: number, maxDrivers: number): Observable<DeliveryCompany> {
    const body = { maxDrivers };
    
    return this.http.patch<DeliveryCompany>(`${this.baseUrl}/${id}/max-drivers`, body).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  hardDeleteDeliveryCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/hard`).pipe(
      catchError(this.handleError)
    );
  }

  softDeleteDeliveryCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/soft`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getDeliveryCompanyStats(): Observable<DeliveryCompanyStats> {
    return this.http.get<DeliveryCompanyStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  countTotalDeliveryCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/total`).pipe(
      catchError(this.handleError)
    );
  }

  countActiveDeliveryCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  countVerifiedDeliveryCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/verified`).pipe(
      catchError(this.handleError)
    );
  }

  countActiveAndVerifiedDeliveryCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  countPopularDeliveryCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/popular`).pipe(
      catchError(this.handleError)
    );
  }

  countHighRatedDeliveryCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/high-rated`).pipe(
      catchError(this.handleError)
    );
  }

  countDeliveryCompaniesByOwner(ownerId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/count/by-owner/${ownerId}`).pipe(
      catchError(this.handleError)
    );
  }

  countDeliveryCompaniesByServiceType(serviceType: string): Observable<number> {
    const params = new HttpParams().set('serviceType', serviceType);
    return this.http.get<number>(`${this.baseUrl}/count/by-service-type`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Validation endpoints
  existsByCompanyName(companyName: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/name/${encodeURIComponent(companyName)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByOperatingLicense(operatingLicense: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/license/${encodeURIComponent(operatingLicense)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByContactEmail(contactEmail: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(contactEmail)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByCompanyNameAndIdNot(companyName: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/name/${encodeURIComponent(companyName)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByOperatingLicenseAndIdNot(operatingLicense: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/license/${encodeURIComponent(operatingLicense)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByContactEmailAndIdNot(contactEmail: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(contactEmail)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }
}