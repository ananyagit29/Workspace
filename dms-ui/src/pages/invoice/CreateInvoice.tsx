import type React from "react";
import { useContext, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { Header } from "../../components/Header";
import { Footer } from "../../components/Footer";
import { dmsApi } from "../../api/dmsApi";

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const MAX_FILE_SIZE = 1024 * 1024;

const CreateInvoice = () => {
  const { user, loading } = useContext(AuthContext);
  const navigate = useNavigate();
  const fileRef = useRef<HTMLInputElement | null>(null);

  const [selections, setSelections] = useState<Selections | null>(null);
  const [invoiceNumber, setInvoiceNumber] = useState("");
  const [invoiceExists, setInvoiceExists] = useState<boolean | null>(null);
  const [checkingInvoice, setCheckingInvoice] = useState(false);
  const [invoiceFile, setInvoiceFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [uploadType, setUploadType] = useState<"main" | "other">("main");
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

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

  const handleInvoiceBlur = async () => {
    if (!invoiceNumber.trim()) return;
    setCheckingInvoice(true);
    setInvoiceExists(null);
    try {
      const res = await dmsApi.get("/invoice/exists", { params: { invoiceNumber: invoiceNumber.trim().toUpperCase() } });
      const exists = !!res.data;
      setInvoiceExists(exists);
      if (uploadType === "main" && exists) showToast("Invoice number already exists", "error");
      if (uploadType === "other" && !exists) showToast("Invoice number not found. Create it first.", "error");
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
    if (fileRef.current) fileRef.current.value = "";
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selections) return;
    if (!invoiceNumber.trim()) { showToast("Please enter Invoice Number", "error"); return; }
    if (uploadType === "main" && invoiceExists) { showToast("Invoice number already exists", "error"); return; }
    if (uploadType === "other" && invoiceExists === false) { showToast("Invoice number not found", "error"); return; }
    if (!invoiceFile) { showToast("Please upload a PDF", "error"); return; }

    setSaving(true);
    try {
      const fd = new FormData();
      if (uploadType === "main") {
        fd.append("invoiceNumber", invoiceNumber.trim().toUpperCase());
        fd.append("companyId", selections.com);
        fd.append("locationId", selections.loc);
        fd.append("divisionName", selections.div);
        fd.append("applicationName", selections.app);
        fd.append("invoiceFile", invoiceFile);
        await dmsApi.post("/invoice/save", fd, { headers: { "Content-Type": "multipart/form-data" } });
        showToast("Invoice saved successfully");
      } else {
        fd.append("invoiceNumber", invoiceNumber.trim().toUpperCase());
        fd.append("otherFile", invoiceFile);
        await dmsApi.post("/invoice/attach", fd, { headers: { "Content-Type": "multipart/form-data" } });
        showToast("Other file attached successfully");
      }
      handleCancel();
    } catch (err: unknown) {
      const error = err as { response?: { data?: string } };
      showToast(error.response?.data || "Failed to save document", "error");
    } finally {
      setSaving(false);
    }
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
        pageTitle="Create Invoice"
        breadcrumb="Dashboard > Invoice Documents > Create"
        contextMeta={[
          { label: "Company", value: selections.com },
          { label: "Division", value: selections.div },
          { label: "Location", value: selections.loc },
        ]}
        onCreateClick={() => navigate("/invoice")}
        createLabel="Search Invoice"
      />

      {toast && (
        <div style={{ position: "fixed", top: 16, right: 16, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ flex: 1, padding: "8px 16px", overflowY: "auto" }}>
        <div style={{ maxWidth: 560, margin: "0 auto" }}>
          <div style={{ display: "flex", gap: 8, marginBottom: 16 }}>
            <button
              onClick={() => { setUploadType("main"); handleCancel(); }}
              style={{ ...tabButton, background: uploadType === "main" ? "#003366" : "#fff", color: uploadType === "main" ? "#fff" : "#374151", borderColor: uploadType === "main" ? "#003366" : "#d1d5db" }}
            >
              Upload Main Invoice
            </button>
            <button
              onClick={() => { setUploadType("other"); handleCancel(); }}
              style={{ ...tabButton, background: uploadType === "other" ? "#003366" : "#fff", color: uploadType === "other" ? "#fff" : "#374151", borderColor: uploadType === "other" ? "#003366" : "#d1d5db" }}
            >
              Attach Other File
            </button>
          </div>

          <form onSubmit={handleSubmit}>
            <div style={cardStyle}>
              <div style={sectionTitle}>{uploadType === "main" ? "Invoice Information" : "Attach To Invoice"}</div>
              <div style={fieldStyle}>
                <label style={labelStyle}>Invoice Number <span style={{ color: "#ef4444" }}>*</span></label>
                <input
                  value={invoiceNumber}
                  onChange={e => { setInvoiceNumber(e.target.value.toUpperCase()); setInvoiceExists(null); }}
                  onBlur={handleInvoiceBlur}
                  placeholder="Enter invoice number"
                  style={{
                    ...inputStyle,
                    borderColor: invoiceExists !== null ? (
                      uploadType === "main" ? (invoiceExists ? "#ef4444" : "#10b981") : (!invoiceExists ? "#ef4444" : "#10b981")
                    ) : "#e5e7eb"
                  }}
                />
                <span style={{ fontSize: 10, color: invoiceExists !== null ? (uploadType === "main" ? (invoiceExists ? "#ef4444" : "#10b981") : (!invoiceExists ? "#ef4444" : "#10b981")) : "#9ca3af", marginTop: 2 }}>
                  {checkingInvoice ? "Validating invoice number..." : invoiceExists !== null ? (
                    uploadType === "main" ? (invoiceExists ? "This invoice number already exists." : "Invoice number is available.")
                      : (!invoiceExists ? "Invoice number not found. Create main invoice first." : "Invoice found, you can attach files.")
                  ) : "Used for validation before saving."}
                </span>
              </div>
            </div>

            <div style={{ ...cardStyle, marginTop: 10 }}>
              <div style={sectionTitle}>{uploadType === "main" ? "Upload Document" : "Upload Additional File"}</div>
              <div style={{ fontSize: 10, color: "#9ca3af", marginBottom: 10 }}>Upload {uploadType === "main" ? "invoice" : "other file"} in PDF format only. Maximum file size: 1 MB.</div>
              <div
                onClick={() => fileRef.current?.click()}
                style={{ display: "flex", alignItems: "center", gap: 8, border: "1px dashed #d1d5db", borderRadius: 6, padding: "8px 10px", background: invoiceFile ? "#f0fdf4" : "#fafafa", borderColor: invoiceFile ? "#86efac" : "#d1d5db", cursor: "pointer" }}
              >
                <span style={{ fontSize: 12, fontWeight: 700, color: invoiceFile ? "#166534" : "#2563eb" }}>PDF</span>
                <span style={{ fontSize: 12, color: invoiceFile ? "#166534" : "#2563eb", fontWeight: 500, flex: 1 }}>
                  {invoiceFile ? invoiceFile.name : "Choose file"}
                </span>
                <span style={{ fontSize: 10, color: "#9ca3af" }}>
                  {invoiceFile ? `${(invoiceFile.size / 1024).toFixed(0)} KB` : "No file chosen"}
                </span>
                <input ref={fileRef} type="file" accept=".pdf,application/pdf" style={{ display: "none" }} onChange={e => handleFileChange(e.target.files?.[0] || null)} />
              </div>
            </div>

            <div style={{ display: "flex", gap: 8, marginTop: 8, justifyContent: "flex-end" }}>
              <button type="button" onClick={handleCancel} style={secondaryButton}>Cancel</button>
              <button type="submit" disabled={saving || invoiceExists === (uploadType === "main") || invoiceExists === null || !invoiceFile} style={{
                ...primaryButton,
                background: saving || invoiceExists === (uploadType === "main") || invoiceExists === null || !invoiceFile ? "#e5e7eb" : "#003366",
                color: saving || invoiceExists === (uploadType === "main") || invoiceExists === null || !invoiceFile ? "#9ca3af" : "#fff",
                cursor: saving || invoiceExists === (uploadType === "main") || invoiceExists === null || !invoiceFile ? "not-allowed" : "pointer",
              }}>
                {saving ? "Saving..." : (uploadType === "main" ? "Save Invoice" : "Attach File")}
              </button>
            </div>
          </form>
        </div>
      </main>
      <Footer />
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
