import { Client, type IFrame, type IMessage, type StompSubscription } from '@stomp/stompjs';

interface ConnectOptions {
  wsUrl: string;
  accessToken: string;
  userId: string;
  onMessage: (message: IMessage) => void;
  onConnectionChange: (state: 'connecting' | 'connected' | 'disconnected' | 'error') => void;
  onStompError: (frame: IFrame) => void;
}

export interface ChatWsClient {
  disconnect: () => void;
  refreshAuth: (accessToken: string) => void;
  isConnected: () => boolean;
}

export function connectMessageSocket(options: ConnectOptions): ChatWsClient {
  let subscription: StompSubscription | null = null;

  const client = new Client({
    brokerURL: options.wsUrl,
    connectHeaders: {
      Authorization: options.accessToken.startsWith('Bearer ')
        ? options.accessToken
        : `Bearer ${options.accessToken}`,
    },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  options.onConnectionChange('connecting');

  client.onConnect = () => {
    subscription = client.subscribe(`/topic/user/${options.userId}/messages`, options.onMessage);
    options.onConnectionChange('connected');
  };

  client.onStompError = (frame) => {
    options.onConnectionChange('error');
    options.onStompError(frame);
  };

  client.onWebSocketError = () => {
    options.onConnectionChange('error');
  };

  client.onWebSocketClose = () => {
    options.onConnectionChange('disconnected');
  };

  client.activate();

  return {
    disconnect() {
      subscription?.unsubscribe();
      subscription = null;
      client.deactivate();
      options.onConnectionChange('disconnected');
    },
    refreshAuth(accessToken) {
      if (!client.connected) {
        return;
      }
      const authHeader = accessToken.startsWith('Bearer ') ? accessToken : `Bearer ${accessToken}`;
      try {
        client.publish({
          destination: '/app/auth/refresh',
          headers: { Authorization: authHeader, 'content-type': 'application/json' },
          body: JSON.stringify({ accessToken }),
        });
      } catch {
        // Ignore publish failures during reconnect windows.
      }
    },
    isConnected() {
      return client.connected;
    },
  };
}
