import React, { useState, useEffect, useContext } from "react";
import { AuthContext } from "../../auth/AuthContext";
import { getTlsScmInvoices, createTlsRecords } from "../../api/dmsApi";

const CreateTruckLoadStuff = () => {
  const { user } = useContext(AuthContext);

  const [availableInvoices, setAvailableInvoices] = useState<string[]>([]);
  const [selectedInvoices, setSelectedInvoices] = useState<string[]>([]);
  
  // To handle the visual selection inside the select boxes
  const [leftSelection, setLeftSelection] = useState<string[]>([]);
  const [rightSelection, setRightSelection] = useState<string[]>([]);

  const [file, setFile] = useState<File | null>(null);
  
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const showToast = (msg: string, type: "success" | "error" = "success") => {
    window.dispatchEvent(new CustomEvent("app-toast", { detail: { msg, type } }));
  };

  // Parse session storage for selections
  const selectionsStr = sessionStorage.getItem("dms2Selections");
  const selections = selectionsStr ? JSON.parse(selectionsStr) : null;
  
  // Fetch invoice details using the helper function
  const [invoices, setInvoices] = useState<any[]>([]);

  useEffect(() => {
    if (!selections) return;
    setLoading(true);
    getTlsScmInvoices(selections.com, selections.loc, selections.year)
      .then(res => {
        setAvailableInvoices(res.data || []);
      })
      .catch(err => {
        console.error(err);
        showToast("Failed to load SCM invoices", "error");
      })
      .finally(() => setLoading(false));
  }, []);

  const moveToRight = () => {
    if (leftSelection.length === 0) {
      alert("Please select an Invoice Number to add.");
      return;
    }
    const newAvailable = availableInvoices.filter(inv => !leftSelection.includes(inv));
    const newSelected = [...selectedInvoices, ...leftSelection];
    setAvailableInvoices(newAvailable);
    setSelectedInvoices(newSelected);
    setLeftSelection([]);
  };

  const moveToLeft = () => {
    if (rightSelection.length === 0) {
      alert("Please select an Invoice Number to remove.");
      return;
    }
    const newSelected = selectedInvoices.filter(inv => !rightSelection.includes(inv));
    const newAvailable = [...availableInvoices, ...rightSelection].sort(); // Optional: sort if needed
    setSelectedInvoices(newSelected);
    setAvailableInvoices(newAvailable);
    setRightSelection([]);
  };

  const handleSubmit = () => {
    if (selectedInvoices.length === 0) {
      alert("Please select atleast one Invoice Number");
      return;
    }
    if (!file) {
      alert("Please select File");
      return;
    }

    const ext = file.name.split('.').pop()?.toLowerCase();
    if (ext !== "pdf" && ext !== "doc" && ext !== "docx") {
      alert("Only Word or PDF format are allowed");
      setFile(null);
      return;
    }

    if (!user || !selections) return;

    setSubmitting(true);
    const formData = new FormData();
    formData.append("file", file);
    formData.append("invoiceNos", selectedInvoices.join(","));
    formData.append("companyId", selections.com);
    formData.append("locationId", selections.loc);
    formData.append("year", selections.year);
    formData.append("division", selections.div);
    formData.append("app", selections.app);
    formData.append("createdBy", user.userId);

    createTlsRecords(formData)
      .then(() => {
        showToast("Records created successfully!", "success");
        setSelectedInvoices([]);
        setFile(null);
        // Refresh available invoices
        return getTlsScmInvoices(selections.com, selections.loc, selections.year);
      })
      .then(res => {
        if (res) setAvailableInvoices(res.data || []);
      })
      .catch(err => {
        console.error(err);
        showToast("Failed to create records", "error");
      })
      .finally(() => setSubmitting(false));
  };

  const selectStyle: React.CSSProperties = {
    height: 250, 
    width: "100%", 
    border: "1px solid #e5e7eb", 
    borderRadius: 6, 
    padding: "8px",
    fontSize: 12, 
    color: "#374151",
    outline: "none",
    boxShadow: "inset 0 1px 2px rgba(0,0,0,0.05)"
  };

  const btnStyle = {
    display: "flex", alignItems: "center", justifyContent: "center",
    width: 32, height: 32,
    background: "#003366", color: "#fff",
    border: "none", borderRadius: "50%",
    cursor: "pointer", fontWeight: "bold" as const,
    boxShadow: "0 2px 4px rgba(0, 51, 102, 0.3)"
  };

  return (
    <div style={{ flex: 1, display: "flex", flexDirection: "column", background: "#f3f4f6", height: "calc(100vh - 100px)", padding: "24px 32px" }}>


      <div style={{ background: "#fff", borderRadius: 10, border: "1px solid #e5e7eb", padding: "20px 24px", maxWidth: 800, margin: "0 auto", width: "100%", boxShadow: "0 1px 3px rgba(0,0,0,0.05)" }}>
        <h2 style={{ fontSize: 13, fontWeight: 700, color: "#5f7a98", margin: "0 0 20px 0", borderBottom: "1px solid #f3f4f6", paddingBottom: 12 }}>CREATE TRUCK LOAD STUFF</h2>
        
        {loading ? (
          <div style={{ fontSize: 13, color: "#333", textAlign: "center", padding: "40px 0" }}>Loading invoices from SCM...</div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
            
            <div style={{ display: "flex", gap: 20, alignItems: "center", justifyContent: "center" }}>
              {/* Left Box */}
              <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 8 }}>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#374151" }}>Select Inv No/s. (Multi Select)</label>
                <select 
                  multiple 
                  style={selectStyle}
                  value={leftSelection}
                  onChange={e => setLeftSelection(Array.from(e.target.selectedOptions, option => option.value))}
                >
                  {availableInvoices.map(inv => (
                    <option key={inv} value={inv} style={{ padding: "4px 8px" }}>{inv}</option>
                  ))}
                </select>
              </div>

              {/* Action Buttons */}
              <div style={{ display: "flex", flexDirection: "column", gap: 16, marginTop: 24 }}>
                <button type="button" onClick={moveToRight} style={btnStyle} title="Move to Selected">
                  &gt;
                </button>
                <button type="button" onClick={moveToLeft} style={btnStyle} title="Remove from Selected">
                  &lt;
                </button>
              </div>

              {/* Right Box */}
              <div style={{ flex: 1, display: "flex", flexDirection: "column", gap: 8 }}>
                <label style={{ fontSize: 12, fontWeight: 600, color: "#374151" }}>Selected Inv No/s.</label>
                <select 
                  multiple 
                  style={selectStyle}
                  value={rightSelection}
                  onChange={e => setRightSelection(Array.from(e.target.selectedOptions, option => option.value))}
                >
                  {selectedInvoices.map(inv => (
                    <option key={inv} value={inv} style={{ padding: "4px 8px" }}>{inv}</option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 10 }}>
              <label style={{ fontSize: 12, fontWeight: 600, color: "#374151" }}>Upload File:</label>
              <input 
                type="file" 
                onChange={e => setFile(e.target.files ? e.target.files[0] : null)}
                style={{ fontSize: 13 }}
              />
              <span style={{ color: "#ef4444", fontSize: 11, fontWeight: 500 }}>Note: Please compress file before uploading.</span>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-start", marginTop: 10 }}>
              <button 
                onClick={handleSubmit} 
                disabled={submitting}
                style={{
                  background: submitting ? "#94a3b8" : "#003366",
                  color: "#fff", border: "none", borderRadius: 6,
                  padding: "8px 24px", fontSize: 13, fontWeight: 600,
                  cursor: submitting ? "not-allowed" : "pointer",
                  transition: "background 0.2s"
                }}
              >
                {submitting ? "Submitting..." : "Submit"}
              </button>
            </div>

          </div>
        )}
      </div>
    </div>
  );
};

export default CreateTruckLoadStuff;
