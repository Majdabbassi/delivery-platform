import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import {
  PoolService,
  OrderPoolStatistics,
  OrderPoolFilter
} from '../../services/pool.service';
import {
  BidService,
  Bid,
  BidStatus,
  BidderType,
  BidStatistics
} from '../../services/bid.service';
import {
  OrderService,
  OrderDTO,
  OrderPriority,
  OrderType
} from '../../services/order.service';
import { DeliveryCompanyService, DeliveryCompany } from '../../services/delivery-company.service';
import { DriverPersonService, DriverPerson } from '../../services/driver-person.service';
import { AuthService, User, UserRole } from '../../services/auth.service';

interface StatCard {
  icon: string;
  label: string;
  value: number | string;
  color: string;
}

interface PoolOrder extends OrderDTO {
  myBid?: Bid;
}

interface BidDraft {
  orderId: number;
  bidAmount: number | null;
  estimatedDeliveryTime: string;
  message: string;
}

@Component({
  selector: 'app-pool',
  templateUrl: './pool.component.html',
  styleUrls: ['./pool.component.css'],
  standalone: false
})
export class PoolComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;

  stats: OrderPoolStatistics | null = null;
  bidStats: BidStatistics | null = null;
  recommended: PoolOrder[] = [];
  browseOrders: PoolOrder[] = [];
  allOrders: PoolOrder[] = [];

  statCards: StatCard[] = [];
  ownedCompanyName = '';
  ownedCompanyId: number | null = null;
  driverPerson: DriverPerson | null = null;
  driverId: number | null = null;

  // Delivery company selector for SUPER_ADMIN (required by the backend).
  deliveryCompanies: DeliveryCompany[] = [];
  selectedCompanyId: number | null = null;

  // Browse filters
  filters: OrderPoolFilter = {
    priority: '',
    sortBy: 'created',
    sortDirection: 'desc',
    limit: 50
  };
  priorityOptions = Object.values(OrderPriority);
  sortByOptions = [
    { value: 'created', label: 'Newest' },
    { value: 'orderAmount', label: 'Order Value' },
    { value: 'priority', label: 'Priority' },
    { value: 'distanceKm', label: 'Distance' }
  ];

  // Bid form state
  bidForms: { [orderId: number]: BidDraft } = {};
  expandedOrderId: number | null = null;
  flatBids: Bid[] = [];

  loading = false;
  error: string | null = null;

  UserRole = UserRole;
  BidStatus = BidStatus;

  private subscriptions: Subscription[] = [];

  constructor(
    private poolService: PoolService,
    private bidService: BidService,
    private orderService: OrderService,
    private deliveryCompanyService: DeliveryCompanyService,
    private driverPersonService: DriverPersonService,
    private authService: AuthService
  ) {}

  get isSuperAdmin(): boolean {
    return this.currentUser?.role === UserRole.SUPER_ADMIN;
  }

  get isDriver(): boolean {
    return this.currentUser?.role === UserRole.DRIVER;
  }

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadMarketplace();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private resolveCompany(): Promise<void> {
    if (!this.currentUser) return Promise.resolve();
    if (this.currentUser.role === UserRole.DELIVERY_OWNER) {
      return new Promise((resolve) => {
        this.subscriptions.push(
          this.deliveryCompanyService.getDeliveryCompaniesByOwner(this.currentUser!.id).subscribe({
            next: (companies) => {
              if (companies && companies.length > 0) {
                this.ownedCompanyId = companies[0].id ?? null;
                this.ownedCompanyName = companies[0].name || '';
              }
              resolve();
            },
            error: () => resolve()
          })
        );
      });
    }
    if (this.currentUser.role === UserRole.DRIVER) {
      return new Promise((resolve) => {
        this.subscriptions.push(
          this.driverPersonService.getCurrentDriver().subscribe({
            next: (driver) => {
              this.driverPerson = driver;
              this.driverId = driver.id ?? null;
              resolve();
            },
            error: () => resolve()
          })
        );
      });
    }
    if (this.currentUser.role === UserRole.SUPER_ADMIN) {
      return new Promise((resolve) => {
        this.subscriptions.push(
          this.deliveryCompanyService.getAllDeliveryCompanies(0, 100).subscribe({
            next: (response) => {
              this.deliveryCompanies = response.content || [];
              if (this.deliveryCompanies.length > 0) {
                this.selectedCompanyId = this.deliveryCompanies[0].id ?? null;
              }
              resolve();
            },
            error: () => resolve()
          })
        );
      });
    }
    return Promise.resolve();
  }

  loadMarketplace(): void {
    this.error = null;
    this.loading = true;
    this.resolveCompany().then(() => {
      if (this.isDriver) {
        // Drivers browse the open-for-bid pool via the order endpoint.
        this.loadDriverOpenForBid();
        this.loadMyBids();
        this.loading = false;
        return;
      }
      this.loadStats();
      this.loadRecommended();
      this.loadMyBids();
      this.loadBrowse();
    });
  }

  private loadDriverOpenForBid(): void {
    this.subscriptions.push(
      this.orderService.getOpenForBidOrders().subscribe({
        next: (orders) => {
          const poolOrders = this.toPoolOrders(orders);
          this.recommended = poolOrders;
          this.browseOrders = poolOrders;
          this.applyMyBids(this.flatBids);
        },
        error: (err) => {
          console.error('Error loading open-for-bid orders:', err);
          this.error = 'Failed to load available orders.';
          this.loading = false;
        }
      })
    );
  }

  private resolveDeliveryCompanyId(): number | undefined {
    return this.isSuperAdmin ? this.selectedCompanyId ?? undefined : undefined;
  }

  private loadStats(): void {
    this.subscriptions.push(
      this.poolService.getPoolStats(this.resolveDeliveryCompanyId()).subscribe({
        next: (stats) => {
          this.stats = stats;
          this.buildStatCards();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading pool stats:', err);
          this.error = 'Failed to load marketplace stats.';
          this.loading = false;
        }
      })
    );
  }

  private loadRecommended(): void {
    this.subscriptions.push(
      this.poolService.getRecommendedOrders(20, this.resolveDeliveryCompanyId()).subscribe({
        next: (orders) => {
          this.recommended = this.toPoolOrders(orders);
        },
        error: (err) => console.error('Error loading recommended orders:', err)
      })
    );
  }

  private loadBrowse(): void {
    if (this.isDriver) {
      this.loadDriverOpenForBid();
      return;
    }
    this.subscriptions.push(
      this.poolService.getFilteredOrders(this.filters, this.resolveDeliveryCompanyId()).subscribe({
        next: (orders) => {
          this.browseOrders = this.toPoolOrders(orders);
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading filtered orders:', err);
          this.error = 'Failed to load marketplace orders.';
          this.loading = false;
        }
      })
    );
  }

  loadMyBids(): void {
    if (this.isDriver) {
      this.subscriptions.push(
        this.bidService.getMyDriverBids(this.driverId ?? undefined).subscribe({
          next: (page) => {
            const bids: Bid[] = page.content || [];
            this.flatBids = bids;
            this.allOrders.forEach(o => {
              o.myBid = this.findSubmittedBid(bids, o.id);
            });
            this.applyMyBids(bids);
          },
          error: (err) => console.error('Error loading my driver bids:', err)
        })
      );
      if (this.driverId != null) {
        this.subscriptions.push(
          this.bidService.getMyDriverBidStats(this.driverId).subscribe({
            next: (stats) => { this.bidStats = stats; },
            error: (err) => console.error('Error loading driver bid stats:', err)
          })
        );
      }
      return;
    }

    const companyId = this.isSuperAdmin ? this.selectedCompanyId : this.ownedCompanyId;
    this.subscriptions.push(
      this.bidService.getMyBids(companyId ?? undefined).subscribe({
        next: (page) => {
          const bids: Bid[] = page.content || [];
          const byOrder: { [orderId: number]: Bid } = {};
          bids.forEach(b => {
            if (b.status === BidStatus.SUBMITTED && !byOrder[b.orderId]) {
              byOrder[b.orderId] = b;
            }
          });
          this.allOrders.forEach(o => {
            o.myBid = byOrder[o.id];
          });
          this.applyMyBids(bids);
        },
        error: (err) => console.error('Error loading my bids:', err)
      })
    );
    if (companyId != null) {
      this.subscriptions.push(
        this.bidService.getMyBidStats(companyId).subscribe({
          next: (stats) => { this.bidStats = stats; },
          error: (err) => console.error('Error loading bid stats:', err)
        })
      );
    }
  }

  private findSubmittedBid(bids: Bid[], orderId: number): Bid | undefined {
    return bids.find(b => b.orderId === orderId && b.status === BidStatus.SUBMITTED);
  }

  private applyMyBids(bids: Bid[]): void {
    const byOrder: { [orderId: number]: Bid } = {};
    bids.forEach(b => {
      if (b.status === BidStatus.SUBMITTED && !byOrder[b.orderId]) {
        byOrder[b.orderId] = b;
      }
    });
    this.recommended.forEach(o => { o.myBid = byOrder[o.id]; });
    this.browseOrders.forEach(o => { o.myBid = byOrder[o.id]; });
  }

  private toPoolOrders(orders: any[]): PoolOrder[] {
    return (orders || []).map(o => this.orderService.mapOrderEntity(o) as PoolOrder);
  }

  private buildStatCards(): void {
    if (!this.stats) return;
    const dist = this.stats.priorityDistribution || {};
    this.statCards = [
      { icon: '📦', label: 'Available Orders', value: this.stats.currentOrderCount, color: 'blue' },
      { icon: '💰', label: 'Pool Value', value: this.formatCurrency(this.stats.totalValue), color: 'teal' },
      { icon: '🚨', label: 'Urgent', value: dist.URGENT || 0, color: 'red' },
      { icon: '🔥', label: 'High Priority', value: dist.HIGH || 0, color: 'amber' },
      { icon: '📥', label: 'Added Today', value: this.stats.ordersAdded, color: 'green' },
      { icon: '👀', label: 'Viewed', value: this.stats.ordersViewed, color: 'indigo' }
    ];
  }

  applyFilters(): void {
    this.loading = true;
    this.error = null;
    this.loadBrowse();
  }

  resetFilters(): void {
    this.filters = { priority: '', sortBy: 'created', sortDirection: 'desc', limit: 50 };
    this.applyFilters();
  }

  onCompanyChange(): void {
    this.loadMarketplace();
  }

  toggleBidForm(order: PoolOrder): void {
    if (this.expandedOrderId === order.id) {
      this.expandedOrderId = null;
      return;
    }
    this.expandedOrderId = order.id;
    if (!this.bidForms[order.id]) {
      this.bidForms[order.id] = {
        orderId: order.id,
        bidAmount: order.orderValue ? Math.round(order.orderValue * 0.9) : null,
        estimatedDeliveryTime: '',
        message: ''
      };
    }
  }

  getBidForm(order: PoolOrder): BidDraft {
    if (!this.bidForms[order.id]) {
      this.bidForms[order.id] = {
        orderId: order.id,
        bidAmount: order.orderValue ? Math.round(order.orderValue * 0.9) : null,
        estimatedDeliveryTime: '',
        message: ''
      };
    }
    return this.bidForms[order.id];
  }

  submitBid(order: PoolOrder): void {
    const draft = this.getBidForm(order);
    if (!draft.bidAmount || draft.bidAmount <= 0) {
      this.error = 'A valid bid amount is required.';
      return;
    }

    if (this.isDriver) {
      if (!this.driverId) {
        this.error = 'No driver profile is linked to your account yet.';
        return;
      }
      this.error = null;
      this.loading = true;
      this.subscriptions.push(
        this.bidService.submitBid({
          orderId: order.id,
          bidderType: BidderType.INDEPENDENT_DRIVER,
          driverId: this.driverId,
          bidAmount: draft.bidAmount,
          estimatedDeliveryTime: draft.estimatedDeliveryTime || undefined,
          message: draft.message || undefined
        }).subscribe({
          next: (bid) => {
            this.expandedOrderId = null;
            this.loadMyBids();
            this.loading = false;
          },
          error: (err) => {
            console.error('Error submitting bid:', err);
            this.error = 'Failed to submit bid.';
            this.loading = false;
          }
        })
      );
      return;
    }

    const companyId = this.isSuperAdmin ? this.selectedCompanyId : this.ownedCompanyId;
    if (!companyId) {
      this.error = 'No delivery company is linked to your account yet.';
      return;
    }

    this.error = null;
    this.loading = true;
    this.subscriptions.push(
      this.bidService.submitBid({
        orderId: order.id,
        bidderType: BidderType.COMPANY,
        deliveryCompanyId: companyId,
        bidAmount: draft.bidAmount,
        estimatedDeliveryTime: draft.estimatedDeliveryTime || undefined,
        message: draft.message || undefined
      }).subscribe({
        next: (bid) => {
          this.expandedOrderId = null;
          this.loadMyBids();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error submitting bid:', err);
          this.error = 'Failed to submit bid.';
          this.loading = false;
        }
      })
    );
  }

  withdrawBid(order: PoolOrder): void {
    if (!order.myBid) return;
    this.error = null;
    this.loading = true;
    this.subscriptions.push(
      this.bidService.withdrawBid(order.myBid.bidId).subscribe({
        next: () => {
          this.loadMyBids();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error withdrawing bid:', err);
          this.error = 'Failed to withdraw bid.';
          this.loading = false;
        }
      })
    );
  }

  trackOrder(order: PoolOrder): void {
    window.location.href = `/tracking/${order.trackingNumber}`;
  }

  bidClass(status: BidStatus): string {
    switch (status) {
      case BidStatus.SUBMITTED: return 'bid-submitted';
      case BidStatus.ACCEPTED: return 'bid-accepted';
      case BidStatus.REJECTED: return 'bid-rejected';
      case BidStatus.WITHDRAWN: return 'bid-withdrawn';
      case BidStatus.EXPIRED: return 'bid-expired';
      default: return 'bid-submitted';
    }
  }

  formatCurrency(amount: number | undefined | null): string {
    if (amount == null) return '$0.00';
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  formatDate(date: string | null | undefined): string {
    if (!date) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(date));
  }

  getStatusClass(status: string): string {
    return this.orderService.getStatusClass(status as any);
  }

  getOrderSourceLabel(order: OrderDTO): string {
    if (order.orderType === OrderType.GENERAL_DELIVERY) {
      return order.recipientName ? `📦 → ${order.recipientName}` : 'General Delivery';
    }
    return order.vendorCompanyName || 'Vendor';
  }

  getPriorityClass(priority: string): string {
    return this.orderService.getPriorityClass(priority as any);
  }
}