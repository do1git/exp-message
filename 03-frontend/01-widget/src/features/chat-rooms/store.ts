import { create } from 'zustand';
import { createChatRoomApi, getChatRoomsApi, type ChatRoom } from './api';
import { toApiErrorMessage } from '@/shared/http/client';

type RoomStatus = 'idle' | 'loading' | 'ready' | 'error';

interface RoomSessionState {
    activeRoomId: string | null;
    activeRoomName: string | null;
    roomStatus: RoomStatus;
    errorMessage: string | null;
    ensureActiveRoom: (channelId?: string) => Promise<void>;
    resetRoomSession: () => void;
}

function selectReusableRoom(rooms: ChatRoom[]) {
    if (rooms.length === 0) {
        return null;
    }

    const sorted = [...rooms].sort((a, b) => {
        const left = a.updatedAt ? new Date(a.updatedAt).getTime() : 0;
        const right = b.updatedAt ? new Date(b.updatedAt).getTime() : 0;
        return right - left;
    });

    return sorted[0];
}

export const useRoomSessionStore = create<RoomSessionState>((set, get) => ({
    activeRoomId: null,
    activeRoomName: null,
    roomStatus: 'idle',
    errorMessage: null,
    async ensureActiveRoom(channelId) {
        const current = get();
        if (current.roomStatus === 'loading' || current.activeRoomId) {
            return;
        }

        set({ roomStatus: 'loading', errorMessage: null });

        try {
            const rooms = await getChatRoomsApi();
            const reusableRoom = selectReusableRoom(rooms);

            if (reusableRoom?.id) {
                set({
                    activeRoomId: reusableRoom.id,
                    activeRoomName: reusableRoom.name,
                    roomStatus: 'ready',
                    errorMessage: null,
                });
                return;
            }

            const createdRoom = await createChatRoomApi(channelId);
            set({
                activeRoomId: createdRoom.id,
                activeRoomName: createdRoom.name ?? 'Customer Chat',
                roomStatus: 'ready',
                errorMessage: null,
            });
        } catch (error) {
            set({
                activeRoomId: null,
                activeRoomName: null,
                roomStatus: 'error',
                errorMessage: toApiErrorMessage(error, 'Unable to prepare chat room'),
            });
        }
    },
    resetRoomSession() {
        set({
            activeRoomId: null,
            activeRoomName: null,
            roomStatus: 'idle',
            errorMessage: null,
        });
    },
}));
