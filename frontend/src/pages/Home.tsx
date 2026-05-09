import { useEffect, useRef, useState } from "react";
import { analyticsApi, organizationApi, OrganizationPayload, profileApi, AnalyticsPayload } from "../lib/apiClient";
import "./Home.css";
import Sidebar from "../components/Sidebar";

const AVATAR_STORAGE_KEY = "qme_avatar";
const ORG_PHOTO_KEY_PREFIX = "qme_org_photo_";

interface Profile {
  fullName: string;
  email: string;
  role: string;
  organization: string;
  avatarUrl: string | null;
}

interface HomeProps {
  onNavigateToProfile: () => void;
  onLogout: () => void;
  onViewOrg: (orgId: string) => void;
}

interface CreateQueueForm {
  name: string;
  openingHours: string;
  closingHours: string;
  waitTimeMin: string;
  waitTimeMax: string;
  location: string;
  contactNumber: string;
}

const EMPTY_FORM: CreateQueueForm = {
  name: "",
  openingHours: "",
  closingHours: "",
  waitTimeMin: "",
  waitTimeMax: "",
  location: "",
  contactNumber: "",
};

function Home({ onNavigateToProfile, onLogout, onViewOrg }: HomeProps) {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [organizations, setOrganizations] = useState<OrganizationPayload[]>([]);
  const [analytics, setAnalytics] = useState<AnalyticsPayload>({
    activeOrganizations: 0,
    totalWaitingCustomers: 0,
    totalServedToday: 0,
  });
  const [avatarDataUrl, setAvatarDataUrl] = useState<string | null>(null);

  // ── Create Queue Modal ──
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState<CreateQueueForm>(EMPTY_FORM);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState("");
  // Photo upload state (for the new queue photo)
  const [photoPreview, setPhotoPreview] = useState<string | null>(null);
  const photoInputRef = useRef<HTMLInputElement>(null);

  // ── Delete confirm ──
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [togglingId, setTogglingId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  // ── Load all data ──
  useEffect(() => {
    loadData();

    // Listen for avatar changes from Profile page
    const onStorage = (e: StorageEvent) => {
      if (e.key === AVATAR_STORAGE_KEY) setAvatarDataUrl(e.newValue);
    };
    window.addEventListener("storage", onStorage);
    return () => window.removeEventListener("storage", onStorage);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadData = async () => {
    setLoadingProfile(true);

    // Run profile, org list, analytics in parallel
    const [pRes, oRes, aRes] = await Promise.all([
      profileApi.get(),
      organizationApi.list(),
      analyticsApi.get(),
    ]);

    if (pRes.data) {
      setProfile({
        fullName: pRes.data.fullName,
        email: pRes.data.email,
        role: pRes.data.role,
        organization: pRes.data.organization ?? "",
        avatarUrl: pRes.data.avatarUrl ?? null,
      });
    }

    const saved = localStorage.getItem(AVATAR_STORAGE_KEY);
    if (saved) setAvatarDataUrl(saved);

    if (oRes.data) setOrganizations(oRes.data);
    if (aRes.data) setAnalytics(aRes.data);

    setLoadingProfile(false);
  };

  const getInitials = () => {
    const name = profile?.fullName || "";
    return name.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2) || "A";
  };

  const displayName = loadingProfile ? "…" : (profile?.fullName || "Admin");

  // ── Toggle status ──
  const handleToggleStatus = async (org: OrganizationPayload) => {
    if (togglingId) return;
    setTogglingId(org.id);
    const newStatus = org.status === "ACTIVE" ? "PAUSED" : "ACTIVE";
    const result = await organizationApi.updateStatus(org.id, newStatus);
    if (result.data) {
      setOrganizations((prev) =>
        prev.map((o) => (o.id === org.id ? result.data! : o))
      );
      // Refresh analytics
      const aRes = await analyticsApi.get();
      if (aRes.data) setAnalytics(aRes.data);
    }
    setTogglingId(null);
  };

  // ── Delete ──
  const handleDelete = async (id: string) => {
    setDeletingId(id);
    await organizationApi.delete(id);
    setOrganizations((prev) => prev.filter((o) => o.id !== id));
    localStorage.removeItem(ORG_PHOTO_KEY_PREFIX + id);
    const aRes = await analyticsApi.get();
    if (aRes.data) setAnalytics(aRes.data);
    setDeletingId(null);
    setConfirmDeleteId(null);
  };

  // ── Modal ──
  const openModal = () => {
    setForm(EMPTY_FORM);
    setCreateError("");
    setPhotoPreview(null);
    setShowModal(true);
  };

  const closeModal = () => {
    if (!creating) setShowModal(false);
  };

  const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setCreateError("Please select a valid image (JPG, PNG, WebP).");
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setCreateError("Image must be smaller than 2MB.");
      return;
    }
    setCreateError("");
    const reader = new FileReader();
    reader.onload = () => setPhotoPreview(reader.result as string);
    reader.readAsDataURL(file);
    e.target.value = "";
  };

  const handleCreate = async () => {
    setCreateError("");
    if (!form.name.trim()) { setCreateError("Queue name is required."); return; }
    if (!form.openingHours.trim()) { setCreateError("Opening hours are required."); return; }
    if (!form.closingHours.trim()) { setCreateError("Closing hours are required."); return; }
    if (!form.waitTimeMin || isNaN(Number(form.waitTimeMin))) { setCreateError("Min wait time must be a number."); return; }
    if (!form.waitTimeMax || isNaN(Number(form.waitTimeMax))) { setCreateError("Max wait time must be a number."); return; }
    if (Number(form.waitTimeMin) > Number(form.waitTimeMax)) { setCreateError("Min wait time cannot exceed max wait time."); return; }
    if (!form.location.trim()) { setCreateError("Location is required."); return; }
    if (!form.contactNumber.trim()) { setCreateError("Contact number is required."); return; }

    setCreating(true);
    try {
      const res = await organizationApi.create({
        name: form.name.trim(),
        openingHours: form.openingHours.trim(),
        closingHours: form.closingHours.trim(),
        waitTimeMin: Number(form.waitTimeMin),
        waitTimeMax: Number(form.waitTimeMax),
        location: form.location.trim(),
        contactNumber: form.contactNumber.trim(),
      });

      if (res.error) {
        setCreateError(res.error);
      } else if (res.data) {
        // Save photo for this org in localStorage
        if (photoPreview) {
          try { localStorage.setItem(ORG_PHOTO_KEY_PREFIX + res.data.id, photoPreview); } catch { /* quota */ }
        }
        setOrganizations((prev) => [...prev, res.data!]);
        setShowModal(false);
        const aRes = await analyticsApi.get();
        if (aRes.data) setAnalytics(aRes.data);
      }
    } catch {
      setCreateError("A network error occurred. Please try again.");
    } finally {
      setCreating(false);
    }
  };

  const getOrgPhoto = (id: string) => localStorage.getItem(ORG_PHOTO_KEY_PREFIX + id);

  return (
    <div className="app-shell">
      <div className="app-body">
        <Sidebar
          activeItem="dashboard"
          onNavigateToDashboard={() => { }}
          onNavigateToProfile={onNavigateToProfile}
          onLogout={onLogout}
        />

        <div className="right-panel">
          {/* ── App Header ── */}
          <header className="app-header">
            <div className="app-header-brand">
              <img src="/Qme_Logo.png" alt="QMe" className="header-logo-img" />
              {profile?.organization && (
                <span className="header-org-chip">{profile.organization}</span>
              )}
            </div>

            <div className="app-header-right">
              <div className="header-user-block">
                <span className="header-user-name">{displayName}</span>
                <span className="header-user-role">{profile?.role || "Admin"}</span>
              </div>
              <button className="header-avatar-btn" onClick={onNavigateToProfile} title="View Profile">
                {avatarDataUrl ? (
                  <img src={avatarDataUrl} alt="Avatar" className="header-avatar-img" />
                ) : (
                  <span className="header-avatar-initials">
                    {loadingProfile ? "…" : getInitials()}
                  </span>
                )}
              </button>
            </div>
          </header>

          {/* ── Main Content ── */}
          <main className="dash-main">
            {/* Page Title Row */}
            <div className="dash-title-row">
              <div>
                <h1 className="dash-title">Queue Dashboard</h1>
                <p className="dash-subtitle">Monitor your queues and customer flow in real time</p>
              </div>
              <button className="create-queue-btn" onClick={openModal}>
                <PlusIcon /> Create Queue
              </button>
            </div>

            {/* Analytics Stats Cards */}
            <div className="stats-row">
              <div className="stat-card">
                <div className="stat-icon-box stat-icon-box--cyan">
                  <BarChartIcon />
                </div>
                <div className="stat-body">
                  <span className="stat-num">{loadingProfile ? "—" : analytics.activeOrganizations}</span>
                  <span className="stat-lbl">Active Queues</span>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon-box stat-icon-box--sky">
                  <PeopleIcon />
                </div>
                <div className="stat-body">
                  <span className="stat-num">{loadingProfile ? "—" : analytics.totalWaitingCustomers}</span>
                  <span className="stat-lbl">Waiting Customers</span>
                  <span className="stat-sublbl">Across all active queues</span>
                </div>
              </div>

              <div className="stat-card">
                <div className="stat-icon-box stat-icon-box--teal">
                  <ClockCircleIcon />
                </div>
                <div className="stat-body">
                  <span className="stat-num">{loadingProfile ? "—" : analytics.totalServedToday}</span>
                  <span className="stat-lbl">Total Served Today</span>
                </div>
              </div>
            </div>

            {/* My Queues Card Grid */}
            <div className="queues-card">
              <div className="queues-card-head">
                <div>
                  <h2 className="queues-card-title">My Queues</h2>
                  <p className="queues-card-sub">
                    {organizations.length > 0
                      ? `${organizations.length} queue${organizations.length !== 1 ? "s" : ""} total`
                      : "No queues yet"}
                  </p>
                </div>
              </div>

              {/* Scrollable card grid */}
              <div className="queue-grid-scroll">
                {organizations.length === 0 ? (
                  <div className="empty-state">
                    <span className="empty-state-icon">🎟️</span>
                    <p className="empty-state-msg">No queues yet</p>
                    <p className="empty-state-sub">Create your first queue to start managing customers.</p>
                    <button className="create-queue-btn empty-state-btn" onClick={openModal}>
                      <PlusIcon /> Create Queue
                    </button>
                  </div>
                ) : (
                  <div className="queue-grid">
                    {organizations.map((org) => {
                      const orgPhoto = getOrgPhoto(org.id);
                      return (
                        <div key={org.id} className="queue-card">
                          {/* Card header: photo + name + code + status */}
                          <div className="queue-card-header">
                            {orgPhoto ? (
                              <img src={orgPhoto} alt="" className="queue-card-photo" />
                            ) : (
                              <div className="queue-card-photo queue-card-photo--placeholder">
                                <PeopleIcon />
                              </div>
                            )}
                            <div className="queue-card-title-block">
                              <div className="queue-card-name">{org.name}</div>
                              <span className="q-code">{org.queueCode}</span>
                            </div>
                            <span className={`q-status q-status--${org.status.toLowerCase()} queue-card-status`}>
                              <span className="q-status-dot" />
                              {org.status}
                            </span>
                          </div>

                          {/* Meta info */}
                          <div className="queue-card-meta">
                            <div className="queue-card-meta-row">
                              <ClockSmIcon />
                              <span>{org.openingHours} – {org.closingHours}</span>
                            </div>
                            <div className="queue-card-meta-row">
                              <TimerSmIcon />
                              <span>{org.waitTimeMin}–{org.waitTimeMax} min wait</span>
                            </div>
                            <div className="queue-card-meta-row">
                              <LocationSmIcon />
                              <span className="queue-card-meta-location">{org.location}</span>
                            </div>
                          </div>

                          {/* Stats row */}
                          <div className="queue-card-stats">
                            <div className="queue-card-stat">
                              <span className="queue-card-stat-num q-waiting">
                                {org.totalWaitingCustomers ?? "0"}
                              </span>
                              <span className="queue-card-stat-lbl">Waiting</span>
                            </div>
                            <div className="queue-card-stat-divider" />
                            <div className="queue-card-stat">
                              <span className="queue-card-stat-num q-served">
                                {org.totalServedToday ?? "0"}
                              </span>
                              <span className="queue-card-stat-lbl">Served Today</span>
                            </div>
                          </div>

                          {/* Actions */}


                          <div className="queue-card-actions">
                            <button
                              className="q-btn q-btn--view queue-card-btn-view"
                              onClick={() => onViewOrg(org.id)}
                              title="View Queue"
                            >
                              <EyeIcon /> View Queue
                            </button>

                            <div className="queue-card-actions-right">
                              <button
                                className={`q-btn ${org.status === "ACTIVE" ? "q-btn--pause" : "q-btn--resume"}`}
                                onClick={() => handleToggleStatus(org)}
                                disabled={togglingId === org.id}
                                title={org.status === "ACTIVE" ? "Pause" : "Resume"}
                              >
                                {togglingId === org.id ? (
                                  <span className="spinner-xs" />
                                ) : org.status === "ACTIVE" ? (
                                  <><PauseIcon /> Pause</>
                                ) : (
                                  <><PlayIcon /> Resume</>
                                )}
                              </button>

                              <button
                                className="q-btn q-btn--close"
                                onClick={() => setConfirmDeleteId(org.id)}
                                disabled={deletingId === org.id}
                                title="Delete"
                              >
                                {deletingId === org.id ? <span className="spinner-xs" /> : <XIcon />}
                              </button>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          </main>
        </div>
      </div>

      {/* ── Create Queue Modal ── */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-box modal-box--wide" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-header-icon"><QueueIcon /></div>
              <div>
                <h2 className="modal-title">Create New Queue</h2>
                <p className="modal-subtitle">Set up a new service queue — code will be auto-generated</p>
              </div>
              <button className="modal-close-btn" onClick={closeModal} disabled={creating}>✕</button>
            </div>

            {createError && (
              <div className="modal-error">⚠ {createError}</div>
            )}

            <div className="modal-body modal-body--grid">
              {/* Photo Upload */}
              <div className="modal-field modal-field--photo">
                <label className="modal-label">Queue Photo <span className="modal-optional">(optional)</span></label>
                <div className="modal-photo-area" onClick={() => photoInputRef.current?.click()}>
                  {photoPreview ? (
                    <img src={photoPreview} alt="Preview" className="modal-photo-preview" />
                  ) : (
                    <div className="modal-photo-placeholder">
                      <CameraUploadIcon />
                      <span>Click to upload</span>
                      <span className="modal-photo-hint">JPG, PNG, WebP · max 2 MB</span>
                    </div>
                  )}
                </div>
                <input
                  ref={photoInputRef}
                  type="file"
                  accept="image/*"
                  style={{ display: "none" }}
                  onChange={handlePhotoSelect}
                />
                {photoPreview && (
                  <button
                    type="button"
                    className="modal-photo-remove"
                    onClick={() => setPhotoPreview(null)}
                  >
                    Remove photo
                  </button>
                )}
              </div>

              {/* Right side fields */}
              <div className="modal-fields-right">
                {/* Queue Name */}
                <div className="modal-field">
                  <label className="modal-label">Queue Name <span className="modal-req">*</span></label>
                  <input
                    className="modal-input"
                    type="text"
                    placeholder="e.g. Customer Support"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    autoFocus
                  />
                </div>

                {/* Hours row */}
                <div className="modal-row-2">
                  <div className="modal-field">
                    <label className="modal-label">Opening Hours <span className="modal-req">*</span></label>
                    <input
                      className="modal-input"
                      type="text"
                      placeholder="e.g. 08:00 AM"
                      value={form.openingHours}
                      onChange={(e) => setForm({ ...form, openingHours: e.target.value })}
                    />
                  </div>
                  <div className="modal-field">
                    <label className="modal-label">Closing Hours <span className="modal-req">*</span></label>
                    <input
                      className="modal-input"
                      type="text"
                      placeholder="e.g. 05:00 PM"
                      value={form.closingHours}
                      onChange={(e) => setForm({ ...form, closingHours: e.target.value })}
                    />
                  </div>
                </div>

                {/* Wait time row */}
                <div className="modal-row-2">
                  <div className="modal-field">
                    <label className="modal-label">Min Wait Time <span className="modal-req">*</span></label>
                    <div className="modal-input-suffix-wrap">
                      <input
                        className="modal-input modal-input--num"
                        type="number"
                        min={0}
                        placeholder="5"
                        value={form.waitTimeMin}
                        onChange={(e) => setForm({ ...form, waitTimeMin: e.target.value })}
                      />
                      <span className="modal-input-suffix">min</span>
                    </div>
                  </div>
                  <div className="modal-field">
                    <label className="modal-label">Max Wait Time <span className="modal-req">*</span></label>
                    <div className="modal-input-suffix-wrap">
                      <input
                        className="modal-input modal-input--num"
                        type="number"
                        min={0}
                        placeholder="15"
                        value={form.waitTimeMax}
                        onChange={(e) => setForm({ ...form, waitTimeMax: e.target.value })}
                      />
                      <span className="modal-input-suffix">min</span>
                    </div>
                  </div>
                </div>

                {/* Location */}
                <div className="modal-field">
                  <label className="modal-label">Location <span className="modal-req">*</span></label>
                  <input
                    className="modal-input"
                    type="text"
                    placeholder="e.g. Building A, Room 101"
                    value={form.location}
                    onChange={(e) => setForm({ ...form, location: e.target.value })}
                  />
                </div>

                {/* Contact Number */}
                <div className="modal-field">
                  <label className="modal-label">Contact Number <span className="modal-req">*</span></label>
                  <input
                    className="modal-input"
                    type="text"
                    placeholder="e.g. +63 912 345 6789"
                    value={form.contactNumber}
                    onChange={(e) => setForm({ ...form, contactNumber: e.target.value })}
                  />
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="modal-btn-cancel" onClick={closeModal} disabled={creating}>Cancel</button>
              <button className="modal-btn-create" onClick={handleCreate} disabled={creating}>
                {creating ? <><span className="spinner-xs" /> Creating…</> : <><PlusIcon /> Create Queue</>}
              </button>
            </div>
          </div>
        </div>
      )}
      {confirmDeleteId && (
        <div className="modal-overlay" onClick={() => setConfirmDeleteId(null)}>
          <div className="modal-box" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-header-icon" style={{ background: "linear-gradient(135deg, #ef4444, #dc2626)" }}>
                <XIcon />
              </div>
              <div>
                <h2 className="modal-title">Delete Queue</h2>
                <p className="modal-subtitle">
                  This action cannot be undone.
                </p>
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setConfirmDeleteId(null)}
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <p style={{ fontSize: "0.9rem", color: "#475569" }}>
                Are you sure you want to delete this queue?
              </p>
            </div>

            <div className="modal-footer">
              <button
                className="modal-btn-cancel"
                onClick={() => setConfirmDeleteId(null)}
                disabled={deletingId === confirmDeleteId}
              >
                Cancel
              </button>

              <button
                className="modal-btn-create"
                style={{
                  background: "linear-gradient(135deg, #ef4444, #dc2626)"
                }}
                onClick={() => handleDelete(confirmDeleteId)}
                disabled={deletingId === confirmDeleteId}
              >
                {deletingId === confirmDeleteId ? (
                  <>
                    <span className="spinner-xs" /> Deleting…
                  </>
                ) : (
                  <>Delete</>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function PlusIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
    </svg>
  );
}
function BarChartIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="20" x2="18" y2="10" /><line x1="12" y1="20" x2="12" y2="4" /><line x1="6" y1="20" x2="6" y2="14" />
    </svg>
  );
}
function PeopleIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  );
}
function ClockCircleIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
    </svg>
  );
}
function ClockSmIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
    </svg>
  );
}
function TimerSmIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="13" r="8" /><polyline points="12 9 12 13 15 16" /><path d="M9.5 2.5h5" /><path d="M12 2.5v2" />
    </svg>
  );
}
function LocationSmIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
    </svg>
  );
}
function PauseIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
      <rect x="6" y="4" width="4" height="16" /><rect x="14" y="4" width="4" height="16" />
    </svg>
  );
}
function PlayIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
      <polygon points="5 3 19 12 5 21 5 3" />
    </svg>
  );
}
function XIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}
function EyeIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" /><circle cx="12" cy="12" r="3" />
    </svg>
  );
}
function QueueIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" />
      <line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
    </svg>
  );
}
function CameraUploadIcon() {
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
      <circle cx="12" cy="13" r="4" />
      <line x1="12" y1="11" x2="12" y2="15" /><line x1="10" y1="13" x2="14" y2="13" />
    </svg>
  );
}

export default Home;
