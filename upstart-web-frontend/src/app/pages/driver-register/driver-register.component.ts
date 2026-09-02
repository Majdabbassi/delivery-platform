import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

interface DriverRegisterForm {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  licenseNumber?: string;
  vehicleType?: string;
  vehiclePlate?: string;
  vehicleModel?: string;
  vehicleColor?: string;
  deliveryZone?: string;
}

@Component({
  selector: 'app-driver-register',
  templateUrl: './driver-register.component.html',
  styleUrl: './driver-register.component.css',
  standalone: false
})
export class DriverRegisterComponent {
  registerForm: DriverRegisterForm = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phoneNumber: '',
    licenseNumber: '',
    vehicleType: 'CAR',
    vehiclePlate: '',
    vehicleModel: '',
    vehicleColor: '',
    deliveryZone: ''
  };

  confirmPassword = '';
  loading = false;
  success = false;
  error = '';
  showPassword = false;

  vehicleTypeOptions = ['MOTORCYCLE', 'CAR', 'VAN', 'TRUCK', 'BICYCLE'];

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.error = '';
    this.success = false;

    if (!this.registerForm.firstName || !this.registerForm.lastName ||
        !this.registerForm.email || !this.registerForm.username || !this.registerForm.password) {
      this.error = 'Please fill in all required fields';
      return;
    }

    if (this.registerForm.password.length < 8) {
      this.error = 'Password must be at least 8 characters long';
      return;
    }

    if (this.registerForm.password !== this.confirmPassword) {
      this.error = 'Passwords do not match';
      return;
    }

    this.loading = true;
    this.authService.registerDriver(this.registerForm).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
        this.error = '';
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 400) {
          const message = typeof err.error === 'string' ? err.error : (err.error?.message || 'Invalid registration data.');
          this.error = message || 'Invalid registration data. Please check your details.';
        } else if (err.status === 409) {
          this.error = 'An account with this username or email already exists.';
        } else if (err.status === 0) {
          this.error = 'Unable to connect to server. Please try again later.';
        } else {
          this.error = 'Registration failed. Please try again.';
        }
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}