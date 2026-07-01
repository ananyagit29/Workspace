import { useContext, useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { AuthContext } from "../auth/AuthContext";
import { Header } from "../components/Header";

import { useAppRights } from "../hooks/useAppRights";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const BatchLayout = () => {
  const { user, loading: authLoading } = useContext(AuthContext);
  const navigate = useNavigate();
  const { canCreate, canReport, loading: rightsLoading } = useAppRights();
  const [selections, setSelections] = useState<Selections | null>(null);

  useEffect(() => {
    if (authLoading) return;
    if (!user) { navigate("/"); return; }
    const raw = sessionStorage.getItem("dms2Selections");
    if (!raw) { navigate("/dashboard"); return; }
    setSelections(JSON.parse(raw));
  }, [authLoading, user, navigate]);

  if (authLoading || rightsLoading || !selections) {
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100vh" }}>
        <div style={{ width: 28, height: 28, border: "3px solid #e5e7eb", borderTopColor: "#003366", borderRadius: "50%" }} />
      </div>
    );
  }
  if (!user) return null;

  const tabs = (
    <>
      <TabLink to="/batch" end label="Search" />
      {canCreate && <TabLink to="/batch/create" label="Create" />}
    </>
  );

  return (
    <div style={{ display: "flex", flexDirection: "column", position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "#f3f4f6", overflow: "hidden" }}>
      <Header
        username={`${user.firstName} ${user.lastName}`}
        userId={user.userId}
        locationName={user.locationName}
        departmentName={user.departmentName}
        applicationName={selections.app}
        pageTitle="Batch Details"
        breadcrumb={`Dashboard › Batch Details${selections.subApp ? ` › ${selections.subApp}` : ""}`}
        contextMeta={[
          { label: "Company",  value: selections.com },
          { label: "Division", value: selections.div },
          { label: "Location", value: selections.loc },
          ...(selections.subApp ? [{ label: "Sub App", value: selections.subApp }] : []),
          ...(selections.year   ? [{ label: "Year",    value: selections.year }]   : []),
        ]}
        tabs={tabs}
      />
      <Outlet context={{ selections, user }} />
    </div>
  );
};

// Tab styled as pill button on dark header
const TabLink = ({ to, label, end }: { to: string; label: string; end?: boolean }) => (
  <NavLink
    to={to}
    end={end}
    style={({ isActive }) => ({
      padding: "4px 12px",
      fontSize: 11,
      fontWeight: 500,
      color: isActive ? "#003366" : "rgba(255,255,255,0.9)",
      background: isActive ? "#ffffff" : "rgba(255,255,255,0.1)",
      border: "1px solid",
      borderColor: isActive ? "#ffffff" : "rgba(255,255,255,0.3)",
      borderRadius: 5,
      textDecoration: "none",
      transition: "all 0.15s",
      whiteSpace: "nowrap",
    })}
  >
    {label}
  </NavLink>
);

export default BatchLayout;