import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { AuthService, User, UserRole } from './auth.service';
import { API_BASE_URL } from '../config';

export interface OrderRealtimeEvent {
  type: string;
  orderId: number;
  orderNumber?: string;
  trackingNumber?: string;
  status?: string;
  driverPersonId?: number;
  driverName?: string;
  latitude?: number;
  longitude?: number;
  speedKmh?: number;
  timestamp?: string;
  involvedUserIds?: number[];
}

@Injectable({
  providedIn: 'root'
})
export class RealtimeService implements OnDestroy {
  private client: Client | null = null;
  private connected = false;
  private eventsSubject = new BehaviorSubject<OrderRealtimeEvent | null>(null);
  public events$ = this.eventsSubject.asObservable();
  private locationSubject = new BehaviorSubject<OrderRealtimeEvent | null>(null);
  public locationEvents$ = this.locationSubject.asObservable();

  constructor(private authService: AuthService) {}

  connect(): void {
    const token = this.authService.getToken();
    if (!token || this.connected || this.client) {
      return;
    }

    const wsBaseUrl = API_BASE_URL.replace(/\/api\/?$/, '');
    const sockJsUrl = `${wsBaseUrl}/ws?token=${encodeURIComponent(token)}`;

    this.client = new Client({
      webSocketFactory: () => new SockJS(sockJsUrl) as any,
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
      onConnect: () => {
        this.connected = true;
        const user: User | null = this.authService.getCurrentUser();
        if (this.client) {
          // The global feeds (every order, every driver) are administrators-only. Everyone else
          // receives their own orders' events, including live driver locations, on their private topic.
          if (user?.role === UserRole.SUPER_ADMIN) {
            this.client.subscribe('/topic/orders', (msg) => this.onMessage(msg));
            this.client.subscribe('/topic/orders/location', (msg) => this.onMessage(msg));
          }
          if (user && user.id) {
            this.client.subscribe(`/topic/users/${user.id}`, (msg) => this.onMessage(msg));
          }
        }
      },
      onDisconnect: () => {
        this.connected = false;
      },
      onWebSocketClose: () => {
        this.connected = false;
      },
      onStompError: () => {
        this.connected = false;
      }
    });

    this.client.activate();
  }

  disconnect(): void {
    if (this.client) {
      try {
        this.client.deactivate();
      } catch {
        // ignore shutdown errors
      }
      this.client = null;
    }
    this.connected = false;
  }

  isConnected(): boolean {
    return this.connected;
  }

  private onMessage(message: IMessage): void {
    try {
      const event = JSON.parse(message.body) as OrderRealtimeEvent;
      this.eventsSubject.next(event);
      if (event.type === 'DRIVER_LOCATION_UPDATE' || (
        event.latitude != null && event.longitude != null
      )) {
        this.locationSubject.next(event);
      }
    } catch {
      // Ignore malformed payloads
    }
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}