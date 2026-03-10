import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue2'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      'lodash/isPlainObject': 'lodash/isPlainObject.js',
      'moment': 'moment/dist/moment.js',
      'util': 'rollup-plugin-node-polyfills/polyfills/util', // or just mock it if we don't want to install the plugin
      // Let's try a simpler alias first: map to an empty object if possible, or use a browser-friendly util.
      // Better: let's not rely on a plugin we haven't installed.
    }
  },
  build: {
    commonjsOptions: {
      transformMixedEsModules: true
    }
  },
  define: {
    'process.env': {}
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
