export const Input = ({
  label,
  error,
  helperText,
  className = '',
  ...props
}) => {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-neutral-700 mb-2">
          {label}
        </label>
      )}
      <input
        className={`w-full px-3 py-2 border border-neutral-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent ${
          error ? 'border-accent-danger focus:ring-accent-danger' : ''
        } ${className}`}
        {...props}
      />
      {error && (
        <p className="text-sm text-accent-danger mt-1">{error}</p>
      )}
      {helperText && !error && (
        <p className="text-sm text-neutral-500 mt-1">{helperText}</p>
      )}
    </div>
  )
}
