import React from "react";

export const Footer: React.FC = () => {
  return (
    <footer style={{ borderTop: "1px solid #e5e7eb", padding: "7px 16px", textAlign: "center", fontSize: 11, color: "#9ca3af", background: "#fff", flexShrink: 0 }}>
      ©2026 Copyright{" "}
      <strong style={{ color: "#374151" }}>Ipca Laboratories Limited</strong>{" "}
      All Rights Reserved. DMS version 2.0
    </footer>
  );
};