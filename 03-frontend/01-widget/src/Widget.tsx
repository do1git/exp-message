import { useEffect, useState } from 'react';
import FloatingButton from './components/FloatingButton';
import type { WidgetConfig } from './types/config';
import AuthView from './features/auth/AuthView';
import ChatRoomView from './features/chat-rooms/ChatRoomView';
import { useAuthStore } from './features/auth/store';
import { configureHttpClient } from './shared/http/client';
import StatusBadge from './shared/ui/StatusBadge';
import { useRoomSessionStore } from './features/chat-rooms/store';
import { useChatStore } from './features/chat-rooms/chat-store';

interface WidgetProps {
    config: WidgetConfig;
}

export default function Widget({ config }: WidgetProps) {
    const [isOpen, setIsOpen] = useState(false);
    const authStatus = useAuthStore((state) => state.authStatus);
    const accessToken = useAuthStore((state) => state.accessToken);
    const userId = useAuthStore((state) => state.userId);
    const hydrateRefreshToken = useAuthStore((state) => state.hydrateRefreshToken);
    const activeRoomId = useRoomSessionStore((state) => state.activeRoomId);
    const activeRoomName = useRoomSessionStore((state) => state.activeRoomName);
    const roomStatus = useRoomSessionStore((state) => state.roomStatus);
    const roomErrorMessage = useRoomSessionStore((state) => state.errorMessage);
    const ensureActiveRoom = useRoomSessionStore((state) => state.ensureActiveRoom);
    const resetRoomSession = useRoomSessionStore((state) => state.resetRoomSession);
    const teardownChat = useChatStore((state) => state.teardown);

    const isAuthorized = authStatus === 'authorized';

    useEffect(() => {
        configureHttpClient(config.apiUrl);
    }, [config.apiUrl]);

    useEffect(() => {
        hydrateRefreshToken();
    }, [hydrateRefreshToken]);

    useEffect(() => {
        if (isAuthorized) {
            void ensureActiveRoom(config.channelId);
            return;
        }
        resetRoomSession();
        teardownChat();
    }, [config.channelId, ensureActiveRoom, isAuthorized, resetRoomSession, teardownChat]);

    return (
        <div className="chat-widget">
            <FloatingButton
                onClick={() => setIsOpen((prev) => !prev)}
                isOpen={isOpen}
                isAuthorized={isAuthorized}
            />

            {isOpen && (
                <div className="fixed bottom-24 right-6 flex h-[560px] w-96 flex-col overflow-hidden rounded-xl bg-white shadow-2xl">
                    <div className="flex items-center justify-between bg-blue-600 px-4 py-3 text-white">
                        <div>
                            <h3 className="text-sm font-semibold">r-message Chat Support</h3>
                            <p className="text-xs text-blue-100">Always-on status</p>
                        </div>
                        <StatusBadge isAuthorized={isAuthorized} />
                    </div>

                    {!isAuthorized && <AuthView />}

                    {isAuthorized && roomStatus === 'loading' && (
                        <div className="flex flex-1 items-center justify-center bg-gray-50 p-4 text-sm text-gray-600">
                            Preparing chat room...
                        </div>
                    )}

                    {isAuthorized && roomStatus === 'error' && (
                        <div className="flex flex-1 flex-col items-center justify-center gap-3 bg-gray-50 p-4">
                            <p className="text-center text-sm text-red-600">{roomErrorMessage ?? 'Unable to prepare chat room'}</p>
                            <button
                                type="button"
                                className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700"
                                onClick={() => void ensureActiveRoom(config.channelId)}
                            >
                                Retry
                            </button>
                        </div>
                    )}

                    {isAuthorized && roomStatus === 'ready' && activeRoomId && activeRoomName && (
                        <ChatRoomView
                            roomId={activeRoomId}
                            roomName={activeRoomName}
                            wsUrl={config.wsUrl}
                            accessToken={accessToken}
                            userId={userId}
                            currentUserId={userId}
                        />
                    )}
                </div>
            )}
        </div>
    );
}
