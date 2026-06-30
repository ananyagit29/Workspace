import React, { useContext, useEffect, useState } from "react";
import { AuthContext } from "../auth/AuthContext";
import { useNavigate } from "react-router-dom";
import {
  getCompanies, getDivisions, getLocations, getApplications,
  getYears, getSubApplications, getModules,
} from "../api/dmsApi";
import logo from "../assets/logo.png";

const APP_ROUTES: Record<string, string> = {
  BATCH_DETAILS: "/batch",
  INVOICE_DOCUMENT: "/invoice",
  INVOICE_DOCUMENT_SYSTEM: "/invoice",
  INVOICE_DOCUMENTS: "/invoice",
  CAPEX_BUDGET: "/capex",
  SUPPLIER_AND_CUSTOMER: "/supplier-customer",
  TRUCK_LOAD_STUFF: "/truck-load-stuff",
  ACCOUNTS: "/accounts",
};

const sel: React.CSSProperties = {    
  width: "100%", border: "1px solid #e5e7eb", borderRadius: 6,
  padding: "5px 8px", fontSize: 12, color: "#374151",
  background: "#f9fafb", outline: "none", appearance: "auto",
  boxSizing: "border-box" as const,
};

const selFilled: React.CSSProperties = {
  ...sel, borderColor: "#003366", background: "#fff", color: "#111827",
};

const lbl: React.CSSProperties = {
  display: "block", fontSize: 10, fontWeight: 600, color: "#333",
  textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 3,
};

const Dashboard: React.FC = () => {
  const { user, loading } = useContext(AuthContext);
  const navigate = useNavigate();

  const [companies, setCompanies]       = useState<any[]>([]);
  const [divisions, setDivisions]       = useState<any[]>([]);
  const [locations, setLocations]       = useState<any[]>([]);
  const [applications, setApplications] = useState<any[]>([]);
  const [years, setYears]               = useState<string[]>([]);
  const [subApps, setSubApps]           = useState<string[]>([]);
  const [modules, setModules]           = useState<string[]>([]);

  const [com, setCom]       = useState("");
  const [div, setDiv]       = useState("");
  const [loc, setLoc]       = useState("");
  const [app, setApp]       = useState("");
  const [year, setYear]     = useState("");
  const [subApp, setSubApp] = useState("");
  const [module, setModule] = useState("");

  useEffect(() => {
    if (loading) return;
    if (!user) navigate("/");
  }, [loading, user, navigate]);

  useEffect(() => {
    getCompanies().then(res => {
      const data = res.data || [];
      setCompanies(data);
      if (data.length === 1) setCom(data[0][0]);
    }).catch(console.error);
  }, []);

  useEffect(() => {
    if (!com) { setDivisions([]); setDiv(""); return; }
    getDivisions(com).then(res => {
      const data = res.data || [];
      setDivisions(data); 
      if (data.length === 1) setDiv(data[0][0]);
    }).catch(console.error);
  }, [com]);

  useEffect(() => {
    if (!com || !div) { setLocations([]); setLoc(""); return; }
    getLocations(com, div).then(res => {
      const data = res.data || [];
      setLocations(data);
      if (data.length === 1) setLoc(data[0][0]);
    }).catch(console.error);
  }, [com, div]);

  useEffect(() => {
    if (!com || !div || !loc) { setApplications([]); setApp(""); return; }
    getApplications(com, div, loc, user?.userId).then(res => {
      const data = res.data || [];
      setApplications(data);
      if (data.length === 1) setApp(data[0][0] || "");
    }).catch(console.error);
  }, [com, div, loc]);

  useEffect(() => {
    if (!app) {
      setYears([]); setSubApps([]); setModules([]);
      setYear(""); setSubApp(""); setModule("");
      return;
    }
    getYears(app).then(res => {
      const data = res.data || [];
      setYears(data);
      if (data.length === 1) setYear(data[0]);
    }).catch(console.error);

    getSubApplications(com, div, loc, app, user?.userId).then(res => {
      const data = res.data || [];
      setSubApps(data as string[]);
      setModules([]); setModule("");
      if (data.length === 1) setSubApp(data[0] || "");
      else setSubApp("");
    }).catch(console.error);
  }, [com, div, loc, app]);

  useEffect(() => {
    if (!subApp) { setModules([]); setModule(""); return; }
    getModules(com, div, loc, app, subApp).then(res => {
      const data = res.data || [];
      setModules(data);
      if (data.length === 1) setModule(data[0]);
      else setModule("");
    }).catch(console.error);
  }, [com, div, loc, app, subApp]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    sessionStorage.setItem("dms2Selections", JSON.stringify({
      com, div, loc, app, year,
      subApp: subApp || null,
      module: module || null,
    }));
    const normalizedApp = app.trim().toUpperCase().replace(/[\s-]+/g, "_");
    const route = APP_ROUTES[app] || APP_ROUTES[normalizedApp];
    if (route) navigate(route);
    else alert(`No module found for application: ${app}`);
  };

  const canSubmit = !!(com && div && loc && app);
  const hasSubApps = subApps.some(s => s !== null);
  const hasModules = modules.some(m => m !== null);

  const initials = user
    ? `${user.firstName?.[0] || ""}${user.lastName?.[0] || ""}`.toUpperCase()
    : "??";

  if (loading) return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100vh", background: "#f3f4f6" }}>
      <div style={{ width: 28, height: 28, border: "3px solid #e5e7eb", borderTopColor: "#003366", borderRadius: "50%" }} />
    </div>
  );
  if (!user) return null;

  return (
    <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "#f3f4f6", display: "flex", flexDirection: "column" }}>

      {/* Top bar */}
      <div style={{ background: "#003366", display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 20px", borderBottom: "1px solid rgba(255,255,255,0.08)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <div style={{ width: 30, height: 30, borderRadius: "50%", background: "rgba(255,255,255,0.15)", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11, fontWeight: 700, color: "#fff" }}>
            {initials}
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 600, color: "#fff" }}>{user.firstName} {user.lastName}</div>
            <div style={{ fontSize: 10, color: "rgba(255,255,255,0.5)" }}>{user.userId} · {user.locationName} · {user.departmentName}</div>
          </div>
        </div>
        <button
          onClick={() => { localStorage.removeItem("jwtToken"); localStorage.removeItem("userId"); sessionStorage.clear(); navigate("/"); }}
          style={{ background: "rgba(255,255,255,0.1)", border: "1px solid rgba(255,255,255,0.2)", borderRadius: 6, padding: "4px 12px", fontSize: 11, color: "rgba(255,255,255,0.7)", cursor: "pointer" }}>
          Sign out
        </button>
      </div>

      {/* Centered card */}
      <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: 20 }}>
        <div style={{ width: "100%", maxWidth: 360, background: "#fff", borderRadius: 7, overflow: "hidden", boxShadow: "0 4px 24px rgba(0,0,0,0.4)", border: "1px solid #e5e7eb" }}>

          {/* Card header */}
          <div style={{ background: "#003366", padding: "12px 18px", display: "flex", alignItems: "center", gap: 12 }}>
            <img src={logo} alt="logo" style={{ height: 26, objectFit: "contain", filter: "brightness(0) invert(1)" }} />
            <div style={{ width: 1, height: 22, background: "rgba(255,255,255,0.2)" }} />
            <div>
              <div style={{ fontSize: 13, fontWeight: 600, color: "#fff" }}>Select Workspace</div>
              <div style={{ fontSize: 10, color: "rgba(255,255,255,0.5)", marginTop: 1 }}>Choose your workspace to continue</div>
            </div>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} style={{ padding: "16px 18px", display: "flex", flexDirection: "column", gap: 9 }}>

            <div><label style={lbl}>Company</label>
              <select value={com} onChange={e => setCom(e.target.value)} style={com ? selFilled : sel}>
                <option value="">Select</option>
                {companies.map((c: any) => <option key={c[0]} value={c[0]}>{c[1] || c[0]}</option>)}
              </select>
            </div>

            <div><label style={lbl}>Division</label>
              <select value={div} onChange={e => setDiv(e.target.value)} style={div ? selFilled : sel} disabled={!com}>
                <option value="">Select</option>
                {divisions.map((d: any, i: number) => <option key={i} value={Array.isArray(d) ? d[0] : d}>{Array.isArray(d) ? d[0] : d}</option>)}
              </select>
            </div>

            <div><label style={lbl}>Location</label>
              <select value={loc} onChange={e => setLoc(e.target.value)} style={loc ? selFilled : sel} disabled={!div}>
                <option value="">Select</option>
                {locations.map((l: any) => <option key={l[0]} value={l[0]}>{l[1] || l[0]}</option>)}
              </select>
            </div>

            <div><label style={lbl}>Application</label>
              <select value={app} onChange={e => setApp(e.target.value)} style={app ? selFilled : sel} disabled={!loc}>
                <option value="">Select</option>
                {applications.map((a: any, i: number) => <option key={i} value={Array.isArray(a) ? a[0] : a}>{Array.isArray(a) ? a[0] : a}</option>)}
              </select>
            </div>

            {years.length > 0 && (
              <div><label style={lbl}>Year</label>
                <select value={year} onChange={e => setYear(e.target.value)} style={year ? selFilled : sel}>
                  <option value="">Select</option>
                  {years.map((y, i) => <option key={i} value={y}>{y}</option>)}
                </select>
              </div>
            )}

            {hasSubApps && (
              <div><label style={lbl}>Sub Application</label>
                <select value={subApp} onChange={e => setSubApp(e.target.value)} style={subApp ? selFilled : sel}>
                  <option value="">Select</option>
                  {subApps.filter(s => s !== null).map((s, i) => <option key={i} value={s}>{s}</option>)}
                </select>
              </div>
            )}

            {hasModules && (
              <div><label style={lbl}>Module</label>
                <select value={module} onChange={e => setModule(e.target.value)} style={module ? selFilled : sel}>
                  <option value="">Select</option>
                  {modules.filter(m => m !== null).map((m, i) => <option key={i} value={m}>{m}</option>)}
                </select>
              </div>
            )}

            <div style={{ height: 1, background: "#f3f4f6", margin: "2px 0" }} />

            <button type="submit" disabled={!canSubmit} style={{
              background: canSubmit ? "#003366" : "#e5e7eb",
              color: canSubmit ? "#fff" : "#9ca3af",
              border: "none", borderRadius: 7, padding: "8px",
              fontSize: 12, fontWeight: 600,
              cursor: canSubmit ? "pointer" : "not-allowed",
              transition: "background 0.2s",
            }}>
              Continue →
            </button>
          </form>
        </div>
      </div>

      {/* Footer */}
      <div style={{ textAlign: "center", padding: "8px 0", fontSize: 10, color: "#333" }}>
        ©2026 Copyright <strong style={{ color: "#333" }}>Ipca Laboratories Limited</strong> · DMS v2.0
      </div>

    </div>
  );
};

export default Dashboard;