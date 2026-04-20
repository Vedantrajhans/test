import axios from 'axios';

const API_BASE = '/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?session=expired';
      }
    }
    return Promise.reject(err);
  }
);

export const extractErrorMessage = (err, fallback = 'Something went wrong') => {
  const data = err?.response?.data;
  if (!data) return fallback;
  if (typeof data.message === 'string') return data.message;
  if (typeof data === 'string') return data;
  const first = Object.values(data)[0];
  return typeof first === 'string' ? first : fallback;
};

export const authApi = {
  register:       (data) => api.post('/auth/register', data),
  login:          (data) => api.post('/auth/login', data),
  requestOtpLogin:(data) => api.post('/auth/login/otp/request', data),
  verifyMfa:      (data) => api.post('/auth/mfa/verify', data),
  enableMfa:      ()     => api.post('/auth/mfa/enable'),
  disableMfa:     ()     => api.post('/auth/mfa/disable'),
  setupPassword:  (data) => api.post('/auth/organizer/setup-password', data),
  forgotPassword: (data) => api.post('/auth/forgot-password', data),
  resetPassword:  (data) => api.post('/auth/reset-password', data),
  requestChangePasswordOtp: () => api.post('/auth/change-password/request-otp'),
  changePassword: (data) => api.post('/auth/change-password', data),
  updateProfile:  (data) => api.put('/auth/profile', data),
  getProfile:     ()     => api.get('/auth/profile'),
};

export const concertApi = {
  search:         (params) => api.get('/concerts', { params }),
  getById:        (id)     => api.get(`/concerts/${id}`),
  getMyConcerts:  (params) => api.get('/organizer/concerts', { params }),
  create:         (data)   => api.post('/organizer/concerts', data),
  update:         (id, data) => api.put(`/organizer/concerts/${id}`, data),
  publish:        (id)     => api.patch(`/organizer/concerts/${id}/publish`),
  delete:         (id)     => api.delete(`/organizer/concerts/${id}`),
  exportAttendees:(id)     => api.get(`/organizer/concerts/${id}/attendees/export`, { responseType: 'blob' }),
};

export const bookingApi = {
  reserve:        (data)        => api.post('/bookings/reserve', data),
  confirmPayment: (data)        => api.post('/bookings/confirm', data),
  getMyBookings:  (params)      => api.get('/bookings/my', { params }),
  cancelBooking:  (bookingUuid) => api.post(`/bookings/${bookingUuid}/cancel`),
};

export const feedbackApi = {
  submit:           (data)      => api.post('/feedback', data),
  update:           (id, data)  => api.put(`/feedback/${id}`, data),
  delete:           (id)        => api.delete(`/feedback/${id}`),
  getForConcert:    (concertId) => api.get(`/feedback/concert/${concertId}`),
  getMyForConcert:  (concertId) => api.get(`/feedback/concert/${concertId}/my`),
};

export const producerApi = {
  createPromoter:   (data)  => api.post('/producer/promoters', data),
  listPromoters:    ()      => api.get('/producer/promoters'),
  updatePromoter:   (id, data) => api.put(`/producer/promoters/${id}`, data),
  deactivatePromoter:(id)   => api.delete(`/producer/promoters/${id}`),
  hardDeletePromoter:(id)   => api.delete(`/producer/promoters/${id}/hard`),
  listAllUsers:     (params)=> api.get('/producer/users', { params }),
  hardDeleteUser:   (id)    => api.delete(`/producer/users/${id}/hard`),
};

export const promoterApi = {
  createOrganizer:   (data)  => api.post('/promoter/organizers', data),
  listOrganizers:    ()      => api.get('/promoter/organizers'),
  updateOrganizer:   (id, data) => api.put(`/promoter/organizers/${id}`, data),
  deactivateOrganizer:(id)   => api.delete(`/promoter/organizers/${id}`),
  hardDeleteOrganizer:(id)   => api.delete(`/promoter/organizers/${id}/hard`),
  importOrganizers:  (file)  => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/promoter/organizers/import', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  exportOrganizers: (params) => api.get('/promoter/organizers/export', { params, responseType: 'blob' }),
};

export default api;
