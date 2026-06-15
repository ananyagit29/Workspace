import { useEffect, useState } from "react";
import { authApi } from "../api/authApi";

export interface UserRight {
  userId: string;
  companyId?: string;
  divisionName?: string;
  locationId?: string;
  applicationName?: string;
  subApplicationName?: string;
  module?: string;
  accessType?: string; // "Creator" | "Viewer" | "Report" | "Remove" | "Approve"
}

export interface AppRights {
  canSearch: boolean;
  canCreate: boolean;
  canReport: boolean;
  canRemove: boolean;
  canApprove: boolean;
  rights: UserRight[];
  loading: boolean;
}

interface Selections {
  com: string;
  div: string;
  loc: string;
  app: string;
  subApp?: string | null;
}

export const useAppRights = (): AppRights => {
  const [rights, setRights] = useState<UserRight[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const userId = localStorage.getItem("userId") || "";
    if (!userId) { setLoading(false); return; }

    authApi
      .get("/rights/byUser", { params: { userId } })
      .then((res) => setRights(res.data || []))
      .catch(() => setRights([]))
      .finally(() => setLoading(false));
  }, []);

  const raw = sessionStorage.getItem("dms2Selections");
  const sel: Selections = raw ? JSON.parse(raw) : {};

  // Base filter: company + division + location + application (no subApp filter)
  const baseRights = rights.filter(
    (r) =>
      r.companyId === sel.com &&
      r.divisionName === sel.div &&
      r.locationId === sel.loc &&
      r.applicationName === sel.app
  );

  // Tab-level rights: filter by subApp if selected (controls Create/Report tabs)
  const tabRights = baseRights.filter(
    (r) => !sel.subApp || r.subApplicationName === sel.subApp
  );

  // Row-level rights: check across ALL subApps in this app
  // (Remove/Approve may be granted per subApp but apply to the search table globally)
  const hasTabRight = (type: string) =>
    tabRights.some((r) => r.accessType?.toLowerCase() === type.toLowerCase());

  const hasAnyRight = (type: string) =>
    baseRights.some((r) => r.accessType?.toLowerCase() === type.toLowerCase());

  return {
    canSearch: true,
    canCreate: hasTabRight("creator"),
    canReport: hasTabRight("report"),
    canRemove: hasAnyRight("remove"),   // across all subApps
    canApprove: hasAnyRight("approve"), // across all subApps
    rights: baseRights,
    loading,
  };
};