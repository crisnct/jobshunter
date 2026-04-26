const trimTrailingSlash = (value) => value.replace(/\/+$/, '');

const ensureLeadingSlash = (value) => (value.startsWith('/') ? value : `/${value}`);

const normalizePath = (value) => ensureLeadingSlash(trimTrailingSlash(value.trim()));

const normalizeAbsoluteUrl = (value) => trimTrailingSlash(value.trim());
const ensureApiSuffix = (value) => (value.endsWith('/api') ? value : `${value}/api`);

export const resolveApiBase = (env = import.meta.env) => {
  const baseUrl = env?.VITE_API_BASE_URL?.trim();
  if (baseUrl) {
    return ensureApiSuffix(normalizeAbsoluteUrl(baseUrl));
  }

  const pathPrefix = env?.VITE_API_PATH_PREFIX?.trim();
  if (pathPrefix) {
    return normalizePath(pathPrefix);
  }

  return '/api';
};

export const buildApiUrl = (base, path) => {
  const normalizedPath = ensureLeadingSlash(String(path || '').trim());
  if (!base || base === '/') {
    return normalizedPath;
  }

  const normalizedBase = base.endsWith('/') ? base.slice(0, -1) : base;
  return `${normalizedBase}${normalizedPath}`;
};
