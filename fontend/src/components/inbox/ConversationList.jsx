import { formatDistanceToNow } from 'date-fns'
import { vi } from 'date-fns/locale'
import { SearchIcon } from 'lucide-react'
import { Input } from '../Input'

export const ConversationList = ({
  conversations = [],
  selectedId = null,
  onSelectConversation,
  isLoading = false,
  searchQuery = '',
  onSearchChange,
  onStatusFilterChange,
  statusFilter = 'all'
}) => {
  const getPlatformBadge = (channelIdentityId) => {
    if (channelIdentityId?.startsWith('FACEBOOK:')) {
      return { bg: 'bg-blue-100', text: 'text-blue-700', label: 'FB' }
    }
    if (channelIdentityId?.startsWith('ZALO:')) {
      return { bg: 'bg-cyan-100', text: 'text-cyan-700', label: 'Z' }
    }
    if (channelIdentityId?.startsWith('EMAIL:')) {
      return { bg: 'bg-purple-100', text: 'text-purple-700', label: 'E' }
    }
    return { bg: 'bg-neutral-100', text: 'text-neutral-700', label: 'C' }
  }

  const getUnreadCount = (conv) => {
    return conv.unreadCount || 0
  }

  if (isLoading && conversations.length === 0) {
    return (
      <div className="h-full flex flex-col bg-white border-r border-neutral-200">
        <div className="p-4 space-y-3">
          {[...Array(8)].map((_, i) => (
            <div key={i} className="skeleton h-16 rounded-lg" />
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="h-full flex flex-col bg-white border-r border-neutral-200">
      {/* Search & Filters */}
      <div className="p-4 space-y-3 border-b border-neutral-200 flex-shrink-0">
        <div className="relative">
          <SearchIcon className="absolute left-3 top-2.5 h-4 w-4 text-neutral-400" />
          <Input
            type="text"
            placeholder="Tìm kiếm..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            className="pl-10"
          />
        </div>

        <div className="flex gap-2">
          <button
            onClick={() => onStatusFilterChange('all')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              statusFilter === 'all'
                ? 'bg-primary-600 text-blue'
                : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
            }`}
          >
            Tất cả
          </button>
          <button
            onClick={() => onStatusFilterChange('unread')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              statusFilter === 'unread'
                ? 'bg-primary-600 text-blue'
                : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
            }`}
          >
            Chưa đọc
          </button>
          <button
            onClick={() => onStatusFilterChange('archived')}
            className={`px-3 py-1.5 text-xs font-medium rounded-lg transition-colors ${
              statusFilter === 'archived'
                ? 'bg-primary-600 text-blue'
                : 'bg-neutral-100 text-neutral-700 hover:bg-neutral-200'
            }`}
          >
            Lưu trữ
          </button>
        </div>
      </div>

      {/* Conversations List */}
      <div className="flex-1 overflow-y-auto">
        {conversations.length === 0 ? (
          <div className="p-4 text-center text-neutral-500">
            <p className="text-sm">Chưa có cuộc hội thoại nào</p>
          </div>
        ) : (
          <div className="space-y-1 p-2">
            {conversations.map((conversation) => {
              const platformBadge = getPlatformBadge(conversation.channelIdentityId)
              const unreadCount = getUnreadCount(conversation)
              const isSelected = selectedId === conversation.id
              
              // Extract a readable name from channelIdentityId if customerName is not provided
              const displayName = conversation.customerName || 
                (conversation.channelIdentityId ? conversation.channelIdentityId.split(':').pop() : 'Khách hàng');

              return (
                <button
                  key={conversation.id}
                  onClick={() => onSelectConversation(conversation.id)}
                  className={`w-full text-left px-3 py-3 rounded-lg border transition-colors ${
                    isSelected
                      ? 'bg-primary-50 border-primary-300'
                      : 'border-transparent hover:bg-neutral-50'
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div className={`flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center font-medium text-sm ${platformBadge.bg} ${platformBadge.text}`}>
                      {platformBadge.label}
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className={`text-sm font-medium truncate ${
                          isSelected ? 'text-primary-900' : 'text-neutral-900'
                        }`}>
                          {displayName}
                        </h3>
                        {unreadCount > 0 && (
                          <span className="bg-accent-danger text-white text-xs font-bold px-1.5 py-0.5 rounded-full flex-shrink-0">
                            {unreadCount}
                          </span>
                        )}
                      </div>

                      <p className={`text-xs truncate ${
                        isSelected ? 'text-primary-700' : 'text-neutral-600'
                      }`}>
                        {conversation.lastMessage || 'Nhấn để xem chi tiết'}
                      </p>

                      <p className={`text-xs mt-1 ${
                        isSelected ? 'text-primary-600' : 'text-neutral-500'
                      }`}>
                        {formatDistanceToNow(new Date(conversation.lastActivityAt), {
                          addSuffix: true,
                          locale: vi
                        })}
                      </p>
                    </div>
                  </div>
                </button>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
