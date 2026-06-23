// import { Fragment } from 'react'
// import { Menu, Transition } from '@headlessui/react'
// import {
//   MoreVerticalIcon,
//   XIcon,
//   ClockIcon,
//   CheckIcon,
//   XCircleIcon
// } from 'lucide-react'
// import { Select } from '../Select'
// import { Button } from '../Button'

// export const ChatHeader = ({
//   conversation,
//   onClose,
//   onAssign,
//   onStatusChange,
//   agents = [],
//   isLoadingAssign = false,
//   agentId = null
// }) => {
//   const getChannelBadge = () => {
//     if (conversation?.customerId?.startsWith('FACEBOOK:')) {
//       return { label: 'Facebook', color: 'bg-blue-100 text-blue-700' }
//     }
//     if (conversation?.customerId?.startsWith('ZALO:')) {
//       return { label: 'Zalo', color: 'bg-cyan-100 text-cyan-700' }
//     }
//     if (conversation?.customerId?.startsWith('EMAIL:')) {
//       return { label: 'Email', color: 'bg-purple-100 text-purple-700' }
//     }
//     return { label: 'Chat', color: 'bg-neutral-100 text-neutral-700' }
//   }

//   const getStatusInfo = () => {
//     switch (conversation?.status) {
//       case 'OPEN':
//         return { icon: ClockIcon, label: 'Mở', color: 'text-accent-info' }
//       case 'ASSIGNED':
//         return { icon: CheckIcon, label: 'Được giao', color: 'text-accent-warning' }
//       case 'CLOSED':
//         return { icon: XCircleIcon, label: 'Đóng', color: 'text-accent-danger' }
//       default:
//         return { icon: ClockIcon, label: conversation?.status, color: 'text-neutral-500' }
//     }
//   }

//   const { label: channelLabel, color: channelColor } = getChannelBadge()
//   const { icon: StatusIcon, label: statusLabel, color: statusColor } = getStatusInfo()
//   const availableAgents = agents.filter(a => a.id !== conversation?.assignedAgentId)

//   return (
//     <div className="border-b border-neutral-200 bg-white p-4">
//       <div className="flex items-center justify-between mb-4">
//         <div className="flex-1">
//           <h2 className="text-lg font-semibold text-neutral-900">
//             {conversation?.customerName || 'Khách hàng'}
//           </h2>
//           <p className="text-sm text-neutral-500">
//             {conversation?.customerId}
//           </p>
//         </div>

//         <button
//           onClick={onClose}
//           className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg"
//         >
//           <XIcon className="h-5 w-5" />
//         </button>
//       </div>

//       <div className="flex items-center justify-between gap-2">
//         <div className="flex items-center gap-2">
//           <span className={`text-xs font-medium px-2 py-1 rounded ${channelColor}`}>
//             {channelLabel}
//           </span>

//           <div className={`flex items-center gap-1 text-xs font-medium ${statusColor}`}>
//             <StatusIcon className="h-4 w-4" />
//             {statusLabel}
//           </div>
//         </div>

//         <Menu as="div" className="relative">
//           <Menu.Button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg">
//             <MoreVerticalIcon className="h-5 w-5" />
//           </Menu.Button>

//           <Transition
//             as={Fragment}
//             enter="transition ease-out duration-100"
//             enterFrom="transform opacity-0 scale-95"
//             enterTo="transform opacity-100 scale-100"
//             leave="transition ease-in duration-75"
//             leaveFrom="transform opacity-100 scale-100"
//             leaveTo="transform opacity-0 scale-95"
//           >
//             <Menu.Items className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-neutral-200 z-50">
//               {conversation?.status !== 'CLOSED' && (
//                 <>
//                   {availableAgents.length > 0 && (
//                     <Menu.Item>
//                       {({ active }) => (
//                         <div className={`px-4 py-3 ${active ? 'bg-neutral-50' : ''}`}>
//                           <p className="text-xs font-medium text-neutral-600 mb-2">
//                             Giao cho agent
//                           </p>
//                           <Select
//                             options={availableAgents.map(a => ({
//                               value: a.id,
//                               label: a.name
//                             }))}
//                             value=""
//                             onChange={(agentId) => onAssign(agentId)}
//                             disabled={isLoadingAssign}
//                           />
//                         </div>
//                       )}
//                     </Menu.Item>
//                   )}

//                   <Menu.Item>
//                     {({ active }) => (
//                       <button
//                         onClick={() => onStatusChange('CLOSED')}
//                         className={`w-full text-left px-4 py-2 text-sm text-accent-danger flex items-center gap-2 ${
//                           active ? 'bg-neutral-50' : ''
//                         }`}
//                       >
//                         <XCircleIcon className="h-4 w-4" />
//                         Đóng cuộc hội thoại
//                       </button>
//                     )}
//                   </Menu.Item>
//                 </>
//               )}

//               {conversation?.status === 'CLOSED' && (
//                 <Menu.Item>
//                   {({ active }) => (
//                     <button
//                       onClick={() => onStatusChange('OPEN')}
//                       className={`w-full text-left px-4 py-2 text-sm text-accent-info flex items-center gap-2 ${
//                         active ? 'bg-neutral-50' : ''
//                       }`}
//                     >
//                       <ClockIcon className="h-4 w-4" />
//                       Mở lại cuộc hội thoại
//                     </button>
//                   )}
//                 </Menu.Item>
//               )}
//             </Menu.Items>
//           </Transition>
//         </Menu>
//       </div>
//     </div>
//   )
// }
import { Fragment } from 'react'
import { Menu, Transition } from '@headlessui/react'
import {
  MoreVerticalIcon,
  XIcon,
  ClockIcon,
  CheckIcon,
  XCircleIcon
} from 'lucide-react'
import { Select } from '../Select'
import { Button } from '../Button'

export const ChatHeader = ({
  conversation,
  onClose,
  onAssign,
  onStatusChange,
  agents = [],
  isLoading = false,
  isLoadingAssign = false,
  agentId = null
}) => {
  const getChannelBadge = () => {
    if (conversation?.customerId?.startsWith('FACEBOOK:')) {
      return { label: 'Facebook', color: 'bg-blue-100 text-blue-700' }
    }
    if (conversation?.customerId?.startsWith('ZALO:')) {
      return { label: 'Zalo', color: 'bg-cyan-100 text-cyan-700' }
    }
    if (conversation?.customerId?.startsWith('EMAIL:')) {
      return { label: 'Email', color: 'bg-purple-100 text-purple-700' }
    }
    return { label: 'Chat', color: 'bg-neutral-100 text-neutral-700' }
  }

  const getStatusInfo = () => {
    switch (conversation?.status) {
      case 'OPEN':
        return { icon: ClockIcon, label: 'Mở', color: 'text-accent-info' }
      case 'ASSIGNED':
        return { icon: CheckIcon, label: 'Được giao', color: 'text-accent-warning' }
      case 'CLOSED':
        return { icon: XCircleIcon, label: 'Đóng', color: 'text-accent-danger' }
      default:
        return { icon: ClockIcon, label: conversation?.status, color: 'text-neutral-500' }
    }
  }

  const { label: channelLabel, color: channelColor } = getChannelBadge()
  const { icon: StatusIcon, label: statusLabel, color: statusColor } = getStatusInfo()
  const availableAgents = agents.filter(a => a.id !== conversation?.assignedAgentId)

  if (isLoading) {
    return (
      <div className="border-b border-neutral-200 bg-white p-4 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <div className="skeleton h-6 w-40 mb-2" />
            <div className="skeleton h-4 w-32" />
          </div>
          <div className="skeleton h-10 w-10 rounded-lg" />
        </div>
        <div className="flex items-center gap-2">
          <div className="skeleton h-6 w-16 rounded" />
          <div className="skeleton h-6 w-16 rounded" />
        </div>
      </div>
    )
  }

  return (
    <div className="border-b border-neutral-200 bg-white p-4">
      <div className="flex items-center justify-between mb-4">
        <div className="flex-1">
          <h2 className="text-lg font-semibold text-neutral-900">
            {conversation?.customerName || 'Khách hàng'}
          </h2>
          <p className="text-sm text-neutral-500">
            {conversation?.customerId}
          </p>
        </div>

        <button
          onClick={onClose}
          className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg"
        >
          <XIcon className="h-5 w-5" />
        </button>
      </div>

      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <span className={`text-xs font-medium px-2 py-1 rounded ${channelColor}`}>
            {channelLabel}
          </span>

          <div className={`flex items-center gap-1 text-xs font-medium ${statusColor}`}>
            <StatusIcon className="h-4 w-4" />
            {statusLabel}
          </div>
        </div>

        <Menu as="div" className="relative">
          <Menu.Button className="p-2 text-neutral-500 hover:bg-neutral-100 rounded-lg">
            <MoreVerticalIcon className="h-5 w-5" />
          </Menu.Button>

          <Transition
            as={Fragment}
            enter="transition ease-out duration-100"
            enterFrom="transform opacity-0 scale-95"
            enterTo="transform opacity-100 scale-100"
            leave="transition ease-in duration-75"
            leaveFrom="transform opacity-100 scale-100"
            leaveTo="transform opacity-0 scale-95"
          >
            <Menu.Items className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-neutral-200 z-50">
              {conversation?.status !== 'CLOSED' && (
                <>
                  {availableAgents.length > 0 && (
                    <Menu.Item>
                      {({ active }) => (
                        <div className={`px-4 py-3 ${active ? 'bg-neutral-50' : ''}`}>
                          <p className="text-xs font-medium text-neutral-600 mb-2">
                            Giao cho agent
                          </p>
                          <Select
                            options={availableAgents.map(a => ({
                              value: a.id,
                              label: a.name
                            }))}
                            value=""
                            onChange={(agentId) => onAssign(agentId)}
                            disabled={isLoadingAssign}
                          />
                        </div>
                      )}
                    </Menu.Item>
                  )}

                  <Menu.Item>
                    {({ active }) => (
                      <button
                        onClick={() => onStatusChange('CLOSED')}
                        className={`w-full text-left px-4 py-2 text-sm text-accent-danger flex items-center gap-2 ${
                          active ? 'bg-neutral-50' : ''
                        }`}
                      >
                        <XCircleIcon className="h-4 w-4" />
                        Đóng cuộc hội thoại
                      </button>
                    )}
                  </Menu.Item>
                </>
              )}

              {conversation?.status === 'CLOSED' && (
                <Menu.Item>
                  {({ active }) => (
                    <button
                      onClick={() => onStatusChange('OPEN')}
                      className={`w-full text-left px-4 py-2 text-sm text-accent-info flex items-center gap-2 ${
                        active ? 'bg-neutral-50' : ''
                      }`}
                    >
                      <ClockIcon className="h-4 w-4" />
                      Mở lại cuộc hội thoại
                    </button>
                  )}
                </Menu.Item>
              )}
            </Menu.Items>
          </Transition>
        </Menu>
      </div>
    </div>
  )
}
