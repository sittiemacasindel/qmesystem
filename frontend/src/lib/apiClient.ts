/**
 * apiClient.ts
 * Centralised HTTP client for the QMe Spring Boot backend.
 * Base URL: http://localhost:8080
 */

const BASE_URL = 'http://localhost:8080';

/** Generic fetch wrapper that handles auth header and JSON parsing. */
async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<{ data?: T; error?: string }> {
  const token = localStorage.getItem('qme_token');
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  try {
    const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });
    const json = await res.json();

    if (!res.ok || !json.success) {
      return { error: json.message || 'An error occurred.' };
    }
    return { data: json.data };
  } catch {
    return { error: 'A network error occurred. Please check your connection.' };
  }
}

/* ── Auth ── */

export interface AuthPayload {
  token: string;
  email: string;
  fullName: string;
  role: string;
  userId: string;
  organization: string;
}

export const authApi = {
  /**
   * Backend expects: { name, email, password, role }
   * Frontend collects: { fullName, email, password }
   * We map fullName → name and hard-code role = 'ADMIN'.
   */
  register: (body: {
    fullName: string;
    email: string;
    password: string;
    organization?: string;
  }) =>
    request<AuthPayload>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        name: body.fullName,
        email: body.email,
        password: body.password,
        role: 'ADMIN',
        organization: body.organization,
      }),
    }),

  login: (body: { email: string; password: string }) =>
    request<AuthPayload>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
};

/* ── Profile ── */

export interface ProfilePayload {
  id: string;
  email: string;
  fullName: string;
  role: string;
  organization: string;
  // kept optional for UI compatibility
  username?: string;
  avatarUrl?: string | null;
}

export const profileApi = {
  /** GET /api/users/me → maps backend `name` → `fullName` */
  get: async () => {
    const res = await request<{ id: string; name: string; email: string; role: string; organization?: string }>(
      '/api/users/me'
    );
    if (res.error) return { error: res.error };
    const d = res.data!;
    return {
      data: {
        id: String(d.id),
        fullName: d.name,
        email: d.email,
        role: d.role,
        organization: d.organization ?? '',
      } as ProfilePayload,
    };
  },

  /** PUT /api/users/me — backend accepts `{ name, organization }` */
  update: async (body: { fullName: string; organization?: string }) => {
    const res = await request<{ id: string; name: string; email: string; role: string; organization?: string }>(
      '/api/users/me',
      {
        method: 'PUT',
        body: JSON.stringify({ name: body.fullName, organization: body.organization }),
      }
    );
    if (res.error) return { error: res.error };
    const d = res.data!;
    return {
      data: {
        id: String(d.id),
        fullName: d.name,
        email: d.email,
        role: d.role,
        organization: d.organization ?? '',
      } as ProfilePayload,
    };
  },

  /** PUT /api/users/me/password — verifies currentPassword on the backend */
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    request<void>('/api/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  // Avatar upload is handled via localStorage base64
  updateAvatar: (_avatarUrl: string | null) =>
    Promise.resolve({ error: 'Avatar upload is handled via localStorage directly.' }),
};

/* ── Organizations (displayed as "Queues" in the UI) ── */

export interface OrganizationPayload {
  id: string;
  name: string;
  queueCode: string;
  openingHours: string;
  closingHours: string;
  waitTimeMin: number;
  waitTimeMax: number;
  location: string;
  contactNumber: string;
  status: 'ACTIVE' | 'PAUSED';
  createdAt: string;
  totalWaitingCustomers?: number;
  totalServedToday?: number;
}

export interface CreateOrgBody {
  name: string;
  openingHours: string;
  closingHours: string;
  waitTimeMin: number;
  waitTimeMax: number;
  location: string;
  contactNumber: string;
}

export const organizationApi = {
  /** GET /api/organizations — list all organizations owned by the logged-in admin */
  list: () => request<OrganizationPayload[]>('/api/organizations'),

  /** GET /api/organizations/:id — get full details including queue stats */
  get: (id: string) => request<OrganizationPayload>(`/api/organizations/${id}`),

  /** POST /api/organizations — create a new organization (queue code is auto-generated) */
  create: (body: CreateOrgBody) =>
    request<OrganizationPayload>('/api/organizations', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  /** PUT /api/organizations/:id — update organization details */
  update: (id: string, body: Partial<CreateOrgBody>) =>
    request<OrganizationPayload>(`/api/organizations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  /** PUT /api/organizations/:id/status — toggle ACTIVE ↔ PAUSED */
  updateStatus: (id: string, status: 'ACTIVE' | 'PAUSED') =>
    request<OrganizationPayload>(`/api/organizations/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }),

  /** DELETE /api/organizations/:id */
  delete: (id: string) =>
    request<void>(`/api/organizations/${id}`, { method: 'DELETE' }),
};

/* ── Analytics ── */

export interface AnalyticsPayload {
  activeOrganizations: number;
  totalWaitingCustomers: number;
  totalServedToday: number;
}

export const analyticsApi = {
  /** GET /api/analytics */
  get: () => request<AnalyticsPayload>('/api/analytics'),
};

/* ── Queue Entries (Admin Operations) ── */

export interface AdminQueueEntry {
  entryId: string;
  queueNumber: number;
  customerName: string;
  position: number;
  status: 'WAITING' | 'SERVING';
  joinedAt: string;
}

export const queueApi = {
  /** GET /api/queues/organization/:orgId — get all active queue entries for an org */
  getEntries: (orgId: string) =>
    request<AdminQueueEntry[]>(`/api/queues/organization/${orgId}`),

  /** POST /api/queues/organization/:orgId/call-next — mark serving→served, next waiting→serving */
  callNext: (orgId: string) =>
    request<void>(`/api/queues/organization/${orgId}/call-next`, { method: 'POST' }),

  /** POST /api/queues/organization/:orgId/skip — skip the currently serving customer */
  skip: (orgId: string) =>
    request<void>(`/api/queues/organization/${orgId}/skip`, { method: 'POST' }),

  /** POST /api/queues/entries/:entryId/serve — mark a specific entry as served */
  markAsServed: (entryId: string) =>
    request<void>(`/api/queues/entries/${entryId}/serve`, { method: 'POST' }),
};

/* ── Session helpers ── */

/** Raw shape returned by the backend's auth endpoints */
interface BackendAuthResponse {
  token: string;
  name: string;
  email: string;
  role: string;
  userId: string;
}

export const session = {
  /** Maps backend field names to the UI's AuthPayload shape before persisting. */
  save: (payload: BackendAuthResponse | AuthPayload) => {
    localStorage.setItem('qme_token', payload.token);
    const normalised: AuthPayload = {
      token: payload.token,
      email: payload.email,
      fullName: (payload as BackendAuthResponse).name ?? (payload as AuthPayload).fullName ?? '',
      role: payload.role,
      userId: String(payload.userId),
      organization: (payload as any).organization ?? '',
    };
    localStorage.setItem('qme_user', JSON.stringify(normalised));
  },
  get: (): AuthPayload | null => {
    const raw = localStorage.getItem('qme_user');
    return raw ? JSON.parse(raw) : null;
  },
  clear: () => {
    localStorage.removeItem('qme_token');
    localStorage.removeItem('qme_user');
  },
  isLoggedIn: () => !!localStorage.getItem('qme_token'),
};
