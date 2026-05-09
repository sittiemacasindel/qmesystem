import { useEffect, useRef, useState } from "react";
import { profileApi, ProfilePayload } from "../lib/apiClient";
import "./Profile.css";
import Sidebar from "../components/Sidebar";

const AVATAR_STORAGE_KEY = "qme_avatar";

interface ProfilePageProps {
  onNavigateToDashboard: () => void;
  onLogout: () => void;
}

type ActiveTab = "info" | "password" | "photo";

function ProfilePage({ onNavigateToDashboard, onLogout }: ProfilePageProps) {
  const [profile, setProfile] = useState<ProfilePayload | null>(null);
  const [activeTab, setActiveTab] = useState<ActiveTab>("info");

  // Edit profile fields
  const [editMode, setEditMode] = useState(false);
  const [fullName, setFullName] = useState("");
  const [organization, setOrganization] = useState("");
  const [saving, setSaving] = useState(false);
  const [profileMsg, setProfileMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // Password fields
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordMsg, setPasswordMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // Photo
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [avatarDataUrl, setAvatarDataUrl] = useState<string | null>(null);
  const [photoUploading, setPhotoUploading] = useState(false);
  const [photoMsg, setPhotoMsg] = useState<{ type: "success" | "error"; text: string } | null>(null);

  /* ── Fetch Profile ── */
  useEffect(() => {
    const fetchProfile = async () => {
      const res = await profileApi.get();
      if (res.data) {
        setProfile(res.data);
        setFullName(res.data.fullName || "");
        setOrganization(res.data.organization || "");
      }
      // Load saved avatar from localStorage
      const saved = localStorage.getItem(AVATAR_STORAGE_KEY);
      if (saved) setAvatarDataUrl(saved);
    };
    fetchProfile();
  }, []);

  const getInitials = () => {
    const name = profile?.fullName || "";
    return name.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2) || "A";
  };

  /* ── Save Profile ── */
  const handleSaveProfile = async () => {
    if (!profile) return;
    setProfileMsg(null);
    setSaving(true);
    try {
      const res = await profileApi.update({
        fullName: fullName.trim(),
        organization: organization.trim(),
      });

      if (res.error) {
        setProfileMsg({ type: "error", text: "Failed to save: " + res.error });
      } else if (res.data) {
        setProfile(res.data);
        setFullName(res.data.fullName || "");
        setOrganization(res.data.organization || "");
        setProfileMsg({ type: "success", text: "Profile updated successfully!" });
        setEditMode(false);
      }
    } catch {
      setProfileMsg({ type: "error", text: "A network error occurred. Please try again." });
    } finally {
      setSaving(false);
    }
  };

  /* ── Change Password ── */
  const handleChangePassword = async () => {
    setPasswordMsg(null);
    if (!currentPassword || !newPassword || !confirmNewPassword) {
      setPasswordMsg({ type: "error", text: "Please fill in all password fields." });
      return;
    }
    if (newPassword !== confirmNewPassword) {
      setPasswordMsg({ type: "error", text: "New passwords do not match." });
      return;
    }
    const rules = [
      { test: newPassword.length >= 8, rule: "at least 8 characters" },
      { test: /[A-Z]/.test(newPassword), rule: "one uppercase letter" },
      { test: /[0-9]/.test(newPassword), rule: "one number" },
      { test: /[^A-Za-z0-9]/.test(newPassword), rule: "one special character" },
    ];
    const failed = rules.filter((r) => !r.test).map((r) => r.rule);
    if (failed.length > 0) {
      setPasswordMsg({ type: "error", text: "Password must include: " + failed.join(", ") + "." });
      return;
    }

    setPasswordSaving(true);
    try {
      const res = await profileApi.changePassword({ currentPassword, newPassword });
      if (res.error) {
        setPasswordMsg({ type: "error", text: res.error });
      } else {
        setPasswordMsg({ type: "success", text: "Password updated! You will be signed out." });
        setCurrentPassword(""); setNewPassword(""); setConfirmNewPassword("");
        setTimeout(() => onLogout(), 2000);
      }
    } catch {
      setPasswordMsg({ type: "error", text: "A network error occurred. Please try again." });
    } finally {
      setPasswordSaving(false);
    }
  };

  /* ── Upload Photo (stored as base64 in localStorage) ── */
  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith("image/")) {
      setPhotoMsg({ type: "error", text: "Please select a valid image file (JPG, PNG, WebP)." });
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setPhotoMsg({ type: "error", text: "Image must be smaller than 2MB." });
      return;
    }

    setPhotoMsg(null);
    setPhotoUploading(true);

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      try {
        localStorage.setItem(AVATAR_STORAGE_KEY, dataUrl);
        setAvatarDataUrl(dataUrl);
        setPhotoMsg({ type: "success", text: "Profile photo updated successfully!" });
      } catch {
        // localStorage quota exceeded
        setPhotoMsg({ type: "error", text: "Image is too large to store. Please use a smaller image." });
      }
      setPhotoUploading(false);
    };
    reader.onerror = () => {
      setPhotoMsg({ type: "error", text: "Failed to read image file. Please try again." });
      setPhotoUploading(false);
    };
    reader.readAsDataURL(file);

    // Reset input so the same file can be re-selected
    e.target.value = "";
  };

  const handleRemovePhoto = () => {
    localStorage.removeItem(AVATAR_STORAGE_KEY);
    setAvatarDataUrl(null);
    setPhotoMsg({ type: "success", text: "Profile photo removed." });
  };


  return (
    <div className="app-shell">
      <div className="app-body">
        <Sidebar
          activeItem="profile"
          onNavigateToDashboard={onNavigateToDashboard}
          onNavigateToProfile={() => { }}
          onLogout={onLogout}
        />
        <div className="right-panel">

          {/* ✅ HEADER from Home */}
          <header className="app-header">
            <div className="app-header-brand">
              <img src="/Qme_Logo.png" alt="QMe" className="header-logo-img" />
              {profile?.organization && (
                <span className="header-org-chip">{profile.organization}</span>
              )}
            </div>

            <div className="app-header-right">
              <div className="header-user-block">
                <span className="header-user-name">{profile?.fullName || "User"}</span>
                <span className="header-user-role">{profile?.role || "Admin"}</span>
              </div>

              <button className="header-avatar-btn">
                {avatarDataUrl ? (
                  <img src={avatarDataUrl} alt="Avatar" className="header-avatar-img" />
                ) : (
                  <span className="header-avatar-initials">{getInitials()}</span>
                )}
              </button>
            </div>
          </header>

          {/* ✅ CENTERED CONTENT */}
          <main className="profile-main">
            <div className="profile-content">
              {/* ── Page Header ── */}

              <div className="profile-page-header">
                <div>
                  <h1 className="profile-page-title">My Profile</h1>
                  <p className="profile-page-subtitle">Manage your account information and security</p>
                </div>
              </div>

              {/* ── Hero Card ── */}
              <div className="profile-hero-card">
                <div className="profile-hero-avatar">
                  {avatarDataUrl ? (
                    <img src={avatarDataUrl} alt="Avatar" className="hero-avatar-img" />
                  ) : (
                    <span className="hero-avatar-initials">{getInitials()}</span>
                  )}
                </div>
                <div className="profile-hero-info">
                  <h2 className="profile-hero-name">{profile?.fullName || "—"}</h2>
                  <p className="profile-hero-email">{profile?.email || "—"}</p>
                  <div className="profile-hero-meta">
                    <span className="profile-hero-badge">
                      <ShieldIcon /> {profile?.role || "Administrator"}
                    </span>
                    {profile?.organization && (
                      <span className="profile-hero-org">
                        <BuildingIcon /> {profile.organization}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* ── Tabs ── */}
              <div className="profile-tabs">
                <button
                  className={`profile-tab ${activeTab === "info" ? "active" : ""}`}
                  onClick={() => { setActiveTab("info"); setProfileMsg(null); }}
                >
                  <PersonIcon /> Account Info
                </button>
                <button
                  className={`profile-tab ${activeTab === "password" ? "active" : ""}`}
                  onClick={() => { setActiveTab("password"); setPasswordMsg(null); }}
                >
                  <LockIcon /> Change Password
                </button>
                <button
                  className={`profile-tab ${activeTab === "photo" ? "active" : ""}`}
                  onClick={() => { setActiveTab("photo"); setPhotoMsg(null); }}
                >
                  <CameraIcon /> Profile Photo
                </button>
              </div>

              {/* ── Tab: Account Information ── */}
              {activeTab === "info" && (
                <div className="profile-card-panel">
                  <div className="panel-header">
                    <h3 className="panel-title">Account Information</h3>
                    {!editMode ? (
                      <button className="edit-profile-btn" onClick={() => setEditMode(true)}>
                        <PencilIcon /> Edit
                      </button>
                    ) : (
                      <div className="edit-actions">
                        <button className="cancel-btn" onClick={() => {
                          setEditMode(false); setProfileMsg(null);
                          setFullName(profile?.fullName || "");
                          setOrganization(profile?.organization || "");
                        }}>
                          Cancel
                        </button>
                        <button className="save-btn" onClick={handleSaveProfile} disabled={saving}>
                          {saving ? <><span className="spinner-xs" /> Saving…</> : "Save Changes"}
                        </button>
                      </div>
                    )}
                  </div>

                  {profileMsg && (
                    <div className={`panel-message ${profileMsg.type}`} role="alert">
                      {profileMsg.type === "success" ? "✓" : "⚠"} {profileMsg.text}
                    </div>
                  )}

                  <div className="info-grid">
                    {/* Full Name – editable */}
                    <div className="info-field">
                      <label className="info-label"><PersonIcon /> FULL NAME</label>
                      {editMode ? (
                        <input className="info-input" value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Your full name" />
                      ) : (
                        <div className="info-value">{profile?.fullName || "—"}</div>
                      )}
                    </div>

                    {/* Email – read only */}
                    <div className="info-field">
                      <label className="info-label"><MailIcon /> EMAIL ADDRESS</label>
                      <div className="info-value info-value--muted">{profile?.email || "—"}</div>
                      {editMode && <p className="info-hint">Email cannot be changed.</p>}
                    </div>

                    {/* Organization – editable */}
                    <div className="info-field">
                      <label className="info-label"><BuildingIcon /> ORGANIZATION</label>
                      {editMode ? (
                        <input className="info-input" value={organization} onChange={(e) => setOrganization(e.target.value)} placeholder="Your organization name" />
                      ) : (
                        <div className="info-value">{profile?.organization || "—"}</div>
                      )}
                    </div>

                    {/* Role – read only */}
                    <div className="info-field">
                      <label className="info-label"><ShieldIcon /> ROLE</label>
                      <div className="info-value info-value--role">{profile?.role || "Administrator"}</div>
                      {editMode && <p className="info-hint">Role is assigned by the system.</p>}
                    </div>
                  </div>
                </div>
              )}

              {/* ── Tab: Change Password ── */}
              {activeTab === "password" && (
                <div className="profile-card-panel">
                  <div className="panel-header">
                    <h3 className="panel-title">Change Password</h3>
                  </div>

                  {passwordMsg && (
                    <div className={`panel-message ${passwordMsg.type}`} role="alert">
                      {passwordMsg.type === "success" ? "✓" : "⚠"} {passwordMsg.text}
                    </div>
                  )}

                  <div className="password-fields">
                    <div className="info-field info-field--full">
                      <label className="info-label"><LockIcon /> CURRENT PASSWORD</label>
                      <div className="pw-input-wrapper">
                        <input className="info-input" type={showCurrent ? "text" : "password"} value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} placeholder="Enter your current password" />
                        <button className="pw-toggle" type="button" onClick={() => setShowCurrent(!showCurrent)}>
                          {showCurrent ? <EyeOffIcon /> : <EyeIcon />}
                        </button>
                      </div>
                    </div>

                    <div className="info-field">
                      <label className="info-label"><LockIcon /> NEW PASSWORD</label>
                      <div className="pw-input-wrapper">
                        <input className="info-input" type={showNew ? "text" : "password"} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="Min. 8 chars, A-Z, 0-9, !@#…" />
                        <button className="pw-toggle" type="button" onClick={() => setShowNew(!showNew)}>
                          {showNew ? <EyeOffIcon /> : <EyeIcon />}
                        </button>
                      </div>
                    </div>

                    <div className="info-field">
                      <label className="info-label"><LockIcon /> CONFIRM NEW PASSWORD</label>
                      <div className="pw-input-wrapper">
                        <input className="info-input" type={showConfirm ? "text" : "password"} value={confirmNewPassword} onChange={(e) => setConfirmNewPassword(e.target.value)} placeholder="Re-enter your new password" />
                        <button className="pw-toggle" type="button" onClick={() => setShowConfirm(!showConfirm)}>
                          {showConfirm ? <EyeOffIcon /> : <EyeIcon />}
                        </button>
                      </div>
                    </div>

                    <div className="pw-rules">
                      <p className="pw-rules-title">Password requirements:</p>
                      <ul>
                        <li className={newPassword.length >= 8 ? "met" : ""}>At least 8 characters</li>
                        <li className={/[A-Z]/.test(newPassword) ? "met" : ""}>One uppercase letter (A–Z)</li>
                        <li className={/[0-9]/.test(newPassword) ? "met" : ""}>One number (0–9)</li>
                        <li className={/[^A-Za-z0-9]/.test(newPassword) ? "met" : ""}>One special character (!@#$…)</li>
                      </ul>
                    </div>

                    <button className="save-btn save-btn--full" onClick={handleChangePassword} disabled={passwordSaving}>
                      {passwordSaving ? <><span className="spinner-xs" /> Updating…</> : "Update Password"}
                    </button>
                  </div>
                </div>
              )}

              {/* ── Tab: Profile Photo ── */}
              {activeTab === "photo" && (
                <div className="profile-card-panel">
                  <div className="panel-header">
                    <h3 className="panel-title">Profile Photo</h3>
                  </div>

                  {photoMsg && (
                    <div className={`panel-message ${photoMsg.type}`} role="alert">
                      {photoMsg.type === "success" ? "✓" : "⚠"} {photoMsg.text}
                    </div>
                  )}

                  <div className="photo-section">
                    <div className="photo-preview-wrap">
                      <div className="photo-preview">
                        {avatarDataUrl ? (
                          <img src={avatarDataUrl} alt="Profile" className="photo-preview-img" />
                        ) : (
                          <span className="photo-preview-initials">{getInitials()}</span>
                        )}
                        {photoUploading && (
                          <div className="photo-uploading-overlay">
                            <span className="spinner-lg" />
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="photo-actions">
                      <p className="photo-hint">
                        Upload a clear photo. Max size: <strong>2MB</strong>. Supported: JPG, PNG, WebP.
                      </p>

                      <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/*"
                        style={{ display: "none" }}
                        onChange={handlePhotoChange}
                        id="avatarInput"
                      />

                      <button
                        className="save-btn"
                        onClick={() => fileInputRef.current?.click()}
                        disabled={photoUploading}
                      >
                        <CameraIcon /> {avatarDataUrl ? "Change Photo" : "Upload Photo"}
                      </button>

                      {avatarDataUrl && (
                        <button className="remove-photo-btn" onClick={handleRemovePhoto} disabled={photoUploading}>
                          Remove Photo
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </main>

        </div>
      </div>
    </div>
  );
}

/* ── Icons ── */
function PersonIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" /><circle cx="12" cy="7" r="4" />
    </svg>
  );
}
function MailIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" /><polyline points="22,6 12,13 2,6" />
    </svg>
  );
}
function LockIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" /><path d="M7 11V7a5 5 0 0 1 10 0v4" />
    </svg>
  );
}
function ShieldIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
    </svg>
  );
}
function BuildingIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="3" width="20" height="18" rx="2" /><line x1="9" y1="21" x2="9" y2="3" />
    </svg>
  );
}
function CameraIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" /><circle cx="12" cy="13" r="4" />
    </svg>
  );
}
function PencilIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" /><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
    </svg>
  );
}
function EyeIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" />
    </svg>
  );
}
function EyeOffIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" /><line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  );
}

export default ProfilePage;
