// import { formatDistanceToNow } from 'date-fns'
// import { vi } from 'date-fns/locale'

// const getPlatformInfo = (customerId) => {
//   if (!customerId) return { platform: 'unknown', color: 'bg-neutral-200' }

//   if (customerId.startsWith('FACEBOOK:')) {
//     return { platform: 'Facebook', color: 'bg-blue-100', textColor: 'text-blue-700' }
//   }
//   if (customerId.startsWith('ZALO:')) {
//     return { platform: 'Zalo', color: 'bg-cyan-100', textColor: 'text-cyan-700' }
//   }
//   if (customerId.startsWith('EMAIL:')) {
//     return { platform: 'Email', color: 'bg-purple-100', textColor: 'text-purple-700' }
//   }
//   return { platform: 'Chat', color: 'bg-neutral-200', textColor: 'text-neutral-700' }
// }

// export const MessageBubble = ({
//   message,
//   isAgent = false,
//   agentName = 'Agent',
//   showTimestamp = true,
//   showAvatar = true
// }) => {
//   const { platform, color, textColor } = getPlatformInfo(message.senderId)
//   const isSystem = message.messageType === 'SYSTEM'

//   if (isSystem) {
//     return (
//       <div className="flex justify-center py-3">
//         <div className="bg-neutral-100 text-neutral-600 text-xs px-3 py-1 rounded-full italic">
//           {message.contentText}
//         </div>
//       </div>
//     )
//   }

//   return (
//     <div className={`flex gap-2 mb-3 ${isAgent ? 'justify-end' : 'justify-start'}`}>
//       {!isAgent && showAvatar && (
//         <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-200 flex items-center justify-center text-sm font-medium text-primary-700">
//           {message.senderName?.charAt(0).toUpperCase() || 'C'}
//         </div>
//       )}

//       <div className={`flex flex-col ${isAgent ? 'items-end' : 'items-start'}`}>
//         {!isAgent && (
//           <div className="flex items-center gap-1 mb-1">
//             <span className="text-xs font-medium text-neutral-700">
//               {message.senderName || 'Customer'}
//             </span>
//             <span className={`text-xs px-1.5 py-0.5 rounded ${color} ${textColor}`}>
//               {platform}
//             </span>
//           </div>
//         )}

//         <div
//           className={`message-bubble max-w-xs break-words px-3 py-2 rounded-lg ${
//             isAgent
//               ? 'bg-white border border-neutral-200 text-neutral-900'
//               : 'bg-blue-100 text-neutral-900'
//           }`}
//         >
//           {message.contentText && (
//             <p className="text-sm">{message.contentText}</p>
//           )}

//           {message.attachments && message.attachments.length > 0 && (
//             <div className="mt-2 space-y-1">
//               {message.attachments.map((attachment, idx) => (
//                 <a
//                   key={idx}
//                   href={attachment.url}
//                   target="_blank"
//                   rel="noopener noreferrer"
//                   className="text-xs text-primary-600 hover:underline block"
//                 >
//                   📎 {attachment.filename || 'Attachment'}
//                 </a>
//               ))}
//             </div>
//           )}
//         </div>

//         {showTimestamp && message.createdAt && !isNaN(new Date(message.createdAt).getTime()) && (
//           <span className="text-xs text-neutral-500 mt-1">
//             {formatDistanceToNow(new Date(message.createdAt), {
//               addSuffix: true,
//               locale: vi
//             })}
//           </span>
//         )}
//       </div>

//       {isAgent && showAvatar && (
//         <div className="flex-shrink-0 w-8 h-8 rounded-full bg-neutral-200 flex items-center justify-center text-sm font-medium text-neutral-700">
//           {agentName?.charAt(0).toUpperCase() || 'A'}
//         </div>
//       )}
//     </div>
//   )
// }
import { formatDistanceToNow } from 'date-fns'
import { vi } from 'date-fns/locale'

const getPlatformInfo = (customerId) => {
  if (!customerId) return { platform: 'unknown', color: 'bg-neutral-200' }

  if (customerId.startsWith('FACEBOOK:')) {
    return { platform: 'Facebook', color: 'bg-blue-100', textColor: 'text-blue-700' }
  }
  if (customerId.startsWith('ZALO:')) {
    return { platform: 'Zalo', color: 'bg-cyan-100', textColor: 'text-cyan-700' }
  }
  if (customerId.startsWith('EMAIL:')) {
    return { platform: 'Email', color: 'bg-purple-100', textColor: 'text-purple-700' }
  }
  return { platform: 'Chat', color: 'bg-neutral-200', textColor: 'text-neutral-700' }
}

export const MessageBubble = ({
  message,
  isAgent = false,
  agentName = 'Agent',
  showTimestamp = true,
  showAvatar = true
}) => {
  const { platform, color, textColor } = getPlatformInfo(message.senderId)
  const isSystem = message.messageType === 'SYSTEM'

  if (isSystem) {
    return (
      <div className="flex justify-center py-3">
        <div className="bg-neutral-100 text-neutral-600 text-xs px-3 py-1 rounded-full italic">
          {message.contentText}
        </div>
      </div>
    )
  }

  return (
    <div className={`flex gap-2 mb-3 ${isAgent ? 'justify-end' : 'justify-start'}`}>
      {!isAgent && showAvatar && (
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-primary-200 flex items-center justify-center text-sm font-medium text-primary-700">
          {message.senderName?.charAt(0).toUpperCase() || 'C'}
        </div>
      )}

      <div className={`flex flex-col ${isAgent ? 'items-end' : 'items-start'}`}>
        {!isAgent && (
          <div className="flex items-center gap-1 mb-1">
            <span className="text-xs font-medium text-neutral-700">
              {message.senderName || 'Customer'}
            </span>
            <span className={`text-xs px-1.5 py-0.5 rounded ${color} ${textColor}`}>
              {platform}
            </span>
          </div>
        )}

        <div
          className={`message-bubble max-w-xs break-words px-3 py-2 rounded-lg ${
            isAgent
              ? 'bg-white border border-neutral-200 text-neutral-900'
              : 'bg-primary-100 text-neutral-900'
          }`}
        >
          {message.contentText && (
            <p className="text-sm">{message.contentText}</p>
          )}

          {message.attachments && message.attachments.length > 0 && (
            <div className="mt-2 space-y-1">
              {message.attachments.map((attachment, idx) => (
                <a
                  key={idx}
                  href={attachment.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs text-primary-600 hover:underline block"
                >
                  📎 {attachment.filename || 'Attachment'}
                </a>
              ))}
            </div>
          )}
        </div>

        {showTimestamp && message.createdAt && !isNaN(new Date(message.createdAt).getTime()) && (
          <span className="text-xs text-neutral-500 mt-1">
            {formatDistanceToNow(new Date(message.createdAt), {
              addSuffix: true,
              locale: vi
            })}
          </span>
        )}
      </div>

      {isAgent && showAvatar && (
        <div className="flex-shrink-0 w-8 h-8 rounded-full bg-neutral-200 flex items-center justify-center text-sm font-medium text-neutral-700">
          {agentName?.charAt(0).toUpperCase() || 'A'}
        </div>
      )}
    </div>
  )
}
