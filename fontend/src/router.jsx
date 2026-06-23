import { createBrowserRouter } from 'react-router-dom'
import { AppShell } from './layouts/AppShell'
import { InboxPage } from './pages/InboxPage'
import { CallsPage, CustomersPage, AgentsPage, SettingsPage } from './pages/PlaceholderPage'

const NotFound = () => (
  <div className="h-screen flex items-center justify-center text-neutral-500">
    Trang không tìm thấy
  </div>
)

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    errorElement: <NotFound />,
    children: [
      {
        index: true,
        element: <InboxPage />
      },
      {
        path: 'inbox',
        element: <InboxPage />
      },
      {
        path: 'calls',
        element: <CallsPage />
      },
      {
        path: 'customers',
        element: <CustomersPage />
      },
      {
        path: 'agents',
        element: <AgentsPage />
      },
      {
        path: 'settings',
        element: <SettingsPage />
      }
    ]
  }
])
