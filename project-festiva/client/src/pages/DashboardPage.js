import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getRoleHome } from '../utils/app';

export default function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  useEffect(() => { navigate(getRoleHome(user?.role), { replace: true }); }, [user, navigate]);
  return <div className="page-loader"><div className="spinner" style={{ width: 32, height: 32, borderWidth: 3 }} /></div>;
}
