import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  private client: Client | null = null;
  private connected: boolean = false;

  public connect(onConnected?: () => void) {
    if (this.client && this.connected) {
      if (onConnected) onConnected();
      return;
    }

    const token = localStorage.getItem('drivio_token');
    const wsUrl = token ? `/ws?token=${encodeURIComponent(token)}` : '/ws';

    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        // debug logging disabled in production
      },
    });

    this.client.onConnect = () => {
      this.connected = true;
      if (onConnected) onConnected();
    };

    this.client.onDisconnect = () => {
      this.connected = false;
    };

    this.client.activate();
  }

  public subscribe(destination: string, callback: (message: any) => void) {
    if (!this.client || !this.connected) {
      this.connect(() => {
        this.client?.subscribe(destination, (msg: IMessage) => {
          try {
            callback(JSON.parse(msg.body));
          } catch (e) {
            callback(msg.body);
          }
        });
      });
      return;
    }

    return this.client.subscribe(destination, (msg: IMessage) => {
      try {
        callback(JSON.parse(msg.body));
      } catch (e) {
        callback(msg.body);
      }
    });
  }

  public disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }
}

export const socketService = new WebSocketService();

