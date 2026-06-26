import React, { useContext, useEffect, useRef, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { getTlsSearchOptions, searchTls, removeTlsFile } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const SearchTruckLoadStuff = () => {
  const { selections } = useOutletContext<{ selections: Selections }>();
  const [invoiceOptions, setInvoiceOptions] = useState<string[]>([]);
  const [invoiceNo, setInvoiceNo] = useState("");
  
  // Custom dropdown state
  const [showInvoiceSuggestions, setShowInvoiceSuggestions] = useState(false);
  const invoiceRef = useRef<HTMLDivElement>(null);

  const [results, setResults] = useState<any[]>([]);
  const [hasSearched, setHasSearched] = useState(false);
  const [searching, setSearching] = useState(false);
  
  // Toast
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  useEffect(() => {
    if (!selections) return;
    getTlsSearchOptions(selections.com, selections.loc, selections.year)
      .then(res => setInvoiceOptions(res.data || []))
      .catch(() => showToast("Failed to fetch Invoice options", "error"));
  }, [selections]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (invoiceRef.current && !invoiceRef.current.contains(e.target as Node)) {
        setShowInvoiceSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleSearch = async () => {
    if (!selections) return;
    setSearching(true);
    try {
      const res = await searchTls({
        companyId: selections.com,
        locationId: selections.loc,
        year: selections.year,
        invoiceNo: invoiceNo || undefined,
      });
      setResults(res.data || []);
      setHasSearched(true);
    } catch {
      showToast("Search failed", "error");
    } finally {
      setSearching(false);
    }
  };

  const handleExportExcel = () => {
    if (results.length === 0) return;
    import("xlsx").then((XLSX) => {
      const data = results.map(r => ({
        "Invoice Number": r.invoiceNo || "-",
        "File Name": r.fileName || "-",
        "Created By": r.createdBy || "-",
        "Created On": r.createdOn ? new Date(r.createdOn).toLocaleString("en-GB") : "-"
      }));
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Truck_Load_Stuff");
      const fileName = invoiceNo ? `${invoiceNo.replace(/[^a-zA-Z0-9_-]/g, "_")}.xlsx` : "truck_load_stuff.xlsx";
      XLSX.writeFile(wb, fileName);
    });
  };

  const handleClear = () => {
    setInvoiceNo("");
    setResults([]);
    setHasSearched(false);
  };

  const handleDelete = async (inv: string, file: string) => {
    if (!confirm(`Are you sure you want to remove file "${file}"?`)) return;
    try {
      await removeTlsFile(inv, file);
      showToast("Document removed", "success");
      handleSearch();
    } catch {
      showToast("Failed to remove document", "error");
    }
  };

  const getViewFileUrl = (inv: string, file: string) => {
    return `${import.meta.env.VITE_DMS_API}/truck-load-stuff/view?invoiceNo=${encodeURIComponent(inv)}&fileName=${encodeURIComponent(file)}`;
  };

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", background: "#f3f4f6", height: "calc(100vh - 100px)", position: "relative" }}>
      {toast && (
        <div style={{ position: "fixed", top: 20, right: 20, padding: "12px 20px", borderRadius: 4, color: "#fff", background: toast.type === "success" ? "#10b981" : "#ef4444", zIndex: 9999, boxShadow: "0 4px 6px rgba(0,0,0,0.1)", fontSize: 13, fontWeight: 500 }}>
          {toast.msg}
        </div>
      )}

      <main style={{ flex: 1, overflowY: "auto", padding: "24px 32px" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: 24, maxWidth: 1200, margin: "0 auto" }}>
          <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", padding: "20px 24px", boxShadow: "0 1px 2px rgba(0,0,0,0.05)" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h2 style={sectionTitle}>SEARCH FILTERS</h2>
              <button onClick={handleClear} style={linkButton}>Clear</button>
            </div>

            <div style={{ display: "flex", gap: 16, alignItems: "flex-end" }}>
              <div style={{ width: "300px", position: "relative" }} ref={invoiceRef}>
                <label style={labelStyle}>Select Invoice Number</label>
                <input
                  type="text"
                  value={invoiceNo}
                  onChange={e => { setInvoiceNo(e.target.value); setShowInvoiceSuggestions(true); }}
                  onFocus={() => setShowInvoiceSuggestions(true)}
                  placeholder="Enter invoice..."
                  style={inputStyle}
                />
                {showInvoiceSuggestions && (
                  <ul style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, marginTop: 4, maxHeight: 200, overflowY: "auto", zIndex: 10, listStyle: "none", padding: 0, boxShadow: "0 4px 6px rgba(0,0,0,0.1)" }}>
                    {invoiceOptions.filter(opt => opt.toUpperCase().includes(invoiceNo.toUpperCase())).map(opt => (
                      <li
                        key={opt}
                        onClick={() => { setInvoiceNo(opt); setShowInvoiceSuggestions(false); }}
                        style={{ padding: "8px 12px", fontSize: 12, cursor: "pointer", borderBottom: "1px solid #f3f4f6" }}
                        onMouseEnter={e => e.currentTarget.style.background = "#f9fafb"}
                        onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                      >
                        {opt}
                      </li>
                    ))}
                    {invoiceOptions.filter(opt => opt.toUpperCase().includes(invoiceNo.toUpperCase())).length === 0 && (
                      <li style={{ padding: "8px 12px", fontSize: 12, color: "#9ca3af", textAlign: "center" }}>No matches</li>
                    )}
                  </ul>
                )}
              </div>

              <button onClick={handleSearch} disabled={searching} style={{ ...primaryButton, opacity: searching ? 0.6 : 1, padding: "8px 24px" }}>
                {searching ? "Searching..." : "Search"}
              </button>
            </div>
          </div>

          {hasSearched && (
            <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", overflow: "hidden" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "12px 16px", background: "#f9fafb", borderBottom: "1px solid #e5e7eb" }}>
                <span style={{ fontSize: 13, fontWeight: 600, color: "#374151" }}>
                  Results <span style={{ color: "#6b7280", fontWeight: 400 }}>({results.length})</span>
                </span>
                <button onClick={handleExportExcel} style={exportBtn}>Export to Excel</button>
              </div>

              <div style={{ overflowX: "auto" }}>
                <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: 12 }}>
                  <thead style={{ background: "#f3f4f6" }}>
                    <tr style={{ color: "#374151" }}>
                      <th style={thStyle}>Invoice Number</th>
                      <th style={thStyle}>File Name</th>
                      <th style={thStyle}>Action</th>
                      <th style={thStyle}>Created By</th>
                      <th style={thStyle}>Created On</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((r, i) => (
                      <tr key={i} style={{ borderBottom: "1px solid #e5e7eb", transition: "background 0.15s" }} onMouseEnter={e => e.currentTarget.style.background = "#f9fafb"} onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                        <td style={tdStyle}>{r.invoiceNo}</td>
                        <td style={tdStyle}>
                          <a href={getViewFileUrl(r.invoiceNo, r.fileName)} target="_blank" rel="noreferrer" style={{ color: "#0ea5e9", textDecoration: "none", fontWeight: 500 }}>
                            {r.fileName || "View Document"}
                          </a>
                        </td>
                        <td style={tdStyle}>
                          <button onClick={() => handleDelete(r.invoiceNo, r.fileName)} style={{ padding: "4px 8px", background: "#fef2f2", border: "1px solid #fca5a5", borderRadius: 4, cursor: "pointer", fontSize: 11, fontWeight: 500, color: "#ef4444" }}>
                            Remove
                          </button>
                        </td>
                        <td style={tdStyle}>{r.createdBy || "-"}</td>
                        <td style={tdStyle}>{r.createdOn ? new Date(r.createdOn).toLocaleString("en-GB") : "-"}</td>
                      </tr>
                    ))}
                    {results.length === 0 && (
                      <tr>
                        <td colSpan={5} style={{ padding: "32px", textAlign: "center", color: "#6b7280" }}>
                          No documents found.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

// --- STYLES --- //
const sectionTitle: React.CSSProperties = { fontSize: 11, fontWeight: 600, color: "#5f7a98", textTransform: "uppercase", letterSpacing: "0.05em" };
const linkButton: React.CSSProperties = { background: "none", border: "none", color: "#6b7280", fontSize: 12, fontWeight: 500, cursor: "pointer", padding: 0 };
const labelStyle: React.CSSProperties = { display: "block", fontSize: 11, fontWeight: 600, color: "#5f7a98", marginBottom: 4, textTransform: "uppercase" };
const inputStyle: React.CSSProperties = { width: "100%", padding: "6px 10px", fontSize: 12, color: "#111827", background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, outline: "none", boxSizing: "border-box" };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: "pointer" };

const thStyle: React.CSSProperties = { padding: "10px 16px", fontWeight: 600, borderBottom: "2px solid #e5e7eb", whiteSpace: "nowrap" };
const tdStyle: React.CSSProperties = { padding: "10px 16px", color: "#4b5563", whiteSpace: "nowrap" };
const exportBtn: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 16px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 28 };

export default SearchTruckLoadStuff;
