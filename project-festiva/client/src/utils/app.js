export const roleHomeRoute = {
  PRODUCER: '/producer',
  PROMOTER: '/promoter',
  ORGANIZER: '/organizer/concerts',
  ATTENDEE: '/concerts',
};

export const organizerTypes = [
  'Concert Organizer',
  'Festival Organizer',
  'Club Promoter',
  'Independent Gig Curator',
  'Campus Events Team',
];

export function getRoleHome(role) {
  return roleHomeRoute[role] || '/concerts';
}

export function normalizeConcertStatus(status) {
  if (status === 'ACTIVE') return 'PUBLISHED';
  if (status === 'INACTIVE') return 'CANCELLED';
  if (status === 'PENDING') return 'DRAFT';
  return status || 'DRAFT';
}

export function statusTone(status) {
  return {
    ACTIVE: 'badge-green',
    PUBLISHED: 'badge-green',
    PENDING: 'badge-yellow',
    DRAFT: 'badge-yellow',
    INACTIVE: 'badge-red',
    CANCELLED: 'badge-red',
    LIVE: 'badge-accent',
    COMPLETED: 'badge-purple',
    SUSPENDED: 'badge-red',
  }[status] || 'badge-gray';
}

export function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function savePreferredCity(city) {
  localStorage.setItem('preferredCity', city);
}

export function getPreferredCity() {
  return localStorage.getItem('preferredCity') || '';
}
