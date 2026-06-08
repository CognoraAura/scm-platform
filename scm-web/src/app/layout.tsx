import { ReactNode } from 'react'
import '@/lib/design-tokens.css'

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html suppressHydrationWarning>
      <body>{children}</body>
    </html>
  )
}
