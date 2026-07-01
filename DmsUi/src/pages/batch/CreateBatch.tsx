import { useContext, useEffect, useRef, useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import { dmsApi } from "../../api/dmsApi";

// CreateBatch no longer owns Header/Footer — BatchLayout provides them.
// Selections come via useOutletContext from BatchLayout.

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

interface ProductDetails {
  productName: string;
  vendorCode: string;
  vendorName: string;
}

interface FileEntry {
  subType: string;
  accept: string;
  label: string;
  existingFileName?: string;
  existingFilePath?: string;
  file?: File | null;
}

const MAX_FILE_SIZE = 1024 * 1024;

const CreateBatch = () => {
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();
  const { selections } = useOutletContext<{ selections: Selections }>();

  const [batchType, setBatchType]     = useState("");
  const [productCode, setProductCode] = useState("");
  const [batchNumber, setBatchNumber] = useState("");
  const [productDetails, setProductDetails] = useState<ProductDetails | null>(null);
  const [fileEntries, setFileEntries] = useState<FileEntry[]>([]);

  const [fetchingProduct, setFetchingProduct] = useState(false);
  const [fetchingFiles, setFetchingFiles]     = useState(false);
  const [saving, setSaving]                   = useState(false);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    window.dispatchEvent(new CustomEvent("app-toast", { detail: { msg, type } }));
  };

  const fileRefs = useRef<Record<string, HTMLInputElement | null>>({});

  const handleBatchTypeChange = (type: string) => {
    setBatchType(type);
    setProductCode(""); setBatchNumber("");
    setProductDetails(null); setFileEntries([]);
  };

  const handleProductBlur = async () => {
    if (!productCode || !batchType) return;
    setFetchingProduct(true); setProductDetails(null);
    try {
      const res = await dmsApi.get("/batch/getProductDetails", { params: { batchType, productCode } });
      if (res.data?.productName) setProductDetails(res.data);
      else showToast("No matching product found", "error");
    } catch { showToast("Failed to fetch product details", "error"); }
    finally { setFetchingProduct(false); }
  };

  const handleBatchNumberBlur = async () => {
    if (!batchNumber || !productCode || !batchType) return;
    setFetchingFiles(true);
    try {
      const res = await dmsApi.get("/batch/getFileNames", { params: { productCode, batchType, batchNumber } });
      buildFileEntries(batchType, res.data || []);
    } catch { buildFileEntries(batchType, []); }
    finally { setFetchingFiles(false); }
  };

  const buildFileEntries = (
    type: string,
    existing: { subApplicationName: string; fileName: string; filePath: string; hasFile: boolean }[]
  ) => {
    const find = (sub: string) => existing.find(e => e.subApplicationName === sub);
    const image = find("IMAGE");
    const entries: FileEntry[] = [{
      subType: "IMAGE", label: "Product Image", accept: ".docx,image/*",
      existingFileName: image?.hasFile ? image.fileName : undefined,
      existingFilePath: image?.hasFile ? image.filePath : undefined,
      file: null,
    }];
    if (type === "Third Party") {
      const coa = find("COA"); const invoice = find("INVOICE");
      entries.push({ subType: "COA", label: "COA", accept: ".pdf", existingFileName: coa?.hasFile ? coa.fileName : undefined, existingFilePath: coa?.hasFile ? coa.filePath : undefined, file: null });
      entries.push({ subType: "INVOICE", label: "Invoice", accept: ".pdf", existingFileName: invoice?.hasFile ? invoice.fileName : undefined, existingFilePath: invoice?.hasFile ? invoice.filePath : undefined, file: null });
    }
    setFileEntries(entries);
  };

  const handleFileChange = (subType: string, file: File | null) => {
    if (file && file.size > MAX_FILE_SIZE) {
      showToast(`${file.name} exceeds 1 MB limit`, "error");
      if (fileRefs.current[subType]) fileRefs.current[subType]!.value = "";
      return;
    }
    setFileEntries(prev => prev.map(e => e.subType === subType ? { ...e, file } : e));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!batchType)      { showToast("Please select Batch Type", "error"); return; }
    if (!productCode)    { showToast("Please enter Product Code", "error"); return; }
    if (!batchNumber)    { showToast("Please enter Batch Number", "error"); return; }
    if (!productDetails) { showToast("Please fetch product details first", "error"); return; }

    setSaving(true);
    try {
      const fd = new FormData();
      fd.append("batchType",       batchType);
      fd.append("productCode",     productCode);
      fd.append("productName",     productDetails.productName);
      fd.append("vendorCode",      productDetails.vendorCode);
      fd.append("vendorName",      productDetails.vendorName);
      fd.append("batchNumber",     batchNumber);
      fd.append("companyId",       selections.com);
      fd.append("locationId",      selections.loc);
      fd.append("divisionName",    selections.div);
      fd.append("applicationName", selections.app);

      fileEntries.forEach(entry => {
        if (entry.file) {
          const key = entry.subType === "COA" ? "coaFile"
                    : entry.subType === "INVOICE" ? "invoiceFile"
                    : "imageFile";
          fd.append(key, entry.file);
        }
      });

      await dmsApi.post("/batch/save", fd, { headers: { "Content-Type": "multipart/form-data" } });
      showToast("Batch saved successfully!");
      setBatchType(""); setProductCode(""); setBatchNumber("");
      setProductDetails(null); setFileEntries([]);
    } catch { showToast("Failed to save batch", "error"); }
    finally { setSaving(false); }
  };

  if (!user || !selections) return null;

  return (
    <main style={{ flex: 1, padding: "8px 16px", overflowY: "auto" }}>



      <div style={{ maxWidth: 560, margin: "0 auto" }}>
        <form onSubmit={handleSubmit}>

          <div style={cardStyle}>
            <div style={sectionTitle}>Batch Information</div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 10 }}>
              <div style={fieldStyle}>
                <label style={labelStyle}>Batch Type <span style={{ color: "#ef4444" }}>*</span></label>
                <select value={batchType} onChange={e => handleBatchTypeChange(e.target.value)} style={inputStyle}>
                  <option value="">Select</option>
                  <option value="Third Party">Third Party</option>
                  <option value="Own">Own</option>
                  <option value="Loan License">Loan License</option>
                </select>
              </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: 10 }}>
              <div style={fieldStyle}>
                <label style={labelStyle}>Product Code <span style={{ color: "#ef4444" }}>*</span></label>
                <input value={productCode} onChange={e => setProductCode(e.target.value.toUpperCase())}
                  onBlur={handleProductBlur} disabled={!batchType} placeholder="e.g. AB1234"
                  style={{ ...inputStyle, opacity: !batchType ? 0.5 : 1 }} />
              </div>
              <div style={fieldStyle}>
                <label style={labelStyle}>Product Name</label>
                <input value={fetchingProduct ? "Fetching..." : productDetails?.productName || ""}
                  readOnly placeholder="Auto-filled"
                  style={{ ...inputStyle, background: "#f3f4f6", color: "#333" }} />
              </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: 10 }}>
              <div style={fieldStyle}>
                <label style={labelStyle}>Vendor Code</label>
                <input value={productDetails?.vendorCode || ""} readOnly placeholder="Auto-filled"
                  style={{ ...inputStyle, background: "#f3f4f6", color: "#333" }} />
              </div>
              <div style={fieldStyle}>
                <label style={labelStyle}>Vendor Name</label>
                <input value={productDetails?.vendorName || ""} readOnly placeholder="Auto-filled"
                  style={{ ...inputStyle, background: "#f3f4f6", color: "#333" }} />
              </div>
            </div>

            <div style={fieldStyle}>
              <label style={labelStyle}>Batch Number <span style={{ color: "#ef4444" }}>*</span></label>
              <input value={batchNumber} onChange={e => setBatchNumber(e.target.value.toUpperCase())}
                onBlur={handleBatchNumberBlur} disabled={!productDetails} placeholder="e.g. BCH-2025-001"
                style={{ ...inputStyle, opacity: !productDetails ? 0.5 : 1 }} />
              {fetchingFiles && <span style={{ fontSize: 10, color: "#333", marginTop: 2 }}>Checking files...</span>}
            </div>
          </div>

          {fileEntries.length > 0 && (
            <div style={{ ...cardStyle, marginTop: 10 }}>
              <div style={sectionTitle}>Upload Documents</div>
              <div style={{ fontSize: 10, color: "#333", marginBottom: 10 }}>Max file size: 1 MB per file</div>
              {fileEntries.map(entry => (
                <div key={entry.subType} style={{ ...fieldStyle, marginBottom: 10 }}>
                  <label style={labelStyle}>
                    {entry.label}
                    <span style={{ color: "#333", fontWeight: 400, marginLeft: 4 }}>
                      ({entry.subType === "IMAGE" ? ".docx, .png, .jpg" : ".pdf"})
                    </span>
                  </label>
                  {entry.existingFileName ? (
                    <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "5px 10px", background: "#f0fdf4", border: "1px solid #bbf7d0", borderRadius: 6 }}>
                      <span style={{ fontSize: 13 }}>📄</span>
                      <span style={{ fontSize: 12, color: "#166534", flex: 1 }}>{entry.existingFileName}</span>
                      <button type="button" onClick={() => window.open(`${import.meta.env.VITE_DMS_API}/batch/view?filePath=${encodeURIComponent(entry.existingFilePath!)}`, "_blank")}
                        style={{ fontSize: 11, color: "#2563eb", background: "none", border: "none", cursor: "pointer", padding: 0 }}>View</button>
                      <button type="button" onClick={() => setFileEntries(prev => prev.map(e => e.subType === entry.subType ? { ...e, existingFileName: undefined, existingFilePath: undefined } : e))}
                        style={{ fontSize: 11, color: "#ef4444", background: "none", border: "none", cursor: "pointer", padding: 0 }}>Replace</button>
                    </div>
                  ) : (
                    <div style={{ display: "flex", alignItems: "center", gap: 8, border: "1px dashed #d1d5db", borderRadius: 6, padding: "5px 10px", background: entry.file ? "#f0fdf4" : "#fafafa", borderColor: entry.file ? "#86efac" : "#d1d5db", cursor: "pointer" }}
                      onClick={() => fileRefs.current[entry.subType]?.click()}>
                      <svg width="13" height="13" viewBox="0 0 14 14" fill="none">
                        <path d="M7 9V3M4 6l3-3 3 3M2 11h10" stroke={entry.file ? "#16a34a" : "#2563eb"} strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
                      </svg>
                      <span style={{ fontSize: 12, color: entry.file ? "#166534" : "#2563eb", fontWeight: 500, flex: 1 }}>
                        {entry.file ? entry.file.name : "Click to upload"}
                      </span>
                      {entry.file && <span style={{ fontSize: 10, color: "#333" }}>{(entry.file.size / 1024).toFixed(0)} KB</span>}
                      {!entry.file && <span style={{ fontSize: 10, color: "#333" }}>Max 1 MB</span>}
                      <input ref={el => { fileRefs.current[entry.subType] = el; }} type="file" accept={entry.accept}
                        style={{ display: "none" }} onChange={e => handleFileChange(entry.subType, e.target.files?.[0] || null)} />
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          <div style={{ display: "flex", gap: 8, marginTop: 8, justifyContent: "flex-end" }}>
            <button type="button" onClick={() => navigate("/batch")}
              style={{ padding: "7px 20px", fontSize: 12, fontWeight: 500, border: "1px solid #e5e7eb", borderRadius: 7, background: "#fff", color: "#374151", cursor: "pointer" }}>
              Cancel
            </button>
            <button type="submit" disabled={saving || fileEntries.length === 0}
              style={{ padding: "7px 24px", fontSize: 12, fontWeight: 600, border: "none", borderRadius: 7, background: saving || fileEntries.length === 0 ? "#e5e7eb" : "#003366", color: saving || fileEntries.length === 0 ? "#9ca3af" : "#fff", cursor: saving || fileEntries.length === 0 ? "not-allowed" : "pointer" }}>
              {saving ? "Saving..." : "Save Batch"}
            </button>
          </div>

        </form>
      </div>
    </main>
  );
};

const cardStyle: React.CSSProperties = {
  background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", padding: "10px 14px",
};
const sectionTitle: React.CSSProperties = {
  fontSize: 10, fontWeight: 600, color: "#333", textTransform: "uppercase",
  letterSpacing: "0.05em", marginBottom: 8, paddingBottom: 6, borderBottom: "1px solid #f3f4f6",
};
const fieldStyle: React.CSSProperties = {
  display: "flex", flexDirection: "column", marginBottom: 7,
};
const labelStyle: React.CSSProperties = {
  fontSize: 11, fontWeight: 600, color: "#374151", marginBottom: 3,
};
const inputStyle: React.CSSProperties = {
  border: "1px solid #e5e7eb", borderRadius: 6, padding: "5px 8px",
  fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none",
};

export default CreateBatch;