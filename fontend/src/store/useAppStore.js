import { create } from 'zustand'

export const useAppStore = create((set) => ({
  // Agent state
  currentAgentId: localStorage.getItem('agentId') || null,
  setCurrentAgentId: (agentId) => {
    localStorage.setItem('agentId', agentId)
    set({ currentAgentId: agentId })
  },

  // UI state
  sidebarOpen: true,
  setSidebarOpen: (open) => set({ sidebarOpen: open }),

  // Conversation state
  selectedConversationId: null,
  setSelectedConversationId: (id) => set({ selectedConversationId: id }),

  // Filter & Search state
  searchQuery: '',
  setSearchQuery: (query) => set({ searchQuery: query }),

  statusFilter: 'all', // all, active, archived
  setStatusFilter: (status) => set({ statusFilter: status }),

  channelFilter: 'all', // all, facebook, zalo, etc
  setChannelFilter: (channel) => set({ channelFilter: channel }),

  // Agent list for switching
  agents: [],
  setAgents: (agents) => set({ agents }),

  // Active agent status
  agentStatus: 'online', // online, away, offline
  setAgentStatus: (status) => set({ agentStatus: status }),

  // Notifications
  notifications: [],
  addNotification: (notification) =>
    set((state) => ({
      notifications: [...state.notifications, { ...notification, id: Date.now() }]
    })),
  removeNotification: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.id !== id)
    })),

  // WebSocket connection state
  wsConnected: false,
  setWsConnected: (connected) => set({ wsConnected: connected }),

  // Loading states
  isLoadingConversations: false,
  setIsLoadingConversations: (loading) => set({ isLoadingConversations: loading }),

  // Error state
  error: null,
  setError: (error) => set({ error }),
  clearError: () => set({ error: null })
}))
