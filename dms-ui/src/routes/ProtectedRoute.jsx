import { Navigate } from "react-router-dom";

export default function ProtectedRoute({ children, isAuth }) {
  return isAuth ? children : <Navigate to="/login" />;
}
