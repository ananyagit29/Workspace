import React, { useState, useContext } from 'react';
import { AuthContext } from '../../auth/AuthContext';
import { useOutletContext } from 'react-router-dom';
import { getDoctorDetails, getEmployeeDetails, getInterfaceDetails, createServiceAgreement } from '../../api/dmsApi';

interface Selections {
  com: string; div: string; loc: string; app: string;
  year: string; subApp: string | null; module: string | null;
}

const CreateServiceAgreement = () => {
  const { user } = useContext(AuthContext);
  const { selections } = useOutletContext<{ selections: Selections }>();

  const [pan, setPan] = useState('');
  const [doctorFound, setDoctorFound] = useState(false);
  const [loading, setLoading] = useState(false);

  // Doctor fields
  const [doctorName, setDoctorName] = useState('');
  const [doctorCode, setDoctorCode] = useState('');
  const [inFavourOf, setInFavourOf] = useState('');

  // Employee fields
  const [employeeId, setEmployeeId] = useState('');
  const [employeeName, setEmployeeName] = useState('');
  const [rcCode, setRcCode] = useState('');
  const [rcCodeReadonly, setRcCodeReadonly] = useState(false);

  // Subdivision
  const [subdivisions, setSubdivisions] = useState<any[]>([]);
  const [selectedSubdivision, setSelectedSubdivision] = useState('');
  const [subDivDisabled, setSubDivDisabled] = useState(true);

  // Other fields
  const [interfaceAppNo, setInterfaceAppNo] = useState('');
  const [interfaceError, setInterfaceError] = useState('');
  const [submitDisabled, setSubmitDisabled] = useState(false);
  const [cmeLogNo, setCmeLogNo] = useState('');
  const [amount, setAmount] = useState('');
  const [eventFromDate, setEventFromDate] = useState('');
  const [eventToDate, setEventToDate] = useState('');
  const [eventName, setEventName] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const handleGo = async () => {
    if (!pan) { setError('Please enter a PAN number'); return; }
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const res = await getDoctorDetails(pan, selections?.com || '', user?.userId || '');
      if (res.doctor) {
        setDoctorName(res.doctor.name || '');
        setDoctorCode(res.doctor.code || '');
        setInFavourOf(res.doctor.in_favour_of || '');
        setDoctorFound(true);
      }
      if (res.subdivisions) {
        setSubdivisions(res.subdivisions);
      }
    } catch (err: any) {
      setError(err?.response?.data?.error || 'Doctor Master Not Found');
      setDoctorFound(false);
    } finally {
      setLoading(false);
    }
  };

  const handleEmployeeBlur = async () => {
    if (!employeeId) {
      setEmployeeName('');
      setRcCode('');
      setSubDivDisabled(true);
      setSubmitDisabled(false);
      setError('');
      return;
    }
    setError('');
    try {
      const res = await getEmployeeDetails(employeeId, 'SERVICE_AGREEMENT', user?.userId || '');
      if (!res.employee_name || res.employee_name === 'null') {
        setEmployeeName('');
        setRcCode('');
        setSubDivDisabled(true);
        setError('Employee not found');
        setSubmitDisabled(true);
      } else {
        setEmployeeName(res.employee_name);
        setRcCode(res.rc_code || '');
        setError('');
        setSubmitDisabled(false);

        const subCode = res.subdivision_code;
        if (subCode && subCode !== 'null') {
          // Auto-select but DO NOT lock subdivision according to user request
          setSelectedSubdivision(subCode);
          setRcCodeReadonly(true);
        } else {
          setRcCodeReadonly(false);
        }
        // User explicitly wants it enabled when employee is entered:
        setSubDivDisabled(false);
      }
    } catch (err: any) {
      setError(err?.response?.data?.error || 'Employee not found');
      setEmployeeName('');
      setRcCode('');
    }
  };

  const handleInterfaceBlur = async () => {
    if (!interfaceAppNo) return;
    setInterfaceError('');
    try {
      const res = await getInterfaceDetails(interfaceAppNo);
      if (res.DOC_CODE && res.DOC_CODE !== null) {
        const msg = `Record exists for this Interface no. against Doc Code: ${res.DOC_CODE}, ` +
          `Created On: ${res.CREATED_ON}, Sub Division: ${res.SUBDIVISION_NAME}, ` +
          `Doctor Name: ${res.DOCTOR_NAME}, Employee Name: ${res.EMPLOYEE_NAME}`;
        setInterfaceError(msg);
        setSubmitDisabled(true);
      } else {
        setInterfaceError('');
        setSubmitDisabled(false);
      }
    } catch {
      setInterfaceError('');
    }
  };

  const handleDateCheck = () => {
    if (eventToDate && eventFromDate && eventToDate < eventFromDate) {
      setError('Event To Date cannot be less than Event From Date');
      setEventToDate('');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    // Validations matching old system doClick()
    if (!interfaceAppNo) { alert('Please Enter Interface Application Number!'); return; }
    if (!cmeLogNo) { alert('Please Enter CME Log Number!'); return; }
    if (!employeeId) { alert('Please Enter Employee Number!'); return; }
    if (!amount) { alert('Please Enter Amount'); return; }
    if (!eventFromDate) { alert('Please Select Event From Date'); return; }
    if (!eventToDate) { alert('Please Select Event To Date'); return; }
    if (eventToDate < eventFromDate) { alert('Event To Date cannot be less than Event From Date'); return; }
    if (!eventName) { alert('Please Select Event Name'); return; }
    if (!file) { alert('Please Upload File'); return; }
    if (!selectedSubdivision) { alert('Please Select Sub Division'); return; }

    setLoading(true);
    setError('');
    setMessage('');

    const subDivObj = subdivisions.find((s: any) => s.code === selectedSubdivision);

    const formData = new FormData();
    formData.append('companyId', selections?.com || '');
    formData.append('locationId', selections?.loc || '');
    formData.append('divisionName', selections?.div || '');
    formData.append('applicationName', 'SERVICE_AGREEMENT');
    formData.append('financialYear', selections?.year || '');
    formData.append('subdivisionCode', selectedSubdivision);
    formData.append('subdivisionName', subDivObj ? subDivObj.name : '');
    formData.append('doctorPan', pan);
    formData.append('doctorCode', doctorCode);
    formData.append('doctorName', doctorName);
    formData.append('inFavourOf', inFavourOf);
    formData.append('employeeId', employeeId);
    formData.append('employeeName', employeeName);
    formData.append('rcCode', rcCode);
    formData.append('interfaceAppNo', interfaceAppNo);
    formData.append('cmeLogNo', cmeLogNo);
    formData.append('amount', amount);
    formData.append('eventFromDate', eventFromDate);
    formData.append('eventToDate', eventToDate);
    formData.append('eventName', eventName);
    formData.append('createdBy', user?.userId || '');
    formData.append('file', file);

    try {
      const res = await createServiceAgreement(formData);
      setMessage(res.message || 'Created Successfully');
      // Reset form
      setDoctorFound(false);
      setPan(''); setEmployeeId(''); setEmployeeName(''); setRcCode('');
      setSelectedSubdivision(''); setInterfaceAppNo(''); setCmeLogNo('');
      setAmount(''); setEventFromDate(''); setEventToDate(''); setEventName('');
      setFile(null); setSubDivDisabled(false); setSubmitDisabled(false);
    } catch (err: any) {
      setError(err?.response?.data?.error || 'Failed to create');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ flex: 1, overflowY: 'auto', padding: '24px 32px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      
      {/* Card 1: Doctor PAN Search */}
      <div style={{ background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb', padding: '24px 32px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)', display: 'flex', flexDirection: 'column', alignItems: 'center', width: '100%', maxWidth: 500 }}>
        <h2 style={{ ...sectionTitle, marginBottom: 20 }}>CREATE SERVICE AGREEMENT</h2>

        {message && <div style={{ padding: '8px 12px', background: '#ecfdf5', color: '#065f46', borderRadius: 6, fontSize: 12, marginBottom: 16, border: '1px solid #a7f3d0', width: '100%', textAlign: 'center' }}>{message}</div>}
        {error && <div style={{ padding: '8px 12px', background: '#fef2f2', color: '#991b1b', borderRadius: 6, fontSize: 12, marginBottom: 16, border: '1px solid #fca5a5', width: '100%', textAlign: 'center' }}>{error}</div>}

        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 16 }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label style={labelStyle}>Enter Doctor's PAN :</label>
            <input type="text" value={pan}
              onChange={(e: any) => setPan(e.target.value.toUpperCase())}
              disabled={doctorFound}
              style={{ ...inputStyle, width: 220, background: doctorFound ? '#f3f4f6' : '#fff' }} />
          </div>
          <button onClick={handleGo} disabled={loading || doctorFound} style={{ ...primaryButton, padding: '7px 24px', opacity: (loading || doctorFound) ? 0.6 : 1 }}>
            {loading ? 'Searching...' : (doctorFound ? 'Found' : 'GO')}
          </button>
          {doctorFound && (
            <button onClick={() => {
              setDoctorFound(false);
              setPan(''); setEmployeeId(''); setEmployeeName(''); setRcCode('');
              setSelectedSubdivision(''); setInterfaceAppNo(''); setCmeLogNo('');
              setAmount(''); setEventFromDate(''); setEventToDate(''); setEventName('');
              setFile(null); setSubDivDisabled(true); setSubmitDisabled(false);
              setMessage(''); setError('');
            }} style={{ ...cancelButton, padding: '7px 24px' }}>
              Reset
            </button>
          )}
        </div>
      </div>

      {/* Card 2: Form Details */}
      {doctorFound && (
        <div style={{ background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb', padding: '32px 40px', boxShadow: '0 1px 2px rgba(0,0,0,0.05)', width: '100%', maxWidth: 900, marginTop: 24 }}>
          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', columnGap: '48px' }}>
              {/* Column 1 */}
              <div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Doctor Code :</label>
                  <input type="text" value={doctorCode} readOnly style={{ ...inputFull, background: '#f3f4f6' }} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Doctor Name :</label>
                  <input type="text" value={doctorName} readOnly style={{ ...inputFull, background: '#f3f4f6' }} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>In Favour Of :</label>
                  <input type="text" value={inFavourOf} readOnly style={{ ...inputFull, background: '#f3f4f6' }} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Employee ID :</label>
                  <input type="text" value={employeeId}
                    onChange={(e: any) => setEmployeeId(e.target.value)}
                    onBlur={handleEmployeeBlur}
                    style={inputFull} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Employee Name :</label>
                  <input type="text" value={employeeName} readOnly style={{ ...inputFull, background: '#f3f4f6' }} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>RC Code :</label>
                  <input type="text" value={rcCode}
                    readOnly={rcCodeReadonly}
                    onChange={(e: any) => setRcCode(e.target.value)}
                    style={{ ...inputFull, background: rcCodeReadonly ? '#f3f4f6' : '#fff' }} />
                </div>
              </div>

              {/* Column 2 */}
              <div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Select Sub Division :</label>
                  <select value={selectedSubdivision}
                    onChange={(e: any) => setSelectedSubdivision(e.target.value)}
                    disabled={subDivDisabled}
                    style={{ ...inputFull, background: subDivDisabled ? '#f3f4f6' : '#fff' }}>
                    <option value="">SELECT</option>
                    {subdivisions.map((s: any) => (
                      <option key={s.code} value={s.code}>{s.name}</option>
                    ))}
                  </select>
                </div>
                <div style={{ ...fieldRow, marginBottom: interfaceError ? 4 : 16 }}>
                  <label style={labelStyle}>Interface App No. :</label>
                  <input type="text" maxLength={20} value={interfaceAppNo}
                    onChange={(e: any) => setInterfaceAppNo(e.target.value)}
                    onBlur={handleInterfaceBlur}
                    style={inputFull} />
                </div>
                {interfaceError && (
                  <div style={{ color: '#dc2626', fontSize: 11, marginBottom: 12 }}>{interfaceError}</div>
                )}
                <div style={fieldRow}>
                  <label style={labelStyle}>CME Log No. :</label>
                  <input type="text" maxLength={14} value={cmeLogNo}
                    onChange={(e: any) => setCmeLogNo(e.target.value.toUpperCase())}
                    style={inputFull} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Amount :</label>
                  <input type="text" maxLength={6} value={amount}
                    onChange={(e: any) => setAmount(e.target.value)}
                    style={inputFull} />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                  <div style={fieldRow}>
                    <label style={labelStyle}>Event From Date :</label>
                    <input type="date" value={eventFromDate}
                      onChange={(e: any) => setEventFromDate(e.target.value)}
                      style={inputFull} />
                  </div>
                  <div style={fieldRow}>
                    <label style={labelStyle}>Event To Date :</label>
                    <input type="date" value={eventToDate}
                      onChange={(e: any) => setEventToDate(e.target.value)}
                      onBlur={handleDateCheck}
                      style={inputFull} />
                  </div>
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Event Name :</label>
                  <input type="text" value={eventName}
                    onChange={(e: any) => setEventName(e.target.value.toUpperCase())}
                    style={inputFull} />
                </div>
                <div style={fieldRow}>
                  <label style={labelStyle}>Upload File :</label>
                  <input type="file" accept="application/pdf"
                    onChange={(e: any) => setFile(e.target.files?.[0] || null)}
                    style={{ fontSize: 12, padding: '4px 0' }} />
                </div>
              </div>
            </div>

            <div style={{ marginTop: 24, display: 'flex', justifyContent: 'center', gap: 16, borderTop: '1px solid #e5e7eb', paddingTop: 24 }}>
              <button type="submit" disabled={loading || submitDisabled} style={{ ...primaryButton, opacity: (loading || submitDisabled) ? 0.5 : 1 }}>
                {loading ? 'Submitting...' : 'Submit'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};

// --- COMPACT STYLES (matching other modules) ---
const sectionTitle: React.CSSProperties = { fontSize: 13, fontWeight: 700, color: '#111', letterSpacing: '0.02em', textTransform: 'uppercase' };
const tdLabel: React.CSSProperties = { fontSize: 12, fontWeight: 500, color: '#374151', padding: '6px 12px 6px 0', whiteSpace: 'nowrap', verticalAlign: 'middle' };
const inputStyle: React.CSSProperties = { border: '1px solid #d1d5db', borderRadius: 4, padding: '5px 8px', fontSize: 12, color: '#374151', outline: 'none', boxSizing: 'border-box' as const };
const primaryButton: React.CSSProperties = { background: '#003366', color: '#fff', border: 'none', borderRadius: 5, padding: '6px 20px', fontSize: 12, fontWeight: 600, cursor: 'pointer' };
const cancelButton: React.CSSProperties = { background: '#e5e7eb', color: '#374151', border: 'none', borderRadius: 5, padding: '6px 20px', fontSize: 12, fontWeight: 500, cursor: 'pointer' };

// --- NEW TWO-COLUMN STYLES ---
const fieldRow: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 16 };
const labelStyle: React.CSSProperties = { fontSize: 12, fontWeight: 600, color: '#4b5563' };
const inputFull: React.CSSProperties = { ...inputStyle, width: '100%' };

export default CreateServiceAgreement;
