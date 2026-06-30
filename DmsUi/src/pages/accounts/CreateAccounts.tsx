import React, { useContext, useEffect, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { AuthContext } from "../../auth/AuthContext";
import {
  getAccountsDaybooks,
  getAccountsDocList,
  getAccountsDocDetails,
  getAccountsFieldRequired,
  uploadAccountsDocument,
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

const CreateAccounts: React.FC = () => {
  const { user } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();

  const [daybooks, setDaybooks] = useState<{ code: string; name: string }[]>([]);
  const [selectedDaybook, setSelectedDaybook] = useState("");
  const [selectedMonth, setSelectedMonth] = useState("04");
  const [docCodes, setDocCodes] = useState<string[]>([]);
  const [selectedDocCode, setSelectedDocCode] = useState("");
  const [docDetails, setDocDetails] = useState<any>(null);
  const [fieldRequired, setFieldRequired] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);
  const [disabledMonths, setDisabledMonths] = useState<Set<string>>(new Set());
  const fileRef = React.useRef<HTMLInputElement>(null);

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
        if (data.length > 0) {
          const first = data[0].code + "~" + data[0].name;
          setSelectedDaybook(first);
        }
      })
      .catch(() => showToast("Failed to load daybooks", "error"));
  }, []);

  // Compute disabled months (future months for current FY)
  useEffect(() => {
    if (!selections) return;
    const year = selections.year;
    const now = new Date();
    const currentMonth = now.getMonth() + 1; // 1-12
    let fiscalYear: string;
    let newMonth: number;
    if (currentMonth <= 3) {
      fiscalYear = `${now.getFullYear() - 1}-${now.getFullYear()}`;
      newMonth = currentMonth + 9;
    } else {
      fiscalYear = `${now.getFullYear()}-${now.getFullYear() + 1}`;
      newMonth = currentMonth - 3;
    }
    if (year >= fiscalYear) {
      const disabled = new Set<string>();
      MONTHS.forEach((m, idx) => {
        if (idx >= newMonth) disabled.add(m.value);
      });
      setDisabledMonths(disabled);
    } else {
      setDisabledMonths(new Set());
    }
  }, [selections]);

  // Load doc codes when daybook or month changes
  useEffect(() => {
    if (!selectedDaybook || !selections) return;
    const daybookCode = selectedDaybook.split("~")[0];
    setSelectedDocCode("");
    setDocDetails(null);
    setFile(null);
    if (fileRef.current) fileRef.current.value = "";

    getAccountsDocList({
      locationId: selections.loc,
      daybookCode,
      year: selections.year,
      month: selectedMonth,
    })
      .then(res => {
        const data = res.data || [];
        const codes = data.map((r: any) => String(r.DOC_CODE || r.doc_code || ""));
        setDocCodes(codes);
      })
      .catch(() => {
        setDocCodes([]);
        showToast("Failed to load doc codes", "error");
      });

    // Check field required
    getAccountsFieldRequired(daybookCode)
      .then(res => setFieldRequired(res.data === true))
      .catch(() => setFieldRequired(false));
  }, [selectedDaybook, selectedMonth, selections]);

  // Load doc details when doc code selected
  useEffect(() => {
    if (!selectedDocCode || selectedDocCode === "Select" || !selections) {
      setDocDetails(null);
      return;
    }
    const daybookCode = selectedDaybook.split("~")[0];
    getAccountsDocDetails({
      companyId: selections.com,
      locationId: selections.loc,
      daybookCode,
      docCode: selectedDocCode,
      year: selections.year,
      month: selectedMonth,
    })
      .then(res => setDocDetails(res.data || null))
      .catch(() => {
        setDocDetails(null);
        showToast("Failed to load doc details", "error");
      });
  }, [selectedDocCode]);

  const handleSubmit = async () => {
    if (!file) {
      showToast("Please upload a PDF file", "error");
      return;
    }
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      showToast("Only PDF files are allowed", "error");
      return;
    }
    if (!selectedDocCode || !selections || !docDetails) return;

    const daybookParts = selectedDaybook.split("~");
    const formData = new FormData();
    formData.append("companyId", selections.com);
    formData.append("locationId", selections.loc);
    formData.append("divisionName", selections.div);
    formData.append("applicationName", selections.app);
    formData.append("financialYear", selections.year);
    formData.append("daybookCode", daybookParts[0]);
    formData.append("daybookName", daybookParts[1] || "");
    formData.append("accountCode", docDetails.ACCOUNT_CODE || docDetails.account_code || "");
    formData.append("accountName", docDetails.ACCOUNT_NAME || docDetails.account_name || "");
    formData.append("docCode", selectedDocCode);
    formData.append("docDate", docDetails.DOC_DATE ? String(docDetails.DOC_DATE).substring(0, 10) : "null");
    formData.append("docYear", docDetails.DOC_YEAR || docDetails.doc_year || "");
    formData.append("billNumber", docDetails.BILL_NUMBER || docDetails.bill_number || "");
    formData.append("billDate", docDetails.BILL_DATE ? String(docDetails.BILL_DATE).substring(0, 10) : "null");
    formData.append("tranAmount", String(docDetails.TRAN_AMOUNT || docDetails.tran_amount || "0"));
    formData.append("docMonth", selectedMonth);
    formData.append("createdBy", user?.userId || "");
    formData.append("file", file);

    setSubmitting(true);
    try {
      await uploadAccountsDocument(formData);
      showToast("File uploaded successfully", "success");
      // Reset
      setSelectedDocCode("");
      setDocDetails(null);
      setFile(null);
      if (fileRef.current) fileRef.current.value = "";
      // Refresh doc codes
      const daybookCode = daybookParts[0];
      const res = await getAccountsDocList({
        locationId: selections.loc,
        daybookCode,
        year: selections.year,
        month: selectedMonth,
      });
      const codes = (res.data || []).map((r: any) => String(r.DOC_CODE || r.doc_code || ""));
      setDocCodes(codes);
    } catch (err: any) {
      showToast(err?.response?.data || "Upload failed", "error");
    } finally {
      setSubmitting(false);
    }
  };

  if (!user || !selections) return null;

  return (
    <div style={{ flex: 1, overflow: "auto", background: "#f3f4f6" }}>
      {toast && (
        <div style={{ position: "fixed", bottom: 24, right: 24, zIndex: 50, padding: "8px 14px", borderRadius: 8, color: "#fff", fontSize: 12, fontWeight: 500, background: toast.type === "success" ? "#16a34a" : "#dc2626" }}>
          {toast.msg}
        </div>
      )}

      <main style={{ padding: "24px 32px" }}>
        <div style={{ maxWidth: 700, margin: "0 auto" }}>
          <div style={{ background: "#fff", borderRadius: 8, border: "1px solid #e5e7eb", padding: "24px 28px" }}>
            <h2 style={{ fontSize: 16, fontWeight: 600, color: "#003366", margin: "0 0 20px 0", textAlign: "center" }}>
              Create Accounts Document
            </h2>

            {/* Daybook */}
            <div style={{ display: "flex", alignItems: "center", marginBottom: 14, gap: 12 }}>
              <label style={{ ...labelStyle, minWidth: 140, marginBottom: 0 }}>Select Daybook Code</label>
              <select value={selectedDaybook} onChange={e => setSelectedDaybook(e.target.value)} style={{ ...inputStyle, flex: 1 }}>
                {daybooks.map(d => (
                  <option key={d.code} value={`${d.code}~${d.name}`}>{d.code}~{d.name}</option>
                ))}
              </select>
            </div>

            {/* Month */}
            <div style={{ display: "flex", alignItems: "center", marginBottom: 14, gap: 12 }}>
              <label style={{ ...labelStyle, minWidth: 140, marginBottom: 0 }}>Select Month</label>
              <select value={selectedMonth} onChange={e => setSelectedMonth(e.target.value)} style={{ ...inputStyle, flex: 1 }}>
                {MONTHS.map(m => (
                  <option key={m.value} value={m.value} disabled={disabledMonths.has(m.value)}>{m.label}</option>
                ))}
              </select>
            </div>

            {/* Doc Code */}
            <div style={{ display: "flex", alignItems: "center", marginBottom: 14, gap: 12 }}>
              <label style={{ ...labelStyle, minWidth: 140, marginBottom: 0 }}>Select Doc Code</label>
              <select value={selectedDocCode} onChange={e => setSelectedDocCode(e.target.value)} style={{ ...inputStyle, flex: 1 }}>
                <option value="">Select</option>
                {docCodes.map(c => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            {/* Details (shown when doc code selected) */}
            {docDetails && (
              <>
                {fieldRequired && (
                  <>
                    <div style={{ display: "flex", alignItems: "center", marginBottom: 14, gap: 12 }}>
                      <label style={{ ...labelStyle, minWidth: 140, marginBottom: 0 }}>Account Name</label>
                      <input type="text" readOnly value={docDetails.ACCOUNT_NAME || docDetails.account_name || ""} style={{ ...inputStyle, flex: 1, background: "#f3f4f6" }} />
                    </div>
                    <div style={{ display: "flex", alignItems: "center", marginBottom: 14, gap: 12 }}>
                      <label style={{ ...labelStyle, minWidth: 140, marginBottom: 0 }}>Bill Number</label>
                      <input type="text" readOnly value={docDetails.BILL_NUMBER || docDetails.bill_number || ""} style={{ ...inputStyle, flex: 1, background: "#f3f4f6" }} />
                    </div>
                  </>
                )}

                <div style={{ display: "flex", alignItems: "flex-start", marginBottom: 14, gap: 12 }}>
                  <label style={{ ...labelStyle, minWidth: 140, marginBottom: 0, marginTop: 2 }}>Upload File</label>
                  <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                    <input
                      type="file"
                      ref={fileRef}
                      accept=".pdf"
                      onChange={e => setFile(e.target.files?.[0] || null)}
                      style={{ fontSize: 12, color: "#374151" }}
                    />
                    <span style={{ fontSize: 10, color: "#dc2626", fontWeight: 500 }}>* PDF File Size cannot exceed 1 MB</span>
                  </div>
                </div>

                <div style={{ display: "flex", justifyContent: "center", marginTop: 20 }}>
                  <button onClick={handleSubmit} disabled={submitting} style={{ ...primaryButton, opacity: submitting ? 0.6 : 1, padding: "8px 40px" }}>
                    {submitting ? "Submitting..." : "Submit"}
                  </button>
                </div>
              </>
            )}

            {selectedDocCode && !docDetails && (
              <div style={{ textAlign: "center", padding: "20px 0", color: "#6b7280", fontSize: 12 }}>
                Loading details...
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

const labelStyle: React.CSSProperties = { display: "block", fontSize: 11, fontWeight: 600, color: "#6b7280", textTransform: "uppercase", letterSpacing: "0.04em", marginBottom: 4 };
const inputStyle: React.CSSProperties = { width: "100%", border: "1px solid #e5e7eb", borderRadius: 6, padding: "6px 8px", fontSize: 12, color: "#374151", background: "#f9fafb", outline: "none", boxSizing: "border-box" as const };
const primaryButton: React.CSSProperties = { background: "#003366", color: "#fff", border: "none", borderRadius: 6, padding: "5px 24px", fontSize: 12, fontWeight: 600, cursor: "pointer", height: 32 };

export default CreateAccounts;
