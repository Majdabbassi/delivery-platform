import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { OrderService, OrderDTO, OrderStatus, OrderPriority } from '../../services/order.service';
import { AuthService, User } from '../../services/auth.service';
import { RealtimeService } from '../../services/realtime.service';
import { DriverPersonService, DriverPerson } from '../../services/driver-person.service';

@Component({
  selector: 'app-driver-portal',
  standalone: false,
  templateUrl: './driver-portal.component.html',
  styleUrl: './driver-portal.component.css'
})
export class DriverPortalComponent implements OnInit, OnDestroy {
  currentUser: User | null = null;
  driverPerson: DriverPerson | null = null;
  jobs: OrderDTO[] = [];
  loading = false;
  availabilityLoading = false;
  error = '';
  locationError = '';

  // Enums for template
  OrderStatus = OrderStatus;

  private subscriptions = new Subscription();

  constructor(
    private orderService: OrderService,
    private authService: AuthService,
    private realtimeService: RealtimeService,
    private driverPersonService: DriverPersonService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDriverProfile();
    this.loadMyJobs();

    this.subscriptions.add(
      this.realtimeService.events$.subscribe(event => {
        if (event && ['ORDER_STATUS_CHANGED', 'DRIVER_ASSIGNED', 'DRIVER_LOCATION_UPDATE'].includes(event.type)) {
          this.loadMyJobs();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  get activeJobs(): OrderDTO[] {
    return this.jobs.filter(job => !['DELIVERED', 'CANCELLED', 'FAILED'].includes(job.status));
  }

  get completedJobs(): OrderDTO[] {
    return this.jobs.filter(job => job.status === 'DELIVERED');
  }

  get cancelledJobs(): OrderDTO[] {
    return this.jobs.filter(job => ['CANCELLED', 'FAILED'].includes(job.status));
  }

  get isAvailable(): boolean {
    return this.driverPerson?.isAvailable ?? true;
  }

  loadDriverProfile(): void {
    if (!this.currentUser) return;
    this.subscriptions.add(
      this.driverPersonService.getCurrentDriver().subscribe({
        next: (driver) => {
          this.driverPerson = driver;
        },
        error: () => {
          this.driverPerson = null;
        }
      })
    );
  }

  loadMyJobs(): void {
    this.loading = true;
    this.error = '';
    this.subscriptions.add(
      this.orderService.getMyOrders().subscribe({
        next: (orders) => {
          this.jobs = orders;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load your jobs. Please try again.';
          this.loading = false;
        }
      })
    );
  }

  toggleAvailability(): void {
    if (!this.driverPerson?.id) {
      this.error = 'Driver profile not found.';
      return;
    }
    this.availabilityLoading = true;
    this.error = '';
    this.subscriptions.add(
      this.driverPersonService.updateAvailabilityStatus(this.driverPerson.id, !this.driverPerson.isAvailable).subscribe({
        next: (updated) => {
          this.driverPerson = updated;
          this.availabilityLoading = false;
        },
        error: () => {
          this.error = 'Failed to update availability. Please try again.';
          this.availabilityLoading = false;
        }
      })
    );
  }

  updateStatus(job: OrderDTO, status: OrderStatus): void {
    if (job.status === status) return;
    this.subscriptions.add(
      this.orderService.updateOrderStatus(job.id, status).subscribe({
        next: (updated) => {
          const index = this.jobs.findIndex(j => j.id === job.id);
          if (index !== -1) {
            this.jobs[index] = updated;
          }
        },
        error: () => {
          this.error = `Failed to update status to ${status}. Please try again.`;
        }
      })
    );
  }

  onStatusChange(job: OrderDTO, event: Event): void {
    const value = (event.target as HTMLSelectElement).value as OrderStatus;
    this.updateStatus(job, value);
  }

  allowedStatusTransitions(status: OrderStatus): OrderStatus[] {
    const flow: Record<string, OrderStatus[]> = {
      [OrderStatus.ASSIGNED]: [OrderStatus.CONFIRMED, OrderStatus.IN_PROGRESS],
      [OrderStatus.CONFIRMED]: [OrderStatus.IN_PROGRESS, OrderStatus.PICKED_UP],
      [OrderStatus.IN_PROGRESS]: [OrderStatus.PICKED_UP, OrderStatus.CANCELLED],
      [OrderStatus.PICKED_UP]: [OrderStatus.IN_TRANSIT],
      [OrderStatus.IN_TRANSIT]: [OrderStatus.DELIVERED],
      [OrderStatus.DELIVERED]: []
    };
    return flow[status] || [];
  }

  sendLocation(job: OrderDTO): void {
    this.locationError = '';
    if (!navigator.geolocation) {
      this.locationError = 'Geolocation is not supported by this browser.';
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.subscriptions.add(
          this.orderService.updateOrderLocation(
            job.id,
            position.coords.latitude,
            position.coords.longitude,
            position.coords.speed != null ? position.coords.speed * 3.6 : undefined
          ).subscribe({
            next: () => {
              this.locationError = '';
            },
            error: () => {
              this.locationError = `Failed to send location for order ${job.orderNumber}. Please try again.`;
            }
          })
        );
      },
      () => {
        this.locationError = 'Location access denied. Please enable location services to send updates.';
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  trackJob(job: OrderDTO): void {
    if (job.trackingNumber) {
      this.router.navigate(['/tracking', job.trackingNumber]);
    }
  }

  getStatusClass(status: OrderStatus): string {
    return this.orderService.getStatusClass(status);
  }

  getStatusDisplayName(status: OrderStatus): string {
    return this.orderService.getStatusDisplayName(status);
  }

  getPriorityDisplayName(priority: OrderPriority): string {
    return this.orderService.getPriorityDisplayName(priority);
  }

  formatDate(date?: string): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString();
  }
}