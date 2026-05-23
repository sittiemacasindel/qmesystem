import { useState, useRef } from "react";
import { session } from "./lib/apiClient";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Home from "./pages/Home";
import ProfilePage from "./pages/Profile";
import QueueDetail from "./pages/QueueDetail";

type Page = "login" | "register" | "home" | "profile" | "queue-detail";

const initialPage: Page = session.isLoggedIn() ? "home" : "login";

function App() {
  const [page, setPage] = useState<Page>(initialPage);
  const [selectedOrgId, setSelectedOrgId] = useState<string | null>(null);
  const [confirmLogout, setConfirmLogout] = useState(false);

  const pageRef = useRef<Page>(initialPage);
  const setPageSafe = (p: Page) => {
    pageRef.current = p;
    setPage(p);
  };

  const handleViewOrg = (orgId: string) => {
    setSelectedOrgId(orgId);
    setPageSafe("queue-detail");
  };

  const handleBackToDashboard = () => {
    setSelectedOrgId(null);
    setPageSafe("home");
  };

  const handleConfirmLogout = () => {
    setConfirmLogout(false);
    setPageSafe("login");
  };

  return (
    <>
      {confirmLogout && (
        <div className="modal-overlay" onClick={() => setConfirmLogout(false)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div
                className="modal-header-icon"
                style={{
                  background: "linear-gradient(135deg, #f59e0b, #d97706)"
                }}
              >
                ⚠
              </div>
              <div>
                <h2 className="modal-title">Logout</h2>
                <p className="modal-subtitle">
                  You will be signed out of your account.
                </p>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setConfirmLogout(false)}
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <p style={{ fontSize: "0.9rem", color: "#475569" }}>
                Are you sure you want to logout?
              </p>
            </div>

            <div className="modal-footer">
              <button
                className="modal-btn-cancel"
                onClick={() => setConfirmLogout(false)}
              >
                Cancel
              </button>

              <button
                className="modal-btn-create"
                style={{
                  background: "linear-gradient(135deg, #0284c7, #06b6d4)"
                }}
                onClick={handleConfirmLogout}
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      )}

      {page === "home" && (
        <Home
          onNavigateToProfile={() => setPageSafe("profile")}
          onLogout={() => setConfirmLogout(true)}
          onViewOrg={handleViewOrg}
        />
      )}

      {page === "profile" && (
        <ProfilePage
          onNavigateToDashboard={handleBackToDashboard}
          onLogout={() => setConfirmLogout(true)}
        />
      )}

      {page === "queue-detail" && selectedOrgId && (
        <QueueDetail
          orgId={selectedOrgId}
          onBack={handleBackToDashboard}
          onNavigateToProfile={() => setPageSafe("profile")}
          onLogout={() => setConfirmLogout(true)}
        />
      )}

      {page === "register" && (
        <Register
          onNavigateToLogin={() => setPageSafe("login")}
          onRegisterSuccess={() => setPageSafe("home")}
        />
      )}

      {page === "login" && (
        <Login
          onNavigateToRegister={() => setPageSafe("register")}
          onLoginSuccess={() => setPageSafe("home")}
        />
      )}
    </>
  );
}

export default App;