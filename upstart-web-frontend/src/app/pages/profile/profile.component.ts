import { Component, OnInit } from '@angular/core';

interface UserProfile {
  id: number;
  name: string;
  email: string;
  phone: string;
  role: string;
  department: string;
  joinDate: Date;
  avatar: string;
  bio: string;
  location: string;
  timezone: string;
  language: string;
  notifications: {
    email: boolean;
    push: boolean;
    sms: boolean;
  };
  privacy: {
    profileVisibility: 'public' | 'private' | 'contacts';
    showEmail: boolean;
    showPhone: boolean;
  };
}

@Component({
  selector: 'app-profile',
  standalone: false,
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  user: UserProfile = {
    id: 1,
    name: 'John Smith',
    email: 'john.smith@company.com',
    phone: '+1 (555) 123-4567',
    role: 'Senior Developer',
    department: 'Engineering',
    joinDate: new Date('2022-03-15'),
    avatar: '👤',
    bio: 'Passionate full-stack developer with 5+ years of experience in building scalable web applications. Love working with modern technologies and solving complex problems.',
    location: 'San Francisco, CA',
    timezone: 'PST (UTC-8)',
    language: 'English',
    notifications: {
      email: true,
      push: true,
      sms: false
    },
    privacy: {
      profileVisibility: 'public',
      showEmail: true,
      showPhone: false
    }
  };

  isEditing = false;
  editedUser: UserProfile = { ...this.user };
  activeTab = 'profile';
  
  languages = [
    { code: 'en', name: 'English' },
    { code: 'es', name: 'Español' },
    { code: 'fr', name: 'Français' },
    { code: 'de', name: 'Deutsch' },
    { code: 'it', name: 'Italiano' },
    { code: 'pt', name: 'Português' },
    { code: 'ru', name: 'Русский' },
    { code: 'ja', name: '日本語' },
    { code: 'ko', name: '한국어' },
    { code: 'zh', name: '中文' }
  ];

  timezones = [
    { value: 'PST (UTC-8)', label: 'Pacific Standard Time (UTC-8)' },
    { value: 'MST (UTC-7)', label: 'Mountain Standard Time (UTC-7)' },
    { value: 'CST (UTC-6)', label: 'Central Standard Time (UTC-6)' },
    { value: 'EST (UTC-5)', label: 'Eastern Standard Time (UTC-5)' },
    { value: 'GMT (UTC+0)', label: 'Greenwich Mean Time (UTC+0)' },
    { value: 'CET (UTC+1)', label: 'Central European Time (UTC+1)' },
    { value: 'JST (UTC+9)', label: 'Japan Standard Time (UTC+9)' }
  ];

  ngOnInit() {
    // Initialize component
  }

  setActiveTab(tab: string) {
    this.activeTab = tab;
  }

  startEditing() {
    this.isEditing = true;
    this.editedUser = { ...this.user };
  }

  cancelEditing() {
    this.isEditing = false;
    this.editedUser = { ...this.user };
  }

  saveProfile() {
    this.user = { ...this.editedUser };
    this.isEditing = false;
    // Here you would typically save to a backend service
    console.log('Profile saved:', this.user);
  }

  uploadAvatar() {
    // Implement avatar upload logic
    console.log('Upload avatar clicked');
  }

  changePassword() {
    // Implement password change logic
    console.log('Change password clicked');
  }

  deleteAccount() {
    if (confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
      // Implement account deletion logic
      console.log('Delete account confirmed');
    }
  }

  exportData() {
    // Implement data export logic
    console.log('Export data clicked');
  }

  getUserInitials(): string {
    return this.user.name
      .split(' ')
      .map(name => name.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  formatJoinDate(): string {
    return this.user.joinDate.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getLanguageName(code: string): string {
    const language = this.languages.find(l => l.code === code);
    return language ? language.name : code.toUpperCase();
  }
}