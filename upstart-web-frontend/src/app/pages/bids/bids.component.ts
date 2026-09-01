import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription, forkJoin } from 'rxjs';
import {
  BidService,
  Bid,
  BidRanking,
  BidStatus
} from '../../services/bid.service';
import {
  OrderService,
  OrderDTO
} from '../../services/order.service';
import { AuthService, User, UserRole } from '../../services/auth.service';

@Component({
  selector: 'app-bids',
  templateUrl: './bids.component.html',
  styleUrls: ['./bids.component.css'],
  standalone: false
})
export class BidsComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;

  orders: OrderDTO[] = [];
  bidsByOrder: { [orderId: number]: Bid[] } = {};
  rankingsByOrder: { [orderId: number]: BidRanking[] } = {};
  responseMessages: { [bidId: string]: string } = {};
  selectedOrderId: number | null = null;

  loading = false;
  loadingBids = false;
  error: string | null = null;

  BidStatus = BidStatus;

  private subscriptions: Subscription[] = [];

  constructor(
    private bidService: BidService,
    private orderService: OrderService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadOrders();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  get isSuperAdmin(): boolean {
    return this.currentUser?.role === UserRole.SUPER_ADMIN;
  }

  get selectedOrder(): OrderDTO | null {
    if (this.selectedOrderId == null) return null;
    return this.orders.find(o => o.id === this.selectedOrderId) || null;
  }

  get selectedBids(): Bid[] {
    if (this.selectedOrderId == null) return [];
    return this.bidsByOrder[this.selectedOrderId] || [];
  }

  get selectedRankings(): BidRanking[] {
    if (this.selectedOrderId == null) return [];
    return this.rankingsByOrder[this.selectedOrderId] || [];
  }

  get openBidCount(): number {
    let count = 0;
    Object.values(this.bidsByOrder).forEach(bids => {
      count += bids.filter(b => b.status === BidStatus.SUBMITTED).length;
    });
    return count;
  }

  loadOrders(): void {
    this.loading = true;
    this.error = null;
    const request: import('rxjs').Observable<any> = this.isSuperAdmin
      ? this.orderService.getAllOrders(0, 100)
      : this.orderService.getMyOrders();
    this.subscriptions.push(
      request.subscribe({
        next: (response: any) => {
          this.orders = Array.isArray(response) ? response : (response.content || []);
          this.loading = false;
          if (this.orders.length > 0) {
            this.loadBidsForOrders();
          }
        },
        error: (err: any) => {
          console.error('Error loading orders:', err);
          this.error = 'Failed to load your orders.';
          this.loading = false;
        }
      })
    );
  }

  private loadBidsForOrders(): void {
    const activeOrders = this.orders.filter(o => !o.isCompleted && !o.isCancelled);
    if (activeOrders.length === 0) return;

    this.loadingBids = true;
    const requests = activeOrders.map(order => this.bidService.getBidsForOrder(order.id));
    this.subscriptions.push(
      forkJoin(requests).subscribe({
        next: (results: Bid[][]) => {
          activeOrders.forEach((order, index) => {
            this.bidsByOrder[order.id] = results[index] || [];
          });
          const firstWithBids = activeOrders.find(o => (this.bidsByOrder[o.id] || []).length > 0);
          this.selectedOrderId = firstWithBids ? firstWithBids.id : (activeOrders[0]?.id ?? null);
          if (this.selectedOrderId != null) {
            this.loadRankings(this.selectedOrderId);
          }
          this.loadingBids = false;
        },
        error: (err: any) => {
          console.error('Error loading bids:', err);
          this.loadingBids = false;
        }
      })
    );
  }

  selectOrder(orderId: number): void {
    this.selectedOrderId = orderId;
    if (!this.rankingsByOrder[orderId]) {
      this.loadRankings(orderId);
    }
  }

  loadRankings(orderId: number): void {
    this.subscriptions.push(
      this.bidService.getBidRankings(orderId).subscribe({
        next: (rankings) => {
          this.rankingsByOrder[orderId] = rankings || [];
        },
        error: (err) => console.error('Error loading rankings:', err)
      })
    );
  }

  acceptBid(bid: Bid): void {
    this.error = null;
    this.loadingBids = true;
    this.subscriptions.push(
      this.bidService.acceptBid(bid.bidId, this.responseMessages[bid.bidId]).subscribe({
        next: (updated) => {
          this.replaceBid(updated);
          this.loadingBids = false;
        },
        error: (err) => {
          console.error('Error accepting bid:', err);
          this.error = 'Failed to accept bid.';
          this.loadingBids = false;
        }
      })
    );
  }

  rejectBid(bid: Bid): void {
    this.error = null;
    this.loadingBids = true;
    this.subscriptions.push(
      this.bidService.rejectBid(bid.bidId, this.responseMessages[bid.bidId]).subscribe({
        next: (updated) => {
          this.replaceBid(updated);
          this.loadingBids = false;
        },
        error: (err) => {
          console.error('Error rejecting bid:', err);
          this.error = 'Failed to reject bid.';
          this.loadingBids = false;
        }
      })
    );
  }

  private replaceBid(updated: Bid): void {
    if (this.selectedOrderId == null) return;
    const list = this.bidsByOrder[this.selectedOrderId] || [];
    const index = list.findIndex(b => b.bidId === updated.bidId);
    if (index !== -1) {
      list[index] = updated;
    }
    this.loadRankings(this.selectedOrderId);
  }

  hasActiveBid(orderId: number): boolean {
    return (this.bidsByOrder[orderId] || []).some(b => b.status === BidStatus.SUBMITTED);
  }

  canRespond(bid: Bid): boolean {
    return bid.status === BidStatus.SUBMITTED;
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

  getPriorityClass(priority: string): string {
    return this.orderService.getPriorityClass(priority as any);
  }
}