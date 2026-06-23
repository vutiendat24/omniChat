export const Card = ({ children, className = '', ...props }) => (
  <div className={`bg-white rounded-lg border border-neutral-200 shadow-sm ${className}`} {...props}>
    {children}
  </div>
)

export const CardHeader = ({ children, className = '' }) => (
  <div className={`px-6 py-4 border-b border-neutral-200 ${className}`}>
    {children}
  </div>
)

export const CardBody = ({ children, className = '' }) => (
  <div className={`px-6 py-4 ${className}`}>
    {children}
  </div>
)

export const CardFooter = ({ children, className = '' }) => (
  <div className={`px-6 py-4 border-t border-neutral-200 bg-neutral-50 rounded-b-lg ${className}`}>
    {children}
  </div>
)
