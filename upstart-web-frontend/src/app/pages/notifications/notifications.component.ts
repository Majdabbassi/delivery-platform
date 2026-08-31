import { Component, OnInit } from '@angular/core';

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
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [
    {
      id: 1,
      title: 'New Order Received',
      content: 'Order #12345 has been received and is being processed. Customer: John Smith, Total: $299.99',
      icon: '📦',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 5),
      type: 'success',
      category: 'order',
      priority: 'medium'
    },
    {
      id: 2,
      title: 'System Maintenance Scheduled',
      content: 'Scheduled maintenance will occur tonight at midnight. Expected downtime: 2 hours. Please save your work.',
      icon: '🔧',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 45),
      type: 'warning',
      category: 'system',
      priority: 'high'
    },
    {
      id: 3,
      title: 'Daily Backup Complete',
      content: 'Daily backup completed successfully. All data has been securely backed up to cloud storage.',
      icon: '✅',
      read: true,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 3),
      type: 'info',
      category: 'system',
      priority: 'low'
    },
    {
      id: 4,
      title: 'Security Alert',
      content: 'Unusual login activity detected from IP 192.168.1.100. Please verify this was you.',
      icon: '🔒',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 6),
      type: 'error',
      category: 'security',
      priority: 'urgent'
    },
    {
      id: 5,
      title: 'New Message from Support',
      content: 'Your support ticket #789 has been updated. Our team has provided a solution to your issue.',
      icon: '💬',
      read: true,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 12),
      type: 'info',
      category: 'message',
      priority: 'medium'
    },
    {
      id: 6,
      title: 'Payment Reminder',
      content: 'Invoice #INV-2024-001 is due in 3 days. Amount: $1,250.00. Please process payment to avoid late fees.',
      icon: '💳',
      read: false,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24),
      type: 'warning',
      category: 'reminder',
      priority: 'high'
    },
    {
      id: 7,
      title: 'Software Update Available',
      content: 'Version 2.1.0 is now available with new features and security improvements. Update recommended.',
      icon: '🔄',
      read: true,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 48),
      type: 'info',
      category: 'system',
      priority: 'medium'
    },
    {
      id: 8,
      title: 'Order Shipped',
      content: 'Order #12340 has been shipped via FedEx. Tracking number: 1234567890. Expected delivery: Tomorrow.',
      icon: '🚚',
      read: true,
      timestamp: new Date(Date.now() - 1000 * 60 * 60 * 72),
      type: 'success',
      category: 'order',
      priority: 'low'
    }
  ];

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
    { value: 'message', label: 'Messages', icon: '💬' },
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

  ngOnInit() {
    this.applyFilters();
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