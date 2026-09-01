import { Injectable } from '@angular/core';
import { App, AppState } from '@capacitor/app';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AppLifecycleEvent {
  state: 'active' | 'paused' | 'resumed' | 'destroyed';
  timestamp: number;
}

/**
 * Native App Lifecycle Service
 * Handles app state changes (pause, resume, exit)
 * Uses Capacitor App plugin
 */
@Injectable({
  providedIn: 'root'
})
export class NativeAppService {
  private appStateSubject = new BehaviorSubject<AppState | null>(null);
  public appState$ = this.appStateSubject.asObservable();

  private lifecycleEventsSubject = new BehaviorSubject<AppLifecycleEvent[]>([]);
  public lifecycleEvents$ = this.lifecycleEventsSubject.asObservable();

  private isAppActiveSubject = new BehaviorSubject<boolean>(true);
  public isAppActive$ = this.isAppActiveSubject.asObservable();

  constructor() {
    this.initializeAppListeners();
  }

  /**
   * Initialize app lifecycle listeners
   */
  private initializeAppListeners(): void {
    try {
      // Listen for app pause
      App.addListener('appStateChange', (state: AppState) => {
        this.appStateSubject.next(state);

        const isActive = state.isActive;
        this.isAppActiveSubject.next(isActive);

        // Record lifecycle event
        const event: AppLifecycleEvent = {
          state: isActive ? 'resumed' : 'paused',
          timestamp: Date.now()
        };
        const currentEvents = this.lifecycleEventsSubject.value;
        this.lifecycleEventsSubject.next([...currentEvents, event]);

        console.log('App state changed:', state.isActive ? 'active' : 'paused');
      });

      // Listen for app resume
      App.addListener('resume', () => {
        this.isAppActiveSubject.next(true);
        const event: AppLifecycleEvent = {
          state: 'resumed',
          timestamp: Date.now()
        };
        const currentEvents = this.lifecycleEventsSubject.value;
        this.lifecycleEventsSubject.next([...currentEvents, event]);
      });

      // Listen for app pause
      App.addListener('pause', () => {
        this.isAppActiveSubject.next(false);
        const event: AppLifecycleEvent = {
          state: 'paused',
          timestamp: Date.now()
        };
        const currentEvents = this.lifecycleEventsSubject.value;
        this.lifecycleEventsSubject.next([...currentEvents, event]);
      });

      console.log('App lifecycle listeners initialized');
    } catch (error) {
      console.error('Error initializing app listeners:', error);
    }
  }

  /**
   * Exit the application
   */
  async exitApp(): Promise<void> {
    try {
      await App.exitApp();
    } catch (error) {
      console.error('Error exiting app:', error);
    }
  }

  /**
   * Get current app state
   */
  getCurrentAppState(): AppState | null {
    return this.appStateSubject.value;
  }

  /**
   * Check if app is currently active
   */
  isAppCurrentlyActive(): boolean {
    return this.isAppActiveSubject.value;
  }

  /**
   * Get app lifecycle events
   */
  getLifecycleEvents(): AppLifecycleEvent[] {
    return this.lifecycleEventsSubject.value;
  }

  /**
   * Clear lifecycle events
   */
  clearLifecycleEvents(): void {
    this.lifecycleEventsSubject.next([]);
  }

  /**
   * Handle app pause (good for stopping location tracking, WebSocket subscriptions, etc.)
   */
  onAppPause(): void {
    console.log('App paused - stop non-critical services');
    // Emit event that other services can listen to
  }

  /**
   * Handle app resume (good for resuming location tracking, reconnecting WebSocket, etc.)
   */
  onAppResume(): void {
    console.log('App resumed - restart services');
    // Emit event that other services can listen to
  }
}
