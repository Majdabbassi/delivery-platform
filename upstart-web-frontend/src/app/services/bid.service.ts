import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config';

export enum BidStatus {
  SUBMITTED = 'SUBMITTED',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  WITHDRAWN = 'WITHDRAWN',
  EXPIRED = 'EXPIRED'
}

export enum BidderType {
  COMPANY = 'COMPANY',
  INDEPENDENT_DRIVER = 'INDEPENDENT_DRIVER'
}

export interface Bid {
  id: number;
  bidId: string;
  bidderType: BidderType;
  orderId: number;
  deliveryCompanyId?: number;
  driverId?: number;
  bidAmount: number;
  estimatedDeliveryTime?: string;
  message?: string;
  status: BidStatus;
  submittedAt: string;
  respondedAt?: string;
  responseMessage?: string;
}

export interface BidRequest {
  orderId: number;
  bidderType?: BidderType;
  deliveryCompanyId?: number;
  driverId?: number;
  bidAmount: number;
  estimatedDeliveryTime?: string;
  message?: string;
}

export interface BidRanking {
  bid: Bid;
  score: number;
  reasons: string[];
}

export interface BidStatistics {
  bidsSubmitted: number;
  bidsAccepted: number;
  bidsRejected: number;
  bidsWithdrawn: number;
  bidsExpired: number;
  acceptanceRate: number;
}

export interface PageMetadata {
  content: any[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BidService {
  private readonly baseUrl = `${API_BASE_URL}/bids`;

  constructor(private http: HttpClient) {}

  submitBid(request: BidRequest): Observable<Bid> {
    return this.http.post<Bid>(this.baseUrl, request);
  }

  getBidsForOrder(orderId: number): Observable<Bid[]> {
    return this.http.get<Bid[]>(`${this.baseUrl}/order/${orderId}`);
  }

  getBidRankings(orderId: number): Observable<BidRanking[]> {
    return this.http.get<BidRanking[]>(`${this.baseUrl}/order/${orderId}/rankings`);
  }

  getMyBids(deliveryCompanyId?: number): Observable<PageMetadata> {
    let params = new HttpParams();
    if (deliveryCompanyId != null) {
      params = params.set('deliveryCompanyId', deliveryCompanyId.toString());
    }
    return this.http.get<PageMetadata>(`${this.baseUrl}/my`, { params });
  }

  getMyDriverBids(driverId?: number): Observable<PageMetadata> {
    let params = new HttpParams();
    if (driverId != null) {
      params = params.set('driverId', driverId.toString());
    }
    return this.http.get<PageMetadata>(`${this.baseUrl}/my/driver`, { params });
  }

  acceptBid(bidId: string, message?: string): Observable<Bid> {
    let params = new HttpParams();
    if (message) {
      params = params.set('message', message);
    }
    return this.http.post<Bid>(`${this.baseUrl}/${bidId}/accept`, null, { params });
  }

  rejectBid(bidId: string, message?: string): Observable<Bid> {
    let params = new HttpParams();
    if (message) {
      params = params.set('message', message);
    }
    return this.http.post<Bid>(`${this.baseUrl}/${bidId}/reject`, null, { params });
  }

  withdrawBid(bidId: string): Observable<Bid> {
    return this.http.post<Bid>(`${this.baseUrl}/${bidId}/withdraw`, null);
  }

  getMyBidStats(deliveryCompanyId?: number): Observable<BidStatistics> {
    let params = new HttpParams();
    if (deliveryCompanyId != null) {
      params = params.set('deliveryCompanyId', deliveryCompanyId.toString());
    }
    return this.http.get<BidStatistics>(`${this.baseUrl}/my/stats`, { params });
  }

  getMyDriverBidStats(driverId?: number): Observable<BidStatistics> {
    let params = new HttpParams();
    if (driverId != null) {
      params = params.set('driverId', driverId.toString());
    }
    return this.http.get<BidStatistics>(`${this.baseUrl}/my/stats/driver`, { params });
  }
}