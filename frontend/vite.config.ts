import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // 개발 서버의 /api 요청을 Spring Boot(8080)로 전달한다.
      '/api': 'http://localhost:8080',
    },
  },
})
