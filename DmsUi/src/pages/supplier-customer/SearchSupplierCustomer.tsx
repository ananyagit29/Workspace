import React, { useContext, useEffect, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { getSupplierCustomerSearchOptions, searchSupplierCustomer, removeSupplierCustomer } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const ACCOUNT_TYPE_OPTIONS = ["CUSTOMER", "SUPPLIER", "SUPPLIER AND CUSTOMER", "NON-SCM PARTIES"];

interface SCRecord {
  accountName: string;
  accountCode: string;
  fileName: string;
  filePath?: string;
  createdBy: string;
  createdOn: string;
}

const PAGE_SIZE = 7;

const SearchSupplierCustomer = () => {
  const { user, loading } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();
  const navigate = useNavigate();

  const [accountType, setAccountType] = useState("");
  const [accountOptions, setAccountOptions] = useState<{ code: string; name: string }[]>([]);
  const [selectedAccount, setSelectedAccount] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);

  const [results, setResults] = useState<SCRecord[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

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

  useEffect(() => {
    setSelectedAccount("");
    setAccountOptions([]);
    if (!accountType || !selections) return;

    getSupplierCustomerSearchOptions(accountType, selections.com, selections.loc)
      .then(res => setAccountOptions(res.data || []))
      .catch(() => showToast("Failed to fetch accounts", "error"));
  }, [accountType, selections]);

  const handleSearch = async (page = 0) => {
    if (!selections) return;
    if (!accountType) {
      showToast("Please select an account type to search", "error");
      return;
    }
    setSearching(true);
    setHasSearched(true);

    let accCode = selectedAccount;
    const match = selectedAccount.match(/\(([^)]+)\)$/);
    if (match) accCode = match[1];

    try {
      const res = await searchSupplierCustomer({
        companyId: selections.com,
        locationId: selections.loc,
        accountType: accountType,
        accountCode: accCode || undefined,
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
    setAccountType("");
    setSelectedAccount("");
    setResults([]);
    setTotalPages(0);
    setTotalElements(0);
    setCurrentPage(0);
    setHasSearched(false);
  };

  const handleExportExcel = () => {
    if (results.length === 0) return;
    import("xlsx").then((XLSX) => {
      const data = results.map(r => ({
        "Name": r.accountName || "-",
        "Code": r.accountCode || "-",
        "Filename": r.fileName || "-",
        "Created By": r.createdBy || "-",
        "Created On": r.createdOn ? new Date(r.createdOn).toLocaleString("en-GB") : "-"
      }));
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Supplier_Customer");
      const fileName = selectedAccount ? `${selectedAccount.replace(/[^a-zA-Z0-9_-]/g, "_")}.xlsx` : "supplier_customer.xlsx";
      XLSX.writeFile(wb, fileName);
    });
  };

  const handleDelete = async (code: string, fileName: string) => {
    if (!window.confirm(`Are you sure you want to remove the file '${fileName}'?`)) return;
    try {
      await removeSupplierCustomer(code, fileName);
      showToast("Removed successfully", "success");
      handleSearch(currentPage);
    } catch {
      showToast("Failed to remove", "error");
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
              <span style={sectionTitle}>SEARCH FILTERS</span>
              <button onClick={handleReset} style={linkButton}>Clear</button>
            </div>

            <div style={{ display: "flex", gap: 16, alignItems: "flex-end", padding: "10px 0 4px 0" }}>
              <div style={{ width: "300px" }}>
                <label style={labelStyle}>Select Account Type</label>
                <select value={accountType} onChange={e => setAccountType(e.target.value)} style={inputStyle}>
                  <option value="">SELECT</option>
                  {ACCOUNT_TYPE_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
                </select>
              </div>

              <div style={{ position: "relative", zIndex: 50, width: "300px" }}>
                <label style={labelStyle}>{accountType ? `Select ${accountType === "SUPPLIER" ? "Supplier" : accountType === "NON-SCM PARTIES" ? "Non-SCM Parties" : accountType === "CUSTOMER" ? "Customer" : "Party"}` : "Select"}</label>
                <input
                  value={selectedAccount}
                  onChange={e => {
                    setSelectedAccount(e.target.value.toUpperCase());
                    setShowSuggestions(true);
                  }}
                  onFocus={() => setShowSuggestions(true)}
                  onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                  disabled={!accountType}
                  placeholder="Enter account..."
                  style={{ ...inputStyle, width: "100%", boxSizing: "border-box", opacity: !accountType ? 0.6 : 1 }}
                />
                {showSuggestions && accountType && (
                  <ul style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, zIndex: 10, maxHeight: 150, overflowY: "auto", listStyle: "none", padding: 0, margin: "4px 0 0 0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)" }}>
                    {accountOptions
                      .map(acc => `${acc.name} (${acc.code})`)
                      .filter(c => c.toUpperCase().includes(selectedAccount.toUpperCase()))
                      .map(sug => (
                        <li
                          key={sug}
                          onClick={() => {
                            setSelectedAccount(sug);
                            setShowSuggestions(false);
                          }}
                          style={{ padding: "8px 12px", fontSize: 12, cursor: "pointer", borderBottom: "1px solid #f3f4f6", color: "#374151" }}
                          onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"}
                          onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                        >
                          {sug}
                        </li>
                      ))}
                    {accountOptions.filter(c => `${c.name} (${c.code})`.toUpperCase().includes(selectedAccount.toUpperCase())).length === 0 && (
                      <li style={{ padding: "8px 12px", fontSize: 12, color: "#9ca3af", textAlign: "center" }}>No matches</li>
                    )}
                  </ul>
                )}
              </div>

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
                <button onClick={handleExportExcel} style={exportBtn}>Export to Excel</button>
              </div>

              <div style={{ overflowX: "auto" }}>
                <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: 12 }}>
                  <thead style={{ background: "#f3f4f6" }}>
                    <tr style={{ color: "#374151" }}>
                      <th style={thStyle}>Name</th>
                      <th style={thStyle}>Code</th>
                      <th style={thStyle}>Filename</th>
                      <th style={thStyle}>Remove</th>
                      <th style={thStyle}>Created By</th>
                      <th style={thStyle}>Created On</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((r, i) => (
                      <tr key={`${r.accountCode}-${i}`} style={{ borderBottom: "1px solid #e5e7eb", transition: "background 0.15s" }} onMouseEnter={e => e.currentTarget.style.background = "#f9fafb"} onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                        <td style={tdStyle}>{r.accountName}</td>
                        <td style={tdStyle}>{r.accountCode}</td>
                        <td style={tdStyle}>
                          <a href={`http://localhost:8080/dmsApi/supplier-customer/view?accountCode=${encodeURIComponent(r.accountCode)}&fileName=${encodeURIComponent(r.fileName)}`} target="_blank" rel="noopener noreferrer" style={{ color: "#0ea5e9", textDecoration: "none", fontWeight: 500 }}>
                            {r.fileName}
                          </a>
                        </td>
                        <td style={tdStyle}>
                          <button onClick={() => handleDelete(r.accountCode, r.fileName)} style={{ padding: "4px 8px", background: "#fef2f2", border: "1px solid #fca5a5", borderRadius: 4, cursor: "pointer", fontSize: 11, fontWeight: 500, color: "#ef4444" }}>
                            Remove
                          </button>
                        </td>
                        <td style={tdStyle}>{r.createdBy || "-"}</td>
                        <td style={tdStyle}>{r.createdOn ? new Date(r.createdOn).toLocaleString("en-GB") : "-"}</td>
                      </tr>
                    ))}
                    {results.length === 0 && (
                      <tr>
                        <td colSpan={6} style={{ padding: "32px", textAlign: "center", color: "#6b7280" }}>
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
    </div>
  );
};

// --- STYLES --- //
const sectionTitle: React.CSSProperties = { fontSize: 11, fontWeight: 600, color: "#8aa2b9", textTransform: "uppercase", letterSpacing: "0.05em" };
const linkButton: React.CSSProperties = { background: "none", border: "none", color: "#6b7280", fontSize: 12, fontWeight: 500, cursor: "pointer", padding: 0 };
const labelStyle: React.CSSProperties = { display: "block", fontSize: 11, fontWeight: 600, color: "#8aa2b9", marginBottom: 4, textTransform: "uppercase" };
const inputStyle: React.CSSProperties = { width: "100%", padding: "6px 10px", fontSize: 12, color: "#111827", background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, outline: "none", boxSizing: "border-box" };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: "pointer" };

const thStyle: React.CSSProperties = { padding: "10px 16px", fontWeight: 600, borderBottom: "2px solid #e5e7eb", whiteSpace: "nowrap" };
const tdStyle: React.CSSProperties = { padding: "10px 16px", color: "#4b5563", whiteSpace: "nowrap" };
const exportBtn: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 16px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 28 };

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

export default SearchSupplierCustomer;
