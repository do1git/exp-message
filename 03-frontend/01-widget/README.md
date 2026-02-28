# Chat Widget

Embeddable chat widget built with React, bundled as a single JavaScript file for easy integration.

## How to Build

```bash
# Install dependencies (from workspace root)
npm install

# Build widget
npm run build
```

Output:
- `dist/widget.iife.js` (single file containing all code and styles)
- `../02-widget-example/widget.iife.js` (copied automatically)
- `../00-landing/widget.iife.js` (copied automatically)

## How to Use

Add two scripts to your HTML page (similar to Google Analytics):

```html
<!-- 1. Load widget script -->
<script src="https://your-cdn.com/widget.iife.js"></script>

<!-- 2. Initialize with config -->
<script>
  ChatWidget.init({
    apiUrl: 'https://api.example.com',
    wsUrl: 'wss://api.example.com/ws',
    channelId: 'your-channel-id',
    theme: 'light'
  });
</script>
```

## Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `apiUrl` | string | Backend API endpoint |
| `wsUrl` | string | WebSocket endpoint |
| `channelId` | string | Channel identifier |
| `theme` | 'light' \| 'dark' | Widget theme |

## Good to Know

### Shadow DOM Isolation
- Widget uses Shadow DOM for complete style isolation
- Host page styles won't affect the widget
- Widget styles won't leak to host page

### Zero Dependencies
- No external dependencies required on host page
- All React code bundled into single JS file
- CSS inlined into JavaScript

### Global API
- Exposes `window.ChatWidget` object
- Call `ChatWidget.init(config)` to initialize
- Can be called multiple times with different configs

### Development
```bash
npm run dev      # Start dev server
npm run build    # Production build
npm run preview  # Preview production build
```

### File Structure
```
src/
├── main.tsx              # Entry point, exposes ChatWidget API
├── Widget.tsx            # Main widget component
├── components/
│   ├── FloatingButton.tsx
│   └── ChatWindow.tsx
├── types/config.ts       # TypeScript types
└── styles/index.css      # Tailwind styles
```

### Build Output
- Format: IIFE (Immediately Invoked Function Expression)
- Single file: `widget.iife.js`
- All imports inlined (React, CSS, etc.)
- Ready for CDN deployment
