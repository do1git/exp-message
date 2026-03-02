import { apiRequest } from '@/shared/http/client';

interface AuthRequest {
  email: string;
  password: string;
}

interface AuthTokens {
  accessToken: string;
  refreshToken: string | null;
  userId: string | null;
}

function extractTokens(payload: unknown): AuthTokens {
  const data = payload as {
    success?: boolean;
    data?: {
      accessToken?: string;
      refreshToken?: string;
      userId?: string;
      token?: string;
      tokens?: {
        accessToken?: string;
        refreshToken?: string;
      };
    };
    accessToken?: string;
    refreshToken?: string;
    userId?: string;
    token?: string;
    tokens?: {
      accessToken?: string;
      refreshToken?: string;
    };
  };

  const tokenSource = data.data ?? data;
  const accessToken = tokenSource.accessToken ?? tokenSource.token ?? tokenSource.tokens?.accessToken;
  const refreshToken = tokenSource.refreshToken ?? tokenSource.tokens?.refreshToken ?? null;
  const userId = tokenSource.userId ?? null;

  if (!accessToken) {
    throw new Error('No access token returned by server.');
  }

  return { accessToken, refreshToken, userId };
}

export async function loginApi(email: string, password: string) {
  const payload = await apiRequest<unknown>({
    method: 'POST',
    url: '/auth/login',
    data: { email, password } satisfies AuthRequest,
  });

  return extractTokens(payload);
}

export async function refreshApi(refreshToken: string) {
  const payload = await apiRequest<unknown>({
    method: 'POST',
    url: '/auth/refresh',
    data: { refreshToken },
  });

  return extractTokens(payload);
}

export async function signupApi(email: string, password: string) {
  await apiRequest<unknown>({
    method: 'POST',
    url: '/users',
    data: { email, password } satisfies AuthRequest,
  });
}
