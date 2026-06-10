import { Link, useNavigate } from "react-router-dom";
import { useContext } from "react";
import { AuthContext } from "../auth/AuthContext";

interface HeaderProps {
  username?: string;
  userId?: string;
  locationName?: string;
  departmentName?: string;
  applicationName?: string | null;
  activeTab?: "create" | "search";
  currentPage?: string;
  // New props for module context
  pageTitle?: string;
  breadcrumb?: string;
  contextMeta?: { label: string; value: string }[];
  onCreateClick?: () => void;
  createLabel?: string;
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

  // Initials from username
  const initials = username
    .split(" ")
    .map((n) => n[0])
    .join("")
    .toUpperCase()
    .slice(0, 2);

  const btnClass = (active?: boolean) =>
    `flex items-center gap-1 px-2 py-1 border border-white/40 rounded text-sm text-white
     hover:bg-white hover:text-[#003366] transition
     ${active ? "bg-yellow-400 text-black border-yellow-400 font-semibold" : ""}`;

  return (
    <header style={{ background: "#003366", color: "#fff" }}>

      {/* ── Row 1: User info + nav actions ── */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "6px 16px", borderBottom: "1px solid rgba(255,255,255,0.1)" }}>

        {/* Left: avatar + name + meta */}
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ width: 30, height: 30, borderRadius: "50%", background: "rgba(255,255,255,0.15)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12, fontWeight: 600, color: "#fff", flexShrink: 0 }}>
            {initials}
          </div>
          <div>
            <div style={{ fontSize: 13, fontWeight: 600, color: "#fff" }}>{username}</div>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.5)", marginTop: 1 }}>
              {userId} {locationName && `· ${locationName}`} {departmentName && `· ${departmentName}`}
            </div>
          </div>
        </div>

        {/* Right: nav buttons */}
        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
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
              {applicationName && (
                <ul style={{ display: "flex", borderBottom: "1px solid rgba(255,255,255,0.3)", fontSize: 13, marginRight: 8, listStyle: "none", padding: 0, margin: "0 8px 0 0" }}>
                  <li>
                    <Link to={`/${applicationName}/create`}
                      style={{ display: "block", padding: "4px 12px", color: activeTab === "create" ? "#facc15" : "rgba(255,255,255,0.8)", borderBottom: activeTab === "create" ? "2px solid #facc15" : "none", fontWeight: activeTab === "create" ? 600 : 400, textDecoration: "none" }}>
                      Create
                    </Link>
                  </li>
                  <li>
                    <Link to={`/${applicationName}/search`}
                      style={{ display: "block", padding: "4px 12px", color: activeTab === "search" ? "#facc15" : "rgba(255,255,255,0.8)", borderBottom: activeTab === "search" ? "2px solid #facc15" : "none", fontWeight: activeTab === "search" ? 600 : 400, textDecoration: "none" }}>
                      Search
                    </Link>
                  </li>
                </ul>
              )}
              <Link to="/dashboard" className={btnClass(currentPage === "home")}><i className="bi bi-house-fill" /></Link>
              <button onClick={handleLogout} className={btnClass(false)}><i className="bi bi-box-arrow-right" /></button>
            </>
          )}
        </div>
      </div>

      {/* ── Row 2: Page title + context meta + create button (only when pageTitle is passed) ── */}
      {pageTitle && (
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "5px 16px 6px" }}>

          {/* Left: breadcrumb + title + meta */}
          <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
            <div>
              {breadcrumb && (
                <div style={{ fontSize: 10, color: "rgba(255,255,255,0.4)", marginBottom: 2 }}>{breadcrumb}</div>
              )}
              <div style={{ fontSize: 15, fontWeight: 600, color: "#fff" }}>{pageTitle}</div>
            </div>

            {/* Context meta pills */}
            {contextMeta && contextMeta.length > 0 && (
              <>
                <div style={{ width: 1, height: 28, background: "rgba(255,255,255,0.2)" }} />
                <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
                  {contextMeta.map((m, i) => (
                    <div key={i} style={{ display: "flex", flexDirection: "column", gap: 1 }}>
                      <span style={{ fontSize: 9, color: "rgba(255,255,255,0.45)", textTransform: "uppercase", letterSpacing: "0.05em" }}>{m.label}</span>
                      <span style={{ fontSize: 12, fontWeight: 600, color: "#fff" }}>{m.value}</span>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>

          {/* Right: create button */}
          {onCreateClick && (
            <button
              onClick={onCreateClick}
              style={{ background: "#fff", color: "#003366", border: "none", borderRadius: 7, padding: "6px 16px", fontSize: 12, fontWeight: 600, cursor: "pointer" }}>
              + {createLabel || "Create"}
            </button>
          )}
        </div>
      )}
    </header>
  );
};