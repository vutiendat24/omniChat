import { useState } from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import {
  MenuIcon,
  XIcon,
  MessageCircleIcon,
  PhoneIcon,
  UsersIcon,
  SettingsIcon
} from 'lucide-react'

const navItems = [
  { path: '/inbox', icon: MessageCircleIcon, label: 'Inbox' },
  { path: '/calls', icon: PhoneIcon, label: 'Calls' },
  { path: '/customers', icon: UsersIcon, label: 'Customers' },
  { path: '/agents', icon: UsersIcon, label: 'Agents' },
  { path: '/settings', icon: SettingsIcon, label: 'Settings' },
]

export const AppShell = () => {
  const [sidebarOpen, setSidebarOpen] = useState(true)
  const location = useLocation()

  return (
    <div className="h-screen flex flex-col bg-neutral-50">
      {/* Header */}
      <header className="h-16 bg-white border-b border-neutral-200 flex items-center justify-between px-4 flex-shrink-0">
        <div className="flex items-center gap-3">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-2 text-neutral-600 hover:bg-neutral-100 rounded-lg lg:hidden"
          >
            {sidebarOpen ? <XIcon className="h-5 w-5" /> : <MenuIcon className="h-5 w-5" />}
          </button>
          <Link to="/inbox" className="flex items-center gap-2 font-bold text-xl text-primary-600 hover:text-primary-700">
            <MessageCircleIcon className="h-6 w-6" />
            <span>OmniChat</span>
          </Link>
        </div>
      </header>

      {/* Main Layout */}
      <div className="flex-1 flex overflow-hidden">
        {/* Sidebar */}
        <aside
          className={`
            w-64 bg-white border-r border-neutral-200 flex flex-col transition-all duration-300 z-40
            ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
            lg:translate-x-0 lg:relative
            fixed lg:static inset-y-16 left-0 lg:inset-auto
          `}
        >
          <nav className="flex-1 overflow-y-auto p-4 space-y-2">
            {navItems.map((item) => {
              const Icon = item.icon
              const isActive = location.pathname.startsWith(item.path)
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={() => setSidebarOpen(false)}
                  className={`
                    flex items-center gap-3 px-4 py-3 rounded-lg font-medium text-sm transition-colors
                    ${isActive
                      ? 'bg-blue-100 text-primary-900'
                      : 'text-neutral-700 hover:bg-neutral-100'
                    }
                  `}
                >
                  <Icon className="h-5 w-5" />
                  <span>{item.label}</span>
                </Link>
              )
            })}
          </nav>
        </aside>

        {/* Main Content */}
        <main className="flex-1 overflow-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
