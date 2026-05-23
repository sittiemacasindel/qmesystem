import { useCallback, useEffect, useRef, useState } from "react";
import {
  AdminQueueEntry,
  organizationApi,
  OrganizationPayload,
  profileApi,
  queueApi,
} from "../lib/apiClient";
import "./QueueDetail.css";
import "../pages/Home.css"; // reuse app-shell, app-header, icon-sidebar
import Sidebar from "../components/Sidebar";

const AVATAR_STORAGE_KEY = "qme_avatar";
const ORG_PHOTO_KEY_PREFIX = "qme_org_photo_";
const POLL_INTERVAL_MS = 10_000;

interface QueueDetailProps {
  orgId: string;
  onBack: () => void;
  onNavigateToProfile: () => void;
  onLogout: () => void;
}

type Toast = { type: "success" | "error"; text: string } | null;

function QueueDetail({ orgId, onBack, onNavigateToProfile, onLogout }: QueueDetailProps) {
  const [org, setOrg] = useState<OrganizationPayload | null>(null);
  const [entries, setEntries] = useState<AdminQueueEntry[]>([]);
  const [loadingOrg, setLoadingOrg] = useState(true);
  const [loadingEntries, setLoadingEntries] = useState(true);
  const [profileName, setProfileName] = useState("");
  const [profileRole, setProfileRole] = useState("Admin");
  const [avatarDataUrl, setAvatarDataUrl] = useState<string | null>(null);
  const [orgPhoto, setOrgPhoto] = useState<string | null>(null);

  // Action states
  const [callingNext, setCallingNext] = useState(false);
  const [skipping, setSkipping] = useState(false);
  const [servingId, setServingId] = useState<string | null>(null);
  const [toast, setToast] = useState<Toast>(null);
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pollTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const showToast = (type: "success" | "error", text: string) => {
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    setToast({ type, text });
    toastTimerRef.current = setTimeout(() => setToast(null), 3500);
  };

  const getInitials = (name: string) =>
    name.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2) || "?";

  const fetchEntries = useCallback(async () => {
    const res = await queueApi.getEntries(orgId);
    if (res.data) setEntries(res.data);
    setLoadingEntries(false);
  }, [orgId]);

  const fetchOrg = useCallback(async () => {
    const res = await organizationApi.get(orgId);
    if (res.data) setOrg(res.data);
  }, [orgId]);

  useEffect(() => {
    // Profile
    profileApi.get().then((res) => {
      if (res.data) {
        setProfileName(res.data.fullName);
        setProfileRole(res.data.role);
      }
    });
    const saved = localStorage.getItem(AVATAR_STORAGE_KEY);
    if (saved) setAvatarDataUrl(saved);

    // Org details
    organizationApi.get(orgId).then((res) => {
      if (res.data) {
        setOrg(res.data);
        // Use backend photo if localStorage doesn't have one
        const stored = localStorage.getItem(ORG_PHOTO_KEY_PREFIX + orgId);
        if (stored) {
          setOrgPhoto(stored);
        } else if (res.data.photo) {
          setOrgPhoto(res.data.photo);
        }
      }
      setLoadingOrg(false);
    });

    // Queue entries
    fetchEntries();

    // Poll for updates every 10 seconds
    pollTimerRef.current = setInterval(async () => {
      await fetchEntries();
      await fetchOrg();
    }, POLL_INTERVAL_MS);

    return () => {
      if (pollTimerRef.current) clearInterval(pollTimerRef.current);
      if (toastTimerRef.current) clearTimeout(toastTimerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgId]);

  // ── Queue Actions ──
  const handleCallNext = async () => {
    setCallingNext(true);
    const res = await queueApi.callNext(orgId);
    if (res.error) {
      showToast("error", res.error);
    } else {
      showToast("success", "Called next customer successfully.");
      await fetchEntries();
      await fetchOrg();
    }
    setCallingNext(false);
  };

  const handleSkip = async () => {
    setSkipping(true);
    const res = await queueApi.skip(orgId);
    if (res.error) {
      showToast("error", res.error);
    } else {
      showToast("success", "Customer skipped.");
      await fetchEntries();
      await fetchOrg();
    }
    setSkipping(false);
  };

  const handleMarkServed = async (entryId: string) => {
    setServingId(entryId);
    const res = await queueApi.markAsServed(entryId);
    if (res.error) {
      showToast("error", res.error);
    } else {
      showToast("success", "Marked as served.");
      await fetchEntries();
      await fetchOrg();
    }
    setServingId(null);
  };

  const handlePauseToggle = async () => {
    if (!org) return;
    const newStatus = org.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    const res = await organizationApi.updateStatus(orgId, newStatus as 'ACTIVE' | 'PAUSED');
    if (res.error) {
      showToast('error', res.error);
    } else {
      showToast('success', newStatus === 'PAUSED' ? 'Queue paused.' : 'Queue resumed.');
      await fetchOrg();
    }
  };

  const handleCloseQueue = async () => {
    if (!window.confirm('Close this queue? Customers will no longer be able to join.')) return;
    const res = await organizationApi.updateStatus(orgId, 'INACTIVE' as any);
    if (res.error) {
      showToast('error', res.error);
    } else {
      showToast('success', 'Queue closed.');
      await fetchOrg();
    }
  };

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onloadend = async () => {
      const base64 = reader.result as string;
      setOrgPhoto(base64);
      localStorage.setItem(ORG_PHOTO_KEY_PREFIX + orgId, base64);
      // Sync to backend so mobile can see it
      await organizationApi.update(orgId, { photo: base64 } as any);
    };
    reader.readAsDataURL(file);
  };

  // ── Derived data ──
  const servingEntry = entries.find((e) => e.status === "SERVING") ?? null;
  const waitingEntries = entries.filter((e) => e.status === "WAITING");

  const formatDate = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleDateString([], { month: "short", day: "numeric" }) + " · " +
        d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    } catch {
      return iso;
    }
  };

  if (loadingOrg) {
    return (
      <div className="qd-loading">
        <span className="spinner-icon" />
        Loading queue details…
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <div className="app-body">
        <Sidebar
          activeItem="dashboard"
          onNavigateToDashboard={onBack}
          onNavigateToProfile={onNavigateToProfile}
          onLogout={onLogout}
        />

        <div className="right-panel">
          {/* ── App Header ── */}
          <header className="app-header">
            <div className="app-header-brand">
              <img src="/Qme_Logo.png" alt="QMe" className="header-logo-img" />
              {org?.name && (
                <span className="header-org-chip">{org.name}</span>
              )}
            </div>

            <div className="app-header-right">
              <div className="header-user-block">
                <span className="header-user-name">{profileName || "Admin"}</span>
                <span className="header-user-role">{profileRole}</span>
              </div>
              <button className="header-avatar-btn" onClick={onNavigateToProfile} title="View Profile">
                {avatarDataUrl ? (
                  <img src={avatarDataUrl} alt="Avatar" className="header-avatar-img" />
                ) : (
                  <span className="header-avatar-initials">
                    {profileName ? getInitials(profileName) : "A"}
                  </span>
                )}
              </button>
            </div>
          </header>

          {/* ── Queue Detail Main ── */}
          <main className="qd-main">

            {/* Back button */}
            <div className="qd-back-row">
              <button className="qd-back-btn" onClick={onBack}>
                <BackIcon /> Back to Dashboard
              </button>
            </div>

            {/* ═══ SECTION 1: Org Header Card ═══ */}
            {org && (
              <div className="qd-header-card">
                <div className="qd-header-top">
                  {/* Photo (clickable to upload) */}
                  <label htmlFor="org-photo-upload" style={{ cursor: 'pointer', display: 'block' }} title="Click to change photo">
                    {orgPhoto ? (
                      <img src={orgPhoto} alt={org.name} className="qd-org-photo" />
                    ) : (
                      <div className="qd-org-photo-placeholder">
                        <PeopleIcon />
                      </div>
                    )}
                    <input id="org-photo-upload" type="file" accept="image/*" style={{ display: 'none' }} onChange={handlePhotoUpload} />
                  </label>

                  <div className="qd-header-info">
                    {/* Name row with status controls */}
                    <div className="qd-header-name-row">
                      <h1 className="qd-org-name">{org.name}</h1>
                      <span className="qd-code-chip">
                        <CodeIcon /> {org.queueCode}
                      </span>
                      <span className={`qd-status-badge qd-status-badge--${org.status.toLowerCase()}`}>
                        <span className="qd-status-dot" />
                        {org.status}
                      </span>
                      {/* ── Pause / Close buttons ── */}
                      <button
                        className={`qd-action-btn qd-ctrl-btn ${org.status === 'ACTIVE' ? 'qd-action-btn--skip' : 'qd-action-btn--next'}`}
                        onClick={handlePauseToggle}
                        disabled={org.status === 'INACTIVE'}
                        title={org.status === 'ACTIVE' ? 'Pause queue' : 'Resume queue'}
                        style={{ marginLeft: 8 }}
                      >
                        {org.status === 'ACTIVE' ? <PauseIcon /> : <PlayIcon />}
                        {org.status === 'ACTIVE' ? 'Pause' : 'Resume'}
                      </button>
                      <button
                        className="qd-action-btn qd-ctrl-btn qd-action-btn--danger"
                        onClick={handleCloseQueue}
                        disabled={org.status === 'INACTIVE'}
                        title="Close queue"
                        style={{ marginLeft: 6 }}
                      >
                        <CloseIcon /> Close
                      </button>
                    </div>

                    {/* Meta pills */}
                    <div className="qd-header-meta">
                      <span className="qd-meta-pill">
                        <ClockIcon />
                        {org.openingHours} – {org.closingHours}
                      </span>
                      <span className="qd-meta-pill">
                        <TimerIcon />
                        {org.waitTimeMin}–{org.waitTimeMax} min wait
                      </span>
                      <span className="qd-meta-pill">
                        <LocationIcon />
                        {org.location}
                      </span>
                      <span className="qd-meta-pill">
                        <PhoneIcon />
                        {org.contactNumber}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Stats row */}
                <div className="qd-header-stats">
                  <div className="qd-stat-box">
                    <div className="qd-stat-icon qd-stat-icon--blue">
                      <PeopleIcon />
                    </div>
                    <div className="qd-stat-body">
                      <span className="qd-stat-num">
                        {org.totalWaitingCustomers ?? 0}
                      </span>
                      <span className="qd-stat-lbl">Waiting Customers</span>
                    </div>
                  </div>

                  <div className="qd-stat-box">
                    <div className="qd-stat-icon qd-stat-icon--teal">
                      <CheckIcon />
                    </div>
                    <div className="qd-stat-body">
                      <span className="qd-stat-num">
                        {org.totalServedToday ?? 0}
                      </span>
                      <span className="qd-stat-lbl">Total Served Today</span>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* ═══ SECTIONS 2 & 3: side-by-side ═══ */}
            <div className="qd-body-grid">

              {/* ── SECTION 2 (LEFT): Currently Serving ── */}
              <div className="qd-serving-card">
                <div className="qd-serving-header">
                  <div className="qd-serving-header-icon">
                    <StarIcon />
                  </div>
                  <div>
                    <div className="qd-serving-title">Currently Serving</div>
                  </div>
                </div>

                <div className="qd-serving-body">
                  {servingEntry ? (
                    <>
                      {/* Customer info */}
                      <div className="qd-serving-customer">
                        <div className="qd-serving-avatar">
                          {getInitials(servingEntry.customerName)}
                        </div>
                        <div className="qd-serving-info">
                          <div className="qd-serving-name">{servingEntry.customerName}</div>
                          <div className="qd-serving-badges">
                            <span className="qd-badge qd-badge--number">
                              #{servingEntry.queueNumber}
                            </span>
                            <span className="qd-badge qd-badge--pos">
                              Position 1
                            </span>
                            <span className="qd-badge qd-badge--serving">
                              ● Serving
                            </span>
                          </div>
                        </div>
                      </div>

                      {/* Actions */}
                      <div className="qd-serving-actions">
                        <button
                          className="qd-action-btn qd-action-btn--next"
                          onClick={handleCallNext}
                          disabled={callingNext || skipping || !!servingId}
                          title="Mark current as served and call next customer"
                        >
                          {callingNext
                            ? <><span className="spinner-sm" /> Calling next…</>
                            : <><NextIcon /> Call Next</>}
                        </button>

                        <button
                          className="qd-action-btn qd-action-btn--serve"
                          onClick={() => handleMarkServed(servingEntry.entryId)}
                          disabled={callingNext || skipping || !!servingId}
                          title="Mark this customer as served"
                        >
                          {servingId === servingEntry.entryId
                            ? <><span className="spinner-sm" /> Marking…</>
                            : <><CheckIcon /> Mark as Served</>}
                        </button>

                        <button
                          className="qd-action-btn qd-action-btn--skip"
                          onClick={handleSkip}
                          disabled={callingNext || skipping || !!servingId}
                          title="Skip this customer and call next"
                        >
                          {skipping
                            ? <><span className="spinner-sm" /> Skipping…</>
                            : <><SkipIcon /> Skip</>}
                        </button>
                      </div>
                    </>
                  ) : (
                    <>
                      {/* No one serving — show "Call Next" anyway if there are waiting customers */}
                      <div className="qd-serving-empty">
                        <div className="qd-serving-empty-icon">🪑</div>
                        <div className="qd-serving-empty-title">No one is being served</div>
                        <div className="qd-serving-empty-sub">
                          {waitingEntries.length > 0
                            ? 'Click "Call Next" to serve the first customer.'
                            : "The queue is currently empty."}
                        </div>
                      </div>

                      {waitingEntries.length > 0 && (
                        <div className="qd-serving-actions">
                          <button
                            className="qd-action-btn qd-action-btn--next"
                            onClick={handleCallNext}
                            disabled={callingNext}
                          >
                            {callingNext
                              ? <><span className="spinner-sm" /> Calling…</>
                              : <><NextIcon /> Call Next</>}
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </div>
              </div>

              {/* ── SECTION 3 (RIGHT): Waiting Customers Table ── */}
              <div className="qd-waiting-card">
                <div className="qd-waiting-header">
                  <div className="qd-waiting-header-left">
                    <div className="qd-waiting-header-icon">
                      <PeopleIcon />
                    </div>
                    <div>
                      <div className="qd-waiting-title">Waiting Customers</div>
                      <div className="qd-waiting-count">
                        {loadingEntries ? "Loading…" : `${waitingEntries.length} in queue`}
                      </div>
                    </div>
                  </div>
                  <div className="qd-refresh-info">
                    <span className="qd-refresh-dot" />
                    Live · refreshes every 10s
                  </div>
                </div>

                <div className="qd-waiting-table-wrap">
                  {waitingEntries.length === 0 && !loadingEntries ? (
                    <div className="qd-waiting-empty">
                      <div className="qd-waiting-empty-title">Queue is empty!</div>
                      <div className="qd-waiting-empty-sub">
                        No customers are currently waiting.
                      </div>
                    </div>
                  ) : (
                    <table className="qd-waiting-table">
                      <thead>
                        <tr>
                          <th>QUEUE #</th>
                          <th>CUSTOMER NAME</th>
                          <th>POSITION</th>
                          <th>JOINED AT</th>
                        </tr>
                      </thead>
                      <tbody>
                        {waitingEntries.map((entry) => (
                          <tr key={entry.entryId}>
                            <td>
                              <span className="qd-wt-num">#{entry.queueNumber}</span>
                            </td>
                            <td>
                              <span className="qd-wt-name">{entry.customerName}</span>
                            </td>
                            <td>
                              <span className="qd-wt-pos">{entry.position}</span>
                            </td>
                            <td>
                              <span className="qd-wt-time">{formatDate(entry.joinedAt)}</span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              </div>
            </div>
          </main>
        </div>
      </div>

      {/* ── Toast notification ── */}
      {toast && (
        <div className={`qd-toast qd-toast--${toast.type}`}>
          {toast.type === "success" ? "✓" : "⚠"} {toast.text}
        </div>
      )}
    </div>
  );
}

/* ── Icons ── */
function BackIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="15 18 9 12 15 6" />
    </svg>
  );
}
function QueueLargeIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <line x1="8" y1="6" x2="21" y2="6" /><line x1="8" y1="12" x2="21" y2="12" /><line x1="8" y1="18" x2="21" y2="18" />
      <line x1="3" y1="6" x2="3.01" y2="6" /><line x1="3" y1="12" x2="3.01" y2="12" /><line x1="3" y1="18" x2="3.01" y2="18" />
    </svg>
  );
}
function CodeIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="16 18 22 12 16 6" /><polyline points="8 6 2 12 8 18" />
    </svg>
  );
}
function ClockIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
    </svg>
  );
}
function TimerIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="13" r="8" /><polyline points="12 9 12 13 15 16" /><path d="M9.5 2.5h5" /><path d="M12 2.5v2" />
    </svg>
  );
}
function LocationIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
    </svg>
  );
}
function PhoneIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.58 3.4 2 2 0 0 1 3.56 1.18h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.69a16 16 0 0 0 6 6l.88-.88a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
    </svg>
  );
}
function PeopleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" />
      <path d="M23 21v-2a4 4 0 0 0-3-3.87" /><path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </svg>
  );
}
function CheckIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
function StarIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
    </svg>
  );
}
function NextIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="9 18 15 12 9 6" /><line x1="19" y1="6" x2="19" y2="18" />
    </svg>
  );
}
function SkipIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="5 4 15 12 5 20 5 4" /><line x1="19" y1="5" x2="19" y2="19" />
    </svg>
  );
}
function PauseIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="4" width="4" height="16" /><rect x="14" y="4" width="4" height="16" />
    </svg>
  );
}
function PlayIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polygon points="5 3 19 12 5 21 5 3" />
    </svg>
  );
}
function CloseIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

export default QueueDetail;
