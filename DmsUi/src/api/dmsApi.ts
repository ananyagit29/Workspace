import axios from "axios";
import { authApi } from "./authApi";

export const dmsApi = axios.create({
  baseURL: import.meta.env.VITE_DMS_API,
  withCredentials: false,
});

// Attach JWT token from localStorage to every DMS request
dmsApi.interceptors.request.use((config) => {
  const token = localStorage.getItem("jwtToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (config.url && config.url.startsWith("/")) {
    config.url = config.url.substring(1);
  }
  return config;
});

/* ── Dashboard workspace dropdown APIs (still use authApi) ── */

export interface UserRights {
  userId: string;
  companyId?: string;
  companyName?: string;
  divisionName?: string;
  locationId?: string;
  locationName?: string;
  applicationName?: string;
  subApplicationName?: string;
  module?: string;
  accessType?: string;
}

export const getCompanies = () =>
  authApi.get("/dashboard/getCompanies");

export const deleteInvoice = (invoiceNumber: string) =>
  authApi.delete(`/invoice-document/${invoiceNumber}`);

// CapEx Budget API
export const getCapexBudgetTypes = (companyId: string, locationId: string, year: string) =>
  dmsApi.get("/capex/types", { params: { companyId, locationId, year } });

export const getCapexBudgetCodes = (budgetType: string, companyId: string, locationId: string, year: string) =>
  dmsApi.get("/capex/codes", { params: { budgetType, companyId, locationId, year } });

export const getCapexSearchCodes = (budgetType: string, companyId: string, locationId: string, year: string) =>
  dmsApi.get("/capex/search-codes", { params: { budgetType, companyId, locationId, year } });

export const getCapexRevisions = (budgetCode: string) =>
  dmsApi.get("/capex/revisions", { params: { budgetCode } });

export const saveCapex = (formData: FormData) =>
  dmsApi.post("/capex", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const reviseCapex = (budgetCode: string, formData: FormData) =>
  dmsApi.put("/capex/revise", formData, {
      params: { budgetCode },
      headers: { "Content-Type": "multipart/form-data" },
    });

export const searchCapex = (params: any) =>
  dmsApi.get("/capex/search", { params });

export const deleteCapex = (budgetCode: string, revision?: number) =>
  dmsApi.delete("/capex/remove", { params: { budgetCode, revision } });

export const getDivisions = (companyId: string) =>
  authApi.get("/dashboard/getDivisions", {
    params: { companyId, applicationName: "DMS 2.0" },
  });

export const getLocations = (companyId: string, divisionName: string) =>
  authApi.get("/dashboard/getLocations", {
    params: { companyId, divisionName },
  });

export const getApplications = async (
  companyId: string,
  divisionName: string,
  locationId: string,
  userId?: string
) => {
  const uid = userId || localStorage.getItem("userId") || "";
  const res = await authApi.get("/rights/byUser", {
    params: { userId: uid },
  });
  const rights: UserRights[] = res.data || [];
  const filtered = rights.filter(
    (r) =>
      r.companyId === companyId &&
      r.divisionName === divisionName &&
      r.locationId === locationId
  );
  const unique = [...new Set(filtered.map((r) => r.applicationName).filter(Boolean))];
  return { data: unique.map((a) => [a, a]) };
};

// ── Year config — fetched from .NET API (DMS_GENERAL_PARAMETERS) ──────────────

let _yearConfigCache: Record<string, number> | null = null;

const getYearConfig = async (): Promise<Record<string, number>> => {
  if (_yearConfigCache) return _yearConfigCache;
  try {
    const res = await dmsApi.get("/config/yearParameters");
    const config: Record<string, number> = {};
    for (const item of (res.data as any[])) {
      config[item.applicationName] = item.startYear;
    }
    _yearConfigCache = config;
    return config;
  } catch {
    _yearConfigCache = {};
    return {};
  }
};

/**
 * Returns the start year of the current Indian financial year.
 * FY starts April 1 (month index 3).
 * June 2026  → 2026 (FY 2026-2027)
 * Feb  2026  → 2025 (FY 2025-2026)
 */
const getCurrentFYStart = (): number => {
  const now = new Date();
  return now.getMonth() >= 3 ? now.getFullYear() : now.getFullYear() - 1;
};

export const getYears = async (
  applicationName: string
): Promise<{ data: string[] }> => {
  const config = await getYearConfig();
  let startYear = config[applicationName];
  
  // Fallback to DMS 2.0 start year if specific app is not found in config
  if (!startYear && config["DMS 2.0"]) {
    startYear = config["DMS 2.0"];
  }
  // Hard fallback for CAPEX BUDGET to 2020 as requested by user
  if (applicationName === "CAPEX BUDGET" || applicationName === "CAPEX_BUDGET") {
    // If the database configured a startYear later than 2020, we can override or just default to 2020
    startYear = 2020;
  }
  
  if (!startYear) return { data: [] };

  const currentFYStart = getCurrentFYStart();
  const years: string[] = [];
  for (let y = currentFYStart; y >= startYear; y--) {
    years.push(`${y}-${y + 1}`);
  }
  return { data: years };
};

export const getSubApplications = async (
  companyId: string,
  divisionId: string,
  locationId: string,
  applicationName: string,
  userId?: string
) => {
  const uid = userId || localStorage.getItem("userId") || "";
  const res = await authApi.get("/rights/byUser", {
    params: { userId: uid },
  });
  const rights: UserRights[] = res.data || [];
  const filtered = rights.filter(
    (r) =>
      r.companyId === companyId &&
      r.divisionName === divisionId &&
      r.locationId === locationId &&
      r.applicationName === applicationName &&
      r.subApplicationName  // must have a subApp
  );
  const unique = [...new Set(filtered.map((r) => r.subApplicationName).filter(Boolean))];
  return { data: unique };
};

export const getModules = (
  _companyId: string,
  _divisionId: string,
  _locationId: string,
  applicationName: string,
  subApplicationName: string
) =>
  authApi.get("/dashboard/getModules", {
    params: { applicationName, subApplicationName },
  });

// Supplier & Customer API
export const getSupplierCustomerSearchOptions = (accountType?: string, companyId?: string, locationId?: string) =>
  dmsApi.get("/supplier-customer/search-options", { params: { accountType, companyId, locationId } });

export const getScmSupplierCustomerOptions = (accountType?: string) =>
  dmsApi.get("/supplier-customer/scm-options", { params: { accountType } });

export const searchSupplierCustomer = (params: any) =>
  dmsApi.get("/supplier-customer/search", { params });

export const removeSupplierCustomer = (accountCode: string, fileName: string) =>
  dmsApi.delete("/supplier-customer/remove", { params: { accountCode, fileName } });

export const saveSupplierCustomerFile = (data: FormData) =>
  dmsApi.post("/supplier-customer/add-file", data, {
    headers: { "Content-Type": "multipart/form-data" },
  });