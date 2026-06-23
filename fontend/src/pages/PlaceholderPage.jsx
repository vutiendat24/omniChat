import { Card, CardBody } from '../components/Card'

export const PlaceholderPage = ({ title, description, icon: Icon }) => {
  return (
    <div className="h-full flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardBody className="text-center py-12">
          {Icon && (
            <div className="text-5xl mb-4 flex justify-center">
              {Icon}
            </div>
          )}
          <h1 className="text-2xl font-bold text-neutral-900 mb-2">
            {title}
          </h1>
          <p className="text-neutral-600 mb-6">
            {description}
          </p>
          <p className="text-sm text-neutral-500">
            Tính năng này sẽ sớm có sẵn
          </p>
        </CardBody>
      </Card>
    </div>
  )
}

export const CallsPage = () => (
  <PlaceholderPage
    title="Quản lý Cuộc gọi"
    description="Theo dõi và quản lý tất cả các cuộc gọi đến từ khách hàng"
    icon="📞"
  />
)

export const CustomersPage = () => (
  <PlaceholderPage
    title="Danh sách Khách hàng"
    description="Xem và quản lý thông tin chi tiết của tất cả khách hàng"
    icon="👥"
  />
)

export const AgentsPage = () => (
  <PlaceholderPage
    title="Quản lý Agents"
    description="Quản lý nhân viên hỗ trợ khách hàng và phân công công việc"
    icon="👨‍💼"
  />
)

export const SettingsPage = () => (
  <PlaceholderPage
    title="Cài đặt Hệ thống"
    description="Cấu hình các tùy chọn và cài đặt cho OmniChat"
    icon="⚙️"
  />
)
