import React from 'react';
import ReactDOM from 'react-dom/client';
import widgetStyles from './styles/index.css?inline';
import type { WidgetConfig } from './types/config';
import Widget from './Widget';

declare global {
    interface Window {
        ChatWidget: {
            init: (config?: WidgetConfig) => void;
        };
    }
}

let widgetRoot: ReturnType<typeof ReactDOM.createRoot> | null = null;

window.ChatWidget = {
    init: (config: WidgetConfig = {}) => {
        // Reuse existing host to prevent duplicate widgets during HMR/manual init calls.
        let container = document.getElementById('chat-widget-root') as HTMLDivElement | null;
        if (!container) {
            container = document.createElement('div');
            container.id = 'chat-widget-root';
            document.body.appendChild(container);
            if (import.meta.env.DEV) {
                console.debug('[ChatWidget] host container created');
            }
        }
        container.style.cssText = 'position: fixed; bottom: 0; right: 0; z-index: 2147483647;';

        // Use Shadow DOM for style isolation
        const shadowRoot = container.shadowRoot ?? container.attachShadow({ mode: 'open' });
        let mountPoint = shadowRoot.getElementById('chat-widget-mount') as HTMLDivElement | null;
        if (!mountPoint) {
            mountPoint = document.createElement('div');
            mountPoint.id = 'chat-widget-mount';
            shadowRoot.appendChild(mountPoint);
        }

        // Inject styles into Shadow DOM
        let style = shadowRoot.getElementById('chat-widget-styles') as HTMLStyleElement | null;
        if (!style) {
            style = document.createElement('style');
            style.id = 'chat-widget-styles';
            shadowRoot.appendChild(style);
        }
        style.textContent = widgetStyles;

        // Render React widget
        if (!widgetRoot) {
            widgetRoot = ReactDOM.createRoot(mountPoint);
        }
        widgetRoot.render(
            <React.StrictMode>
                <Widget config={config} />
            </React.StrictMode>
        );

        if (import.meta.env.DEV) {
            console.debug('[ChatWidget] initialized', {
                hasShadowRoot: Boolean(container.shadowRoot),
                hasMountPoint: Boolean(mountPoint),
            });
        }
    },
};

// Auto-initialize for development
if (import.meta.env.DEV) {
    window.ChatWidget.init({
        apiUrl: 'http://localhost:8080/api',
        wsUrl: 'ws://localhost:8080/ws',
        channelId: 'dev-channel',
        theme: 'light'
    });
}
