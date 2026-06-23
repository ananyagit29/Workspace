import type React from "react";
import { useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { getCapexBudgetTypes, getCapexBudgetCodes, saveCapex } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const CAPEX_TYPE_MAP: Record<string, string> = {
  "C": "CAPITAL",
  "IA": "INPRINCIPLE APPROVAL",
  "LC": "LOCAL CAPITAL",
  "LR": "LOCAL REPAIR",
  "R": "REPAIR",
};

const CAPEX_TYPE_OPTIONS = ["C", "IA", "LC", "LR", "R"];

const MAX_FILE_SIZE = 1024 * 1024;

const CreateCapex = () => {
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const fileRef = useRef<HTMLInputElement | null>(null);

  const [budgetTypes, setBudgetTypes] = useState<string[]>([]);
  const [budgetCodes, setBudgetCodes] = useState<string[]>([]);
  const [budgetType, setBudgetType] = useState("");
  const [budgetCode, setBudgetCode] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);

  const [file, setFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);

  const { selections } = useOutletContext<{ selections: Selections }>();
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

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
    if (!budgetType || !selections) return;

    getCapexBudgetCodes(budgetType, selections.com, selections.loc, selections.year)
      .then(res => setBudgetCodes(res.data || []))
      .catch(() => showToast("Failed to fetch budget codes", "error"));
  }, [budgetType, selections]);

  const handleFileChange = (f: File | null) => {
    if (!f) { setFile(null); return; }
    if (!f.name.toLowerCase().endsWith(".pdf")) {
      showToast("Only PDF files are allowed", "error");
      if (fileRef.current) fileRef.current.value = "";
      return;
    }
    if (f.size > MAX_FILE_SIZE) {
      showToast("PDF size must be 1 MB or less", "error");
      if (fileRef.current) fileRef.current.value = "";
      return;
    }
    setFile(f);
  };

  const handleCancel = () => {
    setBudgetType("");
    setBudgetCode("");
    setFile(null);
    if (fileRef.current) fileRef.current.value = "";
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selections || !user) return;
    if (!budgetType) { showToast("Please select Budget Type", "error"); return; }
    if (!budgetCode) { showToast("Please select Budget Code", "error"); return; }
    if (!file) { showToast("Please upload a PDF", "error"); return; }

    setSaving(true);
    try {
      const fd = new FormData();
      const dbBudgetType = CAPEX_TYPE_MAP[budgetType] || budgetType;
      fd.append("budgetType", dbBudgetType);
      fd.append("transactionId", budgetType);
      fd.append("budgetCode", budgetCode);
      fd.append("companyId", selections.com);
      fd.append("locationId", selections.loc);
      fd.append("divisionName", selections.div);
      fd.append("applicationName", selections.app);
      fd.append("year", selections.year);
      fd.append("userId", user.userId);
      fd.append("file", file);

      await saveCapex(fd);
      showToast("CapEx Budget document saved successfully");
      handleCancel();
    } catch (err: unknown) {
      const error = err as { response?: { data?: string } };
      showToast(error.response?.data || "Failed to save document", "error");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ background: "#f3f4f6", minHeight: "100vh" }}>
      {toast && (
        <div style={{ position: "fixed", top: 16, right: 16, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "24px 32px" }}>
        <div style={{ maxWidth: 560, margin: "0 auto" }}>
          <form onSubmit={handleSubmit}>
            <div style={cardStyle}>
              <div style={sectionTitle}>Create CapEx Budget</div>

              <div style={fieldStyle}>
                <div style={{ display: "flex", flexDirection: "column", gap: "8px", flex: "1 1 200px" }}>
                  <label style={{ fontSize: "0.85rem", fontWeight: 600, color: "#374151" }}>Select Budget Type</label>
                  <select value={budgetType} onChange={e => setBudgetType(e.target.value)} style={inputStyle}>
                    <option value="">Select</option>
                    {budgetTypes.map(t => <option key={t} value={t}>{CAPEX_TYPE_MAP[t] || t}</option>)}
                  </select>
                </div>
              </div>

              <div style={{ ...fieldStyle, position: "relative" }}>
                <label style={labelStyle}>Budget Code <span style={{ color: "#ef4444" }}>*</span></label>
                <input
                  value={budgetCode}
                  onChange={e => {
                    setBudgetCode(e.target.value.toUpperCase());
                    setShowSuggestions(true);
                  }}
                  onFocus={() => setShowSuggestions(true)}
                  onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                  disabled={saving || !budgetType}
                  placeholder="Enter budget code"
                  style={{ ...inputStyle, width: "100%", boxSizing: "border-box", opacity: (!budgetType || saving) ? 0.6 : 1 }}
                />
                {showSuggestions && budgetType && !saving && (
                  <ul style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, zIndex: 50, maxHeight: 150, overflowY: "auto", listStyle: "none", padding: 0, margin: "4px 0 0 0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)" }}>
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

              <div style={fieldStyle}>
                <label style={labelStyle}>Upload File (PDF, max 1MB) <span style={{ color: "#ef4444" }}>*</span></label>
                <div style={fileUploadContainer}>
                  <input
                    ref={fileRef}
                    type="file"
                    accept="application/pdf"
                    onChange={e => handleFileChange(e.target.files?.[0] || null)}
                    style={{ position: "absolute", top: 0, left: 0, width: "100%", height: "100%", opacity: 0, cursor: "pointer" }}
                    disabled={saving}
                  />
                  <div style={uploadButton}>Choose File</div>
                  <span style={fileNameStyle}>
                    {file ? file.name : "No file chosen"}
                  </span>
                </div>
              </div>

              <div style={actionRow}>
                <button type="button" onClick={handleCancel} disabled={saving} style={cancelBtn}>Cancel</button>
                <button type="submit" disabled={saving || !budgetCode || !file} style={submitBtn(!!budgetCode && !!file)}>
                  {saving ? "Saving..." : "Submit"}
                </button>
              </div>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
};

// --- STYLES --- //
const cardStyle: React.CSSProperties = {
  background: "#fff",
  borderRadius: 8,
  boxShadow: "0 1px 3px rgba(0,0,0,0.1)",
  border: "1px solid #e5e7eb",
  padding: "24px",
  marginBottom: 20,
};

const sectionTitle: React.CSSProperties = {
  fontSize: 16,
  fontWeight: 600,
  color: "#111827",
  marginBottom: 24,
  borderBottom: "1px solid #e5e7eb",
  paddingBottom: 12,
};

const fieldStyle: React.CSSProperties = {
  marginBottom: 20,
};

const labelStyle: React.CSSProperties = {
  display: "block",
  fontSize: 12,
  fontWeight: 600,
  color: "#374151",
  marginBottom: 6,
};

const inputStyle: React.CSSProperties = {
  width: "100%",
  padding: "8px 12px",
  fontSize: 13,
  color: "#111827",
  background: "#f9fafb",
  border: "1px solid #d1d5db",
  borderRadius: 6,
  outline: "none",
  transition: "border-color 0.2s",
};

const fileUploadContainer: React.CSSProperties = {
  position: "relative",
  display: "flex",
  alignItems: "center",
  gap: 12,
};

const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "8px 24px", fontSize: 13, fontWeight: 600, cursor: "pointer", transition: "all 0.15s" };

const uploadButton: React.CSSProperties = {
  padding: "6px 12px",
  background: "#f3f4f6",
  border: "1px solid #d1d5db",
  borderRadius: 4,
  fontSize: 12,
  fontWeight: 500,
  color: "#374151",
};

const fileNameStyle: React.CSSProperties = {
  fontSize: 12,
  color: "#6b7280",
};

const actionRow: React.CSSProperties = {
  display: "flex",
  justifyContent: "flex-end",
  gap: 12,
  marginTop: 32,
};

const cancelBtn: React.CSSProperties = {
  padding: "8px 16px",
  fontSize: 13,
  fontWeight: 500,
  color: "#374151",
  background: "#fff",
  border: "1px solid #d1d5db",
  borderRadius: 6,
  cursor: "pointer",
};

const submitBtn = (active: boolean): React.CSSProperties => ({
  padding: "8px 16px",
  fontSize: 13,
  fontWeight: 500,
  color: "#fff",
  background: active ? "#003366" : "#94a3b8",
  border: "none",
  borderRadius: 6,
  cursor: active ? "pointer" : "not-allowed",
  transition: "background 0.2s",
});

export default CreateCapex;
