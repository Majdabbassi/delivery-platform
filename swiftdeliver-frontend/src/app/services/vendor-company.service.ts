import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// VendorCompany Interface
export interface VendorCompany {
  id?: number;
  name: string;
  email: string;
  phone: string;
  address: string;
  website?: string;
  businessCategory: string;
  industry?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'PENDING' | 'SUSPENDED';
  contactPerson: string;
  registrationNumber?: string;
  taxNumber?: string;
  licenseNumber?: string;
  establishedDate?: string;
  contractStartDate?: string;
  contractEndDate?: string;
  totalProducts?: number;
  totalOrders?: number;
  totalRevenue?: number;
  lastOrderDate?: string;
  rating?: number;
  isVerified?: boolean;
  businessDescription?: string;
  notes?: string;
  operatingHours?: string;
  socialMediaLinks?: string[];
  bankAccountInfo?: string;
  emergencyContact?: string;
  vendorOwnerId?: number;
  createdAt?: string;
  updatedAt?: string;
}


export interface VendorCompanySearchParams {
  name?: string;
  email?: string;
  phone?: string;
  contactPerson?: string;
  businessCategory?: string;
  status?: string;
  verified?: boolean;
  minRating?: number;
  vendorOwnerId?: number;
  registrationNumber?: string;
  taxNumber?: string;
  licenseNumber?: string;
  establishedAfter?: string;
  establishedBefore?: string;
  createdAfter?: string;
  createdBefore?: string;
  address?: string;
  emergencyContact?: string;
  minRevenue?: number;
  maxRevenue?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface VendorCompanyStats {
  total: number;
  active: number;
  pending: number;
  verified: number;
  activeAndVerified: number;
  highRated: number;
  withMultipleProducts: number;
  recentlyEstablished: number;
  totalRevenue: number;
}

@Injectable({
  providedIn: 'root'
})
export class VendorCompanyService {
  private readonly baseUrl = `${API_BASE_URL}/vendor-companies`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  private handleError = (error: any): Observable<never> => {
    console.error('Vendor company service error:', error);
    throw error;
  }

  private mapCompany(company: any): VendorCompany {
    return {
      ...company,
      name: company.companyName,
      email: company.contactEmail,
      phone: company.contactPhone,
      address: company.businessAddress,
      status: company.isActive ? 'ACTIVE' : 'INACTIVE',
      businessCategory: company.businessCategory || 'General',
      industry: company.businessCategory || 'General',
      contactPerson: company.owner ? `${company.owner.firstName} ${company.owner.lastName}` : 'N/A',
      licenseNumber: company.businessLicense,
      vendorOwnerId: company.owner?.id,
      totalProducts: company.totalProducts || 0,
      totalOrders: company.totalOrders || 0,
      totalRevenue: company.totalRevenue || 0,
      rating: company.rating || 0,
      isVerified: company.isVerified || false
    };
  }

  private mapPage(response: any): PaginatedResponse<VendorCompany> {
    return { ...response, content: (response.content || []).map((company: any) => this.mapCompany(company)) };
  }

  // Create operations
  createVendorCompany(company: VendorCompany): Observable<VendorCompany> {
    return this.http.post<VendorCompany>(this.baseUrl, company).pipe(
      catchError(this.handleError)
    );
  }

  createVendorCompanyWithOwner(ownerId: number, company: VendorCompany): Observable<VendorCompany> {
    return this.http.post<VendorCompany>(`${this.baseUrl}/with-owner/${ownerId}`, company).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getVendorCompanyById(id: number): Observable<VendorCompany> {
    return this.http.get<VendorCompany>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllVendorCompanies(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<VendorCompany>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<VendorCompany>>(this.baseUrl, {
      params
    }).pipe(
      map(response => this.mapPage(response)),
      catchError(this.handleError)
    );
  }

  searchVendorCompanies(searchParams: VendorCompanySearchParams): Observable<PaginatedResponse<VendorCompany>> {
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
    delete backendParams['businessCategory'];
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

    return this.http.get<PaginatedResponse<VendorCompany>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      map(response => this.mapPage(response)),
      catchError(this.handleError)
    );
  }

  // Quick search endpoints
  searchByNameOrDescription(query: string): Observable<VendorCompany[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/search/name-category`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchByContact(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<VendorCompany>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<VendorCompany>>(`${this.baseUrl}/search/contact`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getActiveAndVerifiedVendorCompanies(): Observable<VendorCompany[]> {
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  getHighRatedVendorCompanies(): Observable<VendorCompany[]> {
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/high-rated`).pipe(
      catchError(this.handleError)
    );
  }

  getPopularVendorCompanies(): Observable<VendorCompany[]> {
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/popular`).pipe(
      catchError(this.handleError)
    );
  }

  getNewVendorCompanies(): Observable<VendorCompany[]> {
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/new`).pipe(
      catchError(this.handleError)
    );
  }

  getVendorCompaniesByCategory(category: string): Observable<VendorCompany[]> {
    const params = new HttpParams().set('category', category);
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/category`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getVendorCompaniesByOwner(ownerId: number): Observable<VendorCompany[]> {
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/owner/${ownerId}`).pipe(
      map(companies => (companies || []).map(company => this.mapCompany(company))),
      catchError(this.handleError)
    );
  }

  getTopPerformingVendorCompanies(): Observable<VendorCompany[]> {
    return this.http.get<VendorCompany[]>(`${this.baseUrl}/top-performing`).pipe(
      catchError(this.handleError)
    );
  }

  // Lookup endpoints
  findByCompanyName(companyName: string): Observable<VendorCompany | null> {
    const params = new HttpParams().set('companyName', companyName);
    return this.http.get<VendorCompany | null>(`${this.baseUrl}/lookup/name`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  findByContactEmail(contactEmail: string): Observable<VendorCompany | null> {
    const params = new HttpParams().set('contactEmail', contactEmail);
    return this.http.get<VendorCompany | null>(`${this.baseUrl}/lookup/email`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  findByBusinessRegistrationNumber(businessRegistrationNumber: string): Observable<VendorCompany | null> {
    const params = new HttpParams().set('businessRegistrationNumber', businessRegistrationNumber);
    return this.http.get<VendorCompany | null>(`${this.baseUrl}/lookup/registration`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  findByTaxIdentificationNumber(taxIdentificationNumber: string): Observable<VendorCompany | null> {
    const params = new HttpParams().set('taxIdentificationNumber', taxIdentificationNumber);
    return this.http.get<VendorCompany | null>(`${this.baseUrl}/lookup/tax`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  findByBusinessLicenseNumber(businessLicenseNumber: string): Observable<VendorCompany | null> {
    const params = new HttpParams().set('businessLicenseNumber', businessLicenseNumber);
    return this.http.get<VendorCompany | null>(`${this.baseUrl}/lookup/license`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getByRegistrationNumber(registrationNumber: string): Observable<VendorCompany> {
    return this.http.get<VendorCompany>(`${this.baseUrl}/registration/${encodeURIComponent(registrationNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByTaxNumber(taxNumber: string): Observable<VendorCompany> {
    return this.http.get<VendorCompany>(`${this.baseUrl}/tax/${encodeURIComponent(taxNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByLicenseNumber(licenseNumber: string): Observable<VendorCompany> {
    return this.http.get<VendorCompany>(`${this.baseUrl}/license/${encodeURIComponent(licenseNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateVendorCompany(id: number, company: VendorCompany): Observable<VendorCompany> {
    return this.http.put<VendorCompany>(`${this.baseUrl}/${id}`, company).pipe(
      catchError(this.handleError)
    );
  }

  updateVendorCompanyDetails(id: number, company: VendorCompany): Observable<VendorCompany> {
    return this.http.put<VendorCompany>(`${this.baseUrl}/${id}/details`, company).pipe(
      catchError(this.handleError)
    );
  }

  updateStatus(id: number, status: string): Observable<VendorCompany> {
    const params = new HttpParams().set('status', status);
    
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/status`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateActiveStatus(id: number, isActive: boolean): Observable<VendorCompany> {
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/active-status`, { isActive }).pipe(
      catchError(this.handleError)
    );
  }

  updateVerificationStatus(id: number, isVerified: boolean): Observable<VendorCompany> {
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/verification-status`, { isVerified }).pipe(
      catchError(this.handleError)
    );
  }

  updateAverageRating(id: number, averageRating: number): Observable<VendorCompany> {
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/average-rating`, { averageRating }).pipe(
      catchError(this.handleError)
    );
  }

  updateTotalOrders(id: number, totalOrders: number): Observable<VendorCompany> {
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/total-orders`, { totalOrders }).pipe(
      catchError(this.handleError)
    );
  }

  updateTotalRevenue(id: number, totalRevenue: number): Observable<VendorCompany> {
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/total-revenue`, { totalRevenue }).pipe(
      catchError(this.handleError)
    );
  }

  updateCommissionRate(id: number, commissionRate: number): Observable<VendorCompany> {
    return this.http.patch<VendorCompany>(`${this.baseUrl}/${id}/commission-rate`, { commissionRate }).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  hardDeleteVendorCompany(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/hard`).pipe(
      catchError(this.handleError)
    );
  }

  softDeleteVendorCompany(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/soft`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getVendorCompanyCounts(): Observable<VendorCompanyStats> {
    return this.http.get<VendorCompanyStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  getTotalCompaniesCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/total`).pipe(
      catchError(this.handleError)
    );
  }

  countActiveVendorCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  countVerifiedVendorCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/verified`).pipe(
      catchError(this.handleError)
    );
  }

  countActiveAndVerifiedVendorCompanies(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active-verified`).pipe(
      catchError(this.handleError)
    );
  }

  getHighRatedCompaniesCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/high-rated`).pipe(
      catchError(this.handleError)
    );
  }

  getCompaniesWithMultipleProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/multiple-products`).pipe(
      catchError(this.handleError)
    );
  }

  getRecentlyEstablishedCompaniesCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/recently-established`).pipe(
      catchError(this.handleError)
    );
  }

  countVendorCompaniesByRatingRange(minRating: number, maxRating: number): Observable<number> {
    const params = new HttpParams()
      .set('minRating', minRating.toString())
      .set('maxRating', maxRating.toString());
    
    return this.http.get<number>(`${this.baseUrl}/count/rating-range`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  countVendorCompaniesByCategory(category: string): Observable<number> {
    const params = new HttpParams().set('category', category);
    return this.http.get<number>(`${this.baseUrl}/count/category`, {
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

  existsByContactEmail(contactEmail: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(contactEmail)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByBusinessRegistrationNumber(businessRegistrationNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/registration/${encodeURIComponent(businessRegistrationNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByTaxIdentificationNumber(taxIdentificationNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/tax/${encodeURIComponent(taxIdentificationNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByBusinessLicenseNumber(businessLicenseNumber: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/license/${encodeURIComponent(businessLicenseNumber)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByCompanyNameAndIdNot(companyName: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/name/${encodeURIComponent(companyName)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByContactEmailAndIdNot(contactEmail: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/email/${encodeURIComponent(contactEmail)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByBusinessRegistrationNumberAndIdNot(businessRegistrationNumber: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/registration/${encodeURIComponent(businessRegistrationNumber)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByTaxIdentificationNumberAndIdNot(taxIdentificationNumber: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/tax/${encodeURIComponent(taxIdentificationNumber)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByBusinessLicenseNumberAndIdNot(businessLicenseNumber: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/license/${encodeURIComponent(businessLicenseNumber)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }
}