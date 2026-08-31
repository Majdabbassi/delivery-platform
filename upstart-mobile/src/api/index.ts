import axios from 'axios';
import * as SecureStore from 'expo-secure-store';
import { UserRole, JwtResponse, User, Order, OrderStatus } from '../types';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://10.0.2.2:8080/api'; // Override via EXPO_PUBLIC_API_URL; default is Android emulator localhost bridge

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

api.interceptors.request.use(async (config) => {
  const token = await SecureStore.getItemAsync('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      const refreshToken = await SecureStore.getItemAsync('refreshToken');
      if (refreshToken) {
        try {
          const { data } = await axios.post(`${API_BASE_URL}/auth/refresh`, { refreshToken });
          await SecureStore.setItemAsync('accessToken', data.accessToken);
          await SecureStore.setItemAsync('refreshToken', data.refreshToken);
          original.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(original);
        } catch (refreshError) {
          await SecureStore.deleteItemAsync('accessToken');
          await SecureStore.deleteItemAsync('refreshToken');
        }
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: async (usernameOrEmail: string, password: string): Promise<JwtResponse> => {
    const response = await api.post('/auth/login', { usernameOrEmail, password });
    return response.data;
  },
  register: async (userData: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Promise<User> => {
    const response = await api.post('/users/register', userData);
    return response.data;
  },
  getMe: async (): Promise<User> => {
    const response = await api.get('/auth/me');
    return response.data;
  },
  logout: async (): Promise<void> => {
    await api.post('/auth/logout');
  },
};

export const orderApi = {
  getMyOrders: async (): Promise<Order[]> => {
    const response = await api.get('/orders/my');
    return response.data;
  },
  getOrder: async (id: number): Promise<Order> => {
    const response = await api.get(`/orders/${id}`);
    return response.data;
  },
  getOrderByTrackingNumber: async (trackingNumber: string): Promise<Order> => {
    const response = await api.get(`/orders/tracking/${trackingNumber}`);
    return response.data;
  },
  updateOrderStatus: async (id: number, status: OrderStatus): Promise<Order> => {
    const response = await api.patch(`/orders/${id}/status`, null, { params: { status } });
    return response.data;
  },
  cancelOrder: async (id: number, cancellationReason: string): Promise<Order> => {
    const response = await api.patch(`/orders/${id}/cancel`, null, { params: { cancellationReason } });
    return response.data;
  },
};

export interface DriverLocation {
  latitude: number;
  longitude: number;
  speedKmh?: number;
}

export const trackingApi = {
  updateLocation: async (orderId: number, location: DriverLocation): Promise<void> => {
    await api.post(`/tracking/orders/${orderId}/location`, {
      latitude: location.latitude,
      longitude: location.longitude,
      speedKmh: location.speedKmh,
    });
  },
  getOrderLocation: async (orderId: number): Promise<DriverLocation | null> => {
    const response = await api.get(`/tracking/orders/${orderId}/location`);
    const data = response.data;
    if (!data || data.latitude === undefined) return null;
    return data;
  },
};

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

export interface StatsMap {
  [key: string]: number | null;
}

export interface VendorCompanyMin {
  id: number;
  companyName?: string;
  isActive?: boolean;
  isVerified?: boolean;
}

export const dashboardApi = {
  getOverview: async (): Promise<DashboardOverview> => {
    const response = await api.get('/dashboard/overview');
    return response.data;
  },
  getSuperAdminStats: async (): Promise<StatsMap> => {
    const response = await api.get('/super-admins/statistics');
    return response.data;
  },
  getCustomerUserStats: async (): Promise<StatsMap> => {
    const response = await api.get('/customer-users/statistics');
    return response.data;
  },
  getDriverStats: async (): Promise<StatsMap> => {
    const response = await api.get('/driver-persons/stats/count');
    return response.data;
  },
  getProductStats: async (): Promise<StatsMap> => {
    const response = await api.get('/products/stats/count');
    return response.data;
  },
};

export const vendorApi = {
  getCompaniesByOwner: async (ownerId: number): Promise<VendorCompanyMin[]> => {
    const response = await api.get(`/vendor-companies/owner/${ownerId}`);
    return response.data;
  },
  getProductsByVendor: async (vendorId: number): Promise<Array<{ id: number; name?: string }>> => {
    const response = await api.get(`/products/vendor/${vendorId}`);
    return response.data;
  },
  getOrdersByVendorCompany: async (vendorCompanyId: number): Promise<Order[]> => {
    const response = await api.get(`/orders/vendor-company/${vendorCompanyId}`);
    return response.data;
  },
};

export { api, API_BASE_URL };
export const roleLabel = (role: UserRole): string => {
  switch (role) {
    case UserRole.SUPER_ADMIN:
      return 'Super Admin';
    case UserRole.VENDOR_OWNER:
      return 'Vendor Owner';
    case UserRole.DELIVERY_OWNER:
      return 'Delivery Owner';
    case UserRole.CLIENT:
      return 'Client';
    case UserRole.DRIVER:
      return 'Driver';
    default:
      return role;
  }
};