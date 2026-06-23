// import { useState } from 'react'
// import { useConversations } from '../hooks/useConversations'
// import { ConversationList } from '../components/inbox/ConversationList'

// export const InboxPage = () => {
//   const [selectedConvId, setSelectedConvId] = useState(null)
//   const [searchQuery, setSearchQuery] = useState('')
//   const [statusFilter, setStatusFilter] = useState('all')

//   // Fetch conversations with error handling
//   const { data: conversations = [], isLoading, error } = useConversations({
//     statusFilter,
//     searchQuery
//   })

//   return (
//     <div className="h-full flex flex-col md:flex-row bg-neutral-50">
//       {/* Conversation List */}
//       <div className={`
//         w-full md:w-80 flex-shrink-0 transition-all duration-300
//         ${selectedConvId ? 'hidden md:flex' : 'flex'}
//       `}>
//         {error ? (
//           <div className="w-full h-full flex items-center justify-center p-4 bg-white">
//             <div className="text-center">
//               <div className="text-4xl mb-2">⚠️</div>
//               <p className="text-neutral-600 text-sm">Failed to load conversations</p>
//               <p className="text-neutral-400 text-xs mt-2">{error.message}</p>
//               <p className="text-neutral-500 text-xs mt-4">Make sure API gateway is running at localhost:8080</p>
//             </div>
//           </div>
//         ) : (
//           <ConversationList
//             conversations={conversations}
//             selectedId={selectedConvId}
//             onSelectConversation={setSelectedConvId}
//             isLoading={isLoading}
//             searchQuery={searchQuery}
//             onSearchChange={setSearchQuery}
//             onStatusFilterChange={setStatusFilter}
//             statusFilter={statusFilter}
//           />
//         )}
//       </div>

//       {/* Empty State or Chat Panel */}
//       <div className="flex-1 flex items-center justify-center p-4">
//         {selectedConvId ? (
//           <div className="text-center text-neutral-500">
//             <p className="text-sm">Chat functionality coming soon</p>
//           </div>
//         ) : (
//           <div className="text-center">
//             <div className="text-6xl mb-4">💬</div>
//             <h1 className="text-2xl font-bold text-neutral-900 mb-2">Inbox</h1>
//             <p className="text-neutral-600 mb-2">Multi-Channel Conversation Management</p>
//             <p className="text-sm text-neutral-500 max-w-sm">
//               Select a conversation from the list to view details. Conversations are synced from Facebook, Zalo, and other channels.
//             </p>
//           </div>
//         )}
//       </div>
//     </div>
//   )
// }
import { useState, useEffect, lazy, Suspense } from 'react'
import { useConversations, useMessages, useSendMessage } from '../hooks/useConversations'
import { useAgents } from '../hooks/useAgents'
import { useAppStore } from '../store/useAppStore'
import { ConversationList } from '../components/inbox/ConversationList'

const ChatHeader = lazy(() => import('../components/inbox/ChatHeader').then(m => ({ default: m.ChatHeader })))
const MessageList = lazy(() => import('../components/inbox/MessageList').then(m => ({ default: m.MessageList })))
const MessageComposer = lazy(() => import('../components/inbox/MessageComposer').then(m => ({ default: m.MessageComposer })))

export const InboxPage = () => {
  const [selectedConvId, setSelectedConvId] = useState(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')

  const { currentAgentId, setIsLoadingConversations } = useAppStore()

  // Fetch conversations list
  const { data: conversations = [], isLoading: isLoadingConvs, error: convError } = useConversations({
    statusFilter,
    searchQuery
  })

  // Fetch selected conversation detail from list
  const selectedConversation = conversations?.find(c => c.id === selectedConvId) || null;
  const isLoadingDetail = false;
  const detailError = null;

  // Fetch messages (only when selectedConvId is set)
  const { data: messages = [], isLoading: isLoadingMessages, error: messagesError } = useMessages(selectedConvId)

  // Fetch agents for assignment
  const { data: agents = [] } = useAgents()

  // Send message mutation
  const { mutate: sendMessage, isPending: isSendingMessage, error: sendError } = useSendMessage()

  useEffect(() => {
    setIsLoadingConversations(isLoadingConvs)
  }, [isLoadingConvs, setIsLoadingConversations])

  // Auto-select first conversation if none selected
  useEffect(() => {
    if (!selectedConvId && conversations.length > 0) {
      setSelectedConvId(conversations[0].id)
    }
  }, [conversations, selectedConvId])

  const handleSendMessage = (messageData) => {
    if (!selectedConvId) return
    sendMessage({
      conversationId: selectedConvId,
      ...messageData
    })
  }

  const handleAssignConversation = (agentId) => {
    console.log('[v0] Assign to agent:', agentId)
  }

  const handleStatusChange = (newStatus) => {
    console.log('[v0] Change status to:', newStatus)
  }

  const error = convError || detailError || messagesError || sendError

  return (
    <div className="h-full flex flex-col md:flex-row bg-neutral-50">
      {/* Chat Panel (Now on the left/middle) */}
      {selectedConvId ? (
        <Suspense fallback={<div className="flex-1 flex items-center justify-center text-neutral-400">Loading...</div>}>
          <div className="flex-1 flex flex-col min-w-0 bg-white">
            <ChatHeader
              conversation={selectedConversation}
              isLoading={isLoadingDetail}
              onClose={() => setSelectedConvId(null)}
              onAssign={handleAssignConversation}
              onStatusChange={handleStatusChange}
              agents={agents}
              agentId={currentAgentId}
            />

            <MessageList
              messages={messages}
              isLoading={isLoadingMessages}
              agentId={currentAgentId}
              agentName="Bạn"
            />

            <MessageComposer
              onSendMessage={handleSendMessage}
              isLoading={isSendingMessage}
              disabled={!selectedConversation || selectedConversation?.status === 'CLOSED'}
            />
          </div>
        </Suspense>
      ) : (
        <div className="hidden md:flex flex-1 items-center justify-center text-center">
          <div>
            <div className="text-6xl mb-4">💬</div>
            <h1 className="text-2xl font-bold text-neutral-900 mb-2">Inbox</h1>
            <p className="text-neutral-600 mb-2">Multi-Channel Conversation Management</p>
            <p className="text-sm text-neutral-500 max-w-sm">
              Select a conversation to view details and messages
            </p>
          </div>
        </div>
      )}

      {/* Conversations List (Moved to the right) */}
      <div className={`
        w-full md:w-80 flex flex-col transition-all duration-300 overflow-hidden flex-shrink-0 border-l border-neutral-200
        ${selectedConvId ? 'hidden md:flex' : 'flex'}
      `}>
        {convError ? (
          <div className="w-full h-full flex items-center justify-center p-4 bg-white">
            <div className="text-center">
              <div className="text-4xl mb-2">⚠️</div>
              <p className="text-neutral-600 text-sm">Failed to load conversations</p>
              <p className="text-neutral-400 text-xs mt-2">{convError.message}</p>
              <p className="text-neutral-500 text-xs mt-4">Make sure API gateway is running at localhost:8080</p>
            </div>
          </div>
        ) : (
          <ConversationList
            conversations={conversations}
            selectedId={selectedConvId}
            onSelectConversation={setSelectedConvId}
            isLoading={isLoadingConvs}
            searchQuery={searchQuery}
            onSearchChange={setSearchQuery}
            onStatusFilterChange={setStatusFilter}
            statusFilter={statusFilter}
          />
        )}
      </div>

      {/* Error Notification */}
      {error && (
        <div className="fixed bottom-4 right-4 bg-red-600 text-white px-4 py-3 rounded-lg shadow-lg max-w-sm text-sm z-50">
          {error.message}
        </div>
      )}
    </div>
  )
}
