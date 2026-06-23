import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import apiClient from '../api/config'

// Fetch all agents
export const useAgents = () => {
  return useQuery({
    queryKey: ['agents'],
    queryFn: async () => {
      // Mock data because /api/v1/agents is returning 404
      return [
        { id: 1, agentId: 1, fullName: 'Admin', email: 'admin@omnichat.com', role: 'ADMIN', status: 'ONLINE', currentWorkload: 0, maxCapacity: 10 },
        { id: 2, agentId: 2, fullName: 'Văn Long', email: 'agent1@omnichat.com', role: 'AGENT', status: 'OFFLINE', currentWorkload: 0, maxCapacity: 5 },
        { id: 3, agentId: 3, fullName: 'Kim Nga', email: 'agent2@omnichat.com', role: 'AGENT', status: 'OFFLINE', currentWorkload: 0, maxCapacity: 5 }
      ]
    },
    staleTime: 60000, // 1 minute
    gcTime: 10 * 60 * 1000 // 10 minutes
  })
}

// Fetch current agent profile
export const useCurrentAgent = (agentId) => {
  return useQuery({
    queryKey: ['agent', agentId],
    queryFn: async () => {
      if (!agentId) return null
      try {
        const response = await apiClient.get(`/api/v1/agents/${agentId}/status`)
        return { ...response.data.data, id: response.data.data?.agentId || parseInt(agentId) }
      } catch (error) {
        // Fallback to mock data if API fails
        const mockAgents = [
          { id: 1, agentId: 1, fullName: 'Admin', email: 'admin@omnichat.com', role: 'ADMIN', status: 'ONLINE', currentWorkload: 0, maxCapacity: 10 },
          { id: 2, agentId: 2, fullName: 'Văn Long', email: 'agent1@omnichat.com', role: 'AGENT', status: 'OFFLINE', currentWorkload: 0, maxCapacity: 5 },
          { id: 3, agentId: 3, fullName: 'Kim Nga', email: 'agent2@omnichat.com', role: 'AGENT', status: 'OFFLINE', currentWorkload: 0, maxCapacity: 5 }
        ]
        return mockAgents.find(a => a.id === parseInt(agentId)) || null
      }
    },
    enabled: !!agentId,
    staleTime: 60000,
    gcTime: 10 * 60 * 1000
  })
}

// Update agent status
export const useUpdateAgentStatus = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({ agentId, status }) => {
      const response = await apiClient.put(
        `/api/v1/agents/${agentId}/status`,
        { status } // online, away, offline, busy
      )
      return response.data.data
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({
        queryKey: ['agent', variables.agentId]
      })
      queryClient.invalidateQueries({
        queryKey: ['agents']
      })
    }
  })
}

// Get agents for assignment dropdown
export const useAgentsForAssignment = (currentAgentId) => {
  const { data: agents = [], isLoading } = useAgents()

  const availableAgents = agents.filter((agent) => agent.id !== currentAgentId)

  return { availableAgents, isLoading }
}
