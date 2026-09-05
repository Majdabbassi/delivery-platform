import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import {
  PartnershipService,
  Partnership
} from '../../services/partnership.service';
import { PaginatedResponse } from '../../models/paginated-response';
import { VendorCompanyService } from '../../services/vendor-company.service';
import { DeliveryCompanyService } from '../../services/delivery-company.service';
import { AuthService, User, UserRole } from '../../services/auth.service';

export type PartnershipStatus = 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'TERMINATED' | 'EXPIRED';

export interface PartnershipView {
  id: number;
  vendorCompanyId: number;
  vendorCompanyName: string;
  deliveryCompanyId: number;
  deliveryCompanyName: string;
  status: PartnershipStatus;
  commissionRate: number;
  serviceAreas: string[];
  isExclusive: boolean;
  minimumOrderValue: number;
  estimatedDeliveryTimeHours: number | null;
  contractStartDate: string;
  contractEndDate: string | null;
  totalOrdersCompleted: number;
  totalRevenueGenerated: number;
  averageRating: number | null;
  partnershipTerms: string | null;
  notes: string | null;
  createdAt: string;
}

interface StatCard {
  icon: string;
  label: string;
  value: number | string;
  color: string;
}

@Component({
  selector: 'app-partnerships',
  templateUrl: './partnerships.component.html',
  styleUrls: ['./partnerships.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
  })
export class PartnershipsComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  partnerships: PartnershipView[] = [];
  statCards: StatCard[] = [];
  ownedCompanyName = '';

  loading = false;
  error: string | null = null;

  // SUPER_ADMIN pagination
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  UserRole = UserRole;

  private subscriptions: Subscription[] = [];

  constructor(
    private partnershipService: PartnershipService,
    private vendorCompanyService: VendorCompanyService,
    private deliveryCompanyService: DeliveryCompanyService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadPartnerships();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  get isSuperAdmin(): boolean {
    return this.currentUser?.role === UserRole.SUPER_ADMIN;
  }

  get isVendorOwner(): boolean {
    return this.currentUser?.role === UserRole.VENDOR_OWNER;
  }

  get isDeliveryOwner(): boolean {
    return this.currentUser?.role === UserRole.DELIVERY_OWNER;
  }

  loadPartnerships(): void {
    if (!this.currentUser) {
      this.error = 'Please log in to view partnerships.';
      return;
    }

    if (this.currentUser.role === UserRole.SUPER_ADMIN) {
      this.loadAllPartnerships();
    } else if (this.currentUser.role === UserRole.VENDOR_OWNER) {
      this.loadVendorOwnerPartnerships();
    } else if (this.currentUser.role === UserRole.DELIVERY_OWNER) {
      this.loadDeliveryOwnerPartnerships();
    }
  }

  private loadAllPartnerships(): void {
    this.loading = true;
    this.error = null;
    this.subscriptions.push(
      this.partnershipService.getAllPartnerships(this.currentPage, this.pageSize).subscribe({
        next: (response: PaginatedResponse<Partnership>) => {
          this.partnerships = response.content.map(p => this.mapPartnership(p));
          this.totalElements = response.totalElements;
          this.totalPages = response.totalPages;
          this.calculateStats();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading partnerships:', error);
          this.error = 'Failed to load partnerships. Please try again.';
          this.loading = false;
        }
      })
    );
  }

  private loadVendorOwnerPartnerships(): void {
    const ownerId = this.currentUser?.id;
    if (!ownerId) {
      this.error = 'Unable to determine your vendor profile.';
      return;
    }

    this.loading = true;
    this.error = null;
    this.subscriptions.push(
      this.vendorCompanyService.getVendorCompaniesByOwner(ownerId).subscribe({
        next: (companies) => {
          if (!companies || companies.length === 0) {
            this.partnerships = [];
            this.loading = false;
            return;
          }
          this.ownedCompanyName = companies[0].name || '';
          this.subscriptions.push(
            this.partnershipService
              .getPartnershipsByVendorCompany(companies[0].id!)
              .subscribe({
                next: (list) => {
                  this.partnerships = (list || []).map(p => this.mapPartnership(p));
                  this.calculateStats();
                  this.loading = false;
                },
                error: (error) => {
                  console.error('Error loading vendor partnerships:', error);
                  this.error = 'Failed to load partnerships. Please try again.';
                  this.loading = false;
                }
              })
          );
        },
        error: (error) => {
          console.error('Error loading vendor companies:', error);
          this.error = 'Failed to load your company. Please try again.';
          this.loading = false;
        }
      })
    );
  }

  private loadDeliveryOwnerPartnerships(): void {
    const ownerId = this.currentUser?.id;
    if (!ownerId) {
      this.error = 'Unable to determine your delivery profile.';
      return;
    }

    this.loading = true;
    this.error = null;
    this.subscriptions.push(
      this.deliveryCompanyService.getDeliveryCompaniesByOwner(ownerId).subscribe({
        next: (companies) => {
          if (!companies || companies.length === 0) {
            this.partnerships = [];
            this.loading = false;
            return;
          }
          this.ownedCompanyName = companies[0].name || '';
          this.subscriptions.push(
            this.partnershipService
              .getPartnershipsByDeliveryCompany(companies[0].id!)
              .subscribe({
                next: (list) => {
                  this.partnerships = (list || []).map(p => this.mapPartnership(p));
                  this.calculateStats();
                  this.loading = false;
                },
                error: (error) => {
                  console.error('Error loading delivery partnerships:', error);
                  this.error = 'Failed to load partnerships. Please try again.';
                  this.loading = false;
                }
              })
          );
        },
        error: (error) => {
          console.error('Error loading delivery companies:', error);
          this.error = 'Failed to load your company. Please try again.';
          this.loading = false;
        }
      })
    );
  }

  // Status actions
  changeStatus(partnership: PartnershipView, action: 'activate' | 'suspend' | 'terminate'): void {
    this.loading = true;
    this.error = null;
    const call = action === 'activate'
      ? this.partnershipService.activatePartnership(partnership.id)
      : action === 'suspend'
        ? this.partnershipService.suspendPartnership(partnership.id)
        : this.partnershipService.terminatePartnership(partnership.id);

    this.subscriptions.push(
      call.subscribe({
        next: (updated) => {
          const index = this.partnerships.findIndex(p => p.id === partnership.id);
          if (index !== -1) {
            this.partnerships[index] = this.mapPartnership(updated);
          }
          this.calculateStats();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error updating partnership status:', error);
          this.error = 'Failed to update partnership status. Please try again.';
          this.loading = false;
        }
      })
    );
  }

  // Pagination
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPartnerships();
  }

  // Stats
  private calculateStats(): void {
    const active = this.partnerships.filter(p => p.status === 'ACTIVE').length;
    const suspended = this.partnerships.filter(p => p.status === 'SUSPENDED').length;
    const pending = this.partnerships.filter(p => p.status === 'PENDING').length;
    const totalRevenue = this.partnerships.reduce((sum, p) => sum + (p.totalRevenueGenerated || 0), 0);
    const totalOrders = this.partnerships.reduce((sum, p) => sum + (p.totalOrdersCompleted || 0), 0);
    const rated = this.partnerships.filter(p => p.averageRating != null);
    const avgRating = rated.length
      ? (rated.reduce((sum, p) => sum + (p.averageRating || 0), 0) / rated.length).toFixed(1)
      : '—';

this.statCards = [
      { icon: 'fa-solid fa-handshake', label: 'Total Partnerships', value: this.partnerships.length, color: 'blue' },
      { icon: 'fa-solid fa-circle-check', label: 'Active', value: active, color: 'green' },
      { icon: 'fa-solid fa-clock', label: 'Pending', value: pending, color: 'amber' },
      { icon: 'fa-solid fa-ban', label: 'Suspended', value: suspended, color: 'orange' },
      { icon: 'fa-solid fa-box', label: 'Orders Completed', value: totalOrders, color: 'indigo' },
      { icon: 'fa-solid fa-dollar-sign', label: 'Total Revenue', value: this.formatCurrency(totalRevenue), color: 'teal' },
      { icon: 'fa-solid fa-star', label: 'Avg Rating', value: avgRating, color: 'pink' }
    ];
  }

  // Maps the nested backend Partnership entity JSON into the flat UI model.
  private mapPartnership(raw: Partnership | any): PartnershipView {
    const vendor = raw.vendorCompany || {};
    const delivery = raw.deliveryCompany || {};
    return {
      id: raw.id,
      vendorCompanyId: vendor.id,
      vendorCompanyName: vendor.companyName || '',
      deliveryCompanyId: delivery.id,
      deliveryCompanyName: delivery.companyName || '',
      status: raw.status as PartnershipStatus,
      commissionRate: Number(raw.commissionRate ?? 0),
      serviceAreas: Array.isArray(raw.serviceAreas) ? raw.serviceAreas : [],
      isExclusive: raw.isExclusive === true,
      minimumOrderValue: Number(raw.minimumOrderValue ?? 0),
      estimatedDeliveryTimeHours: raw.estimatedDeliveryTimeHours ?? null,
      contractStartDate: raw.contractStartDate || raw.createdAt || '',
      contractEndDate: raw.contractEndDate || null,
      totalOrdersCompleted: Number(raw.totalOrdersCompleted ?? 0),
      totalRevenueGenerated: Number(raw.totalRevenueGenerated ?? 0),
      averageRating: raw.averageRating == null ? null : Number(raw.averageRating),
      partnershipTerms: raw.partnershipTerms || null,
      notes: raw.notes || null,
      createdAt: raw.createdAt || ''
    };
  }

  formatCurrency(amount: number | undefined): string {
    if (!amount && amount !== 0) return '$0.00';
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
      day: 'numeric'
    }).format(new Date(date));
  }

  getStatusClass(status: PartnershipStatus): string {
    switch (status) {
      case 'ACTIVE': return 'status-active';
      case 'PENDING': return 'status-pending';
      case 'SUSPENDED': return 'status-suspended';
      case 'TERMINATED': return 'status-terminated';
      case 'EXPIRED': return 'status-expired';
      default: return '';
    }
  }

  canManage(partnership: PartnershipView): boolean {
    if (!this.currentUser) return false;
    if (this.currentUser.role === UserRole.SUPER_ADMIN) return true;
    if (partnership.status === 'TERMINATED' || partnership.status === 'EXPIRED') return false;
    return true;
  }
}