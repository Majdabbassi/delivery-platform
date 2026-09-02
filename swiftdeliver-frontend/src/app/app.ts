import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';
import { ThemeService } from './services/theme.service';
import { AuthService } from './services/auth.service';
import { RealtimeService } from './services/realtime.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.css'
})
export class App implements OnInit, OnDestroy {
  protected title = 'SwiftDeliver';
  isAuthenticated: boolean = false;
  isLoginPage: boolean = false;
  private routerSubscription: Subscription = new Subscription();
  private authSubscription: Subscription = new Subscription();
  
  constructor(
    private themeService: ThemeService,
    private authService: AuthService,
    private router: Router,
    private realtimeService: RealtimeService
  ) {
    // ThemeService will initialize automatically when injected
  }
  
  ngOnInit(): void {
    // Check initial authentication status
    this.isAuthenticated = this.authService.isAuthenticated();
    
    // Check initial route
    this.isLoginPage = this.isPublicPage(this.router.url);

    // Start the realtime (WebSocket / STOMP) connection if already authenticated
    if (this.isAuthenticated) {
      this.realtimeService.connect();
    }
    
    // Subscribe to authentication state changes
    this.authSubscription = this.authService.isAuthenticated$.subscribe(
      (isAuthenticated) => {
        this.isAuthenticated = isAuthenticated;
        if (isAuthenticated) {
          this.realtimeService.connect();
        } else {
          this.realtimeService.disconnect();
        }
      }
    );
    
    // Subscribe to router events to track route changes
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.isLoginPage = this.isPublicPage(event.url);
      });
  }

  private isPublicPage(url: string): boolean {
    return url.startsWith('/login') || url.startsWith('/register');
  }
  
  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
    this.realtimeService.disconnect();
  }
}
