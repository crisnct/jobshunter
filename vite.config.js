import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

const staticRoot = path.resolve('application/src/main/resources/static');

export default defineConfig({
  root: staticRoot,
  plugins: [react()],
  build: {
    outDir: path.resolve('application/target/classes/static'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
  },
});
