import { useContext, useEffect, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { dmsApi } from "../../api/dmsApi";
import { useAppRights } from "../../hooks/useAppRights";

const searchBatch = (params: Record<string, any>) =>
  dmsApi.get("/batch/search", { params });

const loadFilterData = (locationId: string, type?: string) =>
  dmsApi.get("/batch/loadFilterData", { params: { locationId, type } });

const removeBatchFile = (batchNumber: string, fileName: string, filePath: string) =>
  dmsApi.delete("/batch/remove", { params: { batchNumber, fileName, filePath } });

const getViewFileUrl = (filePath: string) => {
  const token = localStorage.getItem("jwtToken");
  return `${import.meta.env.VITE_DMS_API}/batch/view?filePath=${encodeURIComponent(filePath)}&token=${token}`;
};

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

interface BatchRecord {
  type: string; productCode: string; productName: string;
  vendorCode: string; vendorName: string; batchNumber: string;
  subApplicationName: string; fileName: string; filePath: string;
  createdBy: string; createdOn: string;
}

const SUBTYPE_COLOR: Record<string, string> = {
  COA: "bg-blue-100 text-blue-700",
  INVOICE: "bg-purple-100 text-purple-700",
  IMAGE: "bg-green-100 text-green-700",
};

const TYPE_COLOR: Record<string, string> = {
  "Third Party": "bg-orange-100 text-orange-700",
  Own: "bg-teal-100 text-teal-700",
  "Loan License": "bg-indigo-100 text-indigo-700",
};

const Badge = ({ label, colorClass }: { label: string; colorClass: string }) => (
  <span className={`inline-block px-2 py-0.5 rounded-full text-[11px] font-medium whitespace-nowrap ${colorClass}`}>
    {label}
  </span>
);

const SearchBatch = () => {
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  const { selections } = useOutletContext<{ selections: Selections }>();

  // ── Rights — must be inside component ────────────────────────────────────
  const { canRemove } = useAppRights();

  const [batchType, setBatchType]     = useState("");
  const [vendorCode, setVendorCode]   = useState("");
  const [productCode, setProductCode] = useState("");
  const [batchNumber, setBatchNumber] = useState("");

  const [vendorOptions, setVendorOptions]   = useState<string[]>([]);
  const [productOptions, setProductOptions] = useState<string[]>([]);
  const [batchOptions, setBatchOptions]     = useState<string[]>([]);

  const [results, setResults]             = useState<BatchRecord[]>([]);
  const [totalPages, setTotalPages]       = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [currentPage, setCurrentPage]     = useState(0);
  const PAGE_SIZE = 8;

  const [searching, setSearching]         = useState(false);
  const [hasSearched, setHasSearched]     = useState(false);
  const [selected, setSelected]           = useState<Set<string>>(new Set());
  const [mailing, setMailing]             = useState(false);
  const [showMailModal, setShowMailModal] = useState(false);
  const [mailTo, setMailTo]               = useState("");
  const [recipients, setRecipients]       = useState<string[]>([]);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(t);
  }, [toast]);

  const showToast = (msg: string, type: "success" | "error" = "success") => setToast({ msg, type });

  const handleBatchTypeChange = async (type: string) => {
    setBatchType(type);
    setVendorCode(""); setProductCode(""); setBatchNumber("");
    if (!type || !selections) {
      setVendorOptions([]); setProductOptions([]); setBatchOptions([]);
      return;
    }
    try {
      const res = await loadFilterData(selections.loc, type);
      setVendorOptions(res.data.vendorCodes || []);
      setProductOptions(res.data.productCodes || []);
      setBatchOptions(res.data.batchNumbers || []);
    } catch { showToast("Failed to load filter data", "error"); }
  };

  const handleSearch = async (page = 0) => {
    if (!selections) return;
    setSearching(true); setHasSearched(true); setResults([]); setSelected(new Set());
    try {
      const res = await searchBatch({
        locationId: selections.loc, page, size: PAGE_SIZE,
        ...(batchType   && { type: batchType }),
        ...(vendorCode  && { vendorCode }),
        ...(productCode && { productCode }),
        ...(batchNumber && { batchNumber }),
      });
      setResults(res.data.content || []);
      setTotalPages(res.data.totalPages || 0);
      setTotalElements(res.data.totalElements || 0);
      setCurrentPage(page);
    } catch { showToast("Search failed", "error"); }
    finally { setSearching(false); }
  };

  const handleRemove = async (batchNum: string, fileName: string, filePath: string) => {
    if (!confirm("Are you sure you want to remove this document?")) return;
    try {
      await removeBatchFile(batchNum, fileName, filePath);
      showToast("File removed successfully");
      handleSearch(currentPage);
    } catch { showToast("Failed to remove file", "error"); }
  };

  const handleReset = () => {
    setBatchType(""); setVendorCode(""); setProductCode(""); setBatchNumber("");
    setVendorOptions([]); setProductOptions([]); setBatchOptions([]);
    setResults([]); setHasSearched(false); setSelected(new Set());
  };

  const toggleSelect = (filePath: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(filePath)) { next.delete(filePath); }
      else {
        if (next.size >= 10) { showToast("Maximum 10 files can be selected", "error"); return prev; }
        next.add(filePath);
      }
      return next;
    });
  };

  const handleZipAndMail = async () => {
    if (recipients.length === 0) { showToast("Please add at least one recipient", "error"); return; }
    if (selected.size === 0) return;
    setMailing(true); setShowMailModal(false);
    try {
      await dmsApi.post("/batch/zipAndMail", { filePaths: Array.from(selected), recipients }, { timeout: 60000 });
      showToast(`${selected.size} file(s) zipped and mailed to ${recipients.length} recipient(s)!`);
      setSelected(new Set()); setRecipients([]); setMailTo("");
    } catch (err: any) {
      if (err.code === "ECONNABORTED") showToast("Request timed out — but email may have been sent.");
      else showToast("Failed to zip and mail files", "error");
    } finally { setMailing(false); }
  };

  const handleAddRecipient = () => {
    const email = mailTo.trim();
    if (!email) return;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { showToast("Invalid email address", "error"); return; }
    if (recipients.includes(email)) { showToast("Email already added", "error"); return; }
    if (recipients.length >= 5) { showToast("Maximum 5 recipients allowed", "error"); return; }
    setRecipients((prev) => [...prev, email]);
    setMailTo("");
  };

  const handleRecipientKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") { e.preventDefault(); handleAddRecipient(); }
  };

  if (!user || !selections) return null;

  return (
    <main style={{ flex: 1, overflowY: "auto", padding: "8px 12px" }}>
      <div style={{ maxWidth: 1200, margin: "0 auto" }}>

        {/* Toast */}
        {toast && (
          <div style={{ position: "fixed", top: 16, right: 16, zIndex: 50, display: "flex", alignItems: "center", gap: 6, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
            {toast.type === "success" ? "✓" : "✕"} {toast.msg}
          </div>
        )}

        {/* Filter card */}
        <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "8px 12px", marginBottom: 8 }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 6 }}>
            <span style={{ fontSize: 11, fontWeight: 600, color: "#9ca3af", textTransform: "uppercase", letterSpacing: "0.05em" }}>Search Filters</span>
            <button onClick={handleReset} style={{ fontSize: 11, color: "#9ca3af", background: "none", border: "none", cursor: "pointer", padding: 0 }}>✕ Clear</button>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr 1fr", gap: 10 }}>
            <div>
              <div style={{ fontSize: 10, fontWeight: 600, color: "#6b7280", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.04em" }}>Batch Type</div>
              <select value={batchType} onChange={(e) => handleBatchTypeChange(e.target.value)}
                style={{ width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "5px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none" }}>
                <option value="">All Types</option>
                <option value="Third Party">Third Party</option>
                <option value="Own">Own</option>
                <option value="Loan License">Loan License</option>
              </select>
            </div>
            <div>
              <div style={{ fontSize: 10, fontWeight: 600, color: "#6b7280", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.04em" }}>Vendor Code</div>
              <input list="vendorList" value={vendorCode} onChange={(e) => setVendorCode(e.target.value.toUpperCase())} placeholder="Type or select..."
                style={{ width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "5px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" }} />
              <datalist id="vendorList">{vendorOptions.map((v) => <option key={v} value={v} />)}</datalist>
            </div>
            <div>
              <div style={{ fontSize: 10, fontWeight: 600, color: "#6b7280", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.04em" }}>Product Code</div>
              <input list="productList" value={productCode} onChange={(e) => setProductCode(e.target.value.toUpperCase())} placeholder="Type or select..."
                style={{ width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "5px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" }} />
              <datalist id="productList">{productOptions.map((p) => <option key={p} value={p} />)}</datalist>
            </div>
            <div>
              <div style={{ fontSize: 10, fontWeight: 600, color: "#6b7280", marginBottom: 4, textTransform: "uppercase", letterSpacing: "0.04em" }}>Batch No.</div>
              <input list="batchList" value={batchNumber} onChange={(e) => setBatchNumber(e.target.value.toUpperCase())} placeholder="Type or select..."
                style={{ width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "5px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" }} />
              <datalist id="batchList">{batchOptions.map((b) => <option key={b} value={b} />)}</datalist>
            </div>
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 8 }}>
            <button onClick={() => handleSearch(0)} disabled={searching}
              style={{ background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "6px 24px", fontSize: 12, fontWeight: 500, cursor: "pointer", opacity: searching ? 0.6 : 1 }}>
              {searching ? "Searching..." : "Search"}
            </button>
          </div>
        </div>

        {/* Results */}
        {hasSearched && (
          <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", overflow: "hidden" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 16px", background: "#f9fafb", borderBottom: "1px solid #e5e7eb" }}>
              <span style={{ fontSize: 12, fontWeight: 600, color: "#374151" }}>
                {results.length > 0 ? `${totalElements.toLocaleString()} records found` : "No records found"}
              </span>
              {results.length > 0 && <span style={{ fontSize: 11, color: "#9ca3af" }}>Page {currentPage + 1} of {totalPages}</span>}
            </div>

            {results.length > 0 ? (
              <>
                <div style={{ overflowX: "auto", overflowY: "auto", maxHeight: "calc(100vh - 320px)" }}>
                  <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                    <thead>
                      <tr style={{ borderBottom: "1px solid #e5e7eb", background: "#f9fafb" }}>
                        {[
                          "", "Type", "Product Code", "Product Name",
                          "Vendor Code", "Vendor Name", "Batch No.",
                          "Sub Type", "File", "Uploaded By", "Date",
                          ...(canRemove ? [""] : []),   // Action col only if canRemove
                        ].map((h, i) => (
                          <th key={i} style={{ padding: "8px 10px", textAlign: "left", fontSize: 10, fontWeight: 600, color: "#6b7280", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap" }}>
                            {h}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {results.map((row, i) => {
                        const prev = results[i - 1];
                        const isNew = !prev || prev.batchNumber !== row.batchNumber;
                        return (
                          <tr key={i} style={{ borderBottom: "1px solid #f3f4f6", background: i % 2 === 0 ? "#fff" : "#fafafa" }}>
                            <td style={{ padding: "7px 8px", width: 32 }}>
                              {row.fileName && (
                                <input type="checkbox" checked={selected.has(row.filePath)} onChange={() => toggleSelect(row.filePath)}
                                  style={{ cursor: "pointer", accentColor: "#003366", width: 13, height: 13 }} />
                              )}
                            </td>
                            <td style={{ padding: "7px 10px", whiteSpace: "nowrap" }}>
                              {isNew && row.type && <Badge label={row.type} colorClass={TYPE_COLOR[row.type] || "bg-gray-100 text-gray-600"} />}
                            </td>
                            <td style={{ padding: "7px 10px", fontFamily: "monospace", fontSize: 11, color: "#374151", whiteSpace: "nowrap" }}>{isNew ? row.productCode : ""}</td>
                            <td style={{ padding: "7px 10px", color: "#374151", maxWidth: 140, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={row.productName}>{isNew ? row.productName : ""}</td>
                            <td style={{ padding: "7px 10px", fontFamily: "monospace", fontSize: 11, color: "#374151", whiteSpace: "nowrap" }}>{isNew ? row.vendorCode : ""}</td>
                            <td style={{ padding: "7px 10px", color: "#374151", maxWidth: 140, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={row.vendorName}>{isNew ? row.vendorName : ""}</td>
                            <td style={{ padding: "7px 10px", fontFamily: "monospace", fontSize: 11, fontWeight: 600, color: "#111827", whiteSpace: "nowrap" }}>{isNew ? row.batchNumber : ""}</td>
                            <td style={{ padding: "7px 10px", whiteSpace: "nowrap" }}>
                              <Badge label={row.subApplicationName} colorClass={SUBTYPE_COLOR[row.subApplicationName] || "bg-gray-100 text-gray-600"} />
                            </td>
                            <td style={{ padding: "7px 10px", whiteSpace: "nowrap", maxWidth: 160, overflow: "hidden", textOverflow: "ellipsis" }}>
                              {row.fileName
                                ? <button onClick={() => window.open(getViewFileUrl(row.filePath), "_blank")}
                                    style={{ color: "#1d4ed8", fontSize: 11, background: "none", border: "none", cursor: "pointer", padding: 0, textDecoration: "underline", textUnderlineOffset: 2 }}>
                                    📄 {row.fileName}
                                  </button>
                                : <span style={{ color: "#d1d5db" }}>—</span>}
                            </td>
                            <td style={{ padding: "7px 10px", fontSize: 11, color: "#9ca3af", whiteSpace: "nowrap" }}>{row.createdBy}</td>
                            <td style={{ padding: "7px 10px", fontSize: 11, color: "#9ca3af", whiteSpace: "nowrap" }}>
                              {row.createdOn ? new Date(row.createdOn).toLocaleDateString("en-GB") : "—"}
                            </td>
                            {/* Remove — only rendered if user has Remove right */}
                            {canRemove && (
                              <td style={{ padding: "7px 10px", whiteSpace: "nowrap" }}>
                                {row.fileName && (
                                  <button onClick={() => handleRemove(row.batchNumber, row.fileName, row.filePath)}
                                    style={{ color: "#ef4444", fontSize: 11, background: "none", border: "none", cursor: "pointer", padding: 0, fontWeight: 500 }}>
                                    Remove
                                  </button>
                                )}
                              </td>
                            )}
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {totalPages > 1 && (
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "8px 16px", background: "#f9fafb", borderTop: "1px solid #e5e7eb" }}>
                    <span style={{ fontSize: 11, color: "#9ca3af" }}>
                      Showing {currentPage * PAGE_SIZE + 1}–{Math.min((currentPage + 1) * PAGE_SIZE, totalElements)} of {totalElements.toLocaleString()}
                    </span>
                    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                      <button onClick={() => handleSearch(currentPage - 1)} disabled={currentPage === 0}
                        style={{ fontSize: 11, padding: "4px 10px", border: "1px solid #e5e7eb", borderRadius: 6, background: "#fff", cursor: currentPage === 0 ? "not-allowed" : "pointer", opacity: currentPage === 0 ? 0.4 : 1, color: "#374151" }}>
                        ← Prev
                      </button>
                      {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                        let start = Math.max(0, currentPage - 3);
                        const end = Math.min(totalPages, start + 7);
                        start = Math.max(0, end - 7);
                        const page = start + i;
                        return (
                          <button key={page} onClick={() => handleSearch(page)}
                            style={{ width: 28, height: 28, fontSize: 11, border: "1px solid", borderRadius: 6, cursor: "pointer", fontWeight: page === currentPage ? 600 : 400,
                              background: page === currentPage ? "#003366" : "#fff",
                              color: page === currentPage ? "#fff" : "#374151",
                              borderColor: page === currentPage ? "#003366" : "#e5e7eb" }}>
                            {page + 1}
                          </button>
                        );
                      })}
                      <button onClick={() => handleSearch(currentPage + 1)} disabled={currentPage === totalPages - 1}
                        style={{ fontSize: 11, padding: "4px 10px", border: "1px solid #e5e7eb", borderRadius: 6, background: "#fff", cursor: currentPage === totalPages - 1 ? "not-allowed" : "pointer", opacity: currentPage === totalPages - 1 ? 0.4 : 1, color: "#374151" }}>
                        Next →
                      </button>
                    </div>
                  </div>
                )}
              </>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "48px 16px", color: "#9ca3af" }}>
                <div style={{ fontSize: 32, marginBottom: 12 }}>🔍</div>
                <div style={{ fontSize: 13, fontWeight: 500, color: "#6b7280" }}>No records found</div>
                <div style={{ fontSize: 11, marginTop: 4 }}>Try adjusting your search filters</div>
              </div>
            )}
          </div>
        )}

        {!hasSearched && (
          <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "48px 16px", color: "#9ca3af" }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>📋</div>
            <div style={{ fontSize: 13, color: "#6b7280" }}>Select filters and click Search to view batch records</div>
          </div>
        )}
      </div>

      {/* Floating selection bar */}
      {selected.size > 0 && (
        <div style={{ position: "fixed", bottom: 32, left: "50%", transform: "translateX(-50%)", zIndex: 50, background: "#003366", borderRadius: 10, padding: "10px 16px", display: "flex", alignItems: "center", gap: 14, boxShadow: "0 4px 20px rgba(0,0,0,0.25)" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <div style={{ background: "rgba(255,255,255,0.15)", borderRadius: 99, padding: "2px 10px", fontSize: 12, fontWeight: 700, color: "#fff" }}>{selected.size}/10</div>
            <span style={{ fontSize: 12, color: "rgba(255,255,255,0.8)" }}>{selected.size === 1 ? "file selected" : "files selected"}</span>
          </div>
          <div style={{ width: 1, height: 20, background: "rgba(255,255,255,0.2)" }} />
          <button onClick={() => setSelected(new Set())} style={{ fontSize: 11, color: "rgba(255,255,255,0.6)", background: "none", border: "none", cursor: "pointer", padding: 0 }}>Clear</button>
          <button onClick={() => setShowMailModal(true)} disabled={mailing}
            style={{ background: "#fff", color: "#003366", border: "none", borderRadius: 7, padding: "6px 16px", fontSize: 12, fontWeight: 600, cursor: mailing ? "not-allowed" : "pointer", opacity: mailing ? 0.7 : 1 }}>
            {mailing ? "Processing..." : "📦 Zip & Mail"}
          </button>
        </div>
      )}

      {/* Email modal */}
      {showMailModal && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center" }}
          onClick={() => { setShowMailModal(false); setMailTo(""); setRecipients([]); }}>
          <div style={{ background: "#fff", borderRadius: 12, padding: "20px 24px", width: 380, boxShadow: "0 8px 32px rgba(0,0,0,0.2)" }} onClick={(e) => e.stopPropagation()}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
              <div>
                <div style={{ fontSize: 14, fontWeight: 600, color: "#111827" }}>Send via Email</div>
                <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 2 }}>{selected.size} file(s) · max 5 recipients</div>
              </div>
              <button onClick={() => { setShowMailModal(false); setMailTo(""); setRecipients([]); }}
                style={{ background: "none", border: "none", cursor: "pointer", fontSize: 16, color: "#9ca3af", padding: 0 }}>✕</button>
            </div>
            <div style={{ marginBottom: 10 }}>
              <label style={{ display: "block", fontSize: 11, fontWeight: 600, color: "#374151", textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 5 }}>
                Recipient Email ({recipients.length}/5)
              </label>
              <div style={{ display: "flex", gap: 6 }}>
                <input type="email" value={mailTo} onChange={(e) => setMailTo(e.target.value)} onKeyDown={handleRecipientKeyDown}
                  placeholder="Enter email and press Enter" disabled={recipients.length >= 5} autoFocus
                  style={{ flex: 1, border: "1px solid #e5e7eb", borderRadius: 7, padding: "7px 10px", fontSize: 12, color: "#111827", outline: "none", opacity: recipients.length >= 5 ? 0.5 : 1 }} />
                <button onClick={handleAddRecipient} disabled={recipients.length >= 5}
                  style={{ padding: "7px 12px", background: "#003366", color: "#fff", border: "none", borderRadius: 7, fontSize: 12, fontWeight: 600, cursor: recipients.length >= 5 ? "not-allowed" : "pointer", opacity: recipients.length >= 5 ? 0.5 : 1 }}>
                  Add
                </button>
              </div>
              <div style={{ fontSize: 10, color: "#9ca3af", marginTop: 3 }}>Press Enter or click Add</div>
            </div>
            {recipients.length > 0 && (
              <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 14, padding: "8px 10px", background: "#f9fafb", borderRadius: 7, border: "1px solid #e5e7eb" }}>
                {recipients.map((r, i) => (
                  <div key={i} style={{ display: "flex", alignItems: "center", gap: 5, background: "#e0e7ff", borderRadius: 99, padding: "3px 8px 3px 10px", fontSize: 11, color: "#3730a3" }}>
                    {r}
                    <button onClick={() => setRecipients((prev) => prev.filter((_, j) => j !== i))}
                      style={{ background: "none", border: "none", cursor: "pointer", padding: 0, fontSize: 12, color: "#6366f1" }}>✕</button>
                  </div>
                ))}
              </div>
            )}
            <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
              <button onClick={() => { setShowMailModal(false); setMailTo(""); setRecipients([]); }}
                style={{ padding: "7px 16px", fontSize: 12, fontWeight: 500, border: "1px solid #e5e7eb", borderRadius: 7, background: "#fff", color: "#374151", cursor: "pointer" }}>
                Cancel
              </button>
              <button onClick={handleZipAndMail} disabled={recipients.length === 0}
                style={{ padding: "7px 20px", fontSize: 12, fontWeight: 600, border: "none", borderRadius: 7, background: recipients.length > 0 ? "#003366" : "#e5e7eb", color: recipients.length > 0 ? "#fff" : "#9ca3af", cursor: recipients.length > 0 ? "pointer" : "not-allowed" }}>
                📦 Send ZIP
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
};

export default SearchBatch;