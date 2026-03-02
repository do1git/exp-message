import { apiRequest } from '@/shared/http/client';

export interface ChatMessage {
  id: string;
  chatRoomId: string;
  userId: string;
  content: string;
  createdAt: string;
}

interface MessageListResponse {
  data?: unknown;
  pageInfo?: {
    nextCursor?: string | null;
    limit?: number;
  };
}

interface MessageListResult {
  items: ChatMessage[];
  nextCursor: string | null;
}

function unwrapPayload(payload: unknown) {
  const wrapped = payload as { data?: unknown };
  return wrapped.data ?? payload;
}

export async function getMessagesApi(chatRoomId: string, cursor?: string | null, limit = 20): Promise<MessageListResult> {
  const payload = await apiRequest<unknown>({
    method: 'GET',
    url: '/messages',
    params: {
      chatRoomId,
      cursor: cursor ?? undefined,
      limit,
    },
  });

  const wrapped = payload as MessageListResponse;
  const data = unwrapPayload(payload);
  const items = Array.isArray(data) ? (data as ChatMessage[]) : [];

  return {
    items,
    nextCursor: wrapped.pageInfo?.nextCursor ?? null,
  };
}

export async function sendMessageApi(chatRoomId: string, content: string): Promise<ChatMessage> {
  const payload = await apiRequest<unknown>({
    method: 'POST',
    url: '/messages',
    data: {
      chatRoomId,
      content,
    },
  });

  const data = unwrapPayload(payload) as Partial<ChatMessage>;
  if (!data.id || !data.chatRoomId || !data.userId || !data.content || !data.createdAt) {
    throw new Error('Invalid message response');
  }

  return {
    id: data.id,
    chatRoomId: data.chatRoomId,
    userId: data.userId,
    content: data.content,
    createdAt: data.createdAt,
  };
}
