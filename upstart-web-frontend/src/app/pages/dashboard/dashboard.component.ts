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
  standalone: false,
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
      [OrderStatus.COMPLETED, OrderStatus.DELIVERED].includes(o.status)
    ).length;

    this.myActivityCards = [
      { icon: '📋', label: 'Total Orders', value: orders.length, color: 'blue' },
      { icon: '🚚', label: 'In Progress', value: inProgress, color: 'orange' },
      { icon: '✅', label: 'Completed', value: completed, color: 'teal' },
      { icon: '⏳', label: 'Pending', value: orders.filter(o => o.status === OrderStatus.PENDING).length, color: 'amber' },
      { icon: '💵', label: 'Total Value', value: this.formatCurrency(orders.reduce((sum, o) => sum + (o.totalAmount || o.orderValue || 0), 0)), color: 'green' },
      { icon: '❌', label: 'Cancelled', value: orders.filter(o => o.status === OrderStatus.CANCELLED).length, color: 'red' }
    ];
  }

  private buildQuickLinks(): void {
    const role = this.currentUser?.role;
    switch (role) {
      case UserRole.VENDOR_OWNER:
        this.quickLinks = [
          { icon: '🏢', label: 'My Companies', detail: 'Manage vendor companies', route: '/vendorcompanies' },
          { icon: '📦', label: 'Products', detail: 'Manage your product catalog', route: '/products' },
          { icon: '📋', label: 'Orders', detail: 'View and manage orders', route: '/orders' },
          { icon: '🤝', label: 'Partnerships', detail: 'View partnership agreements', route: '/partnerships' }
        ];
        break;
      case UserRole.DELIVERY_OWNER:
        this.quickLinks = [
          { icon: '🚛', label: 'My Companies', detail: 'Manage delivery companies', route: '/deliverycompanies' },
          { icon: '🚗', label: 'Drivers', detail: 'Manage your driver fleet', route: '/drivers' },
          { icon: '📋', label: 'Orders', detail: 'View and manage orders', route: '/orders' },
          { icon: '🤝', label: 'Partnerships', detail: 'View partnership agreements', route: '/partnerships' }
        ];
        break;
      case UserRole.CLIENT:
        this.quickLinks = [
          { icon: '📋', label: 'My Orders', detail: 'Track and manage your orders', route: '/orders' },
          { icon: '🔍', label: 'Track Package', detail: 'Live location tracking', route: '/tracking' }
        ];
        break;
      case UserRole.DRIVER:
        this.quickLinks = [
          { icon: '🧭', label: 'My Jobs', detail: 'Assigned delivery jobs & live updates', route: '/my-jobs' },
          { icon: '🔍', label: 'Track Delivery', detail: 'Live location tracking', route: '/tracking' }
        ];
        break;
      default:
        this.quickLinks = [
          { icon: '📋', label: 'Orders', detail: 'View orders', route: '/orders' },
          { icon: '🔍', label: 'Track Package', detail: 'Live location tracking', route: '/tracking' }
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