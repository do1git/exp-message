import { create } from 'zustand';
import { REFRESH_TOKEN_STORAGE_KEY } from '@/shared/constants/auth';
import { setAccessTokenProvider, setUnauthorizedHandler, toApiErrorMessage } from '@/shared/http/client';
import { loginApi, refreshApi, signupApi } from './api';

type AuthStatus = 'idle' | 'loading' | 'authorized' | 'unauthorized' | 'error';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  userId: string | null;
  authStatus: AuthStatus;
  errorMessage: string | null;
  login: (email: string, password: string) => Promise<void>;
  signupAndLogin: (email: string, password: string) => Promise<void>;
  refreshAccessToken: () => Promise<string | null>;
  logout: () => void;
  hydrateRefreshToken: () => void;
}

function persistRefreshToken(refreshToken: string | null) {
  if (typeof window === 'undefined') {
    return;
  }

  if (refreshToken) {
    window.localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, refreshToken);
    return;
  }

  window.localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: null,
  userId: null,
  authStatus: 'idle',
  errorMessage: null,
  async login(email, password) {
    set({ authStatus: 'loading', errorMessage: null });

    try {
      const tokens = await loginApi(email, password);
      persistRefreshToken(tokens.refreshToken);
      set({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: tokens.userId,
        authStatus: 'authorized',
        errorMessage: null,
      });
    } catch (error) {
      persistRefreshToken(null);
      set({
        accessToken: null,
        refreshToken: null,
        userId: null,
        authStatus: 'unauthorized',
        errorMessage: toApiErrorMessage(error, 'Invalid email or password'),
      });
    }
  },
  async signupAndLogin(email, password) {
    set({ authStatus: 'loading', errorMessage: null });

    try {
      await signupApi(email, password);
      await useAuthStore.getState().login(email, password);
    } catch (error) {
      set({
        authStatus: 'error',
        errorMessage: toApiErrorMessage(error, 'Unable to create account'),
      });
    }
  },
  async refreshAccessToken() {
    const refreshToken = useAuthStore.getState().refreshToken;
    if (!refreshToken) {
      return null;
    }

    try {
      const tokens = await refreshApi(refreshToken);
      persistRefreshToken(tokens.refreshToken);
      set({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        userId: tokens.userId,
        authStatus: 'authorized',
        errorMessage: null,
      });
      return tokens.accessToken;
    } catch {
      useAuthStore.getState().logout();
      return null;
    }
  },
  logout() {
    persistRefreshToken(null);
    set({
      accessToken: null,
      refreshToken: null,
      userId: null,
      authStatus: 'unauthorized',
      errorMessage: null,
    });
  },
  hydrateRefreshToken() {
    if (typeof window === 'undefined') {
      return;
    }

    const refreshToken = window.localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
    set((state) => ({
      refreshToken,
      authStatus: state.accessToken ? 'authorized' : 'unauthorized',
    }));
  },
}));

setAccessTokenProvider(() => useAuthStore.getState().accessToken);
setUnauthorizedHandler(() => useAuthStore.getState().logout());
