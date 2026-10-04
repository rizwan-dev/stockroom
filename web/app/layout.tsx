import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Stockroom',
  description: 'Inventory and orders - Spring Boot and Next.js reference implementation',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <main>{children}</main>
      </body>
    </html>
  );
}
