import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderPriority } from './order.service';
import { API_BASE_URL } from '../config';

export interface OrderPoolStatistics {
  currentOrderCount: number;
  totalValue: number;
  priorityDistribution: { LOW: number; NORMAL: number; HIGH: number; URGENT: number };
  ordersAdded: number;
  ordersRemoved: number;
  ordersViewed: number;
  lastActivity: string;
}

export interface OrderPoolFilter {
  minValue?: number;
  maxValue?: number;
  priority?: OrderPriority | '';
  sortBy?: string;
  sortDirection?: string;
  limit?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PoolService {
  private readonly baseUrl = `${API_BASE_URL}/pool`;

  constructor(private http: HttpClient) {}

  getPoolStats(deliveryCompanyId?: number): Observable<OrderPoolStatistics> {
    let params = new HttpParams();
    if (deliveryCompanyId != null) {
      params = params.set('deliveryCompanyId', deliveryCompanyId.toString());
    }
    return this.http.get<OrderPoolStatistics>(`${this.baseUrl}/stats`, { params });
  }

  getRecommendedOrders(limit: number = 20, deliveryCompanyId?: number): Observable<any[]> {
    let params = new HttpParams().set('limit', limit.toString());
    if (deliveryCompanyId != null) {
      params = params.set('deliveryCompanyId', deliveryCompanyId.toString());
    }
    return this.http.get<any[]>(`${this.baseUrl}/orders/recommended`, { params });
  }

  getFilteredOrders(filter: OrderPoolFilter, deliveryCompanyId?: number): Observable<any[]> {
    let params = new HttpParams();
    if (deliveryCompanyId != null) {
      params = params.set('deliveryCompanyId', deliveryCompanyId.toString());
    }
    if (filter.minValue != null) {
      params = params.set('minValue', filter.minValue.toString());
    }
    if (filter.maxValue != null) {
      params = params.set('maxValue', filter.maxValue.toString());
    }
    if (filter.priority) {
      params = params.set('priority', filter.priority);
    }
    params = params.set('sortBy', filter.sortBy || 'created');
    params = params.set('sortDirection', filter.sortDirection || 'desc');
    params = params.set('limit', (filter.limit || 50).toString());
    return this.http.get<any[]>(`${this.baseUrl}/orders/filtered`, { params });
  }

  markOrderViewed(orderId: number, deliveryCompanyId?: number): Observable<void> {
    let params = new HttpParams();
    if (deliveryCompanyId != null) {
      params = params.set('deliveryCompanyId', deliveryCompanyId.toString());
    }
    return this.http.post<void>(`${this.baseUrl}/orders/${orderId}/viewed`, null, { params });
  }
}