import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { AuthService, User, UserRole } from '../../services/auth.service';
import {
  DashboardService,
  DashboardOverview,
  StatsMap,
  AssignmentStatistics
} from '../../services/dashboard.service';
import { OrderService, OrderDTO, OrderStatus, OrderPriority } from '../../services/order.service';

interface MetricCard {
  icon: string;
  label: string;
  value: number | string;
  color: string;
}

interface QuickLink {
  icon: string;
  label: string;
  detail: string;
  route: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  private userSubscription: Subscription = new Subscription();
  private orderSubscription: Subscription = new Subscription();

  overview: DashboardOverview | null = null;
  customerStats: StatsMap | null = null;
  driverStats: StatsMap | null = null;
  superAdminStats: StatsMap | null = null;
  productStats: StatsMap | null = null;
  assignmentStats: AssignmentStatistics | null = null;

  loading = true;
  errorMessage = '';

  kpiCards: MetricCard[] = [];
  entityCards: MetricCard[] = [];
  detailMetrics: MetricCard[] = [];

  myOrders: OrderDTO[] = [];
  myActivityCards: MetricCard[] = [];
  quickLinks: QuickLink[] = [];

  OrderStatus = OrderStatus;
  OrderPriority = OrderPriority;

  constructor(
    private authService: AuthService,
    private dashboardService: DashboardService,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.userSubscription = this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
    });

    const user = this.authService.getCurrentUser();
    if (user?.role === UserRole.SUPER_ADMIN) {
      this.loadOverview();
      this.loadDetailStats();
      this.loadAssignmentStats();
    } else {
      this.loadMyActivity();
    }
  }

  ngOnDestroy(): void {
    this.userSubscription.unsubscribe();
    this.orderSubscription.unsubscribe();
  }

  get isSuperAdmin(): boolean {
    return this.currentUser?.role === UserRole.SUPER_ADMIN;
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

  private loadMyActivity(): void {
    this.buildQuickLinks();
    this.orderSubscription.add(
      this.orderService.getMyOrders().subscribe({
        next: (orders) => {
          this.myOrders = orders;
          this.calculateMyActivity(orders);
          this.loading = false;
        },
        error: () => {
          this.errorMessage = 'Unable to load your activity. Please try again later.';
          this.loading = false;
        }
      })
    );
  }

  private calculateMyActivity(orders: OrderDTO[]): void {
    const inProgress = orders.filter(o =>
      [OrderStatus.ASSIGNED, OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS,
       OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT].includes(o.status)
    ).length;
    const completed = orders.filter(o =>
      o.status === OrderStatus.DELIVERED
    ).length;

this.myActivityCards = [
      { icon: 'fa-solid fa-clipboard-list', label: 'Total Orders', value: orders.length, color: 'blue' },
      { icon: 'fa-solid fa-truck', label: 'In Progress', value: inProgress, color: 'orange' },
      { icon: 'fa-solid fa-circle-check', label: 'Completed', value: completed, color: 'teal' },
      { icon: 'fa-solid fa-clock', label: 'Pending', value: orders.filter(o => o.status === OrderStatus.PENDING).length, color: 'amber' },
      { icon: 'fa-solid fa-dollar-sign', label: 'Total Value', value: this.formatCurrency(orders.reduce((sum, o) => sum + (o.totalAmount || o.orderValue || 0), 0)), color: 'green' },
      { icon: 'fa-solid fa-ban', label: 'Cancelled', value: orders.filter(o => o.status === OrderStatus.CANCELLED).length, color: 'red' }
    ];
  }

  private buildQuickLinks(): void {
    const role = this.currentUser?.role;
    switch (role) {
case UserRole.VENDOR_OWNER:
        this.quickLinks = [
          { icon: 'fa-solid fa-building', label: 'My Companies', detail: 'Manage vendor companies', route: '/vendorcompanies' },
          { icon: 'fa-solid fa-box', label: 'Products', detail: 'Manage your product catalog', route: '/products' },
          { icon: 'fa-solid fa-clipboard-list', label: 'Orders', detail: 'View and manage orders', route: '/orders' },
          { icon: 'fa-solid fa-handshake', label: 'Partnerships', detail: 'View partnership agreements', route: '/partnerships' }
        ];
        break;
      case UserRole.DELIVERY_OWNER:
        this.quickLinks = [
          { icon: 'fa-solid fa-truck-fast', label: 'My Companies', detail: 'Manage delivery companies', route: '/deliverycompanies' },
          { icon: 'fa-solid fa-cart-shopping', label: 'Marketplace', detail: 'Browse and bid on available orders', route: '/pool' },
          { icon: 'fa-solid fa-clipboard-list', label: 'Orders', detail: 'View and manage orders', route: '/orders' },
          { icon: 'fa-solid fa-handshake', label: 'Partnerships', detail: 'View partnership agreements', route: '/partnerships' }
        ];
        break;
      case UserRole.CLIENT:
        this.quickLinks = [
          { icon: 'fa-solid fa-clipboard-list', label: 'My Orders', detail: 'Track and manage your orders', route: '/orders' },
          { icon: 'fa-solid fa-magnifying-glass', label: 'Track Package', detail: 'Live location tracking', route: '/tracking' }
        ];
        break;
      case UserRole.DRIVER:
        this.quickLinks = [
          { icon: 'fa-solid fa-compass', label: 'My Jobs', detail: 'Assigned delivery jobs & live updates', route: '/my-jobs' },
          { icon: 'fa-solid fa-magnifying-glass', label: 'Track Delivery', detail: 'Live location tracking', route: '/tracking' }
        ];
        break;
      default:
        this.quickLinks = [
          { icon: 'fa-solid fa-clipboard-list', label: 'Orders', detail: 'View orders', route: '/orders' },
          { icon: 'fa-solid fa-magnifying-glass', label: 'Track Package', detail: 'Live location tracking', route: '/tracking' }
        ];
    }
  }

  private loadAssignmentStats(): void {
    this.dashboardService.getAssignmentStatistics().subscribe({
      next: (stats) => this.assignmentStats = stats,
      error: (error) => console.error('Failed to load assignment statistics:', error)
    });
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
      { icon: 'fa-solid fa-users', label: 'Customers', value: overview.customers, color: 'blue' },
      { icon: 'fa-solid fa-clipboard-list', label: 'Total Orders', value: overview.orders, color: 'indigo' },
      { icon: 'fa-solid fa-dollar-sign', label: 'Revenue', value: this.formatCurrency(overview.totalRevenue), color: 'green' },
      { icon: 'fa-solid fa-clock', label: 'Pending Orders', value: overview.pendingOrders, color: 'amber' },
      { icon: 'fa-solid fa-truck', label: 'In Progress', value: overview.inProgressOrders, color: 'orange' },
      { icon: 'fa-solid fa-circle-check', label: 'Completed', value: overview.completedOrders, color: 'teal' },
      { icon: 'fa-solid fa-ban', label: 'Cancelled', value: overview.cancelledOrders, color: 'red' }
    ];

    this.entityCards = [
      { icon: 'fa-solid fa-building', label: 'Vendor Companies', value: overview.vendorCompanies, color: 'blue' },
      { icon: 'fa-solid fa-shop', label: 'Vendor Owners', value: overview.vendorOwners, color: 'indigo' },
      { icon: 'fa-solid fa-truck-fast', label: 'Delivery Companies', value: overview.deliveryCompanies, color: 'green' },
      { icon: 'fa-solid fa-truck', label: 'Delivery Owners', value: overview.deliveryOwners, color: 'amber' },
      { icon: 'fa-solid fa-car', label: 'Drivers', value: overview.drivers, color: 'orange' },
      { icon: 'fa-solid fa-box', label: 'Products', value: overview.products, color: 'teal' },
      { icon: 'fa-solid fa-lock', label: 'Admins', value: overview.admins, color: 'purple' },
      { icon: 'fa-solid fa-handshake', label: 'Partnerships', value: overview.partnerships, color: 'pink' }
    ];

    this.detailMetrics = [
      { icon: 'fa-solid fa-star', label: 'Premium Customers', value: this.value(this.customerStats, 'premium'), color: 'amber' },
      { icon: 'fa-solid fa-box', label: 'Customers (Multiple Orders)', value: this.value(this.customerStats, 'withMultipleOrders'), color: 'indigo' },
      { icon: 'fa-solid fa-circle-check', label: 'Verified Drivers', value: this.value(this.driverStats, 'verified'), color: 'green' },
      { icon: 'fa-solid fa-award', label: 'Experienced Drivers', value: this.value(this.driverStats, 'experienced'), color: 'teal' },
      { icon: 'fa-solid fa-shield-halved', label: 'High Security Admins', value: this.value(this.superAdminStats, 'highSecurityLevel'), color: 'red' },
      { icon: 'fa-solid fa-database', label: 'Avg Security Level', value: this.value(this.superAdminStats, 'averageSecurityLevel'), color: 'purple' },
      { icon: 'fa-solid fa-chart-line', label: 'Low Stock Products', value: this.value(this.productStats, 'lowStock'), color: 'orange' },
      { icon: 'fa-solid fa-fire', label: 'High Rated Products', value: this.value(this.productStats, 'highRated'), color: 'pink' }
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