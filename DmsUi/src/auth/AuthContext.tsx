import { createContext, useEffect, useState, type ReactNode } from "react";
import axios from "axios";

export interface User {
  userId: string;
  firstName: string;
  lastName: string;
  locationName?: string;
  departmentName?: string;
  emailId?: string;
  isAdmin?: boolean;
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  refreshUser: () => Promise<void>;
  clearUser: () => void;
}

export const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
  refreshUser: async () => {},
  clearUser: () => {},
});

interface Props {
  children: ReactNode;
}

export const AuthProvider = ({ children }: Props) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchUser = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem("jwtToken");
      if (!token) { setUser(null); return; }

      const res = await axios.get<User>(
        `${import.meta.env.VITE_AUTH_API}/users/me`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setUser(res.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const clearUser = () => setUser(null);

  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      fetchUser();
    } else {
      setLoading(false);
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, refreshUser: fetchUser, clearUser }}>
      {children}
    </AuthContext.Provider>
  );
};