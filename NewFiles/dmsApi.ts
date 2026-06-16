import axios from "axios";
import { authApi } from "./authApi";

// ── DMS .NET API instance ─────────────────────────────────────────────────────
export const dmsApi = axios.create({
  baseURL: import.meta.env.VITE_DMS_API,
  withCredentials: true,
});

dmsApi.interceptors.request.use((config) => {
  const token = localStorage.getItem("jwtToken");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// ── Types ─────────────────────────────────────────────────────────────────────

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

interface YearParameter {
  applicationName: string;
  startYear: number;
}

// ── Session caches ────────────────────────────────────────────────────────────

let _rightsCache: UserRights[] | null = null;
let _yearConfigCache: Record<string, number> | null = null;

// ── Rights ────────────────────────────────────────────────────────────────────

export const getUserRights = async (): Promise<UserRights[]> => {
  if (_rightsCache) return _rightsCache;
  const uid = localStorage.getItem("userId") || "";
  if (!uid) return [];
  const res = await authApi.get("/rights/byUser", { params: { userId: uid } });
  _rightsCache = res.data || [];
  return _rightsCache!;
};

// ── Year config — fetched from .NET API (DMS_GENERAL_PARAMETERS) ──────────────

const getYearConfig = async (): Promise<Record<string, number>> => {
  if (_yearConfigCache) return _yearConfigCache;
  try {
    const res = await dmsApi.get("/config/yearParameters");
    const config: Record<string, number> = {};
    for (const item of (res.data as YearParameter[])) {
      config[item.applicationName] = item.startYear;
    }
    _yearConfigCache = config;
    return config;
  } catch {
    _yearConfigCache = {};
    return {};
  }
};

// ── Clear all caches on logout ────────────────────────────────────────────────

export const clearRightsCache = () => {
  _rightsCache     = null;
  _yearConfigCache = null;
};

// ── Financial year helper ─────────────────────────────────────────────────────

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

// ── Dashboard workspace dropdown APIs ─────────────────────────────────────────

export const getCompanies = () =>
  authApi.get("/dashboard/getCompanies");

export const getDivisions = async (companyId: string) => {
  const rights = await getUserRights();
  const filtered = rights.filter((r) => r.companyId === companyId);
  const seen = new Set<string>();
  const unique: [string, string][] = [];
  for (const r of filtered) {
    if (r.divisionName && !seen.has(r.divisionName)) {
      seen.add(r.divisionName);
      unique.push([r.divisionName, r.divisionName]);
    }
  }
  return { data: unique };
};

export const getLocations = async (companyId: string, divisionName: string) => {
  const rights = await getUserRights();
  const filtered = rights.filter(
    (r) => r.companyId === companyId && r.divisionName === divisionName
  );
  const seen = new Set<string>();
  const unique: [string, string][] = []; // [locationId, locationName]
  for (const r of filtered) {
    if (r.locationId && !seen.has(r.locationId)) {
      seen.add(r.locationId);
      unique.push([r.locationId, r.locationName || r.locationId]);
    }
  }
  return { data: unique };
};

export const getApplications = async (
  companyId: string,
  divisionName: string,
  locationId: string,
  _userId?: string
) => {
  const rights = await getUserRights();
  const filtered = rights.filter(
    (r) =>
      r.companyId === companyId &&
      r.divisionName === divisionName &&
      r.locationId === locationId
  );
  const seen = new Set<string>();
  const unique: [string, string][] = [];
  for (const r of filtered) {
    if (r.applicationName && !seen.has(r.applicationName)) {
      seen.add(r.applicationName);
      unique.push([r.applicationName, r.applicationName]);
    }
  }
  return { data: unique };
};

/**
 * getYears
 *
 * Hits GET /dmsApi/config/yearParameters on the .NET API which reads
 * DMS_GENERAL_PARAMETERS table. Returns financial year options for apps
 * that need a year selector. Format: "2025-2026" (Indian FY, April-March).
 * Returns [] for apps like BATCH_DETAILS that have no year config.
 */
export const getYears = async (
  applicationName: string
): Promise<{ data: string[] }> => {
  const config = await getYearConfig();
  const startYear = config[applicationName];
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
  divisionName: string,
  locationId: string,
  applicationName: string,
  _userId?: string
) => {
  const rights = await getUserRights();
  const filtered = rights.filter(
    (r) =>
      r.companyId === companyId &&
      r.divisionName === divisionName &&
      r.locationId === locationId &&
      r.applicationName === applicationName &&
      r.subApplicationName
  );
  const seen = new Set<string>();
  const unique: string[] = [];
  for (const r of filtered) {
    if (r.subApplicationName && !seen.has(r.subApplicationName)) {
      seen.add(r.subApplicationName);
      unique.push(r.subApplicationName);
    }
  }
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
