/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef2ff',
          100: '#e0e7ff',
          500: '#1a365d', // Primary Navy Blue
          600: '#0d233a',
          700: '#071626',
          gold: '#d4af37', // Accent Gold
          emerald: '#10b981'
        }
      }
    },
  },
  plugins: [],
}
