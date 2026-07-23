const state = {
  token: localStorage.getItem('taskora_token'),
  profile: null,
  users: [],
  selectedUser: null,
  section: 'dashboard'
};

const $ = (selector) => document.querySelector(selector);
let toastTimer;

function escapeHtml(value = '') {
  return String(value).replace(/[&<>'"]/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[character]);
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  $('#toast').textContent = message;
  $('#toast').classList.add('show');
  toastTimer = window.setTimeout(() => $('#toast').classList.remove('show'), 3200);
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers || {}),
      Authorization: `Bearer ${state.token}`
    }
  });
  if (response.status === 204) return null;
  const type = response.headers.get('content-type') || '';
  const data = type.includes('json') ? await response.json().catch(() => ({})) : await response.blob();
  if (response.status === 401) {
    localStorage.removeItem('taskora_token');
    localStorage.removeItem('taskora_role');
    window.location.replace('/index.html');
    throw new Error('Tu sesión terminó.');
  }
  if (!response.ok) throw new Error(data.message || 'No fue posible completar la operación.');
  return data;
}

function formatDate(value) {
  if (!value) return 'Sin fecha';
  return new Date(value).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
}

function emptyState(icon, title, text) {
  return `<div class="empty"><span class="material-symbols-rounded">${icon}</span><strong>${title}</strong><p>${text}</p></div>`;
}

async function showSection(section) {
  state.section = section;
  document.querySelectorAll('[data-section]').forEach((button) => button.classList.toggle('active', button.dataset.section === section));
  const titles = { dashboard: 'Panel administrativo', users: 'Gestión de usuarios', reminders: 'Recordatorios fallidos', audit: 'Auditoría administrativa' };
  $('#page-title').textContent = titles[section];
  $('#content').innerHTML = '<div class="empty"><span class="material-symbols-rounded">progress_activity</span>Cargando información...</div>';
  try {
    if (section === 'dashboard') await loadDashboard();
    if (section === 'users') await loadUsers();
    if (section === 'reminders') await loadReminders();
    if (section === 'audit') await loadAudit();
  } catch (error) { showToast(error.message); }
}

async function loadDashboard() {
  const data = await api('/api/admin/dashboard');
  const cards = [
    ['Usuarios activos', data.activeUsers, ''],
    ['Usuarios suspendidos', data.suspendedUsers, 'coral'],
    ['Mascotas registradas', data.registeredPets, ''],
    ['Cuidadores activos', data.activeCaregivers, ''],
    ['Cuidados pendientes', data.pendingCareTasks, 'sand'],
    ['Cuidados vencidos', data.overdueCareTasks, 'coral'],
    ['Recordatorios fallidos', data.failedReminders, 'coral']
  ];
  $('#content').innerHTML = `
    <div class="stats-grid">${cards.map(([label, value, kind]) => `<article class="stat-card ${kind}"><span>${label}</span><strong>${value}</strong></article>`).join('')}</div>
    <section class="panel"><div class="panel-heading"><div><span class="eyebrow">ACCESOS RÁPIDOS</span><h2>Acciones del parcial</h2><p>Gestiona cuentas sin ver contraseñas ni tokens.</p></div></div><div class="detail-actions"><button class="primary-button" data-go="users">Ver usuarios</button><button class="secondary-button" data-go="reminders">Reintentar recordatorios</button><button class="secondary-button" data-go="audit">Consultar auditoría</button></div></section>`;
  document.querySelectorAll('[data-go]').forEach((button) => button.onclick = () => showSection(button.dataset.go));
}

async function loadUsers() {
  $('#content').innerHTML = `
    <section class="panel" style="margin-top:0"><div class="panel-heading"><div><span class="eyebrow">RF01 · CUENTAS</span><h2>Usuarios registrados</h2><p>Busca, filtra y abre el detalle completo.</p></div></div><div class="toolbar"><input id="user-search" type="search" placeholder="Buscar por nombre o correo"><select id="user-status"><option value="">Todos</option><option value="ACTIVE">Activos</option><option value="SUSPENDED">Suspendidos</option></select></div><div id="users-list" class="data-list" style="margin-top:1rem"></div></section>`;
  $('#user-search').oninput = refreshUsers;
  $('#user-status').onchange = refreshUsers;
  await refreshUsers();
}

async function refreshUsers() {
  const search = $('#user-search')?.value || '';
  const status = $('#user-status')?.value || '';
  const query = new URLSearchParams();
  if (search) query.set('search', search);
  if (status) query.set('status', status);
  state.users = await api(`/api/admin/users?${query}`);
  const list = $('#users-list');
  if (!state.users.length) { list.innerHTML = emptyState('person_search', 'Sin resultados', 'Prueba otra búsqueda o filtro.'); return; }
  list.innerHTML = state.users.map((user) => `<article class="data-row"><div><strong>${escapeHtml(user.fullName)}</strong><small>${escapeHtml(user.email)}</small></div><div><span class="status ${user.status === 'SUSPENDED' ? 'suspended' : ''}">${user.status === 'ACTIVE' ? 'Activo' : 'Suspendido'}</span></div><div><small>${user.role === 'ADMIN' ? 'Administrador único' : 'Usuario'}</small></div><button class="primary-button" data-open-user="${user.id}">Ver detalle</button></article>`).join('');
  list.querySelectorAll('[data-open-user]').forEach((button) => button.onclick = () => openUser(Number(button.dataset.openUser)));
}

async function openUser(id, tab = 'summary') {
  const detail = await api(`/api/admin/users/${id}`);
  state.selectedUser = detail;
  $('#modal-title').textContent = detail.user.fullName;
  $('#modal').classList.remove('hidden');
  const tabs = [
    ['summary', 'Resumen'], ['pets', 'Mascotas'], ['caregivers', 'Cuidadores'], ['routines', 'Rutinas'],
    ['cares', 'Cuidados'], ['channels', 'Canales'], ['history', 'Historial'], ['reports', 'Reportes'], ['settings', 'Configuración']
  ];
  $('#modal-content').innerHTML = `<div class="tabs">${tabs.map(([key, label]) => `<button class="tab ${key === tab ? 'active' : ''}" data-detail-tab="${key}">${label}</button>`).join('')}</div><section id="detail-section"></section>`;
  document.querySelectorAll('[data-detail-tab]').forEach((button) => button.onclick = () => {
    document.querySelectorAll('[data-detail-tab]').forEach((item) => item.classList.toggle('active', item === button));
    renderUserTab(button.dataset.detailTab);
  });
  renderUserTab(tab);
}

function renderUserTab(tab) {
  const detail = state.selectedUser;
  const user = detail.user;
  const target = $('#detail-section');
  if (tab === 'summary') {
    target.innerHTML = `<div class="detail-grid"><article class="detail-card"><strong>${escapeHtml(user.email)}</strong><small>Correo</small></article><article class="detail-card"><strong>${user.status === 'ACTIVE' ? 'Activa' : 'Suspendida'}</strong><small>Cuenta</small></article><article class="detail-card"><strong>${user.role}</strong><small>Rol global</small></article><article class="detail-card"><strong>${detail.pets.length}</strong><small>Mascotas</small></article><article class="detail-card"><strong>${detail.caregivers.length}</strong><small>Asignaciones de cuidadores</small></article><article class="detail-card"><strong>${formatDate(user.createdAt)}</strong><small>Fecha de registro</small></article></div>${user.suspensionReason ? `<p class="warning"><strong>Motivo de suspensión:</strong> ${escapeHtml(user.suspensionReason)}</p>` : ''}<div class="detail-actions"><button class="secondary-button" data-user-action="edit">Editar datos</button>${user.role !== 'ADMIN' && user.status === 'ACTIVE' ? '<button class="danger-button" data-user-action="suspend">Suspender</button>' : ''}${user.role !== 'ADMIN' && user.status === 'SUSPENDED' ? '<button class="primary-button" data-user-action="reactivate">Reactivar</button>' : ''}<button class="secondary-button" data-user-action="reset">Recuperar contraseña</button>${user.role !== 'ADMIN' ? '<button class="danger-button" data-user-action="delete">Eliminar definitivamente</button>' : ''}</div>`;
    target.querySelectorAll('[data-user-action]').forEach((button) => button.onclick = () => userAction(button.dataset.userAction));
  }
  if (tab === 'pets') renderPetsTab(target, detail);
  if (tab === 'caregivers') renderCaregiversTab(target, detail);
  if (tab === 'routines') renderRoutinesTab(target, detail);
  if (tab === 'cares') renderCaresTab(target, detail);
  if (tab === 'channels') renderChannelsTab(target, detail);
  if (tab === 'history') renderHistoryTab(target, detail);
  if (tab === 'reports') renderReportsTab(target, detail);
  if (tab === 'settings') target.innerHTML = `<div class="detail-grid"><article class="detail-card"><strong>${detail.configuration.telegramConnected ? 'Conectado' : 'Desconectado'}</strong><small>Telegram · token oculto</small></article><article class="detail-card"><strong>${detail.configuration.gmailConnected ? 'Conectado' : 'Desconectado'}</strong><small>Gmail · credenciales ocultas</small></article><article class="detail-card"><strong>${user.status === 'ACTIVE' ? 'Permitido' : 'Bloqueado'}</strong><small>Inicio de sesión y JWT</small></article></div>`;
}

function listCards(items, render, emptyText) {
  return items.length ? `<div class="data-list">${items.map(render).join('')}</div>` : emptyState('inbox', emptyText, 'Todavía no hay registros en esta sección.');
}

function renderPetsTab(target, detail) {
  target.innerHTML = `<div class="panel-heading"><div><h3>Mascotas del propietario</h3><p>Crear, editar o archivar.</p></div><button class="primary-button" data-add-pet>Agregar mascota</button></div>${listCards(detail.pets, (pet) => `<article class="data-row"><div><strong>${escapeHtml(pet.name)}</strong><small>${escapeHtml(pet.species)} · ${escapeHtml(pet.breed || 'Sin raza')} · ${escapeHtml(pet.color || 'Sin color')}</small></div><div><small>${escapeHtml(pet.notes || 'Sin observaciones')}</small></div><span class="status">Activa</span><div class="detail-actions"><button class="secondary-button" data-edit-pet="${pet.id}">Editar</button><button class="danger-button" data-delete-pet="${pet.id}">Archivar</button></div></article>`, 'Sin mascotas')}`;
  $('[data-add-pet]').onclick = addPet;
  target.querySelectorAll('[data-edit-pet]').forEach((button) => button.onclick = () => editPet(Number(button.dataset.editPet)));
  target.querySelectorAll('[data-delete-pet]').forEach((button) => button.onclick = () => deletePet(Number(button.dataset.deletePet)));
}

function renderCaregiversTab(target, detail) {
  target.innerHTML = `<div class="panel-heading"><div><h3>Asignaciones de cuidadores</h3><p>Una asignación siempre corresponde a una mascota concreta.</p></div><button class="primary-button" data-add-caregiver>Asignar cuidador</button></div>${listCards(detail.caregivers, (item) => `<article class="data-row"><div><strong>${escapeHtml(item.caregiverName)}</strong><small>${escapeHtml(item.caregiverEmail)}</small></div><div><small>${escapeHtml(item.petName)} · propietario ${escapeHtml(item.ownerName)}</small></div><span class="status">${item.permission}</span><div class="detail-actions"><button class="secondary-button" data-caregiver-permission="${item.id}">Cambiar permiso</button><button class="danger-button" data-delete-caregiver="${item.id}">Revocar</button></div></article>`, 'Sin cuidadores')}`;
  $('[data-add-caregiver]').onclick = addCaregiver;
  target.querySelectorAll('[data-caregiver-permission]').forEach((button) => button.onclick = () => editCaregiver(Number(button.dataset.caregiverPermission)));
  target.querySelectorAll('[data-delete-caregiver]').forEach((button) => button.onclick = () => deleteCaregiver(Number(button.dataset.deleteCaregiver)));
}

function renderRoutinesTab(target, detail) {
  target.innerHTML = `<div class="panel-heading"><div><h3>Rutinas recurrentes</h3><p>Programaciones del usuario.</p></div><button class="primary-button" data-add-series>Crear rutina</button></div>${listCards(detail.routines, (item) => `<article class="data-row"><div><strong>${escapeHtml(item.title)}</strong><small>${escapeHtml(item.petName)} · ${item.frequency}</small></div><div><small>${item.timesOfDay.join(', ')}</small></div><span class="status ${item.status === 'CANCELLED' ? 'suspended' : ''}">${item.status}</span><div class="detail-actions"><button class="secondary-button" data-edit-series="${item.id}">Editar</button><button class="danger-button" data-delete-series="${item.id}">Cancelar</button></div></article>`, 'Sin rutinas')}`;
  $('[data-add-series]').onclick = addSeries;
  target.querySelectorAll('[data-edit-series]').forEach((button) => button.onclick = () => editSeries(Number(button.dataset.editSeries)));
  target.querySelectorAll('[data-delete-series]').forEach((button) => button.onclick = () => deleteSeries(Number(button.dataset.deleteSeries)));
}

function renderCaresTab(target, detail) {
  target.innerHTML = `<div class="panel-heading"><div><h3>Cuidados y tareas</h3><p>Incluye pendientes, realizados y vencidos.</p></div><button class="primary-button" data-add-care>Crear cuidado</button></div>${listCards(detail.cares, (item) => `<article class="data-row"><div><strong>${escapeHtml(item.title)}</strong><small>${escapeHtml(item.petName)} · ${formatDate(item.scheduledAt)}</small></div><div><small>${escapeHtml(item.careType)}${item.warning ? ` · ${escapeHtml(item.warning)}` : ''}</small></div><span class="status ${item.overdue || item.status === 'SKIPPED' ? 'suspended' : ''}">${escapeHtml(item.displayStatus)}</span><div class="detail-actions">${item.status === 'PENDING' ? `<button class="secondary-button" data-complete-care="${item.id}">Realizado</button><button class="danger-button" data-skip-care="${item.id}">No realizado</button>` : ''}<button class="danger-button" data-delete-care="${item.id}">Cancelar</button></div></article>`, 'Sin cuidados')}`;
  $('[data-add-care]').onclick = addCare;
  target.querySelectorAll('[data-complete-care]').forEach((button) => button.onclick = () => updateCare(Number(button.dataset.completeCare), 'COMPLETED'));
  target.querySelectorAll('[data-skip-care]').forEach((button) => button.onclick = () => updateCare(Number(button.dataset.skipCare), 'SKIPPED'));
  target.querySelectorAll('[data-delete-care]').forEach((button) => button.onclick = () => deleteCare(Number(button.dataset.deleteCare)));
}

function renderChannelsTab(target, detail) {
  target.innerHTML = `<h3>Canales conectados</h3><p class="muted">Solo se muestra el estado. Los tokens y secretos nunca se exponen.</p>${listCards(detail.channels, (item) => `<article class="data-row"><div><strong>${item.type}</strong><small>${escapeHtml(item.destination)}</small></div><div><small>${item.verified ? 'Verificado' : 'Sin verificar'}</small></div><span class="status ${!item.active ? 'suspended' : ''}">${item.active ? 'Conectado' : 'Desconectado'}</span><button class="danger-button" data-unlink-channel="${item.id}">Desvincular</button></article>`, 'Sin canales')}`;
  target.querySelectorAll('[data-unlink-channel]').forEach((button) => button.onclick = () => unlinkChannel(Number(button.dataset.unlinkChannel)));
}

function renderHistoryTab(target, detail) {
  target.innerHTML = `<h3>Historial de acciones de cuidado</h3>${listCards(detail.history, (item) => `<article class="detail-card"><strong>${escapeHtml(item.taskTitle)} · ${escapeHtml(item.action)}</strong><small>${escapeHtml(item.actorName)} · ${formatDate(item.createdAt)}${item.detail ? ` · ${escapeHtml(item.detail)}` : ''}</small></article>`, 'Sin historial')}`;
}

function renderReportsTab(target, detail) {
  const report = detail.report;
  target.innerHTML = `<div class="detail-grid"><article class="detail-card"><strong>${report.total}</strong><small>Total</small></article><article class="detail-card"><strong>${report.completed}</strong><small>Realizados</small></article><article class="detail-card"><strong>${report.skipped}</strong><small>No realizados</small></article><article class="detail-card"><strong>${report.overdue}</strong><small>Vencidos</small></article><article class="detail-card"><strong>${report.complianceRate}%</strong><small>Cumplimiento</small></article></div><div class="detail-actions"><button class="primary-button" data-export-report>Exportar CSV</button></div>`;
  $('[data-export-report]').onclick = exportReport;
}

async function userAction(action) {
  const user = state.selectedUser.user;
  try {
    if (action === 'edit') {
      const fullName = prompt('Nombre completo', user.fullName); if (fullName === null) return;
      const email = prompt('Correo electrónico', user.email); if (email === null) return;
      await api(`/api/admin/users/${user.id}`, { method: 'PUT', body: JSON.stringify({ fullName, email, phoneNumber: user.phoneNumber || '' }) });
    }
    if (action === 'suspend') {
      const reason = prompt('Motivo obligatorio de la suspensión'); if (!reason) return;
      await api(`/api/admin/users/${user.id}/suspend`, { method: 'POST', body: JSON.stringify({ reason }) });
    }
    if (action === 'reactivate') await api(`/api/admin/users/${user.id}/reactivate`, { method: 'POST' });
    if (action === 'reset') {
      if (!confirm(`¿Iniciar recuperación de contraseña para ${user.email}?`)) return;
      await api(`/api/admin/users/${user.id}/password-reset`, { method: 'POST' });
    }
    if (action === 'delete') {
      const confirmationEmail = prompt(`Escribe exactamente ${user.email} para eliminar todos sus datos.`); if (confirmationEmail === null) return;
      await api(`/api/admin/users/${user.id}`, { method: 'DELETE', body: JSON.stringify({ confirmationEmail }) });
      closeModal(); await refreshUsers(); showToast('Usuario eliminado definitivamente.'); return;
    }
    await refreshSelected('summary'); showToast('Acción administrativa guardada.');
  } catch (error) { showToast(error.message); }
}

async function addPet() {
  const name = prompt('Nombre de la mascota'); if (!name) return;
  const species = prompt('Especie'); if (!species) return;
  const breed = prompt('Raza'); if (breed === null) return;
  const color = prompt('Color'); if (color === null) return;
  try { await api(`/api/admin/users/${state.selectedUser.user.id}/pets`, { method: 'POST', body: JSON.stringify({ name, species, breed, color }) }); await refreshSelected('pets'); showToast('Mascota creada.'); } catch (error) { showToast(error.message); }
}

async function editPet(id) {
  const pet = state.selectedUser.pets.find((item) => item.id === id);
  const name = prompt('Nombre', pet.name); if (!name) return;
  const species = prompt('Especie', pet.species); if (!species) return;
  const breed = prompt('Raza', pet.breed || ''); if (breed === null) return;
  const color = prompt('Color', pet.color || ''); if (color === null) return;
  try { await api(`/api/admin/pets/${id}`, { method: 'PUT', body: JSON.stringify({ name, species, breed, color, notes: pet.notes || '' }) }); await refreshSelected('pets'); showToast('Mascota actualizada.'); } catch (error) { showToast(error.message); }
}

async function deletePet(id) { if (!confirm('¿Archivar esta mascota?')) return; try { await api(`/api/admin/pets/${id}`, { method: 'DELETE' }); await refreshSelected('pets'); showToast('Mascota archivada.'); } catch (error) { showToast(error.message); } }

async function addCaregiver() {
  if (!state.selectedUser.pets.length) return showToast('El propietario debe tener una mascota.');
  const petId = Number(prompt(`ID de mascota: ${state.selectedUser.pets.map((pet) => `${pet.id}=${pet.name}`).join(', ')}`)); if (!petId) return;
  const caregiverEmail = prompt('Correo del cuidador registrado'); if (!caregiverEmail) return;
  const permission = (prompt('Permiso: VIEWER o EDITOR', 'EDITOR') || '').toUpperCase(); if (!permission) return;
  try { await api(`/api/admin/users/${state.selectedUser.user.id}/caregivers`, { method: 'POST', body: JSON.stringify({ petId, caregiverEmail, permission }) }); await refreshSelected('caregivers'); showToast('Cuidador asignado.'); } catch (error) { showToast(error.message); }
}

async function editCaregiver(id) { const permission = (prompt('Nuevo permiso: VIEWER o EDITOR', 'EDITOR') || '').toUpperCase(); if (!permission) return; try { await api(`/api/admin/caregivers/${id}`, { method: 'PUT', body: JSON.stringify({ permission }) }); await refreshSelected('caregivers'); showToast('Permiso actualizado.'); } catch (error) { showToast(error.message); } }
async function deleteCaregiver(id) { if (!confirm('¿Revocar esta asignación?')) return; try { await api(`/api/admin/caregivers/${id}`, { method: 'DELETE' }); await refreshSelected('caregivers'); showToast('Asignación revocada.'); } catch (error) { showToast(error.message); } }

async function addSeries() {
  if (!state.selectedUser.pets.length) return showToast('El propietario debe tener una mascota.');
  const petId = Number(prompt(`ID de mascota: ${state.selectedUser.pets.map((pet) => `${pet.id}=${pet.name}`).join(', ')}`)); if (!petId) return;
  const title = prompt('Nombre de la rutina'); if (!title) return;
  const careType = prompt('Tipo de cuidado', 'Alimentación'); if (!careType) return;
  const time = prompt('Hora diaria, formato HH:mm', '08:00'); if (!time) return;
  const startDate = prompt('Fecha inicial, formato AAAA-MM-DD', new Date().toISOString().slice(0, 10)); if (!startDate) return;
  try { await api(`/api/admin/users/${state.selectedUser.user.id}/series`, { method: 'POST', body: JSON.stringify({ petId, title, careType, frequency: 'DAILY', timesOfDay: [time], startDate }) }); await refreshSelected('routines'); showToast('Rutina creada.'); } catch (error) { showToast(error.message); }
}

async function editSeries(id) { const item = state.selectedUser.routines.find((series) => series.id === id); const title = prompt('Nuevo nombre', item.title); if (!title) return; try { await api(`/api/admin/series/${id}`, { method: 'PUT', body: JSON.stringify({ title }) }); await refreshSelected('routines'); showToast('Rutina actualizada.'); } catch (error) { showToast(error.message); } }
async function deleteSeries(id) { if (!confirm('¿Cancelar esta rutina?')) return; try { await api(`/api/admin/series/${id}`, { method: 'DELETE' }); await refreshSelected('routines'); showToast('Rutina cancelada.'); } catch (error) { showToast(error.message); } }

async function addCare() {
  if (!state.selectedUser.pets.length) return showToast('El propietario debe tener una mascota.');
  const petId = Number(prompt(`ID de mascota: ${state.selectedUser.pets.map((pet) => `${pet.id}=${pet.name}`).join(', ')}`)); if (!petId) return;
  const title = prompt('Nombre del cuidado'); if (!title) return;
  const careType = prompt('Tipo', 'Alimentación'); if (!careType) return;
  const scheduledAt = prompt('Fecha y hora, formato AAAA-MM-DDTHH:mm'); if (!scheduledAt) return;
  try { await api(`/api/admin/users/${state.selectedUser.user.id}/care-tasks`, { method: 'POST', body: JSON.stringify({ petId, title, careType, scheduledAt }) }); await refreshSelected('cares'); showToast('Cuidado creado.'); } catch (error) { showToast(error.message); }
}

async function updateCare(id, status) { const reason = status === 'SKIPPED' ? prompt('Motivo obligatorio') : null; if (status === 'SKIPPED' && !reason) return; try { await api(`/api/admin/care-tasks/${id}`, { method: 'PUT', body: JSON.stringify({ status, reason }) }); await refreshSelected('cares'); showToast('Estado actualizado.'); } catch (error) { showToast(error.message); } }
async function deleteCare(id) { if (!confirm('¿Cancelar este cuidado?')) return; try { await api(`/api/admin/care-tasks/${id}`, { method: 'DELETE' }); await refreshSelected('cares'); showToast('Cuidado cancelado.'); } catch (error) { showToast(error.message); } }
async function unlinkChannel(id) { if (!confirm('¿Desvincular este canal?')) return; try { await api(`/api/admin/channels/${id}`, { method: 'DELETE' }); await refreshSelected('channels'); showToast('Canal desvinculado.'); } catch (error) { showToast(error.message); } }

async function exportReport() {
  try {
    const blob = await api(`/api/admin/users/${state.selectedUser.user.id}/report/export`);
    const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = 'taskora-pet-reporte.csv'; link.click(); URL.revokeObjectURL(url);
  } catch (error) { showToast(error.message); }
}

async function refreshSelected(tab) { await openUser(state.selectedUser.user.id, tab); }

async function loadReminders() {
  const items = await api('/api/admin/reminders/failed');
  $('#content').innerHTML = `<section class="panel" style="margin-top:0"><div class="panel-heading"><div><span class="eyebrow">RF08</span><h2>Envíos con error</h2><p>Reintenta únicamente canales que siguen conectados.</p></div></div>${listCards(items, (item) => `<article class="data-row"><div><strong>${escapeHtml(item.taskTitle)}</strong><small>${escapeHtml(item.petName)} · ${escapeHtml(item.userEmail)}</small></div><div><small>${item.channelType} · ${formatDate(item.scheduledFor)}</small></div><span class="status failed">Fallido</span><button class="primary-button" data-retry="${item.id}">Reintentar</button></article>`, 'Sin recordatorios fallidos')}</section>`;
  document.querySelectorAll('[data-retry]').forEach((button) => button.onclick = async () => { try { await api(`/api/admin/reminders/${button.dataset.retry}/retry`, { method: 'POST' }); await loadReminders(); showToast('Recordatorio enviado.'); } catch (error) { showToast(error.message); } });
}

async function loadAudit() {
  const items = await api('/api/admin/audit');
  $('#content').innerHTML = `<section class="panel" style="margin-top:0"><div class="panel-heading"><div><span class="eyebrow">TRAZABILIDAD</span><h2>Acciones administrativas</h2><p>Registro de cambios sensibles.</p></div></div>${listCards(items, (item) => `<article class="data-row"><div><strong>${escapeHtml(item.action)}</strong><small>${escapeHtml(item.resourceType)} · ${escapeHtml(item.resourceIdentifier)}</small></div><div><small>${escapeHtml(item.description)}</small></div><div><small>${escapeHtml(item.adminEmail)}</small></div><small>${formatDate(item.createdAt)}</small></article>`, 'Sin acciones administrativas')}</section>`;
}

function closeModal() { $('#modal').classList.add('hidden'); state.selectedUser = null; }

document.addEventListener('DOMContentLoaded', async () => {
  if (!state.token) return window.location.replace('/index.html');
  try {
    state.profile = await api('/users/me');
    if (state.profile.role !== 'ADMIN') return window.location.replace('/dashboard.html');
    $('#admin-name').textContent = state.profile.fullName;
    document.querySelectorAll('[data-section]').forEach((button) => button.onclick = () => showSection(button.dataset.section));
    $('#logout-button').onclick = () => { localStorage.removeItem('taskora_token'); localStorage.removeItem('taskora_role'); window.location.replace('/index.html'); };
    $('#close-modal').onclick = closeModal;
    $('#modal').onclick = (event) => { if (event.target === $('#modal')) closeModal(); };
    await showSection('dashboard');
  } catch (error) { showToast(error.message); }
});
