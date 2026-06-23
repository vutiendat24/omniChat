import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Add interceptor to include X-Agent-Id header
apiClient.interceptors.request.use((config) => {
  const agentId = localStorage.getItem('agentId')
  if (agentId) {
    config.headers['X-Agent-Id'] = agentId
  } else {
     config.headers['X-Agent-Id'] = 11
  }
  return config
}, (error) => {
  return Promise.reject(error)
})

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('agentId')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default apiClient
export { API_BASE_URL }
