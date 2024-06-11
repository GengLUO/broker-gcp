/** @type {import('tailwindcss').Config} */
module.exports = {
  mode:process.env.NODE_ENV ? 'jit': undefined,
  purge:["./src/**/*.html"],
  darkMode: false,
  content: [],
  theme: {
    extend: {},
  },

  plugins: [
    require('@tailwindcss/forms'),
  ],
}

