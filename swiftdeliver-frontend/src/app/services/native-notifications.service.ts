import { Injectable } from '@angular/core';
import { LocalNotifications, LocalNotificationSchema } from '@capacitor/local-notifications';
import { PushNotifications, Token, PushNotificationSchema, ActionPerformed } from '@capacitor/push-notifications';
import { BehaviorSubject, Observable } from 'rxjs';

export interface NotificationPayload {
  id: string;
  title: string;
  body: string;
  data?: Record<string, any>;
  smallIcon?: string;
  largeIcon?: string;
  sound?: string;
}

/**
 * Native Notifications Service
 * Handles both local and push notifications
 * Uses Capacitor Notifications plugins
 */
@Injectable({
  providedIn: 'root'
})
export class NativeNotificationsService {
  private deviceTokenSubject = new BehaviorSubject<string | null>(null);
  public deviceToken$ = this.deviceTokenSubject.asObservable();

  private notificationsSubject = new BehaviorSubject<NotificationPayload[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private pushNotificationSubject = new BehaviorSubject<PushNotificationSchema | null>(null);
  public pushNotification$ = this.pushNotificationSubject.asObservable();

  constructor() {
    this.initializePushNotifications();
  }

  /**
   * Initialize push notifications and register for device token
   */
  private async initializePushNotifications(): Promise<void> {
    try {
      // Register with push notifications
      await PushNotifications.requestPermissions();
      await PushNotifications.register();

      // Listen for token refresh
      PushNotifications.addListener('registration', (token: Token) => {
        this.deviceTokenSubject.next(token.value);
      });

      // Listen for incoming push notifications
      PushNotifications.addListener('pushNotificationReceived', (notification: PushNotificationSchema) => {
        this.pushNotificationSubject.next(notification);
        this.handlePushNotification(notification);
      });

      // Listen for push notification actions
      PushNotifications.addListener('pushNotificationActionPerformed', (notification: ActionPerformed) => {
        this.pushNotificationSubject.next(notification as unknown as PushNotificationSchema);
        this.handlePushNotificationAction(notification);
      });

      // Listen for errors
      PushNotifications.addListener('registrationError', (error: any) => {
        console.error('Push notification registration error:', error);
      });

      console.log('Push notifications initialized');
    } catch (error) {
      console.error('Error initializing push notifications:', error);
    }
  }

  /**
   * Show a local notification
   */
  async showLocalNotification(notification: NotificationPayload): Promise<void> {
    try {
      const localNotif: LocalNotificationSchema = {
        id: parseInt(notification.id, 10) || Date.now(),
        title: notification.title,
        body: notification.body,
        smallIcon: notification.smallIcon || 'ic_stat_icon_config_sample',
        largeIcon: notification.largeIcon,
        sound: notification.sound || 'default',
        extra: notification.data
      };

      await LocalNotifications.schedule({
        notifications: [localNotif]
      });

      // Store notification
      const currentNotifications = this.notificationsSubject.value;
      this.notificationsSubject.next([...currentNotifications, notification]);
    } catch (error) {
      console.error('Error showing local notification:', error);
    }
  }

  /**
   * Show multiple local notifications
   */
  async showLocalNotifications(notifications: NotificationPayload[]): Promise<void> {
    try {
      const localNotifications: LocalNotificationSchema[] = notifications.map((n) => ({
        id: parseInt(n.id, 10) || Date.now(),
        title: n.title,
        body: n.body,
        smallIcon: n.smallIcon || 'ic_stat_icon_config_sample',
        largeIcon: n.largeIcon,
        sound: n.sound || 'default',
        extra: n.data
      }));

      await LocalNotifications.schedule({
        notifications: localNotifications
      });

      // Store notifications
      const currentNotifications = this.notificationsSubject.value;
      this.notificationsSubject.next([...currentNotifications, ...notifications]);
    } catch (error) {
      console.error('Error showing multiple local notifications:', error);
    }
  }

  /**
   * Cancel a local notification
   */
  async cancelNotification(id: string): Promise<void> {
    try {
      await LocalNotifications.cancel({
        notifications: [{ id: parseInt(id, 10) }]
      });
    } catch (error) {
      console.error('Error canceling notification:', error);
    }
  }

  /**
   * Clear all local notifications
   */
  async clearAllNotifications(): Promise<void> {
    try {
      await LocalNotifications.removeAllListeners();
      this.notificationsSubject.next([]);
    } catch (error) {
      console.error('Error clearing notifications:', error);
    }
  }

  /**
   * Get device token for registering with backend
   */
  getDeviceToken(): string | null {
    return this.deviceTokenSubject.value;
  }

  /**
   * Get all stored notifications
   */
  getNotifications(): NotificationPayload[] {
    return this.notificationsSubject.value;
  }

  /**
   * Request notification permissions
   */
  async requestPermissions(): Promise<void> {
    try {
      await PushNotifications.requestPermissions();
    } catch (error) {
      console.error('Error requesting notification permissions:', error);
    }
  }

  /**
   * Check notification permissions
   */
  async checkPermissions(): Promise<boolean> {
    try {
      const result = await PushNotifications.checkPermissions();
      return result.receive === 'granted' || result.receive === 'prompt';
    } catch {
      return false;
    }
  }

  /**
   * Handle incoming push notification
   */
  private handlePushNotification(notification: PushNotificationSchema): void {
    // Override this method in service consumers to handle push notifications
    console.log('Push notification received:', notification);

    // Optionally show local notification when app receives push
    if (notification.title && notification.body) {
      this.showLocalNotification({
        id: notification.id?.toString() || Date.now().toString(),
        title: notification.title,
        body: notification.body,
        data: notification.data
      });
    }
  }

  /**
   * Handle push notification action (user clicked notification)
   */
  private handlePushNotificationAction(notification: ActionPerformed): void {
    // Override this method in service consumers to handle notification clicks
    console.log('Push notification action:', notification);
  }
}
