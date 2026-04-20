import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';

// Public
import HomePage           from './pages/HomePage';
import LoginPage          from './pages/LoginPage';
import RegisterPage       from './pages/RegisterPage';
import MfaVerifyPage      from './pages/MfaVerifyPage';
import SetupPasswordPage  from './pages/SetupPasswordPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ConcertListPage    from './pages/ConcertListPage';
import ConcertDetailPage  from './pages/ConcertDetailPage';
import NotFoundPage       from './pages/NotFoundPage';

// Shared
import DashboardPage      from './pages/DashboardPage';

// Attendee
import MyBookingsPage       from './pages/MyBookingsPage';
import AttendeeProfilePage  from './pages/AttendeeProfilePage';

// Organizer
import OrganizerConcertsPage  from './pages/OrganizerConcertsPage';
import OrganizerAnalyticsPage from './pages/OrganizerAnalyticsPage';
import CreateConcertPage      from './pages/CreateConcertPage';

// Promoter
import PromoterDashboard from './pages/PromoterDashboard';
import PromoterCsvPage   from './pages/PromoterCsvPage';

// Producer
import ProducerDashboard from './pages/ProducerDashboard';

// Shared profile
import ProfilePage from './pages/ProfilePage';

function ProtectedRoute({ children, roles }) {
  const { user, isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user?.role)) return <Navigate to="/" replace />;
  return children;
}

function AppRoutes() {
  return (
    <Routes>
      {/* ── PUBLIC ── */}
      <Route path="/"               element={<HomePage />} />
      <Route path="/login"          element={<LoginPage />} />
      <Route path="/register"       element={<RegisterPage />} />
      <Route path="/mfa-verify"     element={<MfaVerifyPage />} />
      <Route path="/setup-password" element={<SetupPasswordPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/concerts"       element={<ConcertListPage />} />
      <Route path="/concerts/:id"   element={<ConcertDetailPage />} />

      {/* ── SHARED REDIRECT ── */}
      <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />

      {/* ── ATTENDEE ── */}
      <Route path="/my-bookings" element={<ProtectedRoute roles={['ATTENDEE']}><MyBookingsPage /></ProtectedRoute>} />
      <Route path="/profile"     element={<ProtectedRoute roles={['ATTENDEE']}><AttendeeProfilePage /></ProtectedRoute>} />

      {/* ── ORGANIZER ── */}
      <Route path="/organizer/concerts"        element={<ProtectedRoute roles={['ORGANIZER']}><OrganizerConcertsPage /></ProtectedRoute>} />
      <Route path="/organizer/analytics"       element={<ProtectedRoute roles={['ORGANIZER']}><OrganizerAnalyticsPage /></ProtectedRoute>} />
      <Route path="/organizer/concerts/create" element={<ProtectedRoute roles={['ORGANIZER']}><CreateConcertPage /></ProtectedRoute>} />
      <Route path="/organizer/concerts/:id/edit" element={<ProtectedRoute roles={['ORGANIZER']}><CreateConcertPage isEdit /></ProtectedRoute>} />
      <Route path="/organizer/profile"         element={<ProtectedRoute roles={['ORGANIZER']}><ProfilePage /></ProtectedRoute>} />

      {/* ── PROMOTER ── */}
      <Route path="/promoter"         element={<ProtectedRoute roles={['PROMOTER']}><PromoterDashboard /></ProtectedRoute>} />
      <Route path="/promoter/csv"     element={<ProtectedRoute roles={['PROMOTER']}><PromoterCsvPage /></ProtectedRoute>} />
      <Route path="/promoter/profile" element={<ProtectedRoute roles={['PROMOTER']}><ProfilePage /></ProtectedRoute>} />

      {/* ── PRODUCER ── */}
      <Route path="/producer"         element={<ProtectedRoute roles={['PRODUCER']}><ProducerDashboard /></ProtectedRoute>} />
      <Route path="/producer/profile" element={<ProtectedRoute roles={['PRODUCER']}><ProfilePage /></ProtectedRoute>} />

      {/* ── 404 ── */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
        <Toaster
          position="top-right"
          toastOptions={{
            style: {
              background: '#0d1117',
              color: '#f0f4ff',
              border: '1px solid rgba(255,255,255,0.08)',
              borderRadius: '10px',
              fontFamily: "'Inter', sans-serif",
              fontSize: '13.5px',
            },
            success: { iconTheme: { primary: '#22c55e', secondary: '#0d1117' } },
            error:   { iconTheme: { primary: '#ef4444', secondary: '#0d1117' } },
          }}
        />
      </BrowserRouter>
    </AuthProvider>
  );
}
