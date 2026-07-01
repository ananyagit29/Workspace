import { Link, useNavigate } from "react-router-dom";
import { useContext, useEffect, useState } from "react";
import { AuthContext } from "../auth/AuthContext";

interface HeaderProps {
  username?: string;
  userId?: string;
  locationName?: string;
  departmentName?: string;
  applicationName?: string | null;
  activeTab?: "create" | "search";
  currentPage?: string;
  pageTitle?: string;
  breadcrumb?: string;
  contextMeta?: { label: string; value: string }[];
  onCreateClick?: () => void;
  createLabel?: string;
  tabs?: React.ReactNode;
}

export const Header: React.FC<HeaderProps> = ({
  username = "User",
  userId,
  locationName,
  departmentName,
  applicationName,
  activeTab,
  currentPage,
  pageTitle,
  breadcrumb,
  contextMeta,
  onCreateClick,
  createLabel,
  tabs,
}) => {
  const navigate = useNavigate();
  const { clearUser } = useContext(AuthContext);
  const isAdmin = userId?.toLowerCase() === "admin";

  const handleLogout = () => {
    localStorage.removeItem("jwtToken");
    sessionStorage.clear();
    clearUser();
    navigate("/");
  };

  const [globalToast, setGlobalToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    const handleAppToast = (e: any) => {
      setGlobalToast(e.detail);
      setTimeout(() => setGlobalToast(null), 4000);
    };
    window.addEventListener("app-toast", handleAppToast);
    return () => window.removeEventListener("app-toast", handleAppToast);
  }, []);

  const initials = username
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

  // Build full meta list: Application first, then the rest
  const allMeta = [
    ...(pageTitle ? [{ label: "Application", value: pageTitle }] : []),
    ...(contextMeta || []),
  ];

  const btnClass = (active?: boolean) =>
    `flex items-center gap-1 px-2 py-1 border border-white/40 rounded text-sm text-white
     hover:bg-white hover:text-[#003366] transition
     ${active ? "bg-yellow-400 text-black border-yellow-400 font-semibold" : ""}`;

  return (
    <header style={{ background: "#003366", color: "#fff", flexShrink: 0 }}>
      <div style={{
        display: "flex", alignItems: "center",
        padding: "0 16px", height: 52, gap: 0,
      }}>

        {/* LEFT: Avatar + Username + Breadcrumb */}
        <div style={{ display: "flex", alignItems: "center", flexShrink: 0, marginRight: 20 }}>
          <div style={{
            width: 32, height: 32, borderRadius: "50%",
            background: "rgba(255,255,255,0.15)",
            display: "flex", alignItems: "center", justifyContent: "center",
            fontSize: 11, fontWeight: 700, color: "#fff", flexShrink: 0,
            marginRight: 10,
          }}>
            {initials}
          </div>
          <div>
            <div style={{ fontSize: 13, fontWeight: 600, color: "#fff", lineHeight: 1.3, whiteSpace: "nowrap" }}>
              {username}
            </div>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.45)", lineHeight: 1.3, whiteSpace: "nowrap" }}>
              {breadcrumb || `${userId}${locationName ? ` · ${locationName}` : ""}`}
            </div>
          </div>
        </div>

        {/* DIVIDER */}
        {allMeta.length > 0 && (
          <div style={{ width: 1, height: 28, background: "rgba(255,255,255,0.2)", marginRight: 20, flexShrink: 0 }} />
        )}

        {/* META PILLS: Application + Company + Division + Location + SubApp */}
        {allMeta.length > 0 && (
          <div style={{ display: "flex", alignItems: "center", gap: 20, flexShrink: 0 }}>
            {allMeta.map((m, i) => (
              <div key={i} style={{ display: "flex", flexDirection: "column", gap: 1 }}>
                <span style={{ fontSize: 9, color: "rgba(255,255,255,0.4)", textTransform: "uppercase", letterSpacing: "0.05em" }}>
                  {m.label}
                </span>
                <span style={{ fontSize: 11, fontWeight: 600, color: "#fff", whiteSpace: "nowrap" }}>
                  {m.value}
                </span>
              </div>
            ))}
          </div>
        )}

        {/* TOAST CONTAINER (takes up remaining space) */}
        <div style={{ flex: 1, display: "flex", justifyContent: "center", alignItems: "center", overflow: "hidden", padding: "0 16px" }}>
           {globalToast && (
             <div style={{ 
               background: globalToast.type === 'success' ? '#16a34a' : '#dc2626', 
               color: '#fff', 
               padding: '4px 14px', 
               borderRadius: '6px', 
               fontSize: 12, 
               fontWeight: 600, 
               whiteSpace: 'nowrap', 
               textOverflow: 'ellipsis', 
               overflow: 'hidden',
               maxWidth: '100%',
               animation: "fadein 0.3s"
             }}>
               {globalToast.type === "success" ? "✓" : "✕"} {globalToast.msg}
             </div>
           )}
        </div>

        {/* RIGHT: Tabs + divider + Home + Logout */}
        <div style={{ display: "flex", alignItems: "center", gap: 6, flexShrink: 0 }}>
          {isAdmin ? (
            <>
              <Link to="/dashboard" className={btnClass(currentPage === "dashboard")}><i className="bi bi-house-fill" /></Link>
              <Link to="/addUser" className={btnClass(currentPage === "addUser")}><i className="bi bi-person-plus-fill" /> Add User</Link>
              <Link to="/viewUsers" className={btnClass(currentPage === "viewUsers")}><i className="bi bi-people-fill" /> View Users</Link>
              <Link to="/addRights" className={btnClass(currentPage === "addRights")}><i className="bi bi-shield-lock-fill" /> Add Rights</Link>
              <Link to="/viewRights" className={btnClass(currentPage === "viewRights")}><i className="bi bi-shield-check" /> View Rights</Link>
              <button onClick={handleLogout} className={btnClass(false)}><i className="bi bi-box-arrow-right" /></button>
            </>
          ) : (
            <>
              {tabs && (
                <>
                  <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
                    {tabs}
                  </div>
                  <div style={{ width: 1, height: 24, background: "rgba(255,255,255,0.2)", margin: "0 4px" }} />
                </>
              )}
              <Link to="/dashboard" style={{
                display: "flex", alignItems: "center", justifyContent: "center",
                width: 30, height: 30, borderRadius: 6,
                background: "rgba(255,255,255,0.1)", border: "1px solid rgba(255,255,255,0.2)",
                color: "#fff", textDecoration: "none", fontSize: 14,
              }}>
                <i className="bi bi-house-fill" />
              </Link>
              <button onClick={handleLogout} style={{
                display: "flex", alignItems: "center", justifyContent: "center",
                width: 30, height: 30, borderRadius: 6,
                background: "rgba(255,255,255,0.1)", border: "1px solid rgba(255,255,255,0.2)",
                color: "#fff", cursor: "pointer", fontSize: 14,
              }}>
                <i className="bi bi-box-arrow-right" />
              </button>
            </>
          )}
        </div>

      </div>
    </header>
  );
};