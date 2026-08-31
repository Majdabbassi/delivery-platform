import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-messagerie',
  standalone: false,
  templateUrl: './messagerie.component.html',
  styleUrl: './messagerie.component.css'
})
export class MessagerieComponent implements OnInit {
  // Static data for the messagerie interface
  contacts = [
    {
      id: 1,
      name: 'Davil Parnell',
      lastMessage: 'Furent tacitum recteque ad pro',
      time: '2 mins',
      avatar: 'DP',
      isOnline: true,
      unreadCount: 0
    },
    {
      id: 2,
      name: 'Ann Watkinson',
      lastMessage: 'Cum sociis natoque penatibus',
      time: '10 mins',
      avatar: 'AW',
      isOnline: true,
      unreadCount: 0
    },
    {
      id: 3,
      name: 'Marco Walter',
      lastMessage: 'Suspendisse sapien ligula',
      time: '15 mins',
      avatar: 'MW',
      isOnline: false,
      unreadCount: 0
    },
    {
      id: 4,
      name: 'Jeremy Robbins',
      lastMessage: 'Phasellus porttitor tellus nec',
      time: '30 mins',
      avatar: 'JR',
      isOnline: true,
      unreadCount: 0
    },
    {
      id: 5,
      name: 'Reginald Horace',
      lastMessage: 'Nullam consequat urna eget',
      time: '50 mins',
      avatar: 'RH',
      isOnline: false,
      unreadCount: 0
    }
  ];

  selectedContact = {
    id: 1,
    name: 'Abigail kelly',
    isOnline: true,
    avatar: 'AK'
  };

  messages = [
    {
      id: 1,
      text: 'Mauris volutpat magna nibh, et condimentum est rutrum a. Nunc sed turpis mi. In eu massa a sem pulvinar lobortis.',
      timestamp: '20/05/2024 at 09:30',
      isSent: false,
      avatar: 'AK'
    },
    {
      id: 2,
      text: 'Etiam ex accumsan',
      timestamp: '20/05/2024 at 09:33',
      isSent: true,
      avatar: 'ME'
    },
    {
      id: 3,
      text: 'Etiam nec facilisis lacus. Nulla imperdiet augue ullamcorper dui ullamcorper, eu laoreet sem consectetur. Aenean et ligula risus. Praesent sed posuere sem. Cum sociis natoque penatibus et magnis dis parturient montes.',
      timestamp: '20/05/2024 at 10:10',
      isSent: false,
      avatar: 'AK'
    }
  ];

  currentMessage = '';
  activeTab = 'Chat';

  constructor() { }

  ngOnInit(): void {
  }

  selectContact(contact: any): void {
    this.selectedContact = contact;
    // In a real app, this would load messages for the selected contact
  }

  sendMessage(): void {
    if (this.currentMessage.trim()) {
      this.messages.push({
        id: this.messages.length + 1,
        text: this.currentMessage,
        timestamp: new Date().toLocaleString(),
        isSent: true,
        avatar: 'ME'
      });
      this.currentMessage = '';
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }
}