# Widget Integration Example

Pure HTML example demonstrating how to integrate the chat widget into any website.

## Overview

This is a **zero-dependency** example showing real-world widget integration.
No build tools, no npm - just HTML and JavaScript.

## How to Use

1. **Build the widget first:**
   ```bash
   cd ../01-widget
   npm run build
   ```
   This now also copies `dist/widget.iife.js` into `02-widget-example/widget.iife.js`.

2. **Open in browser:**
   - Simply open `index.html` in your browser
   - Or use any static file server

## Integration Code

The example shows the two-script integration pattern (similar to Google Analytics):

```html
<!-- 1. Load widget script -->
<script src="./widget.iife.js"></script>

<!-- 2. Initialize with config -->
<script>
  ChatWidget.init({
    apiUrl: 'http://localhost:8080/api',
    wsUrl: 'ws://localhost:8080/ws',
    channelId: 'demo-channel',
    theme: 'light'
  });
</script>
```

## For Production

Replace the script source with your CDN URL:

```html
<script src="https://your-cdn.com/widget.iife.js"></script>
```

## Features Demonstrated

- Shadow DOM style isolation
- Zero dependencies on host page
- Simple two-script integration
- Runtime configuration
- Responsive design
