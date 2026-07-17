import React, { useState, useEffect, useContext } from 'react';
import { searchServiceAgreement, removeServiceAgreement } from '../../api/dmsApi';
import { useAppRights } from '../../hooks/useAppRights';
import { useOutletContext } from 'react-router-dom';
import { AuthContext } from '../../auth/AuthContext';

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const SearchServiceAgreement = () => {
  const { selections } = useOutletContext<{ selections: Selections }>();
  const { user } = useContext(AuthContext);
  const { rights, canRemove, loading: rightsLoading } = useAppRights();

  // Sub-division dropdown from user rights
  const [subdivisions, setSubdivisions] = useState<any[]>([]);

  const [filters, setFilters] = useState({
    subdivisionCode: '',
    employeeId: '',
    createdDate: '',
    pan: '',
    paymentStatus: ''
  });

  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const rightsStr = JSON.stringify(rights);
  useEffect(() => {
    if (rights && rights.length > 0) {
      // Extract unique sub-application names for the SERVICE_AGREEMENT app
      const saRights = rights.filter((r: any) => r.applicationName === 'SERVICE_AGREEMENT');
      const uniqueSubDivs = Array.from(new Set(saRights.map((r: any) => r.subApplicationName).filter(Boolean)));
      setSubdivisions(prev => {
        const newSubDivs = uniqueSubDivs.map(name => ({ label: name, value: name }));
        if (JSON.stringify(prev) !== JSON.stringify(newSubDivs)) {
          return newSubDivs;
        }
        return prev;
      });
    }
  }, [rightsStr]);

  const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
    window.dispatchEvent(new CustomEvent('app-toast', { detail: { msg, type } }));
  };

  const handleSearch = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setLoading(true);
    try {
      const params: any = {
        financialYear: selections?.year || '',
        userId: user?.userId || localStorage.getItem('userId') || '',
      };
      if (filters.subdivisionCode) params.subdivisionCode = filters.subdivisionCode;
      if (filters.employeeId) params.employeeId = filters.employeeId;
      if (filters.createdDate) params.createdDate = filters.createdDate;
      if (filters.pan) params.pan = filters.pan;
      if (filters.paymentStatus) params.paymentStatus = filters.paymentStatus;

      const res = await searchServiceAgreement(params);
      setData(Array.isArray(res) ? res : []);
      setHasSearched(true);
      if (Array.isArray(res) && res.length === 0) {
        showToast('Data Not Found For Entered Criteria', 'error');
      }
    } catch (err: any) {
      showToast(err?.response?.data?.error || 'Search failed', 'error');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (row: any) => {
    if (!window.confirm('Are you sure you want to remove this record?')) return;
    try {
      await removeServiceAgreement({
        docCode: row.DOC_CODE,
        subdivisionCode: row.SUBDIVISION_CODE,
        fileName: row.FILE_NAME || '',
        filePath: row.FILE_PATH || ''
      });
      showToast('Record removed successfully');
      handleSearch();
    } catch (err: any) {
      showToast(err?.response?.data?.error || 'Failed to remove', 'error');
    }
  };

  const handleExcelDownload = () => {
    if (data.length === 0) { showToast('No data to export', 'error'); return; }
    import('xlsx').then((XLSX) => {
      const exportData = data.map(row => ({
        'Sub Division': row.SUBDIVISION_NAME || '',
        'Doc Code': row.DOC_CODE || '',
        'Doctor PAN': row.DOCTOR_PAN || '',
        'Doctor Name': row.DOCTOR_NAME || '',
        'Interface Number': row.INTERFACE_APP_NO || '',
        'Event From Date': row.EVENT_FROM_DATE ? new Date(row.EVENT_FROM_DATE).toLocaleDateString('en-GB') : '',
        'Event To Date': row.EVENT_TO_DATE ? new Date(row.EVENT_TO_DATE).toLocaleDateString('en-GB') : '',
        'Amount': row.AMOUNT || '',
        'Event Name': row.EVENT_NAME || '',
        'CME Log No': row.CME_LOG_NO || '',
        'Doctor Code': row.DOCTOR_CODE || '',
        'In Favour Of': row.IN_FAVOUR_OF || '',
        'Employee ID': row.EMPLOYEE_ID || '',
        'Employee Name': row.EMPLOYEE_NAME || '',
        'RC Code': row.RC_CODE || '',
        'Voucher No': row.VOUCHER_NO || '',
        'Voucher Date': row.VOUCHER_DATE ? new Date(row.VOUCHER_DATE).toLocaleDateString('en-GB') : '',
        'Cheque No': row.CHEQUE_NO || '',
        'Cheque Date': row.CHEQUE_DATE ? new Date(row.CHEQUE_DATE).toLocaleDateString('en-GB') : '',
        'Created By': row.CREATED_BY || '',
        'Created On': row.CREATED_ON || '',
      }));
      const ws = XLSX.utils.json_to_sheet(exportData);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, 'ServiceAgreementReport');
      XLSX.writeFile(wb, 'ServiceAgreementReport.xlsx');
    });
  };

  const getViewUrl = (row: any) => {
    if (!row.FILE_PATH) return '#';
    return `${import.meta.env.VITE_DMS_API}/service-agreement/view?filePath=${encodeURIComponent(row.FILE_PATH)}`;
  };

  const fmtDate = (val: any) => {
    if (!val) return '';
    try { return new Date(val).toLocaleDateString('en-GB'); } catch { return ''; }
  };

  const fmtDateTime = (val: any) => {
    if (!val) return '';
    try { return new Date(val).toLocaleString('en-GB'); } catch { return ''; }
  };

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', background: '#f3f4f6', height: 'calc(100vh - 100px)', position: 'relative' }}>
      <main style={{ flex: 1, overflowY: 'auto', padding: '24px 32px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 20, maxWidth: 1400, margin: '0 auto' }}>

          {/* Search Filters */}
          <div style={{ background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb', padding: '16px 20px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
              <h2 style={sectionTitle}>SEARCH FILTERS</h2>
              <button onClick={handleExcelDownload} style={exportBtn}>Save As Excel</button>
            </div>

            <form onSubmit={handleSearch}>
              <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end', flexWrap: 'wrap' }}>
                <div>
                  <label style={labelStyle}>Sub Division</label>
                  <select value={filters.subdivisionCode}
                    onChange={(e: any) => setFilters(f => ({ ...f, subdivisionCode: e.target.value }))}
                    style={{ ...inputStyle, width: 140 }}>
                    <option value="">SELECT</option>
                    {subdivisions.map(s => (
                      <option key={s.value} value={s.value}>{s.label}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label style={labelStyle}>Employee ID</label>
                  <input type="text" value={filters.employeeId}
                    onChange={(e: any) => setFilters(f => ({ ...f, employeeId: e.target.value }))}
                    style={{ ...inputStyle, width: 110 }} />
                </div>
                <div>
                  <label style={labelStyle}>Entry Date</label>
                  <input type="date" value={filters.createdDate}
                    onChange={(e: any) => setFilters(f => ({ ...f, createdDate: e.target.value }))}
                    style={{ ...inputStyle, width: 140 }} />
                </div>
                <div>
                  <label style={labelStyle}>Doctor's PAN</label>
                  <input type="text" value={filters.pan}
                    onChange={(e: any) => setFilters(f => ({ ...f, pan: e.target.value }))}
                    style={{ ...inputStyle, width: 120 }} />
                </div>
                <div>
                  <label style={labelStyle}>Payment Status</label>
                  <select value={filters.paymentStatus}
                    onChange={(e: any) => setFilters(f => ({ ...f, paymentStatus: e.target.value }))}
                    style={{ ...inputStyle, width: 100 }}>
                    <option value="">ALL</option>
                    <option value="Paid">Paid</option>
                    <option value="Unpaid">Unpaid</option>
                  </select>
                </div>
                <button type="submit" disabled={loading} style={{ ...primaryButton, opacity: loading ? 0.6 : 1 }}>
                  {loading ? 'Searching...' : 'GO'}
                </button>
              </div>
            </form>
          </div>

          {/* Results Table */}
          <div style={{ background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb', overflow: 'hidden', flex: 1 }}>
            {hasSearched && data.length > 0 ? (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '250%', borderCollapse: 'collapse', textAlign: 'left', fontSize: 11 }}>
                  <thead style={{ background: '#f3f4f6' }}>
                    <tr>
                      <th style={thStyle}>Sub Division</th>
                      <th style={thStyle}>Doc Code</th>
                      <th style={thStyle}>Doctor PAN</th>
                      <th style={thStyle}>Doctor Name</th>
                      <th style={thStyle}>Interface Number</th>
                      <th style={thStyle}>Event From Date</th>
                      <th style={thStyle}>Event To Date</th>
                      <th style={thStyle}>Amount</th>
                      <th style={thStyle}>Filename</th>
                      {canRemove && <th style={thStyle}>Remove</th>}
                      <th style={thStyle}>Event Name</th>
                      <th style={thStyle}>CME Log Number</th>
                      <th style={thStyle}>Doctor Code</th>
                      <th style={thStyle}>In Favour Of</th>
                      <th style={thStyle}>Employee ID</th>
                      <th style={thStyle}>Employee Name</th>
                      <th style={thStyle}>RC Code</th>
                      <th style={thStyle}>Voucher No.</th>
                      <th style={thStyle}>Voucher Date</th>
                      <th style={thStyle}>Cheque No.</th>
                      <th style={thStyle}>Cheque Date</th>
                      <th style={thStyle}>Created By</th>
                      <th style={thStyle}>Created On</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.map((row, idx) => (
                      <tr key={idx} style={{ borderBottom: '1px solid #e5e7eb' }}
                        onMouseEnter={e => e.currentTarget.style.background = '#f9fafb'}
                        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                        <td style={tdStyle}>{row.SUBDIVISION_NAME}</td>
                        <td style={{ ...tdStyle, textAlign: 'right' }}>{row.DOC_CODE}</td>
                        <td style={tdStyle}>{row.DOCTOR_PAN}</td>
                        <td style={tdStyle}>{row.DOCTOR_NAME}</td>
                        <td style={tdStyle}>{row.INTERFACE_APP_NO}</td>
                        <td style={tdStyle}>{fmtDate(row.EVENT_FROM_DATE)}</td>
                        <td style={tdStyle}>{fmtDate(row.EVENT_TO_DATE)}</td>
                        <td style={{ ...tdStyle, textAlign: 'right' }}>{row.AMOUNT}</td>
                        <td style={tdStyle}>
                          {row.FILE_NAME && (
                            <a href={getViewUrl(row)} target="_blank" rel="noreferrer"
                              style={{ color: '#0ea5e9', textDecoration: 'none', fontWeight: 500 }}>
                              {row.FILE_NAME}
                            </a>
                          )}
                        </td>
                        {canRemove && (
                          <td style={tdStyle}>
                            {!row.CHEQUE_NO && row.FILE_NAME && (
                              <button onClick={() => handleRemove(row)}
                                style={{ padding: '3px 8px', background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: 4, cursor: 'pointer', fontSize: 11, fontWeight: 500, color: '#ef4444' }}>
                                Remove
                              </button>
                            )}
                          </td>
                        )}
                        <td style={tdStyle}>{row.EVENT_NAME}</td>
                        <td style={tdStyle}>{row.CME_LOG_NO}</td>
                        <td style={tdStyle}>{row.DOCTOR_CODE}</td>
                        <td style={tdStyle}>{row.IN_FAVOUR_OF}</td>
                        <td style={tdStyle}>{row.EMPLOYEE_ID}</td>
                        <td style={tdStyle}>{row.EMPLOYEE_NAME}</td>
                        <td style={tdStyle}>{row.RC_CODE}</td>
                        <td style={tdStyle}>{row.VOUCHER_NO}</td>
                        <td style={tdStyle}>{fmtDate(row.VOUCHER_DATE)}</td>
                        <td style={tdStyle}>{row.CHEQUE_NO}</td>
                        <td style={tdStyle}>{fmtDate(row.CHEQUE_DATE)}</td>
                        <td style={tdStyle}>{row.CREATED_BY}</td>
                        <td style={tdStyle}>{fmtDateTime(row.CREATED_ON)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div style={{ padding: 32, textAlign: 'center', color: '#9ca3af', fontSize: 13 }}>
                {rightsLoading ? 'Loading...' : hasSearched ? 'Data Not Found For Entered Criteria' : 'Click GO to search'}
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
};

// --- COMPACT STYLES (matching other modules) ---
const sectionTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, color: '#111', letterSpacing: '0.02em', textTransform: 'uppercase' };
const labelStyle: React.CSSProperties = { display: 'block', fontSize: 10, fontWeight: 600, color: '#333', marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.04em' };
const inputStyle: React.CSSProperties = { border: '1px solid #e5e7eb', borderRadius: 6, padding: '6px 8px', fontSize: 12, color: '#374151', background: '#f9fafb', outline: 'none', boxSizing: 'border-box' as const };
const primaryButton: React.CSSProperties = { background: '#003366', color: '#fff', border: 'none', borderRadius: 6, padding: '6px 20px', fontSize: 12, fontWeight: 600, cursor: 'pointer', height: 28 };
const exportBtn: React.CSSProperties = { background: '#003366', color: '#fff', border: 'none', borderRadius: 6, padding: '5px 16px', fontSize: 12, fontWeight: 600, cursor: 'pointer', height: 28 };
const thStyle: React.CSSProperties = { padding: '8px 12px', fontWeight: 600, color: '#003366', borderBottom: '2px solid #e5e7eb', whiteSpace: 'nowrap', fontSize: 11 };
const tdStyle: React.CSSProperties = { padding: '6px 12px', color: '#333', whiteSpace: 'nowrap', fontSize: 11 };

export default SearchServiceAgreement;
