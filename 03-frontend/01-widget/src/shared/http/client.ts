import axios, { AxiosError, type AxiosRequestConfig } from 'axios';

let accessTokenProvider: (() => string | null) | null = null;
let unauthorizedHandler: (() => void) | null = null;

const httpClient = axios.create({
  timeout: 10000,
});

httpClient.interceptors.request.use((config) => {
  const token = accessTokenProvider?.();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      unauthorizedHandler?.();
    }
    return Promise.reject(error);
  }
);

export function configureHttpClient(apiUrl?: string) {
  if (!apiUrl) {
    return;
  }
  httpClient.defaults.baseURL = apiUrl;
}

export function setAccessTokenProvider(provider: (() => string | null) | null) {
  accessTokenProvider = provider;
}

export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

export async function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<T>(config);
  return response.data;
}

export function toApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data as { message?: string } | undefined;
    if (message?.message) {
      return message.message;
    }
  }
  return fallback;
}
