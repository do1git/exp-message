import { X, MessageCircle } from 'lucide-react';

interface FloatingButtonProps {
    onClick: () => void;
    isOpen: boolean;
    isAuthorized: boolean;
}

export default function FloatingButton({ onClick, isOpen, isAuthorized }: FloatingButtonProps) {
    return (
        <button
            onClick={onClick}
            className="fixed bottom-6 right-6 flex h-14 w-14 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg transition-all duration-200 hover:scale-110 hover:bg-blue-700"
            aria-label={isOpen ? 'Close chat' : 'Open chat'}
        >
            {isAuthorized && !isOpen && (
                <span className="absolute -right-0.5 -top-0.5 h-3.5 w-3.5 rounded-full border-2 border-white bg-yellow-300" />
            )}

            {isOpen ? <X className="h-6 w-6" /> : <MessageCircle className="h-6 w-6" />}
        </button>
    );
}
