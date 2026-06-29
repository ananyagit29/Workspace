import React, { useContext, useEffect, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { getMissingInvoices } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const ReportInvoice = () => {
  const { user } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();
  const [results, setResults] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  useEffect(() => {
    if (!selections) return;
    setLoading(true);
    getMissingInvoices(selections.loc, selections.year)
      .then(res => {
        setResults(res.data || []);
      })
      .catch(() => showToast("Failed to load missing invoices", "error"))
      .finally(() => setLoading(false));
  }, [selections]);

  const handleExportExcel = () => {
    if (results.length === 0) return;
    import("xlsx").then((XLSX) => {
      const data = results.map(inv => ({
        "Missing Invoice Numbers": inv
      }));
      const ws = XLSX.utils.json_to_sheet(data);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, "Missing_Invoices");
      XLSX.writeFile(wb, "Missing_Invoice_Documents.xlsx");
    });
  };

  if (!user || !selections) return null;

  return (
    <div style={{ background: "#f3f4f6", minHeight: "100vh" }}>
      {toast && (
        <div style={{ position: "fixed", bottom: 24, right: 24, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "24px 32px" }}>
        <div style={{ maxWidth: 800, margin: "0 auto" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 16 }}>
            <h2 style={{ fontSize: 16, fontWeight: 600, color: "#003366", margin: 0 }}>Invoice Documents Report</h2>
            <button onClick={handleExportExcel} style={{ ...primaryButton, opacity: loading || results.length === 0 ? 0.6 : 1 }} disabled={loading || results.length === 0}>
              Save As Excel
            </button>
          </div>

          <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: 0, maxHeight: "calc(100vh - 200px)", overflowY: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 12 }}>
              <thead>
                <tr style={{ background: "#f9fafb" }}>
                  <th style={thStyle}>Missing Invoice Numbers</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td style={{ padding: "32px", textAlign: "center", color: "#6b7280" }}>
                      <div style={{ display: "inline-block", width: 20, height: 20, border: "2px solid #e5e7eb", borderTopColor: "#003366", borderRadius: "50%", animation: "spin 1s linear infinite" }} />
                    </td>
                  </tr>
                ) : results.length > 0 ? (
                  results.map((invoice, idx) => (
                    <tr key={idx} style={{ transition: "background 0.15s" }} onMouseEnter={e => e.currentTarget.style.background = "#f3f4f6"} onMouseLeave={e => e.currentTarget.style.background = "transparent"}>
                      <td style={tdStyle}>{invoice}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td style={{ padding: "32px", textAlign: "center", color: "#6b7280" }}>No missing invoices found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
};

const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 24px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 28 };
const thStyle: React.CSSProperties = { position: "sticky", top: 0, zIndex: 1, background: "#f9fafb", padding: "10px 16px", textAlign: "left", fontSize: 11, fontWeight: 600, color: "#5f7a98", borderBottom: "2px solid #e5e7eb", textTransform: "uppercase" };
const tdStyle: React.CSSProperties = { padding: "8px 16px", color: "#374151", borderBottom: "1px solid #e5e7eb" };

export default ReportInvoice;
