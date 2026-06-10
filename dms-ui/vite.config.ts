import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => ({
  plugins: [react()],
  base: mode === "production" ? "/dms-ui/" : "/",
  server: {
    port: 5173,
    strictPort: true,  // ← fails with an error instead of auto-picking next port
  }
}));


