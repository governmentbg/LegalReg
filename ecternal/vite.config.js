import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const basePath = command === 'serve' ? '/' : (env.VITE_BASE_PATH || '/urireg-public/')

  return {
    base: basePath,
    plugins: [vue(), vueDevTools()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      },
    },
    build: {
      chunkSizeWarningLimit: 1000,
      rollupOptions: {
        output: {
          manualChunks: {
            'pdfmake': ['pdfmake/build/pdfmake', 'pdfmake/build/vfs_fonts']
          }
        }
      }
    },
    server: {
      historyApiFallback: true,
      proxy: {
        '/urireg': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/urireg/, '/urireg')
        }
      }
    }
  }
})
