import Link from "next/link";
import type { ReactNode } from "react";

export function AuthLayout({
  children,
  mode
}: {
  children: ReactNode;
  mode: "login" | "register";
}) {
  return (
    <div className={`auth-page auth-page-${mode}`}>
      <header className="auth-header">
        <Link href="/" className="auth-brand">
          DotaOps
        </Link>
        {mode === "login" ? (
          <nav aria-label="Auth navigation">
            <Link href="/turnirji">Public Tournaments</Link>
            <span>API Status</span>
          </nav>
        ) : null}
      </header>
      {children}
      <footer className="auth-footer">
        <div>
          <strong>DotaOps</strong>
          <p>(c) 2024 DotaOps Analytics Engine. All rights reserved.</p>
        </div>
        <nav aria-label="Auth footer">
          <span>Data Privacy</span>
          <span>API Documentation</span>
          <span>System Status</span>
          <span>Support</span>
        </nav>
      </footer>
    </div>
  );
}
