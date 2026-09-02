import { Injectable } from '@angular/core';
import { Geolocation, GeolocationOptions } from '@capacitor/geolocation';
import { BehaviorSubject, Observable } from 'rxjs';

export interface LocationCoords {
  latitude: number;
  longitude: number;
  accuracy?: number;
  altitudeAccuracy?: number;
  altitude?: number;
  speed?: number;
  heading?: number;
  timestamp: number;
}

/**
 * Native Geolocation Service
 * Provides real-time location tracking for drivers and delivery features
 * Uses Capacitor Geolocation plugin for native access
 */
@Injectable({
  providedIn: 'root'
})
export class NativeLocationService {
  private currentLocationSubject = new BehaviorSubject<LocationCoords | null>(null);
  public currentLocation$ = this.currentLocationSubject.asObservable();

  private isTrackingSubject = new BehaviorSubject<boolean>(false);
  public isTracking$ = this.isTrackingSubject.asObservable();

  private watchId: string | null = null;

  constructor() {}

  /**
   * Get current device location (one-time)
   */
  async getCurrentLocation(): Promise<LocationCoords> {
    try {
      const options: GeolocationOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      };

      const coordinates = await Geolocation.getCurrentPosition(options);
      
      const location: LocationCoords = {
        latitude: coordinates.coords.latitude,
        longitude: coordinates.coords.longitude,
        accuracy: coordinates.coords.accuracy ?? undefined,
        altitude: coordinates.coords.altitude ?? undefined,
        altitudeAccuracy: coordinates.coords.altitudeAccuracy ?? undefined,
        speed: coordinates.coords.speed ?? undefined,
        heading: coordinates.coords.heading ?? undefined,
        timestamp: coordinates.timestamp
      };

      this.currentLocationSubject.next(location);
      return location;
    } catch (error) {
      console.error('Error getting current location:', error);
      throw error;
    }
  }

  /**
   * Start continuous location tracking (for drivers)
   * Watches location and emits updates via observable
   */
  async startTracking(updateInterval: number = 5000): Promise<void> {
    if (this.isTrackingSubject.value) {
      console.warn('Location tracking already started');
      return;
    }

    try {
      const options: GeolocationOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      };

      this.watchId = await Geolocation.watchPosition(options, (position, error) => {
        if (error) {
          console.error('Location tracking error:', error);
          return;
        }

        if (position) {
          const location: LocationCoords = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy ?? undefined,
            altitude: position.coords.altitude ?? undefined,
            altitudeAccuracy: position.coords.altitudeAccuracy ?? undefined,
            speed: position.coords.speed ?? undefined,
            heading: position.coords.heading ?? undefined,
            timestamp: position.timestamp
          };

          this.currentLocationSubject.next(location);
        }
      });

      this.isTrackingSubject.next(true);
      console.log('Location tracking started');
    } catch (error) {
      console.error('Error starting location tracking:', error);
      throw error;
    }
  }

  /**
   * Stop continuous location tracking
   */
  async stopTracking(): Promise<void> {
    if (this.watchId) {
      await Geolocation.clearWatch({ id: this.watchId });
      this.watchId = null;
      this.isTrackingSubject.next(false);
      console.log('Location tracking stopped');
    }
  }

  /**
   * Check if location services are enabled
   */
  async isLocationEnabled(): Promise<boolean> {
    try {
      // Try to get current position - if it works, location is enabled
      await this.getCurrentLocation();
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Get current tracking status
   */
  isCurrentlyTracking(): boolean {
    return this.isTrackingSubject.value;
  }

  /**
   * Get last known location
   */
  getLastLocation(): LocationCoords | null {
    return this.currentLocationSubject.value;
  }

  /**
   * Calculate distance between two coordinates (in kilometers)
   */
  calculateDistance(
    lat1: number,
    lon1: number,
    lat2: number,
    lon2: number
  ): number {
    const R = 6371; // Earth's radius in km
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) *
        Math.cos((lat2 * Math.PI) / 180) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }
}
