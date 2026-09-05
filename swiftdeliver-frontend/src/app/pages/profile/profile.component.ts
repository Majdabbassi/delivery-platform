import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AuthService, User } from '../../services/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
,  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  user: User | null = null;
  loading = true;
  error = '';

  activeTab = 'profile';

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.authService.getCurrentUserFromAPI().subscribe({
      next: (user) => {
        this.user = user;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading profile:', err);
        this.error = 'Failed to load your profile. Please try again.';
        this.user = this.authService.getCurrentUser();
        this.loading = false;
      }
    });
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  getUserInitials(): string {
    if (!this.user) return '??';
    const first = this.user.firstName?.charAt(0) || '';
    const last = this.user.lastName?.charAt(0) || '';
    return (first + last).toUpperCase() || '??';
  }

  get fullName(): string {
    if (!this.user) return '';
    return [this.user.firstName, this.user.lastName].filter(Boolean).join(' ') || this.user.username;
  }

  get roleLabel(): string {
    const role = this.user?.role;
    switch (role) {
      case 'SUPER_ADMIN': return 'Super Administrator';
      case 'VENDOR_OWNER': return 'Vendor Owner';
      case 'DELIVERY_OWNER': return 'Delivery Owner';
      case 'CLIENT': return 'Client';
      case 'DRIVER': return 'Driver';
      default: return 'User';
    }
  }

  formatJoinDate(): string {
    if (!this.user?.createdAt) return 'N/A';
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }).format(new Date(this.user.createdAt));
  }

  exportData(): void {
    if (!this.user) return;
    const blob = new Blob([JSON.stringify(this.user, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'my-profile.json';
    link.click();
    URL.revokeObjectURL(url);
  }
}