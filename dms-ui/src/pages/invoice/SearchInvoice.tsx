import React from "react";
import { useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { Header } from "../../components/Header";
import { Footer } from "../../components/Footer";
import { dmsApi } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

interface InvoiceRecord {
  id: number;
  invoiceNumber: string;
  fileName: string;
  otherFileName?: string;
  invoiceFileName?: string;
  otherFileId?: number;
  filePath: string;
  createdBy: string;
  createdOn: string;
}

const PAGE_SIZE = 8;

const getInvoiceFileUrl = (id: number, action: "view" | "download", otherId?: number) => {
  const token = localStorage.getItem("jwtToken");
  const otherQuery = otherId ? `&otherId=${otherId}` : "";
  return `${import.meta.env.VITE_DMS_API}/invoice/${id}/${action}?token=${token}${otherQuery}`;
};

const SearchInvoice = () => {
  const { user, loading } = useContext(AuthContext);
  const navigate = useNavigate();

  const [selections, setSelections] = useState<Selections | null>(null);
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

  const [viewPdfUrl, setViewPdfUrl] = useState<string | null>(null);
  const [replacingInvoice, setReplacingInvoice] = useState<string | null>(null);
  const fileRef = React.useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (loading) return;
    if (!user) { navigate("/"); return; }
    const raw = sessionStorage.getItem("dms2Selections");
    if (!raw) { navigate("/dashboard"); return; }
    setSelections(JSON.parse(raw));
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
    try {
      const searchNumber = specificInvoiceNumber || invoiceNumber;
      const res = await dmsApi.get("/invoice/search", {
        params: {
          locationId: selections.loc,
          page,
          size: PAGE_SIZE,
          ...(searchNumber && { invoiceNumber: searchNumber.trim().toUpperCase() }),
        },
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
    setInvoiceNumber("");
    setResults([]);
    setTotalPages(0);
    setTotalElements(0);
    setCurrentPage(0);
    setHasSearched(false);
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
    <div style={{ display: "flex", flexDirection: "column", position: "fixed", inset: 0, background: "#f3f4f6", overflow: "hidden" }}>
      <Header
        username={`${user.firstName} ${user.lastName}`}
        userId={user.userId}
        locationName={user.locationName}
        departmentName={user.departmentName}
        applicationName={user.applicationName}
        pageTitle="Invoice Documents"
        breadcrumb={`Dashboard > Invoice Documents${selections.subApp ? ` > ${selections.subApp}` : ""}`}
        contextMeta={[
          { label: "Company", value: selections.com },
          { label: "Division", value: selections.div },
          { label: "Location", value: selections.loc },
          ...(selections.year ? [{ label: "Year", value: selections.year }] : []),
        ]}
        onCreateClick={() => navigate("/invoice/create")}
        createLabel="Create Invoice"
      />

      {toast && (
        <div style={{ position: "fixed", top: 16, right: 16, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ flex: 1, overflowY: "auto", padding: "8px 12px" }}>
        <div style={{ maxWidth: 1050, margin: "0 auto" }}>
          <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "16px 20px", marginBottom: 16, width: "fit-content", margin: "0 auto 16px auto" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
              <span style={sectionTitle}>Search Filters</span>
              <button onClick={handleReset} style={linkButton}>Clear</button>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "300px 300px", gap: 16, alignItems: "end", padding: "10px 0 4px 0" }}>
              <div style={{ position: "relative" }}>
                <label style={labelStyle}>Invoice Number</label>
                <input
                  value={invoiceNumber}
                  onChange={e => {
                    const val = e.target.value.toUpperCase();
                    setInvoiceNumber(val);
                    if (val.trim().length > 0) {
                      setShowSuggestions(true);
                      dmsApi.get("/invoice/suggest", { params: { query: val } })
                        .then(res => setSuggestions(res.data || []))
                        .catch(() => setSuggestions([]));
                    } else {
                      setShowSuggestions(false);
                      setSuggestions([]);
                    }
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
                          // Trigger search immediately upon selection
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

              <button onClick={() => handleSearch(0)} disabled={searching} style={{ ...primaryButton, opacity: searching ? 0.6 : 1, width: "100%" }}>
                {searching ? "Searching..." : "Search"}
              </button>
            </div>
          </div>

          {hasSearched && (
            <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", overflow: "hidden" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 16px", background: "#f9fafb", borderBottom: "1px solid #e5e7eb" }}>
                <span style={{ fontSize: 12, fontWeight: 600, color: "#374151" }}>
                  {results.length > 0 ? `${totalElements.toLocaleString()} records found` : "No records found"}
                </span>
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  {results.length > 0 && (
                    <button onClick={handleExportExcel} style={{ ...primaryButton, background: "#10b981", height: 26, padding: "0 16px" }}>
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
                          {["Invoice No.", "Other File", "Invoice File", "Created By", "Created On", "Actions"].map(h => (
                            <th key={h} style={thStyle}>{h}</th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {results.map((row, i) => (
                          <tr key={row.id} style={{ background: i % 2 === 0 ? "#fff" : "#fafafa" }}>
                            <td style={{ ...tdStyle, fontFamily: "monospace", fontWeight: 700, color: "#111827" }}>{row.invoiceNumber}</td>
                            <td style={{ ...tdStyle, maxWidth: 320, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={row.otherFileName}>
                              {row.otherFileName || "-"}
                            </td>
                            <td style={{ ...tdStyle, maxWidth: 320, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={row.invoiceFileName}>
                              {row.invoiceFileName ? (
                                <button onClick={() => setViewPdfUrl(getInvoiceFileUrl(row.id, "view"))} style={fileButton}>
                                  {row.invoiceFileName}
                                </button>
                              ) : "-"}
                            </td>
                            <td style={{ ...tdStyle, color: "#6b7280" }}>{row.createdBy || "-"}</td>
                            <td style={{ ...tdStyle, color: "#6b7280" }}>{row.createdOn ? new Date(row.createdOn).toLocaleString("en-GB") : "-"}</td>
                            <td style={{ ...tdStyle, textAlign: "center" }}>
                              {row.otherFileName ? (
                                <div style={{ display: "flex", gap: 8, justifyContent: "center" }}>
                                  <button onClick={() => window.open(getInvoiceFileUrl(row.id, "view", row.otherFileId), "_blank")} style={{ ...iconButton, color: "#1d4ed8" }} title="View">👁️</button>
                                  <button onClick={() => { setReplacingInvoice(row.invoiceNumber); fileRef.current?.click(); }} style={{ ...iconButton, color: "#047857" }} title="Replace">🔄</button>
                                  <button onClick={() => handleDeleteOtherFile(row.invoiceNumber)} style={{ ...iconButton, color: "#b91c1c" }} title="Delete">🗑️</button>
                                </div>
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

      {viewPdfUrl && (
        <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(0,0,0,0.5)", zIndex: 9999, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div style={{ width: "80%", height: "80%", background: "#fff", borderRadius: 8, display: "flex", flexDirection: "column", overflow: "hidden", boxShadow: "0 20px 25px -5px rgba(0, 0, 0, 0.1)" }}>
            <div style={{ padding: "12px 16px", background: "#f3f4f6", borderBottom: "1px solid #e5e7eb", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <span style={{ fontWeight: 600, color: "#374151" }}>View Invoice</span>
              <button onClick={() => setViewPdfUrl(null)} style={{ background: "none", border: "none", fontSize: 20, cursor: "pointer", color: "#6b7280" }}>&times;</button>
            </div>
            <iframe src={viewPdfUrl} style={{ width: "100%", flex: 1, border: "none" }} />
          </div>
        </div>
      )}
      <input type="file" ref={fileRef} accept=".pdf" style={{ display: "none" }} onChange={handleFileChange} />

      <Footer />
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
