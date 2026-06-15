import { useState, useContext } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../api/authApi";
import logo from "../assets/logo.png";
import { AuthContext } from "../auth/AuthContext";

export default function Login() {
  const navigate = useNavigate();
  const { refreshUser } = useContext(AuthContext);

  const [userId, setUserId]           = useState("");
  const [password, setPassword]       = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      await login(userId, password);
      await refreshUser();
      navigate("/dashboard");
    } catch (err: any) {
      setError(err.message || "Invalid credentials");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: "100vh", width: "100%", display: "flex",
      background: "linear-gradient(135deg, #001f3f 0%, #003366 50%, #0a4f8a 100%)",
    }}>
      {/* Left: branding panel */}
      <div style={{
        flex: 1, display: "flex", flexDirection: "column",
        justifyContent: "center", padding: "48px 64px",
      }}>
        <div style={{ fontSize: 28, fontWeight: 700, color: "#fff", lineHeight: 1.3, marginBottom: 12 }}>
          Document Management System
        </div>
        <div style={{ marginTop: 40, display: "flex", flexDirection: "column", gap: 10 }}>
          {["Centralized document storage", "Role-based access control", "Audit trail & version history"].map(f => (
            <div key={f} style={{ display: "flex", alignItems: "center", gap: 10, color: "rgba(255,255,255,0.75)", fontSize: 13 }}>
              <div style={{ width: 18, height: 18, borderRadius: "50%", background: "rgba(255,255,255,0.15)", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
                <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                  <path d="M2 5l2 2 4-4" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </div>
              {f}
            </div>
          ))}
        </div>
      </div>

      {/* Right: login card */}
      <div style={{
        width: 400, display: "flex", alignItems: "center", justifyContent: "center",
        background: "rgba(255,255,255,0.97)", padding: "40px 36px",
        flexDirection: "column",
      }}>
        <div style={{ width: "100%", maxWidth: 320 }}>

          {/* Logo + subtitle */}
          <div style={{ display: "flex", flexDirection: "column", alignItems: "center", marginBottom: 28 }}>
            <img src={logo} alt="Ipca Logo" style={{ height: 48, objectFit: "contain", marginBottom: 16 }} />
            <div style={{ fontSize: 14, fontWeight: 700, color: "#111827", textAlign: "center" }}>
              Sign in to your DMS account
            </div>
          </div>

          {/* Error */}
          {error && (
            <div style={{ marginBottom: 16, padding: "8px 12px", background: "#fef2f2", border: "1px solid #fecaca", borderRadius: 7, fontSize: 12, color: "#dc2626", display: "flex", alignItems: "center", gap: 6 }}>
              <span>⚠</span> {error}
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 14 }}>

            {/* User ID */}
            <div>
              <label style={{ display: "block", fontSize: 11, fontWeight: 600, color: "#374151", textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 5 }}>
                User ID
              </label>
              <input
                value={userId}
                onChange={e => setUserId(e.target.value.toLowerCase())}
                placeholder="Enter your user ID"
                required autoFocus
                style={{ width: "100%", border: "1px solid #e5e7eb", borderRadius: 7, padding: "8px 12px", fontSize: 13, color: "#111827", background: "#f9fafb", outline: "none", boxSizing: "border-box" }}
                onFocus={e => e.target.style.borderColor = "#003366"}
                onBlur={e => e.target.style.borderColor = "#e5e7eb"}
              />
            </div>

            {/* Password */}
            <div>
              <label style={{ display: "block", fontSize: 11, fontWeight: 600, color: "#374151", textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 5 }}>
                Password
              </label>
              <div style={{ position: "relative" }}>
                <input
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                  style={{ width: "100%", border: "1px solid #e5e7eb", borderRadius: 7, padding: "8px 36px 8px 12px", fontSize: 13, color: "#111827", background: "#f9fafb", outline: "none", boxSizing: "border-box" }}
                  onFocus={e => e.target.style.borderColor = "#003366"}
                  onBlur={e => e.target.style.borderColor = "#e5e7eb"}
                />
                <button type="button" onClick={() => setShowPassword(!showPassword)}
                  style={{ position: "absolute", right: 10, top: "50%", transform: "translateY(-50%)", background: "none", border: "none", cursor: "pointer", padding: 0, color: showPassword ? "#003366" : "#9ca3af" }}>
                  {showPassword ? (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/>
                      <line x1="1" y1="1" x2="23" y2="23"/>
                    </svg>
                  ) : (
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  )}
                </button>
              </div>
            </div>

            {/* Submit */}
            <button
              type="submit"
              disabled={loading}
              style={{
                width: "100%", background: loading ? "#e5e7eb" : "#003366",
                color: loading ? "#9ca3af" : "#fff", border: "none",
                borderRadius: 7, padding: "9px", fontSize: 13, fontWeight: 600,
                cursor: loading ? "not-allowed" : "pointer", marginTop: 4,
                transition: "background 0.2s",
              }}>
              {loading ? "Signing in..." : "Sign In →"}
            </button>
          </form>

          {/* Footer */}
          <div style={{ marginTop: 32, paddingTop: 16, borderTop: "1px solid #f3f4f6", textAlign: "center", fontSize: 11, color: "#9ca3af" }}>
            ©2026 Ipca Laboratories Limited · DMS v2.0
          </div>
        </div>
      </div>
    </div>
  );
}