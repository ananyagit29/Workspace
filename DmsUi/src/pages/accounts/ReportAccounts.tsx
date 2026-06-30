import React, { useContext, useEffect, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import {
  getAccountsDaybooks,
  getAccountsPartyNames,
  getAccountsMissing,
  getAccountsReport,
  downloadAccountsZip,
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

const REPORT_TYPES = [
  { value: "", label: "SELECT" },
  { value: "getaccountcodedetails", label: "Account Code Wise" },
  { value: "getpartydetails", label: "Party Wise" },
  { value: "getmissingdocuments", label: "Missing Documents" },
];

const ReportAccounts: React.FC = () => {
  const { user } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();

  const [daybooks, setDaybooks] = useState<{ code: string; name: string }[]>([]);
  const [selectedDaybook, setSelectedDaybook] = useState("");
  const [reportType, setReportType] = useState("");
  const [partyNames, setPartyNames] = useState<{ code: string; name: string }[]>([]);
  const [selectedParty, setSelectedParty] = useState("");
  const [accCode, setAccCode] = useState("");
  const [amountMoreThan, setAmountMoreThan] = useState("0");
  const [fromMonth, setFromMonth] = useState("04");
  const [toMonth, setToMonth] = useState("03");
  const [results, setResults] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);
  const [selectedDocs, setSelectedDocs] = useState<Set<string>>(new Set());

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

  // Load party names when report type is "Party Wise"
  useEffect(() => {
    if (reportType !== "getpartydetails" || !selectedDaybook || !selections) return;
    const daybookCode = selectedDaybook.split("~")[0];
    getAccountsPartyNames({ daybookCode, financialYear: selections.year, locationId: selections.loc })
      .then(res => {
        const data = (res.data || []).map((r: any) => ({
          code: String(r.ACCOUNT_CODE || ""),
          name: String(r.ACCOUNT_NAME || ""),
        }));
        setPartyNames(data);
      })
      .catch(() => setPartyNames([]));
  }, [reportType, selectedDaybook, selections]);

  const handleReportTypeChange = (val: string) => {
    setReportType(val);
    setResults([]);
    setCurrentPage(1);
    setSelectedDocs(new Set());
    setSelectedParty("");
    setAccCode("");
    setAmountMoreThan("0");
    setFromMonth("04");
    setToMonth("03");
  };

  const handleGo = () => {
    if (!selectedDaybook || !selections) return;
    if (!reportType) {
      showToast("Please select a report type", "error");
      return;
    }
    const daybookCode = selectedDaybook.split("~")[0];
    setLoading(true);
    setResults([]);
    setCurrentPage(1);
    setSelectedDocs(new Set());

    if (reportType === "getmissingdocuments") {
      getAccountsMissing({ locationId: selections.loc, daybookCode, year: selections.year })
        .then(res => setResults(res.data || []))
        .catch(() => showToast("Failed to fetch missing documents", "error"))
        .finally(() => setLoading(false));
    } else {
      getAccountsReport({
        locationId: selections.loc,
        daybookCode,
        year: selections.year,
        fromMonth,
        toMonth,
        accName: reportType === "getpartydetails" ? selectedParty : undefined,
        accCode: reportType === "getaccountcodedetails" ? accCode : undefined,
        amountMoreThan: amountMoreThan || "0",
      })
        .then(res => setResults(res.data || []))
        .catch(() => showToast("Failed to fetch report", "error"))
        .finally(() => setLoading(false));
    }
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

  const handleDownloadSelected = async () => {
    if (selectedDocs.size === 0) return;
    const docs = Array.from(selectedDocs).map(d => JSON.parse(d));
    if (docs.length === 1) {
      const { daybookCode, docCode, fileName } = docs[0];
      const url = `${import.meta.env.VITE_DMS_API}accounts/view-file?daybookCode=${daybookCode}&docCode=${docCode}&fileName=${encodeURIComponent(fileName)}&download=true`;
      const token = localStorage.getItem("jwtToken");
      fetch(url, { headers: { Authorization: `Bearer ${token}` } })
        .then(res => res.blob())
        .then(blob => {
          const blobUrl = URL.createObjectURL(blob);
          const a = document.createElement("a");
          a.href = blobUrl;
          a.download = fileName;
          a.click();
          URL.revokeObjectURL(blobUrl);
        })
        .catch(() => showToast("Failed to download file", "error"));
    } else {
      setLoading(true);
      try {
        const res = await downloadAccountsZip(docs);
        const blobUrl = URL.createObjectURL(res.data);
        const a = document.createElement("a");
        a.href = blobUrl;
        a.download = "Accounts_Documents.zip";
        a.click();
        URL.revokeObjectURL(blobUrl);
        showToast("Zip downloaded successfully", "success");
      } catch (e) {
        showToast("Failed to download zip", "error");
      } finally {
        setLoading(false);
      }
    }
  };

  const handleExportExcel = () => {
    if (results.length === 0) return;
    import("xlsx").then((XLSX) => {
      let data: any[];
      if (reportType === "getmissingdocuments") {
        data = results.map((r: any) => ({
          "Document Number": r.DOC_CODE || r.doc_code || "",
          "Doc Date": r.DOC_DATE ? new Date(r.DOC_DATE).toLocaleDateString("en-GB") : (r.doc_date ? new Date(r.doc_date).toLocaleDateString("en-GB") : ""),
          "FAS User": r.FAS_USER || r.fas_user || "",
        }));
      } else {
        data = results.map((r: any) => ({
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
      }
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Report");
      const filename = reportType === "getmissingdocuments" ? "MissingDocumentsReport.xlsx"
        : reportType === "getpartydetails" ? "PartyWiseReport.xlsx" : "AccountCodeWiseReport.xlsx";
      XLSX.writeFile(wb, filename);
    });
  };

  if (!user || !selections) return null;

  const showPartyFilter = reportType === "getpartydetails";
  const showAccCodeFilter = reportType === "getaccountcodedetails";
  const showAmountMonth = showPartyFilter || showAccCodeFilter;
  const isMissing = reportType === "getmissingdocuments";

  return (
    <div style={{ flex: 1, overflow: "auto", background: "#f3f4f6" }}>
      {toast && (
        <div style={{ position: "fixed", bottom: 24, right: 24, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "16px 24px" }}>
        <div style={{ maxWidth: 1200, margin: "0 auto" }}>
          {/* Filters */}
          <div style={{ maxWidth: 900, margin: "0 auto", background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "16px 24px", marginBottom: 12 }}>
            <h2 style={{ fontSize: 16, fontWeight: 600, color: "#003366", margin: "0 0 20px 0", textAlign: "center" }}>
              Accounts Document Report
            </h2>

            {/* Grid layout for filters */}
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px 24px" }}>

              {/* Line 1 */}
              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <label style={{ ...labelStyle, minWidth: 160, marginBottom: 0 }}>Select Daybook Code</label>
                <select value={selectedDaybook} onChange={e => { setSelectedDaybook(e.target.value); setResults([]); }} style={{ ...inputStyle, flex: 1 }}>
                  {daybooks.map(d => (
                    <option key={d.code} value={`${d.code}~${d.name}`}>{d.code}~{d.name}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                <label style={{ ...labelStyle, minWidth: 160, marginBottom: 0 }}>Select Report Type</label>
                <select value={reportType} onChange={e => handleReportTypeChange(e.target.value)} style={{ ...inputStyle, flex: 1 }}>
                  {REPORT_TYPES.map(t => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>

              {/* Line 2 (Conditional) */}

              {/* Col 1 of Line 2: Account Code / Name */}
              {showPartyFilter && (
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <label style={{ ...labelStyle, minWidth: 160, marginBottom: 0 }}>Select Account Name</label>
                  <select value={selectedParty} onChange={e => setSelectedParty(e.target.value)} style={{ ...inputStyle, flex: 1 }}>
                    <option value="">SELECT</option>
                    {partyNames.map(p => (
                      <option key={p.code} value={p.code}>{p.name}</option>
                    ))}
                  </select>
                </div>
              )}
              {showAccCodeFilter && (
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <label style={{ ...labelStyle, minWidth: 160, marginBottom: 0 }}>Enter Account Code</label>
                  <input type="number" value={accCode} onChange={e => setAccCode(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
                </div>
              )}

              {/* Col 2 of Line 2: Amount More Than */}
              {showAmountMonth && (
                <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                  <label style={{ ...labelStyle, minWidth: 160, marginBottom: 0 }}>Amount More Than</label>
                  <input type="number" value={amountMoreThan} onChange={e => setAmountMoreThan(e.target.value)} style={{ ...inputStyle, flex: 1 }} />
                </div>
              )}

              {/* Line 3: From Month, To Month (Left), Go (Right) */}
              {showAmountMonth && (
                <div style={{ gridColumn: "1 / -1", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                  <div style={{ display: "flex", alignItems: "center", gap: 24 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <label style={{ ...labelStyle, minWidth: 160, marginBottom: 0 }}>From Month</label>
                      <select value={fromMonth} onChange={e => setFromMonth(e.target.value)} style={{ ...inputStyle, width: 140 }}>
                        {MONTHS.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
                      </select>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", gap: 12 }}>
                      <label style={{ ...labelStyle, minWidth: 80, marginBottom: 0 }}>To Month</label>
                      <select value={toMonth} onChange={e => setToMonth(e.target.value)} style={{ ...inputStyle, width: 140 }}>
                        {MONTHS.map(m => <option key={m.value} value={m.value}>{m.label}</option>)}
                      </select>
                    </div>
                  </div>
                  <button onClick={handleGo} disabled={loading} style={{ ...primaryButton, opacity: loading ? 0.6 : 1, padding: "5px 40px" }}>
                    {loading ? "Loading..." : "Go"}
                  </button>
                </div>
              )}

              {/* If Missing Documents, render Go button in the grid */}
              {isMissing && (
                <div style={{ display: "flex", alignItems: "center", justifyContent: "flex-end", gridColumn: "1 / -1" }}>
                  <button onClick={handleGo} disabled={loading} style={{ ...primaryButton, opacity: loading ? 0.6 : 1, padding: "8px 40px", marginTop: 10 }}>
                    {loading ? "Loading..." : "Go"}
                  </button>
                </div>
              )}
            </div>


          </div>

          {/* Results */}
          {results.length > 0 && (
            <div>
              <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 8, gap: 8 }}>
                {!isMissing && (
                  <button onClick={handleDownloadSelected} disabled={selectedDocs.size === 0 || loading} style={{ ...primaryButton, opacity: selectedDocs.size === 0 || loading ? 0.6 : 1, background: "#003366" }}>
                    Download
                  </button>
                )}
                <button onClick={handleExportExcel} style={primaryButton}>Save As Excel</button>
              </div>
              <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", overflow: isMissing ? "auto" : "hidden", maxHeight: isMissing ? "calc(100vh - 320px)" : "none" }}>
                {isMissing ? (
                  <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                    <thead>
                      <tr style={{ background: "#f9fafb", position: "sticky", top: 0, zIndex: 1 }}>
                        <th style={thStyle}>Document Number</th>
                        <th style={thStyle}>Doc Date</th>
                        <th style={thStyle}>FAS User</th>
                      </tr>
                    </thead>
                    <tbody>
                      {results.map((r: any, idx: number) => (
                        <tr key={idx}
                          onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"}
                          onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                          <td style={tdStyle}>{r.DOC_CODE || r.doc_code || ""}</td>
                          <td style={tdStyle}>{r.DOC_DATE ? new Date(r.DOC_DATE).toLocaleDateString("en-GB") : (r.doc_date ? new Date(r.doc_date).toLocaleDateString("en-GB") : "")}</td>
                          <td style={tdStyle}>{r.FAS_USER || r.fas_user || ""}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <>
                    <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
                      <thead>
                        <tr style={{ background: "#f9fafb", position: "sticky", top: 0, zIndex: 1 }}>
                          <th style={thStyle}>Doc Code</th>
                          <th style={thStyle}>Doc Date</th>
                          <th style={thStyle}>Account Name</th>
                          <th style={thStyle}>Filename</th>
                          <th style={{ ...thStyle, textAlign: "center" }}>
                            Download<br />
                            <span style={{ fontSize: 10, color: "#333", fontWeight: "normal" }}>(Max 100 files)</span><br />
                            <input
                              type="checkbox"
                              onChange={e => {
                                const pageFiles = results.slice((currentPage - 1) * 5, currentPage * 5)
                                  .filter((r: any) => r.FILE_NAME)
                                  .map((r: any) => JSON.stringify({ daybookCode: r.DAYBOOK_CODE, docCode: r.DOC_CODE, fileName: r.FILE_NAME }));
                                const newSet = new Set(selectedDocs);
                                if (e.target.checked) {
                                  for (const f of pageFiles) {
                                    if (!newSet.has(f)) {
                                      if (newSet.size >= 100) {
                                        alert("Maximum 100 files can be selected at a time.");
                                        break;
                                      }
                                      newSet.add(f);
                                    }
                                  }
                                } else {
                                  pageFiles.forEach(f => newSet.delete(f));
                                }
                                setSelectedDocs(newSet);
                              }}
                            />
                          </th>
                          <th style={thStyle}>Bill No.</th>
                          <th style={thStyle}>Bill Date</th>
                          <th style={thStyle}>Trans Amt</th>
                          <th style={thStyle}>Created By</th>
                          <th style={thStyle}>Created On</th>
                        </tr>
                      </thead>
                      <tbody>
                        {results.slice((currentPage - 1) * 5, currentPage * 5).map((r: any, idx: number) => (
                          <tr key={idx}
                            onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"}
                            onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                            <td style={{ ...tdStyle, textAlign: "right" }}>{r.DOC_CODE || ""}</td>
                            <td style={tdStyle}>{r.DOC_DATE ? new Date(r.DOC_DATE).toLocaleDateString("en-GB") : ""}</td>
                            <td style={{ ...tdStyle, minWidth: 200 }}>{r.ACCOUNT_NAME || ""}</td>
                            <td style={tdStyle}>
                              {r.FILE_NAME ? (
                                <button onClick={() => handleViewFile(r.DAYBOOK_CODE || "", r.DOC_CODE || "", r.FILE_NAME)} style={fileButton}>
                                  {r.FILE_NAME}
                                </button>
                              ) : ""}
                            </td>
                            <td style={{ ...tdStyle, textAlign: "center" }}>
                              {r.FILE_NAME && (
                                <input
                                  type="checkbox"
                                  checked={selectedDocs.has(JSON.stringify({ daybookCode: r.DAYBOOK_CODE, docCode: r.DOC_CODE, fileName: r.FILE_NAME }))}
                                  onChange={e => {
                                    const id = JSON.stringify({ daybookCode: r.DAYBOOK_CODE, docCode: r.DOC_CODE, fileName: r.FILE_NAME });
                                    const newSet = new Set(selectedDocs);
                                    if (e.target.checked) {
                                      if (newSet.size >= 100) {
                                        alert("Maximum 100 files can be selected at a time.");
                                        return;
                                      }
                                      newSet.add(id);
                                    } else {
                                      newSet.delete(id);
                                    }
                                    setSelectedDocs(newSet);
                                  }}
                                />
                              )}
                            </td>
                            <td style={tdStyle}>{r.BILL_NUMBER || ""}</td>
                            <td style={tdStyle}>{r.BILL_DATE ? new Date(r.BILL_DATE).toLocaleDateString("en-GB") : ""}</td>
                            <td style={{ ...tdStyle, textAlign: "right" }}>{r.TRAN_AMOUNT != null ? r.TRAN_AMOUNT : ""}</td>
                            <td style={tdStyle}>{r.CREATED_BY || ""}</td>
                            <td style={tdStyle}>{r.CREATED_ON || ""}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    {results.length > 0 && (
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", padding: "12px 16px", borderTop: "1px solid #e5e7eb", background: "#fff" }}>
                        <button
                          onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                          disabled={currentPage === 1}
                          style={{ ...primaryButton, padding: "4px 12px", background: currentPage === 1 ? "#d1d5db" : "#003366", color: currentPage === 1 ? "#6b7280" : "#fff" }}>
                          Previous
                        </button>
                        <span style={{ fontSize: 12, color: "#374151" }}>
                          Page {currentPage} of {Math.max(1, Math.ceil(results.length / 5))}
                        </span>
                        <button
                          onClick={() => setCurrentPage(p => Math.min(Math.ceil(results.length / 5), p + 1))}
                          disabled={currentPage === Math.ceil(results.length / 5) || results.length === 0}
                          style={{ ...primaryButton, padding: "4px 12px", background: (currentPage === Math.ceil(results.length / 5) || results.length === 0) ? "#d1d5db" : "#003366", color: (currentPage === Math.ceil(results.length / 5) || results.length === 0) ? "#6b7280" : "#fff" }}>
                          Next
                        </button>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          )}

          {!loading && results.length === 0 && reportType && (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "48px 16px", color: "#333" }}>
              <div style={{ fontSize: 28, marginBottom: 10 }}>📊</div>
              <div style={{ fontSize: 13, fontWeight: 600, color: "#333" }}>No data</div>
              <div style={{ fontSize: 11, marginTop: 4 }}>Select report type and click "Go" to view results.</div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

const labelStyle: React.CSSProperties = { display: "block", fontSize: 11, fontWeight: 600, color: "#333", textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 4 };
const inputStyle: React.CSSProperties = { width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "6px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" as const };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 24px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 32 };
const thStyle: React.CSSProperties = { padding: "8px 10px", textAlign: "left", fontSize: 10, fontWeight: 600, color: "#333", textTransform: "uppercase", letterSpacing: "0.04em", whiteSpace: "nowrap", border: "1px solid #e5e7eb", background: "#f9fafb" };
const tdStyle: React.CSSProperties = { padding: "8px 10px", color: "#374151", whiteSpace: "nowrap", border: "1px solid #e5e7eb" };
const fileButton: React.CSSProperties = { color: "#1d4ed8", fontSize: 11, background: "none", border: "none", cursor: "pointer", padding: 0, textDecoration: "underline", textUnderlineOffset: 2 };

export default ReportAccounts;
