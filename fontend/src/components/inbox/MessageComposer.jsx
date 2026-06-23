import { useState, useRef } from 'react'
import { SendIcon, PaperclipIcon, SmileIcon } from 'lucide-react'
import { Button } from '../Button'

export const MessageComposer = ({
  onSendMessage,
  isLoading = false,
  placeholder = 'Nhập tin nhắn của bạn...',
  disabled = false
}) => {
  const [message, setMessage] = useState('')
  const [attachments, setAttachments] = useState([])
  const fileInputRef = useRef(null)

  const handleSend = () => {
    if (!message.trim()) return

    onSendMessage({
      contentText: message,
      attachments
    })

    setMessage('')
    setAttachments([])
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files || [])
    files.forEach(file => {
      const reader = new FileReader()
      reader.onload = (event) => {
        setAttachments(prev => [...prev, {
          filename: file.name,
          size: file.size,
          url: event.target.result,
          type: file.type
        }])
      }
      reader.readAsDataURL(file)
    })
    e.target.value = '' // Reset input
  }

  const removeAttachment = (index) => {
    setAttachments(prev => prev.filter((_, i) => i !== index))
  }

  return (
    <div className="border-t border-neutral-200 bg-white p-4 space-y-3">
      {attachments.length > 0 && (
        <div className="flex flex-wrap gap-2 p-2 bg-neutral-50 rounded-lg">
          {attachments.map((att, idx) => (
            <div
              key={idx}
              className="flex items-center gap-2 px-2 py-1 bg-white border border-neutral-200 rounded text-xs"
            >
              <span>📎 {att.filename}</span>
              <button
                onClick={() => removeAttachment(idx)}
                className="text-neutral-400 hover:text-neutral-600"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="flex gap-2">
        <div className="flex-1">
          <textarea
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder={placeholder}
            disabled={disabled || isLoading}
            rows="3"
            className="w-full px-3 py-2 border border-neutral-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none disabled:bg-neutral-100"
          />
        </div>

        <div className="flex flex-col gap-2">
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || isLoading}
            className="p-2 text-neutral-600 hover:bg-neutral-100 rounded-lg disabled:text-neutral-400"
            title="Đính kèm tệp"
          >
            <PaperclipIcon className="h-5 w-5" />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            onChange={handleFileSelect}
            className="hidden"
          />

          <Button
            onClick={handleSend}
            disabled={!message.trim() || disabled || isLoading}
            isLoading={isLoading}
            size="sm"
            className="flex-shrink-0"
          >
            <SendIcon className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </div>
  )
}
