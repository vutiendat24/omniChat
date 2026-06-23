import { Fragment } from 'react'
import { Listbox, Transition } from '@headlessui/react'
import { ChevronsUpDown } from 'lucide-react'

export const Select = ({
  label,
  value,
  onChange,
  options = [],
  error,
  disabled = false,
  className = ''
}) => {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-neutral-700 mb-2">
          {label}
        </label>
      )}
      <Listbox value={value} onChange={onChange} disabled={disabled}>
        <div className="relative">
          <Listbox.Button
            className={`w-full px-3 py-2 border border-neutral-300 rounded-lg bg-white text-left focus:outline-none focus:ring-2 focus:ring-primary-500 ${
              error ? 'border-accent-danger focus:ring-accent-danger' : ''
            } ${disabled ? 'bg-neutral-100 text-neutral-500 cursor-not-allowed' : ''} ${className}`}
          >
            <span className="flex items-center justify-between">
              {value && options.find(opt => opt.value === value)?.label}
              <ChevronsUpDown className="h-4 w-4 text-neutral-400" />
            </span>
          </Listbox.Button>

          <Transition
            as={Fragment}
            leave="transition ease-in duration-100"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <Listbox.Options className="absolute z-10 w-full mt-1 border border-neutral-200 bg-white rounded-lg shadow-lg">
              {options.map((option) => (
                <Listbox.Option
                  key={option.value}
                  className={({ active }) =>
                    `py-2 px-3 cursor-pointer ${
                      active ? 'bg-blue-100 text-primary-900' : 'text-neutral-900'
                    }`
                  }
                  value={option.value}
                >
                  {option.label}
                </Listbox.Option>
              ))}
            </Listbox.Options>
          </Transition>
        </div>
      </Listbox>

      {error && (
        <p className="text-sm text-accent-danger mt-1">{error}</p>
      )}
    </div>
  )
}
