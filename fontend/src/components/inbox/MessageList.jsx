import { useEffect, useRef } from 'react'
import { MessageBubble } from './MessageBubble'

export const MessageList = ({
  messages = [],
  isLoading = false,
  agentName = 'Agent',
  agentId = null,
  currentUserId = null
}) => {
  const messagesEndRef = useRef(null)
  const containerRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  if (isLoading) {
    return (
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {[...Array(5)].map((_, i) => (
          <div key={i} className="flex gap-2 mb-3">
            <div className="skeleton w-8 h-8 rounded-full" />
            <div className="flex-1">
              <div className="skeleton h-4 w-2/3 mb-2" />
              <div className="skeleton h-10 w-full" />
            </div>
          </div>
        ))}
      </div>
    )
  }

  if (!messages || messages.length === 0) {
    return (
      <div className="flex-1 overflow-y-auto flex flex-col items-center justify-center p-4 text-center">
        <div className="text-neutral-400 mb-2 text-4xl">💬</div>
        <p className="text-neutral-500 text-sm">Chưa có tin nhắn nào</p>
        <p className="text-neutral-400 text-xs mt-1">Bắt đầu cuộc hội thoại với khách hàng</p>
      </div>
    )
  }

  const sortedMessages = [...messages].reverse()

  return (
    <div
      ref={containerRef}
      className="flex-1 overflow-y-auto p-4 space-y-3"
    >
      {sortedMessages.map((msg, idx) => {
        const isAgent = msg.senderType === 'AGENT' || msg.senderId === agentId
        const prevMsg = idx > 0 ? sortedMessages[idx - 1] : null

        // Show avatar only for first message from same sender or if sender changed
        const showAvatar = !prevMsg || prevMsg.senderId !== msg.senderId

        return (
          <MessageBubble
            key={msg.id || idx}
            message={msg}
            isAgent={isAgent}
            agentName={agentName}
            showAvatar={showAvatar}
          />
        )
      })}
      <div ref={messagesEndRef} />
    </div>
  )
}
