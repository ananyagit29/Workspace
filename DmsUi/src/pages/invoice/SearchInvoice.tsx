import React from "react";
import { useContext, useEffect, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { dmsApi } from "../../api/dmsApi";

const searchInvoice = (params: Record<string, any>) =>
  dmsApi.get("/invoice/search", { params });

const removeOtherFile = (invoiceNumber: string) =>
  dmsApi.delete(`/invoice/other-file/${invoiceNumber}`);

const getViewFileUrl = (invoiceNumber: string, type: string) => {
  const token = localStorage.getItem("jwtToken");
  return `${import.meta.env.VITE_DMS_API}/invoice/${encodeURIComponent(invoiceNumber)}/view?type=${type}&token=${token}`;
};

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

interface InvoiceRecord {
  invoiceNumber: string;
  fileName: string;
  otherFileName?: string;
  otherFilePath?: string;
  invoiceFileName?: string;
  filePath: string;
  createdBy: string;
  createdOn: string;
}

const PAGE_SIZE = 8;

const SearchInvoice = () => {
  const { user, loading } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();
  const navigate = useNavigate();

  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [results, setResults] = useState<InvoiceRecord[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);
  const [searchMessage, setSearchMessage] = useState<string | null>(null);


  const [replacingInvoice, setReplacingInvoice] = useState<string | null>(null);
  const fileRef = React.useRef<HTMLInputElement | null>(null);

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

  const handleSearch = async (page = 0, specificInvoiceNumber?: string) => {
    if (!selections) return;
    setSearching(true);
    setHasSearched(true);
    setSearchMessage(null);
    try {
      const searchNumber = specificInvoiceNumber || invoiceNumber;
      const res = await dmsApi.get("/invoice/search", {
        params: {
          locationId: selections.loc,
          year: selections.year,
          page,
          size: PAGE_SIZE,
          ...(searchNumber && { invoiceNumber: searchNumber.trim().toUpperCase() }),
        },
      });

      const fetched = res.data.content || [];

      if (searchNumber && searchNumber.trim().length > 0 && fetched.length === 0) {
        const existsRes = await dmsApi.get("/invoice/exists", {
          params: {
            invoiceNumber: searchNumber.trim().toUpperCase(),
            year: selections.year,
            locationId: selections.loc
          }
        });
        if (existsRes.data) {
          setSearchMessage(`Invoice number "${searchNumber.toUpperCase()}" exists in SCM but not on this page. Try refining your search.`);
        } else {
          setSearchMessage(`Invoice number "${searchNumber.toUpperCase()}" does not exist in the selected financial year (${selections.year}).`);
        }
        setResults([]);
        setTotalPages(0);
        setTotalElements(0);
        setCurrentPage(0);
        return;
      }

      setResults(fetched);
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
    setInvoiceNumber("");
    setResults([]);
    setTotalPages(0);
    setTotalElements(0);
    setCurrentPage(0);
    setHasSearched(false);
    setSearchMessage(null);
  };

  const handleDeleteOtherFile = async (invoiceNumber: string) => {
    if (!window.confirm("Are you sure you want to delete this file?")) return;
    try {
      await dmsApi.delete(`/invoice/other-file/${invoiceNumber}`);
      showToast("File deleted successfully", "success");
      handleSearch(currentPage);
    } catch {
      showToast("Failed to delete file", "error");
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.files || e.target.files.length === 0 || !replacingInvoice) return;
    const file = e.target.files[0];
    
    const formData = new FormData();
    formData.append("newFile", file);

    try {
      await dmsApi.put(`/invoice/other-file/${replacingInvoice}`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      showToast("File replaced successfully", "success");
      handleSearch(currentPage);
    } catch {
      showToast("Failed to replace file", "error");
    } finally {
      setReplacingInvoice(null);
      if (fileRef.current) fileRef.current.value = "";
    }
  };

  const handleExportExcel = () => {
    if (results.length === 0) return;
    import("xlsx").then((XLSX) => {
      const data = results.map(r => ({
        "Invoice No.": r.invoiceNumber,
        "Other File": r.otherFileName || "-",
        "Invoice File": r.invoiceFileName || "-",
        "Created By": r.createdBy || "-",
        "Created On": r.createdOn ? new Date(r.createdOn).toLocaleString("en-GB") : "-"
      }));
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Invoices");
      const fileName = invoiceNumber ? `${invoiceNumber.toUpperCase()}.xlsx` : "invoices.xlsx";
      XLSX.writeFile(wb, fileName);
    });
  };

  if (loading || !selections)
    return (
      <div style={{ display: "flex", alignItems: "center", justifyContent: "center", height: "100vh" }}>
        <div style={{ width: 28, height: 28, border: "3px solid #e5e7eb", borderTopColor: "#003366", borderRadius: "50%" }} />
      </div>
    );
  if (!user) return null;

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
              <div style={{ position: "relative", zIndex: 50, width: "300px" }}>
                <label style={labelStyle}>Invoice Number</label>
                <input
                  value={invoiceNumber}
                  onChange={e => {
                    const val = e.target.value.toUpperCase();
                    if (val.length > 12) return;
                    if (val !== "" && /[^A-Z0-9]/.test(val)) return;
                    setInvoiceNumber(val);
                    setShowSuggestions(true);
                    dmsApi.get("/invoice/suggest", { 
                      params: { 
                        query: val, 
                        locationId: selections.loc,
                        year: selections.year
                      } 
                    })
                      .then(res => setSuggestions(res.data || []))
                      .catch(() => setSuggestions([]));
                  }}
                  onFocus={() => {
                    setShowSuggestions(true);
                    dmsApi.get("/invoice/suggest", { 
                      params: { 
                        query: invoiceNumber, 
                        locationId: selections.loc,
                        year: selections.year
                      } 
                    })
                      .then(res => setSuggestions(res.data || []))
                      .catch(() => setSuggestions([]));
                  }}
                  onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                  onKeyDown={e => { if (e.key === "Enter") { setShowSuggestions(false); handleSearch(0); } }}
                  placeholder="Enter invoice number"
                  style={{ ...inputStyle, width: "100%", boxSizing: "border-box" }}
                />
                {showSuggestions && suggestions.length > 0 && (
                  <ul style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, zIndex: 10, maxHeight: 150, overflowY: "auto", listStyle: "none", padding: 0, margin: "4px 0 0 0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)" }}>
                    {suggestions.map(sug => (
                      <li 
                        key={sug} 
                        onClick={() => {
                          setInvoiceNumber(sug);
                          setShowSuggestions(false);
                          setTimeout(() => {
                            handleSearch(0, sug);
                          }, 0);
                        }}
                        style={{ padding: "8px 12px", fontSize: 12, cursor: "pointer", borderBottom: "1px solid #f3f4f6", color: "#374151" }}
                        onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"}
                        onMouseLeave={e => e.currentTarget.style.background = "transparent"}
                      >
                        {sug}
                      </li>
                    ))}
                  </ul>
                )}
              </div>

              <button onClick={() => handleSearch(0)} disabled={searching} style={{ ...primaryButton, opacity: searching ? 0.6 : 1, padding: "8px 24px" }}>
                {searching ? "Searching..." : "Search"}
              </button>
            </div>
          </div>
          
          {searchMessage && (
            <div style={{ padding: "12px 16px", background: "#fef2f2", border: "1px solid #f87171", borderRadius: 8, color: "#991b1b", fontSize: 13, fontWeight: 500, marginBottom: 16 }}>
              {searchMessage}
            </div>
          )}

          {hasSearched && !searchMessage && (
            <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", overflow: "hidden" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 16px", background: "#f9fafb", borderBottom: "1px solid #e5e7eb" }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: "#374151" }}>
                  {results.length > 0 ? `${totalElements.toLocaleString()} records found` : "No records found"}
                </span>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  {results.length > 0 && (
                    <button onClick={handleExportExcel} style={{ ...primaryButton, height: 26, padding: "0 16px" }}>
                      Export to Excel
                    </button>
                  )}
                  {results.length > 0 && <span style={{ fontSize: 11, color: "#9ca3af" }}>Page {currentPage + 1} of {totalPages}</span>}
                </div>
              </div>

              {results.length > 0 ? (
                <>
                  <div style={{ overflowX: "auto", maxHeight: "calc(100vh - 320px)" }}>
                    <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                      <thead>
                        <tr style={{ background: "#f9fafb" }}>
                          {["Invoice No.", "Invoice File", "Other File", "Created By", "Created On", "Actions"].map(h => (
                            <th key={h} style={thStyle}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {results.map((row, i) => (
                          <tr key={row.invoiceNumber} style={{ background: i % 2 === 0 ? "#fff" : "#fafafa" }}>
                            <td style={{ ...tdStyle, fontFamily: "monospace", fontWeight: 700, color: "#111827" }}>{row.invoiceNumber}</td>
                            <td style={{ ...tdStyle, maxWidth: 320, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={row.invoiceFileName}>
                              {row.invoiceFileName ? (
                                <button onClick={() => window.open(getViewFileUrl(row.invoiceNumber, "invoice"), "_blank")} style={fileButton}>
                                  {row.invoiceFileName}
                                </button>
                              ) : "-"}
                            </td>
                            <td style={{ ...tdStyle, maxWidth: 320, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={row.otherFileName}>
                              {row.otherFileName ? (
                                <button onClick={() => window.open(getViewFileUrl(row.invoiceNumber, "other"), "_blank")} style={fileButton}>
                                  {row.otherFileName}
                                </button>
                              ) : "-"}
                            </td>
                            <td style={{ ...tdStyle, color: "#6b7280" }}>{row.createdBy || "-"}</td>
                            <td style={{ ...tdStyle, color: "#6b7280" }}>{row.createdOn ? new Date(row.createdOn).toLocaleString("en-GB") : "-"}</td>
                            <td style={{ ...tdStyle, textAlign: "center" }}>
                              {row.otherFileName ? (
                                <button onClick={() => handleDeleteOtherFile(row.invoiceNumber)} style={{ color: "#ef4444", background: "#fee2e2", border: "1px solid #fecaca", padding: "4px 12px", borderRadius: "6px", fontSize: 11, fontWeight: 500, cursor: "pointer" }} title="Remove Other File">Remove</button>
                              ) : (
                                <div style={{ fontSize: 11, color: "#9ca3af", fontStyle: "italic" }}>No Other File Present</div>
                              )}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {totalPages > 1 && (
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 16px", background: "#f9fafb", borderTop: "1px solid #e5e7eb" }}>
                      <span style={{ fontSize: 11, color: "#9ca3af" }}>
                        Showing {currentPage * PAGE_SIZE + 1}-{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements.toLocaleString()}
                      </span>
                      <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                        <button onClick={() => handleSearch(currentPage - 1)} disabled={currentPage === 0} style={pagerButton(currentPage === 0)}>Prev</button>
                        <button onClick={() => handleSearch(currentPage + 1)} disabled={currentPage === totalPages - 1} style={pagerButton(currentPage === totalPages - 1)}>Next</button>
                      </div>
                    </div>
                  )}
                </>
              ) : (
                <EmptyState title="No invoice documents found" text="Try another invoice number or create a new invoice document." />
              )}
            </div>
          )}

          {!hasSearched && <EmptyState title="Search invoice documents" text="Enter an invoice number and click Search to view uploaded PDFs." />}
        </div>
      </main>

      <input type="file" ref={fileRef} accept=".pdf" style={{ display: "none" }} onChange={handleFileChange} />
    </div>
  );
};

const EmptyState = ({ title, text }: { title: string; text: string }) => (
  <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "48px 16px", color: "#9ca3af" }}>
    <div style={{ fontSize: 28, marginBottom: 10 }}>PDF</div>
    <div style={{ fontSize: 13, fontWeight: 600, color: "#6b7280" }}>{title}</div>
    <div style={{ fontSize: 11, marginTop: 4 }}>{text}</div>
  </div>
);

const sectionTitle: React.CSSProperties = { fontSize: 11, fontWeight: 600, color: "#9ca3af", textTransform: "uppercase", letterSpacing: "0.05em" };
const labelStyle: React.CSSProperties = { display: "block", fontSize: 10, fontWeight: 600, color: "#6b7280", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.04em" };
const inputStyle: React.CSSProperties = { width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "6px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 24px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 28 };
const linkButton: React.CSSProperties = { fontSize: 11, color: "#9ca3af", background: "none", border: "none", cursor: "pointer", padding: 0 };
const thStyle: React.CSSProperties = { padding: "8px 10px", textAlign: "left", fontSize: 10, fontWeight: 600, color: "#6b7280", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap", border: "1px solid #e5e7eb" };
const tdStyle: React.CSSProperties = { padding: "8px 10px", color: "#374151", whiteSpace: "nowrap", border: "1px solid #e5e7eb" };
const fileButton: React.CSSProperties = { color: "#1d4ed8", fontSize: 11, background: "none", border: "none", cursor: "pointer", padding: 0, textDecoration: "underline", textUnderlineOffset: 2 };
const iconButton: React.CSSProperties = { background: "none", border: "none", cursor: "pointer", fontSize: 14, padding: 2 };

const pagerButton = (disabled: boolean): React.CSSProperties => ({ fontSize: 11, padding: "4px 10px", border: "1px solid #e5e7eb", borderRadius: 6, background: "#fff", cursor: disabled ? "not-allowed" : "pointer", opacity: disabled ? 0.4 : 1, color: "#374151" });

export default SearchInvoice;
