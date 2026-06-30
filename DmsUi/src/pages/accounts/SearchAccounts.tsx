import React, { useContext, useEffect, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { useAppRights } from "../../hooks/useAppRights";
import {
  getAccountsDaybooks,
  searchAccounts,
  removeAccountsDocument,
  dmsApi,
} from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const MONTHS = [
  { value: "04", label: "April" }, { value: "05", label: "May" }, { value: "06", label: "June" },
  { value: "07", label: "July" }, { value: "08", label: "August" }, { value: "09", label: "September" },
  { value: "10", label: "October" }, { value: "11", label: "November" }, { value: "12", label: "December" },
  { value: "01", label: "January" }, { value: "02", label: "February" }, { value: "03", label: "March" },
];

const SearchAccounts: React.FC = () => {
  const { user } = useContext(AuthContext);
  const { canRemove } = useAppRights();
  const { selections } = useOutletContext<{ selections: Selections }>();

  const [daybooks, setDaybooks] = useState<{ code: string; name: string }[]>([]);
  const [selectedDaybook, setSelectedDaybook] = useState("");
  const [selectedMonth, setSelectedMonth] = useState("04");
  const [docCodes, setDocCodes] = useState<string[]>([]);
  const [selectedDocCode, setSelectedDocCode] = useState("");
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  // Load daybooks
  useEffect(() => {
    getAccountsDaybooks()
      .then(res => {
        const data = (res.data || []).map((r: any) => ({
          code: String(r.CODE || r.code || ""),
          name: String(r.NAME || r.name || ""),
        }));
        setDaybooks(data);
        if (data.length > 0) setSelectedDaybook(data[0].code + "~" + data[0].name);
      })
      .catch(() => showToast("Failed to load daybooks", "error"));
  }, []);

  // When daybook or month changes, auto-search and populate doc codes
  useEffect(() => {
    if (!selectedDaybook || !selections) return;
    const daybookCode = selectedDaybook.split("~")[0];
    handleSearch(daybookCode, selectedMonth);
  }, [selectedDaybook, selectedMonth, selections]);

  const handleSearch = (daybookCode?: string, month?: string, docCode?: string) => {
    const dbCode = daybookCode || selectedDaybook.split("~")[0];
    const m = month || selectedMonth;
    const dc = docCode || selectedDocCode;
    if (!dbCode || !selections) return;

    setLoading(true);
    searchAccounts({
      locationId: selections.loc,
      daybookCode: dbCode,
      year: selections.year,
      month: m,
      docCode: dc || undefined,
    })
      .then(res => {
        const data = res.data || [];
        setResults(data);
        // Extract unique doc codes for filter dropdown
        const codes = [...new Set(data.map((r: any) => String(r.DOC_CODE || r.Doc_Code || "")))].filter(Boolean);
        setDocCodes(codes as string[]);
      })
      .catch(() => showToast("Failed to search", "error"))
      .finally(() => setLoading(false));
  };

  const handleRemove = (daybookCode: string, docCode: string, filename: string) => {
    if (!window.confirm("Are you sure you want to remove this document?")) return;
    removeAccountsDocument({ daybookCode, docCode, filename })
      .then(() => {
        showToast("Document removed", "success");
        handleSearch();
      })
      .catch(() => showToast("Failed to remove document", "error"));
  };

  const handleViewFile = (daybookCode: string, docCode: string, fileName: string) => {
    const url = `${import.meta.env.VITE_DMS_API}accounts/view-file?daybookCode=${daybookCode}&docCode=${docCode}&fileName=${encodeURIComponent(fileName)}`;
    const token = localStorage.getItem("jwtToken");
    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.blob())
      .then(blob => {
        const blobUrl = URL.createObjectURL(blob);
        window.open(blobUrl, "_blank");
      })
      .catch(() => showToast("Failed to open file", "error"));
  };

  const handleExportExcel = () => {
    if (results.length === 0) return;
    import("xlsx").then((XLSX) => {
      const data = results.map((r: any) => ({
        "Doc Code": r.DOC_CODE || "",
        "Doc Date": r.DOC_DATE ? new Date(r.DOC_DATE).toLocaleDateString("en-GB") : "",
        "Account Name": r.ACCOUNT_NAME || "",
        "Filename": r.FILE_NAME || "",
        "Bill No.": r.BILL_NUMBER || "",
        "Bill Date": r.BILL_DATE ? new Date(r.BILL_DATE).toLocaleDateString("en-GB") : "",
        "Trans Amt": r.TRAN_AMOUNT || "",
        "Created By": r.CREATED_BY || "",
        "Created On": r.CREATED_ON || "",
      }));
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Accounts");
      XLSX.writeFile(wb, "Accounts_Search.xlsx");
    });
  };

  if (!user || !selections) return null;

  return (
    <div style={{ flex: 1, overflow: "auto", background: "#f3f4f6" }}>
      {toast && (
        <div style={{ position: "fixed", bottom: 24, right: 24, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "24px 32px" }}>
        <div style={{ maxWidth: 1200, margin: "0 auto" }}>
          {/* Filters */}
          <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "16px 20px", marginBottom: 16 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
              <span style={sectionTitle}>Search Filters</span>
            </div>
            <div style={{ display: "flex", gap: 16, alignItems: "flex-end", flexWrap: "wrap", padding: "10px 0 4px 0" }}>
              <div style={{ minWidth: 220 }}>
                <label style={labelStyle}>Daybook Code</label>
                <select
                  value={selectedDaybook}
                  onChange={e => { setSelectedDaybook(e.target.value); setSelectedDocCode(""); }}
                  style={inputStyle}
                >
                  {daybooks.map(d => (
                    <option key={d.code} value={`${d.code}~${d.name}`}>{d.code}~{d.name}</option>
                  ))}
                </select>
              </div>
              <div style={{ minWidth: 140 }}>
                <label style={labelStyle}>Month</label>
                <select
                  value={selectedMonth}
                  onChange={e => { setSelectedMonth(e.target.value); setSelectedDocCode(""); }}
                  style={inputStyle}
                >
                  {MONTHS.map(m => (
                    <option key={m.value} value={m.value}>{m.label}</option>
                  ))}
                </select>
              </div>
              <div style={{ minWidth: 140 }}>
                <label style={labelStyle}>Doc Code</label>
                <select
                  value={selectedDocCode}
                  onChange={e => {
                    setSelectedDocCode(e.target.value);
                    handleSearch(undefined, undefined, e.target.value);
                  }}
                  style={inputStyle}
                >
                  <option value="">All</option>
                  {docCodes.map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </div>
              <button onClick={() => handleSearch()} disabled={loading} style={{ ...primaryButton, opacity: loading ? 0.6 : 1 }}>
                {loading ? "Searching..." : "Search"}
              </button>
            </div>
          </div>

          {/* Results */}
          {results.length > 0 && (
            <div>
              <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 8 }}>
                <button onClick={handleExportExcel} style={primaryButton}>Save As Excel</button>
              </div>
              <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", overflow: "auto", maxHeight: "calc(100vh - 280px)" }}>
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                  <thead>
                    <tr style={{ background: "#f9fafb", position: "sticky", top: 0, zIndex: 1 }}>
                      <th style={thStyle}>Doc Code</th>
                      <th style={thStyle}>Doc Date</th>
                      <th style={thStyle}>Account Name</th>
                      <th style={thStyle}>Filename</th>
                      {canRemove && <th style={thStyle}>Remove</th>}
                      <th style={thStyle}>Bill No.</th>
                      <th style={thStyle}>Bill Date</th>
                      <th style={thStyle}>Trans Amt</th>
                      <th style={thStyle}>Created By</th>
                      <th style={thStyle}>Created On</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((r: any, idx: number) => (
                      <tr key={idx} style={{ transition: "background 0.15s" }}
                        onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"}
                        onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                        <td style={{ ...tdStyle, textAlign: "right", width: 80 }}>{r.DOC_CODE || ""}</td>
                        <td style={{ ...tdStyle, width: 100 }}>{r.DOC_DATE ? new Date(r.DOC_DATE).toLocaleDateString("en-GB") : ""}</td>
                        <td style={{ ...tdStyle, minWidth: 200 }}>{r.ACCOUNT_NAME || ""}</td>
                        <td style={{ ...tdStyle, minWidth: 180 }}>
                          {r.FILE_NAME ? (
                            <button
                              onClick={() => handleViewFile(r.DAYBOOK_CODE || "", r.DOC_CODE || "", r.FILE_NAME)}
                              style={fileButton}
                            >
                              {r.FILE_NAME}
                            </button>
                          ) : ""}
                        </td>
                        {canRemove && (
                          <td style={{ ...tdStyle, textAlign: "center", width: 60 }}>
                            <button
                              onClick={() => handleRemove(r.DAYBOOK_CODE || "", r.DOC_CODE || "", r.FILE_NAME || "")}
                              style={{ ...iconButton, color: "#dc2626" }}
                            >✕</button>
                          </td>
                        )}
                        <td style={{ ...tdStyle, width: 120 }}>{r.BILL_NUMBER || ""}</td>
                        <td style={{ ...tdStyle, width: 100 }}>{r.BILL_DATE ? new Date(r.BILL_DATE).toLocaleDateString("en-GB") : ""}</td>
                        <td style={{ ...tdStyle, textAlign: "right", width: 90 }}>{r.TRAN_AMOUNT != null ? r.TRAN_AMOUNT : ""}</td>
                        <td style={{ ...tdStyle, width: 100 }}>{r.CREATED_BY || ""}</td>
                        <td style={{ ...tdStyle, width: 160 }}>{r.CREATED_ON || ""}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {!loading && results.length === 0 && (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "48px 16px", color: "#9ca3af" }}>
              <div style={{ fontSize: 28, marginBottom: 10 }}>📄</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: "#6b7280" }}>No records found</div>
              <div style={{ fontSize: 11, marginTop: 4 }}>Select a daybook and month to view uploaded documents.</div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

const sectionTitle: React.CSSProperties = { fontSize: 11, fontWeight: 600, color: "#9ca3af", textTransform: "uppercase", letterSpacing: "0.05em" };
const labelStyle: React.CSSProperties = { display: "block", fontSize: 10, fontWeight: 600, color: "#6b7280", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.04em" };
const inputStyle: React.CSSProperties = { width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "6px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 24px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 28 };
const thStyle: React.CSSProperties = { padding: "8px 10px", textAlign: "left", fontSize: 10, fontWeight: 600, color: "#6b7280", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap", border: "1px solid #e5e7eb", background: "#f9fafb" };
const tdStyle: React.CSSProperties = { padding: "8px 10px", color: "#374151", whiteSpace: "nowrap", border: "1px solid #e5e7eb" };
const fileButton: React.CSSProperties = { color: "#1d4ed8", fontSize: 11, background: "none", border: "none", cursor: "pointer", padding: 0, textDecoration: "underline", textUnderlineOffset: 2 };
const iconButton: React.CSSProperties = { background: "none", border: "none", cursor: "pointer", fontSize: 14, padding: 2 };

export default SearchAccounts;
