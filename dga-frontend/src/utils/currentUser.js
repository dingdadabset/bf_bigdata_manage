const USER_KEY = 'user';
const TOKEN_KEY = 'token';

export function clearAuthCache() {
  sessionStorage.removeItem(USER_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(TOKEN_KEY);
}

export function saveAuthSession(payload, token) {
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(TOKEN_KEY);
  if (payload) {
    sessionStorage.setItem(USER_KEY, JSON.stringify(payload));
  }
  const authToken = token || (payload && payload.token);
  if (authToken) {
    sessionStorage.setItem(TOKEN_KEY, authToken);
  }
}

export function hasAuthSession() {
  return !!sessionStorage.getItem(USER_KEY);
}

export function getAuthToken() {
  return sessionStorage.getItem(TOKEN_KEY) || '';
}

export function getCurrentUser() {
  try {
    const userStr = sessionStorage.getItem(USER_KEY);
    if (!userStr) return {};
    const data = JSON.parse(userStr);
    return data.user || data || {};
  } catch (e) {
    return {};
  }
}

export function getCurrentUsername() {
  const user = getCurrentUser();
  return user.username || '';
}

export function canDelete() {
  const user = getCurrentUser();
  return user.username === 'admin' || Number(user.isAdmin) === 1;
}

export function isRootAdmin() {
  return getCurrentUsername() === 'admin';
}

export function deleteForbiddenMessage() {
  return '仅 admin 或超级用户可执行删除操作';
}
