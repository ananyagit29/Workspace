import { authApi } from "./authApi";

import axios from "axios";

export const dmsApi = axios.create({
  baseURL: import.meta.env.VITE_DMS_API,
  withCredentials: false,
});

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
/* ── Dashboard workspace dropdown APIs ── */

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

export const getDivisions = (companyId: string) =>
  authApi.get("/dashboard/getDivisions", {
    params: { companyId, applicationName: "DMS 2.0" },
  });

export const getLocations = (companyId: string, divisionName: string) =>
  authApi.get("/dashboard/getLocations", {
    params: { companyId, divisionName },
  });

// Applications come from the logged-in user's actual rights filtered by workspace
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

export const getYears = (_applicationName: string) =>
  Promise.resolve({ data: [] });

export const getSubApplications = (
  _companyId: string,
  _divisionId: string,
  _locationId: string,
  applicationName: string
) =>
  authApi.get("/dashboard/getSubApplications", {
    params: { applicationName },
  });

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