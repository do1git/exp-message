import { apiRequest } from '@/shared/http/client';

export interface ChatRoom {
    id: string;
    name: string;
    lastMessage?: string;
    unreadCount?: number;
    updatedAt?: string;
}

interface WrappedResponse {
    success?: boolean;
    data?: unknown;
}

function unwrapPayload(payload: unknown) {
    const wrapped = payload as WrappedResponse;
    return wrapped.data ?? payload;
}

export async function getChatRoomsApi() {
    const payload = await apiRequest<unknown>({
        method: 'GET',
        url: '/chat-rooms',
    });

    const data = unwrapPayload(payload);
    if (Array.isArray(data)) {
        return data as ChatRoom[];
    }

    return [];
}

export async function createChatRoomApi(channelId?: string) {
  const roomName = channelId ? `Channel ${channelId}` : 'Customer Chat';
  const payload = await apiRequest<unknown>({
    method: 'POST',
    url: '/chat-rooms',
    data: {
      name: roomName,
    },
  });

    const data = unwrapPayload(payload) as { id?: string; roomId?: string; name?: string };
    const id = data.id ?? data.roomId;
    if (!id) {
        throw new Error('No room id returned by server.');
    }

    return { id, name: data.name };
}
