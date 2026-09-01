import { Injectable } from '@angular/core';
import { Camera, CameraResultType, CameraSource, GalleryImageOptions, ImageOptions } from '@capacitor/camera';
import { BehaviorSubject } from 'rxjs';

export interface CapturedImage {
  path: string;
  webPath?: string;
  exif?: any;
  format?: string;
  savedImageFilePath?: string;
}

/**
 * Native Camera Service
 * Provides photo capture functionality for proof of delivery and driver verification
 * Uses Capacitor Camera plugin for native access
 */
@Injectable({
  providedIn: 'root'
})
export class NativeCameraService {
  private capturedImagesSubject = new BehaviorSubject<CapturedImage[]>([]);
  public capturedImages$ = this.capturedImagesSubject.asObservable();

  private lastImageSubject = new BehaviorSubject<CapturedImage | null>(null);
  public lastImage$ = this.lastImageSubject.asObservable();

  constructor() {}

  /**
   * Take a photo using device camera
   */
  async takePhoto(): Promise<CapturedImage> {
    try {
      const image = await Camera.getPhoto({
        quality: 90,
        allowEditing: false,
        resultType: CameraResultType.Uri,
        source: CameraSource.Camera,
        correctOrientation: true,
        webUseInput: true
      });

      const capturedImage: CapturedImage = {
        path: image.path || '',
        webPath: image.webPath,
        exif: image.exif,
        format: image.format
      };

      // Add to captured images list
      const currentImages = this.capturedImagesSubject.value;
      this.capturedImagesSubject.next([...currentImages, capturedImage]);

      // Set as last image
      this.lastImageSubject.next(capturedImage);

      return capturedImage;
    } catch (error) {
      console.error('Error taking photo:', error);
      throw error;
    }
  }

  /**
   * Pick a photo from device gallery
   */
  async pickPhoto(): Promise<CapturedImage> {
    try {
      const image = await Camera.getPhoto({
        quality: 90,
        allowEditing: false,
        resultType: CameraResultType.Uri,
        source: CameraSource.Photos,
        correctOrientation: true,
        webUseInput: true
      });

      const capturedImage: CapturedImage = {
        path: image.path || '',
        webPath: image.webPath,
        exif: image.exif,
        format: image.format
      };

      // Add to captured images list
      const currentImages = this.capturedImagesSubject.value;
      this.capturedImagesSubject.next([...currentImages, capturedImage]);

      // Set as last image
      this.lastImageSubject.next(capturedImage);

      return capturedImage;
    } catch (error) {
      console.error('Error picking photo:', error);
      throw error;
    }
  }

  /**
   * Get photo or camera input (camera first, gallery fallback)
   */
  async getPhoto(): Promise<CapturedImage> {
    try {
      const image = await Camera.getPhoto({
        quality: 90,
        allowEditing: false,
        resultType: CameraResultType.Uri,
        correctOrientation: true,
        webUseInput: true
      });

      const capturedImage: CapturedImage = {
        path: image.path || '',
        webPath: image.webPath,
        exif: image.exif,
        format: image.format
      };

      // Add to captured images list
      const currentImages = this.capturedImagesSubject.value;
      this.capturedImagesSubject.next([...currentImages, capturedImage]);

      // Set as last image
      this.lastImageSubject.next(capturedImage);

      return capturedImage;
    } catch (error) {
      console.error('Error getting photo:', error);
      throw error;
    }
  }

  /**
   * Get all captured images
   */
  getCapturedImages(): CapturedImage[] {
    return this.capturedImagesSubject.value;
  }

  /**
   * Get last captured image
   */
  getLastImage(): CapturedImage | null {
    return this.lastImageSubject.value;
  }

  /**
   * Clear captured images
   */
  clearCapturedImages(): void {
    this.capturedImagesSubject.next([]);
    this.lastImageSubject.next(null);
  }

  /**
   * Remove specific captured image
   */
  removeCapturedImage(index: number): void {
    const currentImages = this.capturedImagesSubject.value;
    const filteredImages = currentImages.filter((_, i) => i !== index);
    this.capturedImagesSubject.next(filteredImages);

    if (filteredImages.length === 0) {
      this.lastImageSubject.next(null);
    }
  }

  /**
   * Convert image to base64 for upload
   */
  async imageToBase64(webPath: string): Promise<string> {
    try {
      const response = await fetch(webPath);
      const blob = await response.blob();
      return new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onloadend = () => {
          const base64 = reader.result as string;
          resolve(base64);
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      });
    } catch (error) {
      console.error('Error converting image to base64:', error);
      throw error;
    }
  }

  /**
   * Check if camera is available
   */
  async isCameraAvailable(): Promise<boolean> {
    try {
      const status = await Camera.checkPermissions();
      return status.camera === 'granted' || status.camera === 'prompt';
    } catch {
      return false;
    }
  }
}
