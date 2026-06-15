import type React from "react";
import { useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { dmsApi } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const MAX_FILE_SIZE = 1024 * 1024;

const CreateInvoice = () => {
  const navigate = useNavigate();
  const fileRef = useRef<HTMLInputElement | null>(null);

  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [invoiceExists, setInvoiceExists] = useState<boolean | null>(null);
  const [checkingInvoice, setCheckingInvoice] = useState(false);
  const [invoiceFile, setInvoiceFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  const { selections } = useOutletContext<{ selections: Selections }>();
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(t);
  }, [toast]);

  const showToast = (msg: string, type: "success" | "error" = "success") => setToast({ msg, type });

  const validateInvoice = async (val: string) => {
    if (!val.trim()) return;
    setCheckingInvoice(true);
    setInvoiceExists(null);
    try {
      const res = await dmsApi.get("/invoice/exists", { params: { invoiceNumber: val.trim().toUpperCase() } });
      const exists = !!res.data;
      setInvoiceExists(exists);
      if (!exists) showToast("Invoice number not found.", "error");
    } catch {
      showToast("Failed to validate invoice number", "error");
    } finally {
      setCheckingInvoice(false);
    }
  };

  const handleFileChange = (file: File | null) => {
    if (!file) { setInvoiceFile(null); return; }
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      showToast("Only PDF files are allowed", "error");
      if (fileRef.current) fileRef.current.value = "";
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      showToast("PDF size must be 1 MB or less", "error");
      if (fileRef.current) fileRef.current.value = "";
      return;
    }
    setInvoiceFile(file);
  };

  const handleCancel = () => {
    setInvoiceNumber("");
    setInvoiceExists(null);
    setInvoiceFile(null);
    setShowSuggestions(false);
    if (fileRef.current) fileRef.current.value = "";
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selections) return;
    if (!invoiceNumber.trim()) { showToast("Please enter Invoice Number", "error"); return; }
    if (invoiceExists !== true) { showToast("Invoice number not validated", "error"); return; }
    if (!invoiceFile) { showToast("Please upload a PDF", "error"); return; }

    setSaving(true);
    try {
      const fd = new FormData();
      fd.append("invoiceNumber", invoiceNumber.trim().toUpperCase());
      fd.append("otherFile", invoiceFile);
      await dmsApi.post("/invoice/attach", fd, { headers: { "Content-Type": "multipart/form-data" } });
      showToast("Other file attached successfully");
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
              <div style={sectionTitle}>Attach To Invoice</div>
              <div style={{ ...fieldStyle, position: "relative" }}>
                <label style={labelStyle}>Invoice Number <span style={{ color: "#ef4444" }}>*</span></label>
                <input
                  value={invoiceNumber}
                  onChange={e => { 
                    const val = e.target.value.toUpperCase();
                    setInvoiceNumber(val); 
                    setInvoiceExists(null); 
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
                  onBlur={() => {
                    // Slight delay to allow clicking a suggestion
                    setTimeout(() => setShowSuggestions(false), 200);
                    if (invoiceNumber.trim() && invoiceExists === null) {
                      validateInvoice(invoiceNumber);
                    }
                  }}
                  placeholder="Enter invoice number"
                  style={{
                    ...inputStyle,
                    borderColor: invoiceExists !== null ? (!invoiceExists ? "#ef4444" : "#10b981") : "#e5e7eb"
                  }}
                />
                
                {showSuggestions && suggestions.length > 0 && (
                  <ul style={{ position: "absolute", top: "100%", left: 0, right: 0, background: "#fff", border: "1px solid #d1d5db", borderRadius: 6, zIndex: 10, maxHeight: 150, overflowY: "auto", listStyle: "none", padding: 0, margin: "4px 0 0 0", boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)" }}>
                    {suggestions.map(sug => (
                      <li 
                        key={sug} 
                        onClick={() => {
                          setInvoiceNumber(sug);
                          setShowSuggestions(false);
                          validateInvoice(sug);
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

                <span style={{ fontSize: 10, color: invoiceExists !== null ? (!invoiceExists ? "#ef4444" : "#10b981") : "#9ca3af", marginTop: 2 }}>
                  {checkingInvoice ? "Validating invoice number..." : invoiceExists !== null ? (
                    !invoiceExists ? "Invoice number not found." : "Invoice found, you can attach files."
                  ) : "Used for validation before saving."}
                </span>
              </div>
            </div>

            <div style={{ ...cardStyle, marginTop: 10 }}>
              <div style={sectionTitle}>Upload Additional File</div>
              <div style={{ fontSize: 10, color: "#9ca3af", marginBottom: 10 }}>Upload other file in PDF format only. Maximum file size: 1 MB.</div>
              <div
                onClick={() => {
                  if (invoiceExists === true) fileRef.current?.click();
                }}
                style={{ 
                  display: "flex", alignItems: "center", gap: 8, border: "1px dashed #d1d5db", borderRadius: 6, padding: "8px 10px", 
                  background: invoiceExists === true ? (invoiceFile ? "#f0fdf4" : "#fafafa") : "#f3f4f6", 
                  borderColor: invoiceFile ? "#86efac" : "#d1d5db", 
                  cursor: invoiceExists === true ? "pointer" : "not-allowed",
                  opacity: invoiceExists === true ? 1 : 0.6
                }}
              >
                <span style={{ fontSize: 12, fontWeight: 700, color: invoiceExists === true ? (invoiceFile ? "#166534" : "#2563eb") : "#9ca3af" }}>PDF</span>
                <span style={{ fontSize: 12, color: invoiceExists === true ? (invoiceFile ? "#166534" : "#2563eb") : "#9ca3af", fontWeight: 500, flex: 1 }}>
                  {invoiceFile ? invoiceFile.name : "Choose file"}
                </span>
                <span style={{ fontSize: 10, color: "#9ca3af" }}>
                  {invoiceFile ? `${(invoiceFile.size / 1024).toFixed(0)} KB` : "No file chosen"}
                </span>
                <input disabled={invoiceExists !== true} ref={fileRef} type="file" accept=".pdf,application/pdf" style={{ display: "none" }} onChange={e => handleFileChange(e.target.files?.[0] || null)} />
              </div>
            </div>

            <div style={{ display: "flex", gap: 8, marginTop: 8, justifyContent: "flex-end" }}>
              <button type="button" onClick={handleCancel} style={secondaryButton}>Cancel</button>
              <button type="submit" disabled={saving || invoiceExists !== true || !invoiceFile} style={{
                ...primaryButton,
                background: saving || invoiceExists !== true || !invoiceFile ? "#e5e7eb" : "#003366",
                color: saving || invoiceExists !== true || !invoiceFile ? "#9ca3af" : "#fff",
                cursor: saving || invoiceExists !== true || !invoiceFile ? "not-allowed" : "pointer",
              }}>
                {saving ? "Saving..." : "Attach File"}
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
};

const cardStyle: React.CSSProperties = { background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", padding: "10px 14px" };
const sectionTitle: React.CSSProperties = { fontSize: 10, fontWeight: 600, color: "#9ca3af", textTransform: "uppercase", letterSpacing: "0.05em", marginBottom: 8, paddingBottom: 6, borderBottom: "1px solid #f3f4f6" };
const fieldStyle: React.CSSProperties = { display: "flex", flexDirection: "column", marginBottom: 7 };
const labelStyle: React.CSSProperties = { fontSize: 11, fontWeight: 600, color: "#374151", marginBottom: 3 };
const inputStyle: React.CSSProperties = { border: "1px solid #e5e7eb", borderRadius: 6, padding: "6px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none" };
const secondaryButton: React.CSSProperties = { padding: "7px 20px", fontSize: 12, fontWeight: 500, border: "1px solid #e5e7eb", borderRadius: 7, background: "#fff", color: "#374151", cursor: "pointer" };
const primaryButton: React.CSSProperties = { padding: "7px 24px", fontSize: 12, fontWeight: 600, border: "none", borderRadius: 7 };
const tabButton: React.CSSProperties = { flex: 1, padding: "8px 0", fontSize: 12, fontWeight: 600, border: "1px solid", borderRadius: 6, cursor: "pointer", transition: "all 0.2s" };

export default CreateInvoice;
