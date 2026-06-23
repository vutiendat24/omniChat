import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

console.log('[v0] main.jsx loading...')

try {
  ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
  console.log('[v0] App rendered successfully')
} catch (error) {
  console.error('[v0] Error rendering app:', error)
}
