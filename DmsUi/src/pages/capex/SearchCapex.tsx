import React, { useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { getCapexSearchCodes, getCapexRevisions, searchCapex, deleteCapex, reviseCapex } from "../../api/dmsApi";

const getViewFileUrl = (budgetCode: string, revisionNo: number) => {
  const token = localStorage.getItem("jwtToken");
  return `${import.meta.env.VITE_DMS_API}/capex/view?budgetCode=${encodeURIComponent(budgetCode)}&revision=${revisionNo}&token=${token}`;
};

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const CAPEX_TYPE_MAP: Record<string, string> = {
  "1": "CAPITAL",
  "4": "INPRINCIPLE APPROVAL",
  "2A": "LOCAL CAPITAL",
  "2C": "LOCAL REPAIR",
  "3A": "REPAIR",
};

const CAPEX_TYPE_OPTIONS = ["1", "4", "2A", "2C", "3A"];

interface CapexRecord {
  budgetCode: string;
  budgetType: string;
  docDate: string;
  revisionNo: number;
  fileName: string;
  createdBy: string;
  createdOn: string;
}

const PAGE_SIZE = 7;

const SearchCapex = () => {
  const { user, loading } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();
  const navigate = useNavigate();

  const [budgetTypes, setBudgetTypes] = useState<string[]>([]);
  const [budgetCodes, setBudgetCodes] = useState<string[]>([]);
  const [budgetType, setBudgetType] = useState("");
  const [budgetCode, setBudgetCode] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [revisions, setRevisions] = useState<string[]>([]);
  const [revision, setRevision] = useState("Latest");

  const [results, setResults] = useState<CapexRecord[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const [replacingCapex, setReplacingCapex] = useState<string | null>(null);
  const fileRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (loading) return;
    if (!user) { navigate("/"); return; }
  }, [loading, user, navigate]);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(t);
  }, [toast]);

  const showToast = (msg: string, type: "success" | "error" = "success") => setToast({ msg, type });

  // Fetch Budget Types
  // Removed dynamic fetch to match exact legacy budget types
  useEffect(() => {
    if (!selections) return;
    setBudgetTypes(CAPEX_TYPE_OPTIONS);
  }, [selections]);

  // Fetch Budget Codes when Type changes
  useEffect(() => {
    setBudgetCode("");
    setBudgetCodes([]);
    setRevision("Latest");
    setRevisions([]);
    if (!budgetType || !selections) return;
    
    const dbBudgetType = CAPEX_TYPE_MAP[budgetType] || budgetType;
    getCapexSearchCodes(dbBudgetType, selections.com, selections.loc, selections.year)
      .then(res => setBudgetCodes(res.data || []))
      .catch(() => showToast("Failed to fetch search budget codes", "error"));
  }, [budgetType, selections]);

  // Fetch Revisions when a valid Budget Code is selected
  useEffect(() => {
    setRevision("Latest");
    setRevisions([]);
    if (!budgetCode || !budgetCodes.includes(budgetCode)) return;

    getCapexRevisions(budgetCode)
      .then(res => setRevisions(res.data || []))
      .catch(() => console.error("Failed to fetch revisions"));
  }, [budgetCode, budgetCodes]);

  const handleSearch = async (page = 0) => {
    if (!selections) return;
    if (!budgetType) {
      showToast("Please select a budget type to search", "error");
      return;
    }
    setSearching(true);
    setHasSearched(true);
    try {
      const res = await searchCapex({
        companyId: selections.com,
        locationId: selections.loc,
        year: selections.year,
        budgetType: budgetType ? (CAPEX_TYPE_MAP[budgetType] || budgetType) : undefined,
        budgetCode: budgetCode || undefined,
        revision: budgetCode ? (revision || undefined) : undefined,
        page,
        size: PAGE_SIZE,
      });

      setResults(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setTotalElements(res.data.totalElements || 0);
      setCurrentPage(page);
    } catch {
      showToast("Search failed", "error");
      setResults([]);
    } finally {
      setSearching(false);
    }
  };

  const handleReset = () => {
    setBudgetType("");
    setBudgetCode("");
    setResults([]);
    setTotalPages(0);
    setTotalElements(0);
    setCurrentPage(0);
    setHasSearched(false);
  };

  const handleDelete = async (code: string, revisionNo: number) => {
    if (!window.confirm(`Are you sure you want to remove revision ${revisionNo} of this CapEx Budget?`)) return;
    try {
      await deleteCapex(code, revisionNo);
      showToast("Deleted successfully", "success");
      handleSearch(currentPage);
    } catch {
      showToast("Failed to delete", "error");
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0 || !replacingCapex || !user) return;
    const file = e.target.files[0];
    
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      showToast("Only PDF files allowed", "error");
      setReplacingCapex(null);
      if (fileRef.current) fileRef.current.value = "";
      return;
    }
    
    const formData = new FormData();
    formData.append("file", file);
    formData.append("userId", user.userId);

    try {
      await reviseCapex(replacingCapex, formData);
      showToast("Revision successfully uploaded", "success");
      handleSearch(currentPage);
    } catch {
      showToast("Failed to revise document", "error");
    } finally {
      setReplacingCapex(null);
      if (fileRef.current) fileRef.current.value = "";
    }
  };

  const renderPagination = () => {
    if (totalPages === 0) return null;
    let startPage = Math.max(0, currentPage - 2);
    let endPage = Math.min(totalPages - 1, currentPage + 2);
    if (endPage - startPage < 4) {
      if (startPage === 0) endPage = Math.min(totalPages - 1, startPage + 4);
      else if (endPage === totalPages - 1) startPage = Math.max(0, endPage - 4);
    }
    const pages = [];
    for (let i = startPage; i <= endPage; i++) pages.push(i);

    return (
      <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
        <button onClick={() => handleSearch(currentPage - 1)} disabled={currentPage === 0} style={pagerButton(currentPage === 0)}>Previous</button>
        {pages.map(p => (
          <button key={p} onClick={() => handleSearch(p)} style={pagerButton(false, currentPage === p)}>{p + 1}</button>
        ))}
        <button onClick={() => handleSearch(currentPage + 1)} disabled={currentPage >= totalPages - 1} style={pagerButton(currentPage >= totalPages - 1)}>Next</button>
      </div>
    );
  };

  if (loading || !selections) return null;

  return (
    <div style={{ background: "#f3f4f6", minHeight: "100vh" }}>
      {toast && (
        <div style={{ position: "fixed", top: 16, right: 16, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "24px 32px" }}>
        <div style={{ maxWidth: 1200, margin: "0 auto" }}>
          <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "16px 20px", marginBottom: 16 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
              <span style={sectionTitle}>Search Filters</span>
              <button onClick={handleReset} style={linkButton}>Clear</button>
            </div>
            <div style={{ display: "flex", gap: 16, alignItems: "flex-end", padding: "10px 0 4px 0" }}>
              <div style={{ width: "300px" }}>
                <label style={labelStyle}>Budget Type</label>
                <select value={budgetType} onChange={e => setBudgetType(e.target.value)} style={inputStyle}>
                  <option value="">Select</option>
                  {budgetTypes.map(t => <option key={t} value={t}>{CAPEX_TYPE_MAP[t] || t}</option>)}
                </select>
              </div>
              <div style={{ position: "relative", zIndex: 50, width: "300px" }}>
                <label style={labelStyle}>Budget Code</label>
                <input
                  value={budgetCode}
                  onChange={e => {
                    setBudgetCode(e.target.value.toUpperCase());
                    setShowSuggestions(true);
                  }}
                  onFocus={() => setShowSuggestions(true)}
                  onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                  disabled={!budgetType}
                  placeholder="Enter budget code"
                  style={{ ...inputStyle, width: "100%", boxSizing: "border-box", opacity: !budgetType ? 0.6 : 1 }}
                />
                {showSuggestions && budgetType && (
                  <ul style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, zIndex: 10, maxHeight: 150, overflowY: "auto", listStyle: "none", padding: 0, margin: "4px 0 0 0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)" }}>
                    {budgetCodes.filter(c => c.toUpperCase().includes(budgetCode.toUpperCase())).map(sug => (
                      <li 
                        key={sug} 
                        onClick={() => {
                          setBudgetCode(sug);
                          setShowSuggestions(false);
                        }}
                        style={{ padding: "8px 12px", fontSize: 12, cursor: "pointer", borderBottom: "1px solid #f3f4f6", color: "#374151" }}
                        onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"}
                        onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                      >
                        {sug}
                      </li>
                    ))}
                    {budgetCodes.filter(c => c.toUpperCase().includes(budgetCode.toUpperCase())).length === 0 && (
                      <li style={{ padding: "8px 12px", fontSize: 12, color: "#9ca3af", textAlign: "center" }}>No matches</li>
                    )}
                  </ul>
                )}
              </div>

              {budgetCode && budgetCodes.includes(budgetCode) && (
                <div style={{ width: "120px" }}>
                  <label style={labelStyle}>Revision</label>
                  <select value={revision} onChange={e => setRevision(e.target.value)} style={inputStyle}>
                    <option value="Latest">Latest</option>
                    {revisions.map(r => <option key={r} value={r}>{r}</option>)}
                    <option value="All">All</option>
                  </select>
                </div>
              )}

              <button onClick={() => handleSearch(0)} disabled={searching} style={{ ...primaryButton, opacity: searching ? 0.6 : 1, padding: "8px 24px" }}>
                {searching ? "Searching..." : "Search"}
              </button>
            </div>
          </div>

          {hasSearched && (
            <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", overflow: "hidden" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 16px", background: "#f9fafb", borderBottom: "1px solid #e5e7eb" }}>
                <span style={{ fontSize: 13, fontWeight: 600, color: "#374151" }}>
                  Results <span style={{ color: "#6b7280", fontWeight: 400 }}>({totalElements})</span>
                </span>
              </div>

              <div style={{ overflowX: "auto" }}>
                <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: 12 }}>
                  <thead style={{ background: "#f3f4f6" }}>
                    <tr style={{ color: "#374151" }}>
                      <th style={thStyle}>Budget Code</th>
                      <th style={thStyle}>Doc Date</th>
                      <th style={thStyle}>Revision No</th>
                      <th style={thStyle}>Filename</th>
                      <th style={thStyle}>Revision</th>
                      <th style={thStyle}>Remove</th>
                      <th style={thStyle}>Created By</th>
                      <th style={thStyle}>Created On</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((r) => (
                      <tr key={`${r.budgetCode}-${r.revisionNo}`} style={{ borderBottom: "1px solid #e5e7eb", transition: "background 0.15s" }} onMouseEnter={e => e.currentTarget.style.background = "#f9fafb"} onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                        <td style={tdStyle}>{r.budgetCode}</td>
                        <td style={tdStyle}>{r.docDate ? new Date(r.docDate).toLocaleDateString("en-GB") : "-"}</td>
                        <td style={tdStyle}>{r.revisionNo}</td>
                        <td style={tdStyle}>
                          <a href={getViewFileUrl(r.budgetCode, r.revisionNo)} target="_blank" rel="noreferrer" style={{ color: "#0ea5e9", textDecoration: "none", fontWeight: 500 }}>
                            {r.fileName || "View Document"}
                          </a>
                        </td>
                        <td style={tdStyle}>
                          <button onClick={() => { setReplacingCapex(r.budgetCode); fileRef.current?.click(); }} style={{ padding: "4px 8px", background: "#f3f4f6", border: "1px solid #d1d5db", borderRadius: 4, cursor: "pointer", fontSize: 11, fontWeight: 500, color: "#2563eb" }}>
                            Revise
                          </button>
                        </td>
                        <td style={tdStyle}>
                          <button onClick={() => handleDelete(r.budgetCode, r.revisionNo)} style={{ padding: "4px 8px", background: "#fef2f2", border: "1px solid #fca5a5", borderRadius: 4, cursor: "pointer", fontSize: 11, fontWeight: 500, color: "#ef4444" }}>
                            Remove
                          </button>
                        </td>
                        <td style={tdStyle}>{r.createdBy || "-"}</td>
                        <td style={tdStyle}>{r.createdOn ? new Date(r.createdOn).toLocaleString("en-GB") : "-"}</td>
                      </tr>
                    ))}
                    {results.length === 0 && (
                      <tr>
                        <td colSpan={8} style={{ padding: "32px", textAlign: "center", color: "#6b7280" }}>
                          No documents found.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
              
              <div style={{ padding: "12px 16px", borderTop: "1px solid #e5e7eb", background: "#fff", display: "flex", justifyContent: "flex-end" }}>
                {renderPagination()}
              </div>
            </div>
          )}
        </div>
      </main>

      {/* Hidden file input for Revise */}
      <input type="file" accept="application/pdf" ref={fileRef} style={{ display: "none" }} onChange={handleFileChange} />
    </div>
  );
};

// --- STYLES --- //
const sectionTitle: React.CSSProperties = { fontSize: 14, fontWeight: 600, color: "#111827" };
const linkButton: React.CSSProperties = { background: "none", border: "none", color: "#6b7280", fontSize: 12, fontWeight: 500, cursor: "pointer", padding: 0 };
const labelStyle: React.CSSProperties = { display: "block", fontSize: 11, fontWeight: 600, color: "#4b5563", marginBottom: 4, textTransform: "uppercase" };
const inputStyle: React.CSSProperties = { width: "100%", padding: "6px 10px", fontSize: 12, color: "#111827", background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, outline: "none", boxSizing: "border-box" };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: "pointer" };

const thStyle: React.CSSProperties = { padding: "10px 16px", fontWeight: 600, borderBottom: "2px solid #e5e7eb", whiteSpace: "nowrap" };
const tdStyle: React.CSSProperties = { padding: "10px 16px", color: "#4b5563", whiteSpace: "nowrap" };

const pagerButton = (disabled: boolean, active: boolean = false): React.CSSProperties => ({
  padding: "4px 10px",
  fontSize: 11,
  fontWeight: active ? 600 : 500,
  color: disabled ? "#9ca3af" : active ? "#fff" : "#374151",
  background: active ? "#003366" : "#fff",
  border: "1px solid",
  borderColor: active ? "#003366" : "#d1d5db",
  borderRadius: 4,
  cursor: disabled ? "not-allowed" : "pointer",
});

export default SearchCapex;
