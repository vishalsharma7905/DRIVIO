import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';
import { AuthResponse, Role, VerificationStatus } from '../types';

interface AuthContextType {
  user: AuthResponse | null;
  token: string | null;
  isAuthenticated: boolean;
  role: Role | null;
  verificationStatus: VerificationStatus | null;
  login: (email: string, password: string) => Promise<AuthResponse>;
  register: (data: any) => Promise<AuthResponse>;
  logout: () => void;
  updateUserVerification: (status: VerificationStatus) => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const savedToken = localStorage.getItem('drivio_token');
    const savedUser = localStorage.getItem('drivio_user');

    if (savedToken && savedUser) {
      try {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      } catch (e) {
        localStorage.removeItem('drivio_token');
        localStorage.removeItem('drivio_user');
      }
    }
    setLoading(false);
  }, []);

  const login = async (email: string, password: string): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/login', { email, password });
    const data = response.data;
    setToken(data.token);
    setUser(data);
    localStorage.setItem('drivio_token', data.token);
    localStorage.setItem('drivio_user', JSON.stringify(data));
    return data;
  };

  const register = async (data: any): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/register', data);
    const authData = response.data;
    setToken(authData.token);
    setUser(authData);
    localStorage.setItem('drivio_token', authData.token);
    localStorage.setItem('drivio_user', JSON.stringify(authData));
    return authData;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('drivio_token');
    localStorage.removeItem('drivio_user');
  };

  const updateUserVerification = (status: VerificationStatus) => {
    if (user) {
      const updated = { ...user, verificationStatus: status };
      setUser(updated);
      localStorage.setItem('drivio_user', JSON.stringify(updated));
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        role: user?.role || null,
        verificationStatus: user?.verificationStatus || null,
        login,
        register,
        logout,
        updateUserVerification,
        loading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

