import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService, User } from '../../services/auth.service';
import {
  DashboardService,
  DashboardOverview,
  StatsMap
} from '../../services/dashboard.service';

interface MetricCard {
  icon: string;
  label: string;
  value: number | string;
  color: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  private userSubscription: Subscription = new Subscription();

  overview: DashboardOverview | null = null;
  customerStats: StatsMap | null = null;
  driverStats: StatsMap | null = null;
  superAdminStats: StatsMap | null = null;
  productStats: StatsMap | null = null;

  loading = true;
  errorMessage = '';

  kpiCards: MetricCard[] = [];
  entityCards: MetricCard[] = [];
  detailMetrics: MetricCard[] = [];

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    this.loadOverview();
    this.loadDetailStats();
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
  }

  get greeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }

  get displayName(): string {
    const user = this.currentUser;
    if (!user) return 'Administrator';
    return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username;
  }

  get roleLabel(): string {
    const role = this.currentUser?.role;
    switch (role) {
      case 'SUPER_ADMIN': return 'Super Administrator';
      case 'VENDOR_OWNER': return 'Vendor Owner';
      case 'DELIVERY_OWNER': return 'Delivery Owner';
      case 'CLIENT': return 'Client';
      case 'DRIVER': return 'Driver';
      default: return 'Administrator';
    }
  }

  private loadOverview(): void {
    this.dashboardService.getOverview().subscribe({
      next: (overview) => {
        this.overview = overview;
        this.buildCards(overview);
      },
      error: (error) => {
        console.error('Failed to load dashboard overview:', error);
        this.errorMessage = 'Unable to load dashboard data. Please try again later.';
        this.loading = false;
      }
    });
  }

  private loadDetailStats(): void {
    this.dashboardService.getCustomerUserStats().subscribe({
      next: (stats) => this.customerStats = stats,
      error: (error) => console.error('Failed to load customer stats:', error)
    });
    this.dashboardService.getDriverStats().subscribe({
      next: (stats) => this.driverStats = stats,
      error: (error) => console.error('Failed to load driver stats:', error)
    });
    this.dashboardService.getSuperAdminStats().subscribe({
      next: (stats) => this.superAdminStats = stats,
      error: (error) => console.error('Failed to load super admin stats:', error)
    });
    this.dashboardService.getProductStats().subscribe({
      next: (stats) => this.productStats = stats,
      error: (error) => console.error('Failed to load product stats:', error)
    });
  }

  private buildCards(overview: DashboardOverview): void {
    this.kpiCards = [
      { icon: '👥', label: 'Customers', value: overview.customers, color: 'blue' },
      { icon: '📋', label: 'Total Orders', value: overview.orders, color: 'indigo' },
      { icon: '💰', label: 'Revenue', value: this.formatCurrency(overview.totalRevenue), color: 'green' },
      { icon: '⏳', label: 'Pending Orders', value: overview.pendingOrders, color: 'amber' },
      { icon: '🚚', label: 'In Progress', value: overview.inProgressOrders, color: 'orange' },
      { icon: '✅', label: 'Completed', value: overview.completedOrders, color: 'teal' },
      { icon: '❌', label: 'Cancelled', value: overview.cancelledOrders, color: 'red' }
    ];

    this.entityCards = [
      { icon: '🏢', label: 'Vendor Companies', value: overview.vendorCompanies, color: 'blue' },
      { icon: '🏪', label: 'Vendor Owners', value: overview.vendorOwners, color: 'indigo' },
      { icon: '🚛', label: 'Delivery Companies', value: overview.deliveryCompanies, color: 'green' },
      { icon: '🚚', label: 'Delivery Owners', value: overview.deliveryOwners, color: 'amber' },
      { icon: '🚗', label: 'Drivers', value: overview.drivers, color: 'orange' },
      { icon: '📦', label: 'Products', value: overview.products, color: 'teal' },
      { icon: '🔐', label: 'Admins', value: overview.admins, color: 'purple' },
      { icon: '🤝', label: 'Partnerships', value: overview.partnerships, color: 'pink' }
    ];

    this.detailMetrics = [
      { icon: '⭐', label: 'Premium Customers', value: this.value(this.customerStats, 'premium'), color: 'amber' },
      { icon: '📦', label: 'Customers (Multiple Orders)', value: this.value(this.customerStats, 'withMultipleOrders'), color: 'indigo' },
      { icon: '✅', label: 'Verified Drivers', value: this.value(this.driverStats, 'verified'), color: 'green' },
      { icon: '🏆', label: 'Experienced Drivers', value: this.value(this.driverStats, 'experienced'), color: 'teal' },
      { icon: '🛡️', label: 'High Security Admins', value: this.value(this.superAdminStats, 'highSecurityLevel'), color: 'red' },
      { icon: '💾', label: 'Avg Security Level', value: this.value(this.superAdminStats, 'averageSecurityLevel'), color: 'purple' },
      { icon: '📉', label: 'Low Stock Products', value: this.value(this.productStats, 'lowStock'), color: 'orange' },
      { icon: '🔥', label: 'High Rated Products', value: this.value(this.productStats, 'highRated'), color: 'pink' }
    ];

    this.loading = false;
  }

  private value(stats: StatsMap | null, key: string): number | string {
    if (!stats || stats[key] === undefined || stats[key] === null) {
      return 0;
    }
    return stats[key];
  }

  formatCurrency(value: number | string | undefined): string {
    const numeric = typeof value === 'number' ? value : Number(value ?? 0);
    if (isNaN(numeric)) return '0';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      maximumFractionDigits: 2
    }).format(numeric);
  }
}