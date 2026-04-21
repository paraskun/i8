import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [svelte(), tailwindcss()],
  build: {
    lib: {
      name: 'Printer',
      entry: 'src/index.ts',
      formats: ['es'],
      fileName: 'plugin',
      cssFileName: 'plugin',
    },
    rollupOptions: {
      external: [
        "svelte",
        'svelte/internal',
        'svelte/internal/client',
        'bits-ui'
      ]
    }
  }
})
