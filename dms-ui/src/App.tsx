import { Routes, Route } from "react-router-dom";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import SearchBatch from "./pages/batch/SearchBatch";
import CreateBatch from "./pages/batch/CreateBatch";
import SearchInvoice from "./pages/invoice/SearchInvoice";
import CreateInvoice from "./pages/invoice/CreateInvoice";

function App() {
  return (
    <Routes>
      <Route path="/" element={<Login />} />
      <Route path="/dashboard" element={<Dashboard />} />

      {/* Batch Details Module */}
      <Route path="/batch" element={<SearchBatch />} />
      <Route path="/batch/create" element={<CreateBatch />} />

      {/* Invoice Document Module */}
      <Route path="/invoice" element={<SearchInvoice />} />
      <Route path="/invoice/create" element={<CreateInvoice />} />
    </Routes>
  );
}

export default App;
