import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

const AuthContext = createContext(null);

// Fix #33: parse JWT expiry from token payload
function getTokenExpiry(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp ? payload.exp * 1000 : null; // convert to ms
  } catch {
    return null;
  }
}

function isTokenExpired(token) {
  if (!token) return true;
  const expiry = getTokenExpiry(token);
  if (!expiry) return false; // no exp claim — assume valid
  return Date.now() >= expiry;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user');
      const token = localStorage.getItem('accessToken');
      // Fix #33: if token is expired on load, clear immediately
      if (token && isTokenExpired(token)) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        return null;
      }
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('user');
    setUser(null);
  }, []);

  // Fix #33: proactively check token expiry and auto-logout
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) return;
    const expiry = getTokenExpiry(token);
    if (!expiry) return;

    const timeUntilExpiry = expiry - Date.now();
    if (timeUntilExpiry <= 0) {
      logout();
      return;
    }

    // Schedule logout at token expiry
    const timer = setTimeout(() => {
      logout();
      // Redirect to login — but only if still on a protected page
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?session=expired';
      }
    }, timeUntilExpiry);

    return () => clearTimeout(timer);
  }, [user, logout]);

  const login = (tokenResponse, meta = {}) => {
    const nextUser = {
      role: tokenResponse.role,
      accessToken: tokenResponse.accessToken,
      firstLoginRequired: Boolean(tokenResponse.firstLoginRequired),
      email: meta.email || user?.email || null,
    };
    localStorage.setItem('accessToken', tokenResponse.accessToken);
    localStorage.setItem('user', JSON.stringify(nextUser));
    setUser(nextUser);
  };

  const isRole = (...roles) => user && roles.includes(user.role);

  return (
    <AuthContext.Provider value={{ user, login, logout, isRole, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
