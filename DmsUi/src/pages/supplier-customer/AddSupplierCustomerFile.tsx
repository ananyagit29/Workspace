import React, { useContext, useEffect, useRef, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { getSupplierCustomerSearchOptions, saveSupplierCustomerFile } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const ACCOUNT_TYPE_OPTIONS = ["CUSTOMER", "SUPPLIER", "SUPPLIER AND CUSTOMER", "NON-SCM PARTIES"];
const MAX_FILE_SIZE = 1024 * 1024; // 1MB

const AddSupplierCustomerFile = () => {
  const { user } = useContext(AuthContext);
  const fileRef = useRef<HTMLInputElement | null>(null);

  const { selections } = useOutletContext<{ selections: Selections }>();

  const [accountType, setAccountType] = useState("");
  const [accountOptions, setAccountOptions] = useState<{ code: string; name: string }[]>([]);
  const [selectedAccount, setSelectedAccount] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [file, setFile] = useState<File | null>(null);

  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  const showToast = (msg: string, type: "success" | "error" = "success") => setToast({ msg, type });

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(t);
  }, [toast]);

  useEffect(() => {
    setSelectedAccount("");
    setAccountOptions([]);
    if (!accountType || !selections) return;

    getSupplierCustomerSearchOptions(accountType, selections.com, selections.loc)
      .then(res => setAccountOptions(res.data || []))
      .catch(() => showToast("Failed to fetch accounts", "error"));
  }, [accountType, selections]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const f = e.target.files[0];
      if (f.type !== "application/pdf") {
        showToast("Only PDF files are allowed", "error");
        setFile(null);
        if (fileRef.current) fileRef.current.value = "";
        return;
      }
      if (f.size > MAX_FILE_SIZE) {
        showToast("File size must be less than 1MB", "error");
        setFile(null);
        if (fileRef.current) fileRef.current.value = "";
        return;
      }
      setFile(f);
    }
  };

  const handleSave = async () => {
    if (!selections || !user) return;
    if (!accountType || !selectedAccount || !file) {
      showToast("Please fill all fields and select a file", "error");
      return;
    }

    let accCode = selectedAccount;
    let accName = selectedAccount;
    const match = selectedAccount.match(/^(.*?)\s*\(([^)]+)\)$/);
    if (match) {
      accName = match[1].trim();
      accCode = match[2].trim();
    }

    setSaving(true);
    try {
      const formData = new FormData();
      formData.append("accountType", accountType);
      formData.append("accountCode", accCode);
      formData.append("accountName", accName);
      formData.append("companyId", selections.com);
      formData.append("locationId", selections.loc);
      formData.append("divisionName", selections.div);
      formData.append("applicationName", "SUPPLIER_AND_CUSTOMER");
      formData.append("userId", user.userId);
      formData.append("file", file);

      await saveSupplierCustomerFile(formData);

      showToast("File saved successfully", "success");
      setAccountType("");
      setSelectedAccount("");
      setFile(null);
      if (fileRef.current) fileRef.current.value = "";
    } catch {
      showToast("Failed to save file", "error");
    } finally {
      setSaving(false);
    }
  };

  if (!selections) return null;

  return (
    <div style={{ background: "#f3f4f6", minHeight: "100vh" }}>
      {toast && (
        <div style={{ position: "fixed", top: 16, right: 16, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "24px 32px" }}>
        <div style={{ maxWidth: 600, margin: "0 auto", background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "20px 24px" }}>
          <h2 style={{ fontSize: 11, fontWeight: 600, color: "#8aa2b9", textTransform: "uppercase", letterSpacing: "0.05em", margin: "0 0 20px 0" }}>ADD SUPPLIER AND CUSTOMER FILES</h2>

          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <div style={{ width: "100%" }}>
              <label style={labelStyle}>Select Account Type</label>
              <select value={accountType} onChange={e => setAccountType(e.target.value)} style={inputStyle}>
                <option value="">SELECT</option>
                {ACCOUNT_TYPE_OPTIONS.map(opt => <option key={opt} value={opt}>{opt}</option>)}
              </select>
            </div>

            <div style={{ position: "relative", zIndex: 50, width: "100%" }}>
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
                style={{ ...inputStyle, opacity: !accountType ? 0.6 : 1 }}
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

            <div style={{ width: "100%" }}>
              <label style={{ display: "block", fontSize: 11, fontWeight: 600, color: "#8aa2b9", marginBottom: 8, textTransform: "uppercase" }}>Upload File</label>
              <div style={{ fontSize: 12, color: "#8aa2b9", marginBottom: 12 }}>
                Upload other file in PDF format only. Maximum file size: 1 MB.
              </div>
              <div style={fileUploadContainer}>
                <input
                  type="file"
                  accept="application/pdf"
                  onChange={handleFileChange}
                  ref={fileRef}
                  style={{ display: "none" }}
                  id="file-upload"
                />
                <label htmlFor="file-upload" style={uploadButton}>
                  Choose Files
                </label>
                <span style={{ fontSize: 12, color: "#6b7280" }}>
                  {file ? file.name : "No file chosen"}
                </span>
              </div>
            </div>

            <div style={{ marginTop: 8 }}>
              <button
                onClick={handleSave}
                disabled={saving || !accountType || !selectedAccount || !file}
                style={{ ...primaryButton, opacity: (saving || !accountType || !selectedAccount || !file) ? 0.6 : 1 }}
              >
                {saving ? "Submitting..." : "Attach"}
              </button>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

// --- STYLES --- //
const labelStyle: React.CSSProperties = { display: "block", fontSize: 11, fontWeight: 600, color: "#8aa2b9", marginBottom: 4, textTransform: "uppercase" };
const inputStyle: React.CSSProperties = { width: "100%", padding: "8px 12px", fontSize: 13, color: "#111827", background: "#f9fafb", border: "1px solid #d1d5db", borderRadius: 6, outline: "none", boxSizing: "border-box", transition: "border-color 0.2s" };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "8px 24px", fontSize: 13, fontWeight: 600, cursor: "pointer", transition: "all 0.15s" };

const fileUploadContainer: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 12,
  padding: "12px 16px",
  background: "#f8fafc",
  border: "1px dashed #cbd5e1",
  borderRadius: 6
};

const uploadButton: React.CSSProperties = {
  padding: "6px 12px",
  background: "#f3f4f6",
  border: "1px solid #d1d5db",
  borderRadius: 4,
  fontSize: 12,
  fontWeight: 500,
  color: "#374151",
  cursor: "pointer",
};

export default AddSupplierCustomerFile;
