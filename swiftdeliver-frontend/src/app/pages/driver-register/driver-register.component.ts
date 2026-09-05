import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-driver-register',
  templateUrl: './driver-register.component.html',
  styleUrl: './driver-register.component.css',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule]
})
export class DriverRegisterComponent {
  registerForm: FormGroup;

  loading = false;
  success = false;
  error = '';
  showPassword = false;

  vehicleTypeOptions = ['MOTORCYCLE', 'CAR', 'VAN', 'TRUCK', 'BICYCLE'];

  constructor(private authService: AuthService, private router: Router, private fb: FormBuilder) {
    this.registerForm = this.fb.group(
      {
        firstName: ['', [Validators.required]],
        lastName: ['', [Validators.required]],
        username: ['', [Validators.required]],
        email: ['', [Validators.required, Validators.email]],
        phoneNumber: [''],
        licenseNumber: [''],
        vehicleType: ['CAR'],
        vehiclePlate: [''],
        vehicleModel: [''],
        vehicleColor: [''],
        deliveryZone: [''],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', [Validators.required]]
      },
      { validators: this.passwordMatchValidator }
    );
  }

  private passwordMatchValidator(form: FormGroup): Record<string, boolean> | null {
    const password = form.get('password');
    const confirmPassword = form.get('confirmPassword');
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      return { passwordMismatch: true };
    }
    return null;
  }

  onSubmit(): void {
    this.error = '';
    this.success = false;

    if (this.registerForm.invalid) {
      if (this.registerForm.get('email')?.hasError('email')) {
        this.error = 'Please enter a valid email address';
      } else if (this.registerForm.get('password')?.hasError('minlength')) {
        this.error = 'Password must be at least 8 characters long';
      } else if (this.registerForm.errors?.['passwordMismatch']) {
        this.error = 'Passwords do not match';
      } else {
        this.error = 'Please fill in all required fields';
      }
      return;
    }

    const form = this.registerForm.getRawValue();
    const request = {
      firstName: form.firstName,
      lastName: form.lastName,
      username: form.username,
      email: form.email,
      phoneNumber: form.phoneNumber,
      licenseNumber: form.licenseNumber,
      vehicleType: form.vehicleType,
      vehiclePlate: form.vehiclePlate,
      vehicleModel: form.vehicleModel,
      vehicleColor: form.vehicleColor,
      deliveryZone: form.deliveryZone,
      password: form.password
    };

    this.loading = true;
    this.authService.registerDriver(request).subscribe({
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