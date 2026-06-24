import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import BatchLayout from "./layouts/BatchLayout";
import SearchBatch from "./pages/batch/SearchBatch";
import CreateBatch from "./pages/batch/CreateBatch";
import InvoiceLayout from "./layouts/InvoiceLayout";
import SearchInvoice from "./pages/invoice/SearchInvoice";
import CreateInvoice from "./pages/invoice/CreateInvoice";
import CapexLayout from "./layouts/CapexLayout";
import SearchCapex from "./pages/capex/SearchCapex";
import CreateCapex from "./pages/capex/CreateCapex";
import SupplierCustomerLayout from "./layouts/SupplierCustomerLayout";
import SearchSupplierCustomer from "./pages/supplier-customer/SearchSupplierCustomer";
import AddSupplierCustomerFile from "./pages/supplier-customer/AddSupplierCustomerFile";
function App() {
  return (
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
      </Route>

      {/* CapEx Budget Module */}
      <Route path="/capex" element={<CapexLayout />}>
        <Route index element={<SearchCapex />} />
        <Route path="create" element={<CreateCapex />} />
      </Route>

      {/* Supplier & Customer Module */}
      <Route path="/supplier-customer" element={<SupplierCustomerLayout />}>
        <Route index element={<SearchSupplierCustomer />} />
        <Route path="add-files" element={<AddSupplierCustomerFile />} />
      </Route>
    </Routes>
  );
}

export default App;