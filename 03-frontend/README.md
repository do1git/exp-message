# Frontend Workspace

Monorepo for exp-message frontend projects.

## Structure

- **01-widget**: Embeddable chat widget (React + Vite)
  - Development environment with hot reload
  - Builds to single `widget.iife.js` file
- **02-widget-example**: Pure HTML integration example
  - No dependencies, just HTML
  - Demonstrates real-world usage

## Quick Start

```bash
# Install dependencies
npm install

# Develop widget (with hot reload)
npm run dev

# Build widget
npm run build
```

## Development Workflow

### 1. Develop Widget

```bash
npm run dev
```

Opens `http://localhost:5173` with hot reload for development.

### 2. Build Widget

```bash
npm run build
```

Generates `01-widget/dist/widget.iife.js` - single JS file containing the entire widget.

### 3. Test Integration

Open `02-widget-example/index.html` in browser to test the built widget in a real environment.

## Widget Integration (GA-style)

Add two scripts to your HTML:

```html
<!-- 1. Load widget -->
<script src="https://your-cdn.com/widget.iife.js"></script>

<!-- 2. Initialize -->
<script>
  ChatWidget.init({
    apiUrl: "https://api.example.com",
    wsUrl: "wss://api.example.com/ws",
    channelId: "your-channel-id",
    theme: "light",
  });
</script>
```

## Configuration Options

- `apiUrl`: Backend API endpoint
- `wsUrl`: WebSocket endpoint
- `channelId`: Channel identifier
- `theme`: 'light' or 'dark'
