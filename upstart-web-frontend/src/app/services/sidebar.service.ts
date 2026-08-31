import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  private isCollapsedSubject = new BehaviorSubject<boolean>(false);
  public isCollapsed$ = this.isCollapsedSubject.asObservable();

  constructor() { }

  toggleSidebar(): void {
    const currentState = this.isCollapsedSubject.value;
    this.isCollapsedSubject.next(!currentState);
  }

  setSidebarState(collapsed: boolean): void {
    this.isCollapsedSubject.next(collapsed);
  }

  getSidebarState(): boolean {
    return this.isCollapsedSubject.value;
  }
}