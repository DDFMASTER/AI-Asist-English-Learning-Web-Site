import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  base: './',

  plugins: [
    vue(),
    vueDevTools(),
  ],

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },

  server: {
    proxy: {
      '/api': {
        // 本地 Tomcat
        target: 'http://localhost:8080/AAEL_war_exploded/',
        // 服务器 Tomcat，部署时如果需要远程调试再切换
        // target: 'http://http://8.146.204.179/:1145/AAEL',

        changeOrigin: true
      }
    }
  },

  build: {
    outDir: '../src/main/webapp/app',
    emptyOutDir: true
  }
})
