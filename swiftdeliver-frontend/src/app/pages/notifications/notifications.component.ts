import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { RealtimeService, OrderRealtimeEvent } from '../../services/realtime.service';

interface Notification {
  id: number;
  title: string;
  content: string;
  icon: string;
  read: boolean;
  timestamp: Date;
  type: 'info' | 'warning' | 'success' | 'error';
  category: 'system' | 'order' | 'message' | 'reminder' | 'security';
  priority: 'low' | 'medium' | 'high' | 'urgent';
}

@Component({
  selector: 'app-notifications',
  standalone: false,
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css'
})
export class NotificationsComponent implements OnInit, OnDestroy {
  private realtimeSubscription: Subscription = new Subscription();
  private notificationSequence = 1000;
  notifications: Notification[] = [];

  filteredNotifications: Notification[] = [];
  selectedCategory: string = 'all';
  selectedType: string = 'all';
  selectedPriority: string = 'all';
  showUnreadOnly: boolean = false;
  searchQuery: string = '';

  categories = [
    { value: 'all', label: 'All Categories', icon: '📋' },
    { value: 'system', label: 'System', icon: '⚙️' },
    { value: 'order', label: 'Orders', icon: '📦' },
    { value: 'reminder', label: 'Reminders', icon: '⏰' },
    { value: 'security', label: 'Security', icon: '🔒' }
  ];

  types = [
    { value: 'all', label: 'All Types', icon: '📄' },
    { value: 'info', label: 'Info', icon: 'ℹ️' },
    { value: 'success', label: 'Success', icon: '✅' },
    { value: 'warning', label: 'Warning', icon: '⚠️' },
    { value: 'error', label: 'Error', icon: '❌' }
  ];

  priorities = [
    { value: 'all', label: 'All Priorities', icon: '📊' },
    { value: 'low', label: 'Low', icon: '🟢' },
    { value: 'medium', label: 'Medium', icon: '🟡' },
    { value: 'high', label: 'High', icon: '🟠' },
    { value: 'urgent', label: 'Urgent', icon: '🔴' }
  ];

  constructor(private realtimeService: RealtimeService) {}

  ngOnInit() {
    this.realtimeSubscription = this.realtimeService.events$.subscribe(event => {
      if (event) {
        this.handleRealtimeEvent(event);
      }
    });
    this.applyFilters();
  }

  ngOnDestroy(): void {
    this.realtimeSubscription.unsubscribe();
  }

  private handleRealtimeEvent(event: OrderRealtimeEvent): void {
    const notification = this.buildNotification(event);
    if (notification) {
      this.notifications.unshift(notification);
      if (this.notifications.length > 100) {
        this.notifications.pop();
      }
      this.applyFilters();
    }
  }

  private buildNotification(event: OrderRealtimeEvent): Notification | null {
    const orderRef = event.orderNumber || '#' + event.orderId;
    switch (event.type) {
      case 'ORDER_CREATED':
        return {
          id: this.notificationSequence++,
          title: `New Order Received: ${orderRef}`,
          content: `Order ${orderRef} has been created and is being processed.`,
          icon: '📦',
          read: false,
          timestamp: new Date(),
          type: 'success',
          category: 'order',
          priority: 'medium'
        };
      case 'DRIVER_ASSIGNED':
        return {
          id: this.notificationSequence++,
          title: `Driver Assigned: ${orderRef}`,
          content: event.driverName
            ? `Driver ${event.driverName} has been assigned to order ${orderRef}.`
            : `A driver has been assigned to order ${orderRef}.`,
          icon: '🚚',
          read: false,
          timestamp: new Date(),
          type: 'info',
          category: 'order',
          priority: 'high'
        };
      case 'ORDER_STATUS_CHANGED':
        return {
          id: this.notificationSequence++,
          title: `Order Status Updated: ${orderRef}`,
          content: `Order ${orderRef} status is now ${event.status || 'updated'}.`,
          icon: '🔄',
          read: false,
          timestamp: new Date(),
          type: 'warning',
          category: 'order',
          priority: 'medium'
        };
      case 'ORDER_DELIVERED':
        return {
          id: this.notificationSequence++,
          title: `Order Delivered: ${orderRef}`,
          content: `Order ${orderRef} has been delivered successfully.`,
          icon: '✅',
          read: false,
          timestamp: new Date(),
          type: 'success',
          category: 'order',
          priority: 'medium'
        };
      case 'ORDER_CANCELLED':
        return {
          id: this.notificationSequence++,
          title: `Order Cancelled: ${orderRef}`,
          content: `Order ${orderRef} was cancelled.`,
          icon: '🚫',
          read: false,
          timestamp: new Date(),
          type: 'error',
          category: 'order',
          priority: 'high'
        };
      case 'BID_SUBMITTED':
        return {
          id: this.notificationSequence++,
          title: `New Bid: ${orderRef}`,
          content: `Order ${orderRef} received a new bid.`,
          icon: '💼',
          read: false,
          timestamp: new Date(),
          type: 'info',
          category: 'order',
          priority: 'medium'
        };
      case 'BID_ACCEPTED':
        return {
          id: this.notificationSequence++,
          title: `Bid Accepted: ${orderRef}`,
          content: `Your bid for order ${orderRef} was accepted.`,
          icon: '🎉',
          read: false,
          timestamp: new Date(),
          type: 'success',
          category: 'order',
          priority: 'high'
        };
      case 'BID_REJECTED':
        return {
          id: this.notificationSequence++,
          title: `Bid Rejected: ${orderRef}`,
          content: `Your bid for order ${orderRef} was rejected.`,
          icon: '❌',
          read: false,
          timestamp: new Date(),
          type: 'error',
          category: 'order',
          priority: 'medium'
        };
      case 'DRIVER_LOCATION_UPDATE':
        return {
          id: this.notificationSequence++,
          title: `Location Update: ${orderRef}`,
          content: `Driver location updated for order ${orderRef}.`,
          icon: '📍',
          read: false,
          timestamp: new Date(),
          type: 'info',
          category: 'order',
          priority: 'low'
        };
      default:
        return null;
    }
  }

  applyFilters() {
    this.filteredNotifications = this.notifications.filter(notification => {
      const matchesCategory = this.selectedCategory === 'all' || notification.category === this.selectedCategory;
      const matchesType = this.selectedType === 'all' || notification.type === this.selectedType;
      const matchesPriority = this.selectedPriority === 'all' || notification.priority === this.selectedPriority;
      const matchesReadStatus = !this.showUnreadOnly || !notification.read;
      const matchesSearch = !this.searchQuery || 
        notification.title.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        notification.content.toLowerCase().includes(this.searchQuery.toLowerCase());

      return matchesCategory && matchesType && matchesPriority && matchesReadStatus && matchesSearch;
    });
  }

  onCategoryChange(category: string) {
    this.selectedCategory = category;
    this.applyFilters();
  }

  onTypeChange(type: string) {
    this.selectedType = type;
    this.applyFilters();
  }

  onPriorityChange(priority: string) {
    this.selectedPriority = priority;
    this.applyFilters();
  }

  onUnreadToggle() {
    this.applyFilters();
  }

  onSearchChange() {
    this.applyFilters();
  }

  markAsRead(notification: Notification) {
    notification.read = true;
    this.applyFilters();
  }

  markAsUnread(notification: Notification) {
    notification.read = false;
    this.applyFilters();
  }

  markAllAsRead() {
    this.notifications.forEach(notification => notification.read = true);
    this.applyFilters();
  }

  deleteNotification(notification: Notification) {
    const index = this.notifications.findIndex(n => n.id === notification.id);
    if (index > -1) {
      this.notifications.splice(index, 1);
      this.applyFilters();
    }
  }

  clearAllRead() {
    this.notifications = this.notifications.filter(notification => !notification.read);
    this.applyFilters();
  }

  getNotificationTypeClass(type: string): string {
    return `notification-${type}`;
  }

  getPriorityClass(priority: string): string {
    return `priority-${priority}`;
  }

  formatTime(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    
    return timestamp.toLocaleDateString();
  }

  get unreadCount(): number {
    return this.notifications.filter(n => !n.read).length;
  }

  get totalCount(): number {
    return this.notifications.length;
  }
}