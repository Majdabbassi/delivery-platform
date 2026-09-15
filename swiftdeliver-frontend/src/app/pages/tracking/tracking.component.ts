import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import * as L from 'leaflet';
import { OrderService, OrderDTO } from '../../services/order.service';
import { RealtimeService, OrderRealtimeEvent } from '../../services/realtime.service';

@Component({
  selector: 'app-tracking',
  templateUrl: './tracking.component.html',
  styleUrls: ['./tracking.component.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
  })
export class TrackingComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: false })
  mapContainer!: ElementRef<HTMLDivElement>;

  trackingNumber = '';
  order: OrderDTO | null = null;
  lastLocation: OrderRealtimeEvent | null = null;

  loading = false;
  searching = false;
  error: string | null = null;

  private map: L.Map | null = null;
  private pickupMarker: L.Marker | null = null;
  private deliveryMarker: L.Marker | null = null;
  private driverMarker: L.Marker | null = null;
  private subscriptions = new Subscription();
  private searchSub = new Subscription();
  private loadedOrderId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private orderService: OrderService,
    private realtimeService: RealtimeService
  ) {}

  ngOnInit(): void {
    const paramNumber = this.route.snapshot.paramMap.get('trackingNumber');
    if (paramNumber) {
      this.trackingNumber = paramNumber;
      this.trackOrder();
    }

    this.subscriptions.add(
      this.realtimeService.locationEvents$.subscribe(event => {
        if (event && this.loadedOrderId != null && event.orderId === this.loadedOrderId) {
          this.lastLocation = event;
          this.updateDriverMarker();
        }
      })
    );
  }

  ngAfterViewInit(): void {
    window.setTimeout(() => this.initMap(), 0);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.searchSub.unsubscribe();
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
  }

  trackOrder(): void {
    const number = this.trackingNumber.trim();
    if (!number) {
      this.error = 'Please enter a tracking number.';
      return;
    }
    if (this.searching) return;

    this.searching = true;
    this.error = null;
    this.order = null;
    this.lastLocation = null;
    this.loadedOrderId = null;
    this.searchSub.unsubscribe();
    this.searchSub = new Subscription();

    this.searchSub.add(
      this.orderService.getOrderByTrackingNumber(number).subscribe({
        next: (order) => {
          this.order = order;
          this.loadedOrderId = order.id;
          this.realtimeService.subscribeToOrder(order.id);
          this.searching = false;
          this.loadLastLocation(order.id);
          window.setTimeout(() => {
            this.initMap();
            this.renderMarkers();
          }, 0);
        },
        error: () => {
          this.searching = false;
          this.error = 'No order found for this tracking number. Check the number and try again.';
        }
      })
    );
  }

  private loadLastLocation(orderId: number): void {
    this.searchSub.add(
      this.orderService.getTrackingEvent(orderId).subscribe({
        next: (event) => {
          if (event) {
            this.lastLocation = event;
            this.updateDriverMarker();
          }
        },
        error: () => {
          // No known location yet â€” ignore
        }
      })
    );
  }

  private initMap(): void {
    if (!this.mapContainer) return;
    if (this.map) {
      window.setTimeout(() => this.map?.invalidateSize(), 0);
      return;
    }
    this.map = L.map(this.mapContainer.nativeElement, {
      zoomControl: true,
      attributionControl: false
    }).setView([24.7136, 46.6753], 6);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    window.setTimeout(() => this.map?.invalidateSize(), 0);
  }

  private renderMarkers(): void {
    if (!this.order || !this.map) return;

    this.clearMarkers();

    const points: [number, number][] = [];

    const pickup = this.coords(this.order.pickupLatitude, this.order.pickupLongitude);
    const delivery = this.coords(this.order.deliveryLatitude, this.order.deliveryLongitude);

    if (pickup) {
      points.push(pickup);
      this.pickupMarker = L.marker(pickup, {
        icon: L.divIcon({
          className: 'marker-pin marker-pickup',
          html: '<i class="fa-solid fa-box"></i>',
          iconSize: [32, 40],
          iconAnchor: [16, 38]
        }),
        title: 'Pickup'
      })
        .addTo(this.map)
        .bindPopup(`<strong>Pickup</strong><br>${this.order.pickupAddress}`)
        .openPopup();
    }

    if (delivery) {
      points.push(delivery);
      this.deliveryMarker = L.marker(delivery, {
        icon: L.divIcon({
          className: 'marker-pin marker-delivery',
          html: '<i class="fa-solid fa-location-dot"></i>',
          iconSize: [32, 40],
          iconAnchor: [16, 38]
        }),
        title: 'Delivery'
      })
        .addTo(this.map)
        .bindPopup(`<strong>Delivery</strong><br>${this.order.deliveryAddress}`);
    }

    if (points.length > 0) {
      this.map.fitBounds(L.latLngBounds(points), { padding: [40, 40] });
    }

    if (this.lastLocation) {
      this.updateDriverMarker();
    }
  }

  private updateDriverMarker(): void {
    if (!this.map || !this.lastLocation) return;
    const lat = Number(this.lastLocation.latitude);
    const lng = Number(this.lastLocation.longitude);
    if (isNaN(lat) || isNaN(lng)) return;

    if (this.driverMarker) {
      this.driverMarker.setLatLng([lat, lng]);
    } else {
      this.driverMarker = L.marker([lat, lng], {
        icon: L.divIcon({
          className: 'driver-marker',
          html: '<i class="fa-solid fa-truck"></i>',
          iconSize: [36, 36],
          iconAnchor: [18, 18]
        }),
        title: 'Driver'
      }).addTo(this.map);
    }

    const speed = this.lastLocation.speedKmh != null ? this.lastLocation.speedKmh : 0;
    this.driverMarker.bindPopup(
      `<strong>Driver live position</strong><br>Speed: ${speed.toFixed(0)} km/h`
    );
    if (this.loadedOrderId != null) {
      this.map.panTo([lat, lng]);
    }
  }

  private clearMarkers(): void {
    if (this.pickupMarker) { this.pickupMarker.remove(); this.pickupMarker = null; }
    if (this.deliveryMarker) { this.deliveryMarker.remove(); this.deliveryMarker = null; }
    if (this.driverMarker) { this.driverMarker.remove(); this.driverMarker = null; }
  }

  private coords(lat?: number, lng?: number): [number, number] | null {
    if (lat == null || lng == null) return null;
    const nLat = Number(lat);
    const nLng = Number(lng);
    if (isNaN(nLat) || isNaN(nLng)) return null;
    return [nLat, nLng];
  }

  getStatusClass(status: string): string {
    return this.orderService.getStatusClass(status as any);
  }

  getStatusDisplayName(status: string): string {
    return this.orderService.getStatusDisplayName(status as any);
  }

  formatAddress(address: string): string {
    return address || 'Address not provided';
  }
}