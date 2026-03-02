import { create } from 'zustand';
import { toApiErrorMessage } from '@/shared/http/client';
import { getMessagesApi, sendMessageApi, type ChatMessage } from './message-api';
import { connectMessageSocket, type ChatWsClient } from './ws-client';
import { useAuthStore } from '@/features/auth/store';

type ConnectionStatus = 'idle' | 'connecting' | 'connected' | 'disconnected' | 'error';

interface InitRoomParams {
  roomId: string;
  wsUrl?: string;
  accessToken?: string | null;
  userId?: string | null;
}

interface ChatStoreState {
  roomId: string | null;
  messages: ChatMessage[];
  nextCursor: string | null;
  hasNext: boolean;
  loadingInitial: boolean;
  loadingMore: boolean;
  sending: boolean;
  errorMessage: string | null;
  connectionStatus: ConnectionStatus;
  initRoom: (params: InitRoomParams) => Promise<void>;
  refreshSocketAuth: (accessToken?: string | null) => void;
  loadMore: () => Promise<void>;
  sendMessage: (content: string) => Promise<void>;
  teardown: () => void;
}

let wsClient: ChatWsClient | null = null;
let isRefreshingWsAuth = false;

function mergeUnique(messages: ChatMessage[], incoming: ChatMessage[]) {
  const map = new Map<string, ChatMessage>();
  for (const message of [...messages, ...incoming]) {
    map.set(message.id, message);
  }
  return Array.from(map.values()).sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
}

function parseWsMessage(body: string): ChatMessage | null {
  try {
    const parsed = JSON.parse(body) as Partial<ChatMessage>;
    if (!parsed.id || !parsed.chatRoomId || !parsed.userId || !parsed.content || !parsed.createdAt) {
      return null;
    }
    return {
      id: parsed.id,
      chatRoomId: parsed.chatRoomId,
      userId: parsed.userId,
      content: parsed.content,
      createdAt: parsed.createdAt,
    };
  } catch {
    return null;
  }
}

export const useChatStore = create<ChatStoreState>((set, get) => ({
  roomId: null,
  messages: [],
  nextCursor: null,
  hasNext: false,
  loadingInitial: false,
  loadingMore: false,
  sending: false,
  errorMessage: null,
  connectionStatus: 'idle',
  async initRoom({ roomId, wsUrl, accessToken, userId }) {
    const current = get();
    if (current.loadingInitial) {
      return;
    }

    if (current.roomId !== roomId) {
      wsClient?.disconnect();
      wsClient = null;
      set({
        roomId,
        messages: [],
        nextCursor: null,
        hasNext: false,
      });
    }

    set({
      loadingInitial: true,
      errorMessage: null,
    });

    try {
      const firstPage = await getMessagesApi(roomId, null, 20);
      set((state) => ({
        messages: mergeUnique(state.messages, firstPage.items),
        nextCursor: firstPage.nextCursor,
        hasNext: Boolean(firstPage.nextCursor),
      }));
    } catch (error) {
      set({
        errorMessage: toApiErrorMessage(error, 'Unable to load messages'),
      });
    } finally {
      set({ loadingInitial: false });
    }

    if (!wsUrl || !accessToken || !userId || wsClient) {
      return;
    }

    wsClient = connectMessageSocket({
      wsUrl,
      accessToken,
      userId,
      onConnectionChange: (status) => set({ connectionStatus: status }),
      onStompError: async (frame) => {
        const messageHeader = frame.headers?.message ?? '';
        const shouldRefresh = messageHeader.toLowerCase().includes('unauthorized');
        if (!shouldRefresh || isRefreshingWsAuth) {
          return;
        }

        isRefreshingWsAuth = true;
        try {
          const refreshed = await useAuthStore.getState().refreshAccessToken();
          if (refreshed) {
            wsClient?.refreshAuth(refreshed);
            set({ connectionStatus: 'connecting' });
          }
        } finally {
          isRefreshingWsAuth = false;
        }
      },
      onMessage: (frame) => {
        const message = parseWsMessage(frame.body);
        if (!message) {
          return;
        }

        if (message.chatRoomId !== get().roomId) {
          return;
        }

        set((state) => ({
          messages: mergeUnique(state.messages, [message]),
        }));
      },
    });
  },
  refreshSocketAuth(accessToken) {
    if (!wsClient || !accessToken) {
      return;
    }
    if (!wsClient.isConnected()) {
      return;
    }
    wsClient.refreshAuth(accessToken);
  },
  async loadMore() {
    const state = get();
    if (!state.roomId || !state.hasNext || !state.nextCursor || state.loadingMore) {
      return;
    }

    set({ loadingMore: true });
    try {
      const page = await getMessagesApi(state.roomId, state.nextCursor, 20);
      set((current) => ({
        messages: mergeUnique(current.messages, page.items),
        nextCursor: page.nextCursor,
        hasNext: Boolean(page.nextCursor),
      }));
    } catch (error) {
      set({ errorMessage: toApiErrorMessage(error, 'Unable to load more messages') });
    } finally {
      set({ loadingMore: false });
    }
  },
  async sendMessage(content) {
    const state = get();
    if (!state.roomId || !content.trim() || state.sending) {
      return;
    }

    set({ sending: true, errorMessage: null });
    try {
      const sent = await sendMessageApi(state.roomId, content.trim());
      set((current) => ({
        messages: mergeUnique(current.messages, [sent]),
      }));
    } catch (error) {
      set({ errorMessage: toApiErrorMessage(error, 'Unable to send message') });
    } finally {
      set({ sending: false });
    }
  },
  teardown() {
    wsClient?.disconnect();
    wsClient = null;
    set({
      roomId: null,
      messages: [],
      nextCursor: null,
      hasNext: false,
      loadingInitial: false,
      loadingMore: false,
      sending: false,
      errorMessage: null,
      connectionStatus: 'idle',
    });
  },
}));
