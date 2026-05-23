const BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

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

export interface AuthPayload {
  token: string;
  email: string;
  fullName: string;
  role: string;
  userId: string;
  organization: string;
}

export const authApi = {
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

export interface ProfilePayload {
  id: string;
  email: string;
  fullName: string;
  role: string;
  organization: string;
  username?: string;
  avatarUrl?: string | null;
}

export const profileApi = {
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

  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    request<void>('/api/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  updateAvatar: (_avatarUrl: string | null) =>
    Promise.resolve({ error: 'Avatar upload is handled via localStorage directly.' }),
};


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
  status: 'ACTIVE' | 'PAUSED' | 'INACTIVE';
  createdAt: string;
  totalWaitingCustomers?: number;
  totalServedToday?: number;
  photo?: string;
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
  list: () => request<OrganizationPayload[]>('/api/organizations'),

  get: (id: string) => request<OrganizationPayload>(`/api/organizations/${id}`),

  create: (body: CreateOrgBody) =>
    request<OrganizationPayload>('/api/organizations', {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  update: (id: string, body: Partial<CreateOrgBody>) =>
    request<OrganizationPayload>(`/api/organizations/${id}`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  updateStatus: (id: string, status: 'ACTIVE' | 'PAUSED') =>
    request<OrganizationPayload>(`/api/organizations/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }),

  delete: (id: string) =>
    request<void>(`/api/organizations/${id}`, { method: 'DELETE' }),
};


export interface AnalyticsPayload {
  activeOrganizations: number;
  totalWaitingCustomers: number;
  totalServedToday: number;
}

export const analyticsApi = {
  get: () => request<AnalyticsPayload>('/api/analytics'),
};

export interface AdminQueueEntry {
  entryId: string;
  queueNumber: number;
  customerName: string;
  position: number;
  status: 'WAITING' | 'SERVING';
  joinedAt: string;
}

export const queueApi = {
  getEntries: (orgId: string) =>
    request<AdminQueueEntry[]>(`/api/queues/organization/${orgId}`),

  callNext: (orgId: string) =>
    request<void>(`/api/queues/organization/${orgId}/call-next`, { method: 'POST' }),

  skip: (orgId: string) =>
    request<void>(`/api/queues/organization/${orgId}/skip`, { method: 'POST' }),

  markAsServed: (entryId: string) =>
    request<void>(`/api/queues/entries/${entryId}/serve`, { method: 'POST' }),
};

interface BackendAuthResponse {
  token: string;
  name: string;
  email: string;
  role: string;
  userId: string;
}

export const session = {
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