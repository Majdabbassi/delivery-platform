import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';

export interface DashboardOverview {
  customers: number;
  orders: number;
  totalRevenue: number;
  pendingOrders: number;
  inProgressOrders: number;
  completedOrders: number;
  cancelledOrders: number;
  vendorCompanies: number;
  vendorOwners: number;
  deliveryCompanies: number;
  deliveryOwners: number;
  drivers: number;
  products: number;
  admins: number;
  partnerships: number;
}

export type StatsMap = Record<string, number | string>;

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(private http: HttpClient) {}

  getOverview(): Observable<DashboardOverview> {
    return this.http.get<DashboardOverview>(`${API_BASE_URL}/dashboard/overview`);
  }

  getCustomerUserStats(): Observable<StatsMap> {
    return this.http.get<StatsMap>(`${API_BASE_URL}/customer-users/statistics`);
  }

  getDriverStats(): Observable<StatsMap> {
    return this.http.get<StatsMap>(`${API_BASE_URL}/driver-persons/stats/count`);
  }

  getSuperAdminStats(): Observable<StatsMap> {
    return this.http.get<StatsMap>(`${API_BASE_URL}/super-admins/statistics`);
  }

  getProductStats(): Observable<StatsMap> {
    return this.http.get<StatsMap>(`${API_BASE_URL}/products/stats/count`);
  }
}