import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import BatchLayout from "./layouts/BatchLayout";
import SearchBatch from "./pages/batch/SearchBatch";
import CreateBatch from "./pages/batch/CreateBatch";
import InvoiceLayout from "./layouts/InvoiceLayout";
import SearchInvoice from "./pages/invoice/SearchInvoice";
import CreateInvoice from "./pages/invoice/CreateInvoice";

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
    </Routes>
  );
}

export default App;