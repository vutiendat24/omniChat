import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import apiClient from '../api/config'

// Fetch conversations list
export const useConversations = (options = {}) => {
  const { statusFilter = 'all', searchQuery = '', sort = '-last_activity_at' } = options

  return useQuery({
    queryKey: ['conversations', { statusFilter, searchQuery, sort }],
    queryFn: async () => {
      const params = new URLSearchParams()
      if (sort) params.append('sort', sort)
      if (searchQuery) params.append('search', searchQuery)
      if (statusFilter !== 'all') params.append('status', statusFilter)

      const response = await apiClient.get(`/api/v1/conversations?${params.toString()}`)
      return response.data.data || []
    },
    staleTime: 30000, // 30 seconds
    gcTime: 5 * 60 * 1000, // 5 minutes (formerly cacheTime)
    refetchInterval: 60000 // Refetch every minute
  })
}

// Fetch single conversation detail
export const useConversationDetail = (conversationId, enabled = true) => {
  return useQuery({
    queryKey: ['conversation', conversationId],
    queryFn: async () => {
      if (!conversationId) return null
      const response = await apiClient.get(`/api/v1/conversations/${conversationId}`)
      return response.data.data
    },
    enabled: !!conversationId && enabled,
    staleTime: 10000,
    gcTime: 5 * 60 * 1000
  })
}

// Fetch messages for a conversation
export const useMessages = (conversationId, enabled = true) => {
  return useQuery({
    queryKey: ['messages', conversationId],
    queryFn: async () => {
      if (!conversationId) return []
      const response = await apiClient.get(`/api/v1/conversations/${conversationId}/messages`)
      return response.data.data || []
    },
    enabled: !!conversationId && enabled,
    staleTime: 5000,
    gcTime: 5 * 60 * 1000,
    refetchInterval: 10000 // Auto-refetch messages every 10 seconds
  })
}

// Send message mutation
export const useSendMessage = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ conversationId, contentText, attachments = [] }) => {
      const response = await apiClient.post(
        `/api/v1/conversations/${conversationId}/messages`,
        {
          contentText,
          attachments,
          messageType: 'text'
        }
      )
      return response.data.data
    },
    onSuccess: (data, variables) => {
      // Invalidate messages for this conversation
      queryClient.invalidateQueries({
        queryKey: ['messages', variables.conversationId]
      })
      // Invalidate conversation detail
      queryClient.invalidateQueries({
        queryKey: ['conversation', variables.conversationId]
      })
      // Invalidate conversations list
      queryClient.invalidateQueries({
        queryKey: ['conversations']
      })
    },
    onError: (error) => {
      console.error('[v0] Error sending message:', error.response?.data || error.message)
    }
  })
}

// Assign conversation mutation
export const useAssignConversation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ conversationId, targetAgentId }) => {
      const response = await apiClient.put(
        `/api/v1/conversations/${conversationId}/assign`,
        { targetAgentId }
      )
      return response.data.data
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['conversation', variables.conversationId]
      })
      queryClient.invalidateQueries({
        queryKey: ['conversations']
      })
    }
  })
}

// Close conversation mutation
export const useCloseConversation = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ conversationId, reason = '' }) => {
      const response = await apiClient.put(
        `/api/v1/conversations/${conversationId}/close`,
        { reason }
      )
      return response.data.data
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['conversation', variables.conversationId]
      })
      queryClient.invalidateQueries({
        queryKey: ['conversations']
      })
    }
  })
}
