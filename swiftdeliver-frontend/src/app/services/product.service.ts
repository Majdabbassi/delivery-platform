import { PaginatedResponse } from '../models/paginated-response';

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config';

// Product Interface
export interface Product {
  id?: number;
  name: string;
  description: string;
  price: number;
  costPrice: number;
  category: string;
  subcategory?: string;
  brand?: string;
  sku: string;
  barcode?: string;
  weight?: number;
  dimensions?: string;
  color?: string;
  size?: string;
  material?: string;
  stockQuantity: number;
  minStockLevel?: number;
  maxStockLevel?: number;
  status: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK' | 'DISCONTINUED';
  isAvailable: boolean;
  isFeatured?: boolean;
  rating?: number;
  reviewCount?: number;
  imageUrls?: string[];
  tags?: string[];
  specifications?: { [key: string]: string };
  warranty?: string;
  manufacturerDate?: string;
  expiryDate?: string;
  vendorCompanyId?: number;
  totalSold?: number;
  totalRevenue?: number;
  lastOrderDate?: string;
  lastUpdated?: string;
  discount?: number;
  discountType?: 'PERCENTAGE' | 'FIXED';
  discountStartDate?: string;
  discountEndDate?: string;
  createdAt?: string;
  updatedAt?: string;
}


export interface ProductSearchParams {
  name?: string;
  description?: string;
  category?: string;
  subcategory?: string;
  brand?: string;
  sku?: string;
  barcode?: string;
  status?: string;
  available?: boolean;
  featured?: boolean;
  vendorCompanyId?: number;
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  minStock?: number;
  maxStock?: number;
  color?: string;
  productSize?: string;
  material?: string;
  tag?: string;
  hasDiscount?: boolean;
  createdAfter?: string;
  createdBefore?: string;
  manufacturedAfter?: string;
  manufacturedBefore?: string;
  expiryAfter?: string;
  expiryBefore?: string;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDir?: string;
}

export interface ProductStats {
  total: number;
  active: number;
  available: number;
  featured: number;
  outOfStock: number;
  lowStock: number;
  highRated: number;
  withDiscount: number;
  totalValue: number;
  byCategory: { [key: string]: number };
  byStatus: { [key: string]: number };
}

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly baseUrl = `${API_BASE_URL}/products`;

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Remove the getAuthHeaders method as it's no longer needed
  // private getAuthHeaders(): HttpHeaders {
  //   return this.authService.getAuthHeaders();
  // }

  private handleError = (error: any): Observable<never> => {
    console.error('Product service error:', error);
    throw error;
  }

  // Create operations
  createProduct(product: Product): Observable<Product> {
    return this.http.post<Product>(this.baseUrl, product).pipe(
      catchError(this.handleError)
    );
  }

  // Read operations
  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  getAllProducts(page: number = 0, size: number = 10, sortBy: string = 'id', sortDir: string = 'asc'): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);

    return this.http.get<PaginatedResponse<Product>>(this.baseUrl, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchProducts(searchParams: ProductSearchParams): Observable<PaginatedResponse<Product>> {
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

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/search`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Quick search endpoints
  searchByName(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/search/name`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  searchByDescription(searchTerm: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('searchTerm', searchTerm)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/search/description`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getAvailableProducts(page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/available`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getFeaturedProducts(page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/featured`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getHighRatedProducts(page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/high-rated`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getLowStockProducts(page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/low-stock`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getOutOfStockProducts(page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/out-of-stock`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getDiscountedProducts(page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/discounted`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getNewProducts(since?: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (since) {
      params = params.set('since', since);
    }

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/new`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getProductsByCategory(category: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/category/${encodeURIComponent(category)}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getProductsByBrand(brand: string, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/brand/${encodeURIComponent(brand)}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getProductsByVendor(vendorId: number, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/vendor/${vendorId}`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  getProductsByPriceRange(minPrice: number, maxPrice: number, page: number = 0, size: number = 10): Observable<PaginatedResponse<Product>> {
    const params = new HttpParams()
      .set('minPrice', minPrice.toString())
      .set('maxPrice', maxPrice.toString())
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Product>>(`${this.baseUrl}/price-range`, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Lookup endpoints
  getBySku(sku: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/sku/${encodeURIComponent(sku)}`).pipe(
      catchError(this.handleError)
    );
  }

  getByBarcode(barcode: string): Observable<Product> {
    return this.http.get<Product>(`${this.baseUrl}/barcode/${encodeURIComponent(barcode)}`).pipe(
      catchError(this.handleError)
    );
  }

  // Update operations
  updateProduct(id: number, product: Product): Observable<Product> {
    return this.http.put<Product>(`${this.baseUrl}/${id}`, product).pipe(
      catchError(this.handleError)
    );
  }

  updateStatus(id: number, status: string): Observable<Product> {
    const params = new HttpParams().set('status', status);
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/status`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateAvailability(id: number, available: boolean): Observable<Product> {
    const params = new HttpParams().set('available', available.toString());
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/availability`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateFeaturedStatus(id: number, featured: boolean): Observable<Product> {
    const params = new HttpParams().set('featured', featured.toString());
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/featured`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updatePrice(id: number, price: number): Observable<Product> {
    const params = new HttpParams().set('price', price.toString());
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/price`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateStock(id: number, quantity: number): Observable<Product> {
    const params = new HttpParams().set('quantity', quantity.toString());
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/stock`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateRating(id: number, rating: number): Observable<Product> {
    const params = new HttpParams().set('rating', rating.toString());
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/rating`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  updateDiscount(id: number, discount: number, discountType: string, startDate?: string, endDate?: string): Observable<Product> {
    let params = new HttpParams()
      .set('discount', discount.toString())
      .set('discountType', discountType);
    
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    
    return this.http.patch<Product>(`${this.baseUrl}/${id}/discount`, null, {
      params
    }).pipe(
      catchError(this.handleError)
    );
  }

  // Delete operations
  deleteProduct(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  softDeleteProduct(id: number): Observable<{message: string}> {
    return this.http.delete<{message: string}>(`${this.baseUrl}/${id}/soft`).pipe(
      catchError(this.handleError)
    );
  }

  // Statistical endpoints
  getProductCounts(): Observable<ProductStats> {
    return this.http.get<ProductStats>(`${this.baseUrl}/stats/count`).pipe(
      catchError(this.handleError)
    );
  }

  getProductStats(): Observable<ProductStats> {
    return this.getProductCounts();
  }

  getTotalProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/total`).pipe(
      catchError(this.handleError)
    );
  }

  getActiveProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/active`).pipe(
      catchError(this.handleError)
    );
  }

  getAvailableProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/available`).pipe(
      catchError(this.handleError)
    );
  }

  getFeaturedProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/featured`).pipe(
      catchError(this.handleError)
    );
  }

  getOutOfStockProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/out-of-stock`).pipe(
      catchError(this.handleError)
    );
  }

  getLowStockProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/low-stock`).pipe(
      catchError(this.handleError)
    );
  }

  getHighRatedProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/high-rated`).pipe(
      catchError(this.handleError)
    );
  }

  getDiscountedProductsCount(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/stats/count/discounted`).pipe(
      catchError(this.handleError)
    );
  }

  // Validation endpoints
  existsBySku(sku: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/sku/${encodeURIComponent(sku)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByBarcode(barcode: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/barcode/${encodeURIComponent(barcode)}`).pipe(
      catchError(this.handleError)
    );
  }

  existsBySkuAndIdNot(sku: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/sku/${encodeURIComponent(sku)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  existsByBarcodeAndIdNot(barcode: string, id: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/exists/barcode/${encodeURIComponent(barcode)}/exclude/${id}`).pipe(
      catchError(this.handleError)
    );
  }

  // Utility methods
  getCategories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/categories`).pipe(
      catchError(this.handleError)
    );
  }

  getBrands(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/brands`).pipe(
      catchError(this.handleError)
    );
  }

  getTags(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/tags`).pipe(
      catchError(this.handleError)
    );
  }
}