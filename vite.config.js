import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  root: 'application/src/main/resources/static', // Tells Vite to look here for index.html
  build: {
    outDir: '../../target/classes/static', // Adjust based on your build needs
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // If your App.jsx is NOT in resources/static, you'll need to adjust paths
  }
});