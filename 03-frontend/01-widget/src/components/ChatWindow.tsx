import { useState } from 'react';
import { Send } from 'lucide-react';
import type { WidgetConfig } from '../types/config';

interface ChatWindowProps {
    config: WidgetConfig;
}

export default function ChatWindow({ config }: ChatWindowProps) {
    const [message, setMessage] = useState('');

    const handleSend = () => {
        if (!message.trim()) return;
        console.log('Sending message:', message, 'Config:', config);
        setMessage('');
    };

    return (
        <div className="fixed bottom-24 right-6 w-96 h-[500px] bg-white rounded-lg shadow-2xl flex flex-col overflow-hidden">
            {/* Header */}
            <div className="bg-blue-600 text-white p-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 bg-blue-500 rounded-full flex items-center justify-center">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                        </svg>
                    </div>
                    <div>
                        <h3 className="font-semibold">r-message Chat Support</h3>
                        <p className="text-xs text-blue-100">Online</p>
                    </div>
                </div>
            </div>

            {/* Messages */}
            <div className="flex-1 p-4 overflow-y-auto bg-gray-50">
                <div className="mb-4">
                    <div className="bg-white p-3 rounded-lg shadow-sm max-w-[80%]">
                        <p className="text-sm text-gray-800">Hello! To continue to start a conversation, please let us know your email.</p>
                        <span className="text-xs text-gray-400 mt-1 block">Just now</span>
                    </div>
                </div>
            </div>

            {/* Input */}
            <div className="p-4 bg-white border-t border-gray-200">
                <div className=' bg-amber-300'>Authorized</div>
                <div className="flex gap-2 mt-2">
                    <input
                        type="text"
                        value={message}
                        onChange={(e) => setMessage(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && handleSend()}
                        placeholder="Type a message..."
                        className="flex-1 px-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500 text-sm"
                    />
                    <button
                        onClick={handleSend}
                        className="w-10 h-10 bg-blue-600  hover:bg-blue-700 text-white rounded-full flex items-center justify-center transition-colors"
                        aria-label="Send message"
                    >
                        <Send className="w-5 h-5 mr-0.5 mt-0.5" />
                    </button>
                </div>
            </div>
        </div>
    );
}
