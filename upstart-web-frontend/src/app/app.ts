import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ThemeService } from './services/theme.service';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected title = 'Upstart';
  isAuthenticated: boolean = false;
  isLoginPage: boolean = false;
  private routerSubscription: Subscription = new Subscription();
  private authSubscription: Subscription = new Subscription();
  
  constructor(
    private themeService: ThemeService,
    private authService: AuthService,
    private router: Router
  ) {
    // ThemeService will initialize automatically when injected
  }
  
  ngOnInit(): void {
    // Check initial authentication status
    this.isAuthenticated = this.authService.isAuthenticated();
    
    // Check initial route
    this.isLoginPage = this.router.url.startsWith('/login');
    
    // Subscribe to authentication state changes
    this.authSubscription = this.authService.isAuthenticated$.subscribe(
      (isAuthenticated) => {
        this.isAuthenticated = isAuthenticated;
      }
    );
    
    // Subscribe to router events to track route changes
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.isLoginPage = event.url.startsWith('/login');
      });
  }
  
  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
  }
}
