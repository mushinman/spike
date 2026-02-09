#!/usr/bin/env node

const express = require('express');
const { createProxyMiddleware } = require('http-proxy-middleware');
const path = require('path');

const app = express();

// Configuration
const PORT = process.env.PORT || 3000;
const API_TARGET = process.env.API_TARGET || 'http://localhost:8081';
const STATIC_DIR = path.join(__dirname, 'resources', 'public');

// Proxy middleware for /api requests
app.use('/api', createProxyMiddleware({
  target: API_TARGET,
  changeOrigin: true,
  logLevel: 'info',
  onProxyReq: (proxyReq, req, res) => {
    console.log(`[PROXY] ${req.method} ${req.url} -> ${API_TARGET}${req.url}`);
  },
  onError: (err, req, res) => {
    console.error(`[PROXY ERROR] ${err.message}`);
    res.status(500).json({ error: 'Proxy error', message: err.message });
  }
}));

// Serve static files
app.use(express.static(STATIC_DIR));

// Fallback to index.html for SPA routing
app.get('*', (req, res) => {
  res.sendFile(path.join(STATIC_DIR, 'index.html'));
});

// Start server
app.listen(PORT, () => {
  console.log('╔════════════════════════════════════════════════╗');
  console.log('║  Development Server Running                    ║');
  console.log('╠════════════════════════════════════════════════╣');
  console.log(`║  Local:      http://localhost:${PORT.toString().padEnd(19)}║`);
  console.log(`║  Static:     ${STATIC_DIR.padEnd(31)}║`);
  console.log(`║  API Proxy:  ${API_TARGET.padEnd(31)}║`);
  console.log('╠════════════════════════════════════════════════╣');
  console.log('║  /api/*  → Proxied to backend                  ║');
  console.log('║  /*      → Served from static files            ║');
  console.log('╚════════════════════════════════════════════════╝');
  console.log();
});
