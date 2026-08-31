import { API_BASE_URL } from '../api';
import { OrderRealtimeEvent } from '../types';

type MessageHandler = (message: OrderRealtimeEvent) => void;

const WS_URL = API_BASE_URL.replace('http://', 'ws://').replace('/api', '').replace(/\/$/, '') + '/ws';

class RealtimeClient {
  private socket: WebSocket | null = null;
  private subscriptions = new Map<string, Set<MessageHandler>>();
  private pendingFrames: string[] = [];

  private ensureConnected() {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }
    this.socket = new WebSocket(WS_URL);
    this.socket.onopen = () => this.sendFrame('CONNECT', { 'accept-version': '1.2', 'heart-beat': '0,0' });
    this.socket.onmessage = (event) => this.handleFrame(String(event.data));
    this.socket.onerror = () => this.socket?.close();
    this.socket.onclose = () => {
      this.socket = null;
    };
  }

  private sendFrame(command: string, headers: Record<string, string>, body?: string) {
    const headerLines = Object.entries(headers).map(([k, v]) => `${k}:${v}`).join('\n');
    const frame = body ? `${command}\n${headerLines}\n\n${body}\u0000` : `${command}\n${headerLines}\n\n\u0000`;
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      this.pendingFrames.push(frame);
      return;
    }
    this.socket.send(frame);
  }

  private flushPending() {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;
    const pending = this.pendingFrames;
    this.pendingFrames = [];
    pending.forEach((frame) => this.socket!.send(frame));
  }

  private handleFrame(data: string) {
    if (data.startsWith('CONNECTED')) {
      this.flushPending();
      return;
    }
    if (!data.startsWith('MESSAGE')) return;
    const headerEnd = data.indexOf('\n\n');
    const payloadStart = headerEnd + 2;
    const destinationLine = data.slice(0, headerEnd).split('\n').find((l) => l.toLowerCase().startsWith('destination:'));
    if (!destinationLine) return;
    const destination = destinationLine.slice(destinationLine.indexOf(':') + 1);
    const bodyEnd = data.indexOf('\u0000', payloadStart);
    const body = data.slice(payloadStart, bodyEnd === -1 ? undefined : bodyEnd).trim();
    if (!body) return;
    try {
      const event = JSON.parse(body) as OrderRealtimeEvent;
      const handlers = this.subscriptions.get(destination);
      handlers?.forEach((handler) => handler(event));
    } catch {
      // Ignore malformed payloads
    }
  }

  subscribe(topic: string, handler: MessageHandler): () => void {
    this.ensureConnected();
    if (!this.subscriptions.has(topic)) {
      this.subscriptions.set(topic, new Set());
      this.sendFrame('SUBSCRIBE', { id: `sub-${topic}`, destination: topic });
    }
    this.subscriptions.get(topic)!.add(handler);
    return () => {
      const handlers = this.subscriptions.get(topic);
      handlers?.delete(handler);
      if (handlers?.size === 0) {
        this.subscriptions.delete(topic);
      }
    };
  }

  disconnect() {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      const frame = `DISCONNECT\naccept-version:1.2,1.1,1.0\ncontent-length:0\n\n\u0000`;
      this.socket.send(frame);
    }
    this.socket?.close();
    this.socket = null;
    this.subscriptions.clear();
    this.pendingFrames = [];
  }
}

export const realtimeClient = new RealtimeClient();

export function subscribeOrderRealtime(
  orderId: number,
  onEvent: (event: OrderRealtimeEvent) => void
): () => void {
  const unsubStatus = realtimeClient.subscribe(`/topic/orders/${orderId}`, onEvent);
  const unsubAll = realtimeClient.subscribe(`/topic/orders`, (event) => {
    if (event.orderId === orderId) onEvent(event);
  });
  const unsubLocation = realtimeClient.subscribe(`/topic/orders/location`, (event) => {
    if (event.orderId === orderId) onEvent(event);
  });
  return () => {
    unsubStatus();
    unsubAll();
    unsubLocation();
  };
}

export default realtimeClient;