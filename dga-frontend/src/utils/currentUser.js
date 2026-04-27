export function getCurrentUser() {
  try {
    const userStr = localStorage.getItem('user');
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
