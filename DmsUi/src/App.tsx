import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import BatchLayout from "./layouts/BatchLayout";
import SearchBatch from "./pages/batch/SearchBatch";
import CreateBatch from "./pages/batch/CreateBatch";
import InvoiceLayout from "./layouts/InvoiceLayout";
import SearchInvoice from "./pages/invoice/SearchInvoice";
import CreateInvoice from "./pages/invoice/CreateInvoice";
import ReportInvoice from "./pages/invoice/ReportInvoice";
import CapexLayout from "./layouts/CapexLayout";
import SearchCapex from "./pages/capex/SearchCapex";
import CreateCapex from "./pages/capex/CreateCapex";
import SupplierCustomerLayout from "./layouts/SupplierCustomerLayout";
import SearchSupplierCustomer from "./pages/supplier-customer/SearchSupplierCustomer";
import AddSupplierCustomerFile from "./pages/supplier-customer/AddSupplierCustomerFile";
import CreateSupplierCustomer from "./pages/supplier-customer/CreateSupplierCustomer";
import TruckLoadStuffLayout from "./layouts/TruckLoadStuffLayout";
import SearchTruckLoadStuff from "./pages/truck-load-stuff/SearchTruckLoadStuff";
import CreateTruckLoadStuff from "./pages/truck-load-stuff/CreateTruckLoadStuff";
import AccountsLayout from "./layouts/AccountsLayout";
import SearchAccounts from "./pages/accounts/SearchAccounts";
import CreateAccounts from "./pages/accounts/CreateAccounts";
import ReportAccounts from "./pages/accounts/ReportAccounts";
import { useEffect, useState } from "react";
function App() {
  const [globalToast, setGlobalToast] = useState<{ msg: string; type: "success" | "error" } | null>(null);

  useEffect(() => {
    const handleAppToast = (e: any) => {
      setGlobalToast(e.detail);
      setTimeout(() => setGlobalToast(null), 4000);
    };
    window.addEventListener("app-toast", handleAppToast);
    return () => window.removeEventListener("app-toast", handleAppToast);
  }, []);

  return (
    <>
      {globalToast && (
        <div
          style={{
            position: "fixed",
            bottom: 20,
            right: 20,
            background: globalToast.type === "success" ? "#4caf50" : "#f44336",
            color: "#fff",
            padding: "12px 24px",
            borderRadius: 6,
            boxShadow: "0 4px 6px rgba(0,0,0,0.3)",
            zIndex: 9999,
            animation: "fadein 0.3s"
          }}
        >
          {globalToast.msg}
        </div>
      )}
      <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/dashboard" element={<Dashboard />} />

      {/* Batch Details Module — BatchLayout owns Header, Footer, tabs, rights */}
      <Route path="/batch" element={<BatchLayout />}>
        <Route index element={<SearchBatch />} />
        <Route path="create" element={<CreateBatch />} />
      </Route>

      {/* Invoice Document Module */}
      <Route path="/invoice" element={<InvoiceLayout />}>
        <Route index element={<SearchInvoice />} />
        <Route path="create" element={<CreateInvoice />} />
        <Route path="report" element={<ReportInvoice />} />
      </Route>

      {/* CapEx Budget Module */}
      <Route path="/capex" element={<CapexLayout />}>
        <Route index element={<SearchCapex />} />
        <Route path="create" element={<CreateCapex />} />
      </Route>

      {/* Supplier & Customer Module */}
      <Route path="/supplier-customer" element={<SupplierCustomerLayout />}>
        <Route index element={<SearchSupplierCustomer />} />
        <Route path="create" element={<CreateSupplierCustomer />} />
        <Route path="add-files" element={<AddSupplierCustomerFile />} />
      </Route>

      {/* Truck Load Stuff Module */}
      <Route path="/truck-load-stuff" element={<TruckLoadStuffLayout />}>
        <Route index element={<SearchTruckLoadStuff />} />
        <Route path="create" element={<CreateTruckLoadStuff />} />
      </Route>

      {/* Accounts Module */}
      <Route path="/accounts" element={<AccountsLayout />}>
        <Route index element={<SearchAccounts />} />
        <Route path="create" element={<CreateAccounts />} />
        <Route path="report" element={<ReportAccounts />} />
      </Route>
      </Routes>
    </>
  );
}

export default App;