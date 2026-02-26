import { useState } from 'react';
import FloatingButton from './components/FloatingButton';
import type { WidgetConfig } from './types/config';
import ChatWindow from './components/ChatWindow';

interface WidgetProps {
    config: WidgetConfig;
}

export default function Widget({ config }: WidgetProps) {
    const [isOpen, setIsOpen] = useState(false);

    return (
        <div className="chat-widget">
            <FloatingButton onClick={() => setIsOpen(!isOpen)} isOpen={isOpen} />
            {isOpen && <ChatWindow config={config} />}
        </div>
    );
}
