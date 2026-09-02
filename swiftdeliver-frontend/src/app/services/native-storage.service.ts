import { Injectable } from '@angular/core';
import { Storage } from '@capacitor/storage';
import { BehaviorSubject } from 'rxjs';

/**
 * Native Storage Service
 * Provides persistent offline caching using Capacitor Storage
 * Better than browser localStorage for mobile apps (more reliable, more space)
 */
@Injectable({
  providedIn: 'root'
})
export class NativeStorageService {
  private storageReadySubject = new BehaviorSubject<boolean>(false);
  public storageReady$ = this.storageReadySubject.asObservable();

  private cacheSubject = new BehaviorSubject<Map<string, any>>(new Map());
  public cache$ = this.cacheSubject.asObservable();

  constructor() {
    this.initializeStorage();
  }

  /**
   * Initialize storage
   */
  private async initializeStorage(): Promise<void> {
    try {
      // Test storage access
      await this.get('test');
      this.storageReadySubject.next(true);
      console.log('Native storage initialized');
    } catch (error) {
      console.error('Error initializing native storage:', error);
      this.storageReadySubject.next(false);
    }
  }

  /**
   * Set item in storage
   */
  async set(key: string, value: any): Promise<void> {
    try {
      const serialized = typeof value === 'string' ? value : JSON.stringify(value);
      await Storage.set({ key, value: serialized });

      // Update in-memory cache
      const cache = this.cacheSubject.value;
      cache.set(key, value);
      this.cacheSubject.next(cache);
    } catch (error) {
      console.error(`Error setting storage key ${key}:`, error);
      throw error;
    }
  }

  /**
   * Get item from storage
   */
  async get(key: string): Promise<any> {
    try {
      const result = await Storage.get({ key });
      
      if (result.value) {
        try {
          // Try to parse as JSON
          return JSON.parse(result.value);
        } catch {
          // If not JSON, return as-is
          return result.value;
        }
      }
      
      return null;
    } catch (error) {
      console.error(`Error getting storage key ${key}:`, error);
      return null;
    }
  }

  /**
   * Remove item from storage
   */
  async remove(key: string): Promise<void> {
    try {
      await Storage.remove({ key });

      // Update in-memory cache
      const cache = this.cacheSubject.value;
      cache.delete(key);
      this.cacheSubject.next(cache);
    } catch (error) {
      console.error(`Error removing storage key ${key}:`, error);
      throw error;
    }
  }

  /**
   * Clear all storage
   */
  async clear(): Promise<void> {
    try {
      await Storage.clear();
      this.cacheSubject.next(new Map());
    } catch (error) {
      console.error('Error clearing storage:', error);
      throw error;
    }
  }

  /**
   * Get all keys in storage
   */
  async keys(): Promise<string[]> {
    try {
      const result = await Storage.keys();
      return result.keys || [];
    } catch (error) {
      console.error('Error getting storage keys:', error);
      return [];
    }
  }

  /**
   * Cache data with TTL (time to live) in seconds
   */
  async setCacheWithTTL(key: string, value: any, ttlSeconds: number): Promise<void> {
    try {
      const cacheData = {
        value,
        expiresAt: Date.now() + ttlSeconds * 1000
      };
      await this.set(key, cacheData);
    } catch (error) {
      console.error(`Error setting cache with TTL for key ${key}:`, error);
      throw error;
    }
  }

  /**
   * Get cached data with TTL check
   */
  async getCacheWithTTL(key: string): Promise<any> {
    try {
      const cached = await this.get(key);
      
      if (!cached) return null;

      // Check if TTL has expired
      if (cached.expiresAt && Date.now() > cached.expiresAt) {
        await this.remove(key);
        return null;
      }

      return cached.value;
    } catch (error) {
      console.error(`Error getting cache with TTL for key ${key}:`, error);
      return null;
    }
  }

  /**
   * Get all data from storage
   */
  async getAll(): Promise<Record<string, any>> {
    try {
      const keys = await this.keys();
      const result: Record<string, any> = {};

      for (const key of keys) {
        const value = await this.get(key);
        result[key] = value;
      }

      return result;
    } catch (error) {
      console.error('Error getting all storage data:', error);
      return {};
    }
  }

  /**
   * Check if key exists
   */
  async hasKey(key: string): Promise<boolean> {
    try {
      const value = await this.get(key);
      return value !== null;
    } catch {
      return false;
    }
  }

  /**
   * Get storage size estimate (not all platforms support this)
   */
  async getStorageSize(): Promise<number> {
    try {
      // This is a workaround since Capacitor doesn't provide direct size info
      const all = await this.getAll();
      const json = JSON.stringify(all);
      return new Blob([json]).size;
    } catch {
      return 0;
    }
  }

  /**
   * Store JSON object
   */
  async setJSON(key: string, value: any): Promise<void> {
    return this.set(key, value);
  }

  /**
   * Retrieve JSON object
   */
  async getJSON(key: string): Promise<any> {
    return this.get(key);
  }

  /**
   * Store string
   */
  async setString(key: string, value: string): Promise<void> {
    return this.set(key, value);
  }

  /**
   * Retrieve string
   */
  async getString(key: string): Promise<string | null> {
    return this.get(key);
  }

  /**
   * Store number
   */
  async setNumber(key: string, value: number): Promise<void> {
    return this.set(key, value);
  }

  /**
   * Retrieve number
   */
  async getNumber(key: string): Promise<number | null> {
    const value = await this.get(key);
    return typeof value === 'number' ? value : null;
  }

  /**
   * Store boolean
   */
  async setBoolean(key: string, value: boolean): Promise<void> {
    return this.set(key, value);
  }

  /**
   * Retrieve boolean
   */
  async getBoolean(key: string): Promise<boolean | null> {
    const value = await this.get(key);
    return typeof value === 'boolean' ? value : null;
  }
}
