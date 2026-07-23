const page = document.body.dataset.page;
const state = {
  token: localStorage.getItem('taskora_token'),
  profile: null,
  pets: [],
  tasks: [],
  channels: [],
  caregivers: [],
  series: [],
  report: null,
  filters: { petId: '', status: '', careType: '', from: '', to: '', completedById: '' }
};
const $ = (selector) => document.querySelector(selector);
let toastTimer;
let lastFocusedElement;

const commonCareTypes = [
  'Alimentación', 'Agua', 'Higiene', 'Cita', 'Tratamiento',
  'Medicación', 'Control de peso', 'Limpieza del espacio'
];
const careTypesBySpecies = {
  perro: [
    'Paseo', 'Juego', 'Entrenamiento', 'Cepillado', 'Baño', 'Corte de uñas',
    'Limpieza dental', 'Desparasitación', 'Vacunación'
  ],
  gato: [
    'Limpieza de arenero', 'Juego interactivo', 'Cepillado', 'Corte de uñas',
    'Limpieza dental', 'Desparasitación', 'Vacunación'
  ],
  conejo: [
    'Limpieza de jaula', 'Cambio de heno', 'Revisión dental', 'Cepillado',
    'Corte de uñas', 'Tiempo fuera de jaula'
  ],
  ave: [
    'Limpieza de jaula', 'Cambio de semillas', 'Baño para aves',
    'Revisión de pico y uñas', 'Tiempo de vuelo'
  ],
  pez: [
    'Limpieza de acuario', 'Cambio parcial de agua', 'Revisar filtro',
    'Medir temperatura', 'Medir pH', 'Revisar iluminación'
  ],
  hamster: [
    'Limpieza de hábitat', 'Cambio de sustrato', 'Revisar rueda',
    'Revisión dental', 'Juego y enriquecimiento'
  ],
  cobaya: [
    'Limpieza de jaula', 'Cambio de heno', 'Vitamina C', 'Cepillado',
    'Corte de uñas', 'Revisión dental'
  ],
  tortuga: [
    'Limpieza de terrario', 'Revisar lámpara UVB', 'Medir temperatura',
    'Revisión de caparazón', 'Baño de hidratación'
  ],
  huron: [
    'Limpieza de arenero', 'Juego y enriquecimiento', 'Cepillado',
    'Corte de uñas', 'Baño'
  ],
  caballo: [
    'Cepillado', 'Limpieza de cascos', 'Ejercicio', 'Monta',
    'Revisión de herraduras', 'Limpieza de establo'
  ],
  'cerdo miniatura': [
    'Paseo', 'Juego y enriquecimiento', 'Limpieza de pezuñas',
    'Cuidado de la piel', 'Baño'
  ],
  reptil: [
    'Limpieza de terrario', 'Medir temperatura', 'Medir humedad',
    'Revisar lámpara UVB', 'Revisión de muda'
  ],
  otro: ['Juego', 'Ejercicio', 'Cepillado', 'Limpieza del hábitat']
};
const careTypes = [...new Set([...commonCareTypes, ...Object.values(careTypesBySpecies).flat()])].sort();
const careTitleDefaults = {
  Alimentación: 'Dar comida',
  Agua: 'Cambiar agua',
  Higiene: 'Realizar higiene',
  Cita: 'Asistir a cita veterinaria',
  Tratamiento: 'Dar tratamiento',
  Medicación: 'Dar medicamento',
  Paseo: 'Sacar a pasear',
  Juego: 'Jugar con la mascota',
  Entrenamiento: 'Realizar entrenamiento',
  Cepillado: 'Cepillar a la mascota',
  Baño: 'Bañar a la mascota',
  'Corte de uñas': 'Cortar las uñas',
  'Limpieza dental': 'Realizar limpieza dental',
  'Limpieza de arenero': 'Limpiar el arenero',
  'Limpieza de jaula': 'Limpiar la jaula',
  'Limpieza de acuario': 'Limpiar el acuario',
  'Cambio parcial de agua': 'Cambiar parte del agua',
  'Limpieza de terrario': 'Limpiar el terrario',
  'Limpieza de cascos': 'Limpiar los cascos',
  'Medir temperatura': 'Revisar la temperatura',
  'Medir humedad': 'Revisar la humedad',
  'Medir pH': 'Medir el pH del agua',
  'Revisar lámpara UVB': 'Revisar la lámpara UVB'
};
const petSpecies = [
  'Perro', 'Gato', 'Conejo', 'Ave', 'Pez', 'Hámster', 'Cobaya',
  'Tortuga', 'Hurón', 'Caballo', 'Cerdo miniatura', 'Reptil', 'Otro'
];
const petBreeds = {
  perro: [
    'Akita Inu', 'Alaskan Malamute', 'American Bully', 'Australian Shepherd',
    'Basenji', 'Basset Hound', 'Beagle', 'Bichón Frisé', 'Border Collie',
    'Boston Terrier', 'Boxer', 'Boyero de Berna', 'Bull Terrier',
    'Bulldog Francés', 'Bulldog Inglés', 'Cane Corso', 'Caniche',
    'Cavalier King Charles Spaniel', 'Chihuahua', 'Chow Chow', 'Cocker Spaniel',
    'Criollo', 'Dachshund', 'Dálmata', 'Doberman', 'Fox Terrier', 'Galgo',
    'Golden Retriever', 'Gran Danés', 'Husky Siberiano', 'Jack Russell Terrier',
    'Labrador Retriever', 'Maltés', 'Mastín Napolitano', 'Mestizo', 'Papillón',
    'Pastor Alemán', 'Pastor Belga Malinois', 'Pequinés', 'Pinscher', 'Pitbull',
    'Pointer', 'Pomerania', 'Pug', 'Rottweiler', 'Samoyedo', 'San Bernardo',
    'Schnauzer', 'Shar Pei', 'Shetland Sheepdog', 'Shih Tzu',
    'Staffordshire Bull Terrier', 'Vizsla', 'Weimaraner', 'West Highland Terrier',
    'Xoloitzcuintle', 'Yorkshire Terrier', 'Sin raza definida'
  ],
  gato: [
    'Abisinio', 'American Shorthair', 'Angora Turco', 'Azul Ruso', 'Balinés',
    'Bengalí', 'Bobtail Japonés', 'Bombay', 'Bosque de Noruega',
    'British Shorthair', 'Burmés', 'Carey', 'Cornish Rex', 'Criollo',
    'Devon Rex', 'Doméstico de pelo corto', 'Doméstico de pelo largo',
    'Europeo Común', 'Exótico de Pelo Corto', 'Himalayo', 'Maine Coon',
    'Manx', 'Mestizo', 'Munchkin', 'Oriental de pelo corto', 'Persa',
    'Ragdoll', 'Savannah', 'Scottish Fold', 'Siamés', 'Siberiano',
    'Singapura', 'Somalí', 'Sphynx', 'Tabby', 'Toyger', 'Van Turco',
    'Sin raza definida'
  ],
  conejo: [
    'Angora', 'Belier', 'Cabeza de León', 'Californiano', 'Chinchilla',
    'Enano Holandés', 'Gigante de Flandes', 'Holland Lop', 'Mini Rex', 'Rex'
  ],
  ave: [
    'Agapornis', 'Bengalí', 'Cacatúa', 'Canario', 'Diamante Mandarín',
    'Gallina', 'Guacamaya', 'Loro Amazónico', 'Ninfa', 'Paloma',
    'Perico Australiano', 'Periquito', 'Tucán'
  ],
  pez: [
    'Betta', 'Carpa Koi', 'Cíclido', 'Disco', 'Guppy', 'Molly',
    'Neón Tetra', 'Óscar', 'Pez Ángel', 'Pez Dorado', 'Platy', 'Tetra'
  ],
  hamster: [
    'Campbell', 'Chino', 'Roborowski', 'Ruso', 'Sirio'
  ],
  cobaya: [
    'Abisinia', 'Americana', 'Coronet', 'Peruana', 'Rex', 'Sheltie', 'Skinny'
  ],
  tortuga: [
    'De Caja', 'De Orejas Rojas', 'Mediterránea', 'Mora', 'Rusa', 'Sulcata'
  ],
  huron: [
    'Albino', 'Angora', 'Canela', 'Sable'
  ],
  caballo: [
    'Árabe', 'Appaloosa', 'Cuarto de Milla', 'Frisón', 'Islandés',
    'Paso Fino Colombiano', 'Percherón', 'Pura Sangre', 'Trote y Galope'
  ],
  'cerdo miniatura': [
    'Juliana', 'Kunekune', 'Mini Pig', 'Vietnamita'
  ],
  reptil: [
    'Camaleón', 'Dragón Barbudo', 'Gecko Crestado', 'Gecko Leopardo',
    'Iguana Verde', 'Pitón Bola', 'Serpiente de Maíz'
  ],
  otro: ['Criollo', 'Mestizo', 'Sin raza definida']
};
const petColors = [
  'Amarillo', 'Atigrado', 'Azul', 'Beige', 'Blanco', 'Café', 'Canela',
  'Carey', 'Crema', 'Dorado', 'Gris', 'Marrón', 'Naranja', 'Negro',
  'Rojo', 'Tricolor', 'Verde', 'Blanco y café', 'Blanco y gris',
  'Blanco y negro', 'Negro y café', 'Otro'
];
const weekdays = [
  ['MONDAY', 'Lun'], ['TUESDAY', 'Mar'], ['WEDNESDAY', 'Mié'], ['THURSDAY', 'Jue'],
  ['FRIDAY', 'Vie'], ['SATURDAY', 'Sáb'], ['SUNDAY', 'Dom']
];

const navigation = [
  ['dashboard', '/dashboard.html', 'space_dashboard', 'Inicio'],
  ['agenda', '/agenda.html', 'calendar_month', 'Agenda'],
  ['pets', '/pets.html', 'pets', 'Mascotas'],
  ['caregivers', '/caregivers.html', 'group', 'Cuidadores'],
  ['recurrences', '/recurrences.html', 'repeat', 'Recurrencias'],
  ['notifications', '/notifications.html', 'notifications', 'Canales'],
  ['history', '/history.html', 'history', 'Historial'],
  ['reports', '/reports.html', 'assessment', 'Reportes'],
  ['profile', '/profile.html', 'person', 'Mi perfil']
];

function escapeHtml(value = '') {
  return String(value).replace(/[&<>'"]/g, (character) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;'
  })[character]);
}

function showToast(message) {
  const toast = $('#toast');
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add('show');
  toastTimer = window.setTimeout(() => toast.classList.remove('show'), 3200);
}

async function api(path, options = {}) {
  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
    Authorization: `Bearer ${state.token}`
  };
  const response = await fetch(path, { ...options, headers });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => ({}));
  if (response.status === 401) {
    localStorage.removeItem('taskora_token');
    localStorage.removeItem('taskora_role');
    window.location.replace('/index.html');
    throw new Error('Tu sesión expiró. Inicia sesión nuevamente.');
  }
  if (!response.ok) throw new Error(data.message || 'No fue posible completar la operación.');
  return data;
}

function formData(form) { return Object.fromEntries(new FormData(form).entries()); }
function simpleText(value = '') {
  return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}
function careTypesForPet(pet) {
  const species = simpleText(pet?.species || '');
  const match = Object.keys(careTypesBySpecies).find((item) => species.includes(simpleText(item)));
  const specificCareTypes = match ? careTypesBySpecies[match] : careTypesBySpecies.otro;
  return [...new Set([...commonCareTypes, ...specificCareTypes])];
}
function updateCareTypeOptions(petSelect, careSelect, selectedCareType = '') {
  const pet = state.pets.find((item) => item.id == petSelect.value);
  const options = careTypesForPet(pet);
  if (selectedCareType && !options.includes(selectedCareType)) options.unshift(selectedCareType);
  careSelect.innerHTML = options
    .map((type) => `<option value="${escapeHtml(type)}">${escapeHtml(type)}</option>`).join('');
  careSelect.value = selectedCareType && options.includes(selectedCareType) ? selectedCareType : options[0];
}
function breedsForSpecies(species = '') {
  const key = simpleText(species);
  if (!key) return [];
  const match = Object.keys(petBreeds).find((item) => key.includes(simpleText(item)));
  return match ? petBreeds[match] : [...new Set(Object.values(petBreeds).flat())].sort();
}
function updateBreedOptions(speciesInput, breedSelect, selectedBreed = '') {
  const breeds = breedsForSpecies(speciesInput.value);
  if (selectedBreed && !breeds.includes(selectedBreed)) breeds.unshift(selectedBreed);
  breedSelect.innerHTML = '<option value="">Selecciona una raza</option>' +
    breeds.map((breed) => `<option value="${escapeHtml(breed)}">${escapeHtml(breed)}</option>`).join('');
  breedSelect.value = selectedBreed;
}
async function copyToClipboard(value) {
  try {
    await navigator.clipboard.writeText(value);
  } catch (_) {
    const input = document.createElement('textarea');
    input.value = value; input.style.position = 'fixed'; input.style.opacity = '0';
    document.body.appendChild(input); input.select(); document.execCommand('copy'); input.remove();
  }
}
function formatDate(value) { return new Date(value).toLocaleString('es-CO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }); }
function hydrateProtectedImages(root = document) {
  root.querySelectorAll('img[data-auth-image]').forEach(async (image) => {
    const url = image.dataset.authImage;
    image.removeAttribute('data-auth-image');
    try {
      const response = await fetch(url, { headers: { Authorization: `Bearer ${state.token}` } });
      if (!response.ok) throw new Error('No fue posible cargar la imagen.');
      const objectUrl = URL.createObjectURL(await response.blob());
      image.addEventListener('load', () => URL.revokeObjectURL(objectUrl), { once: true });
      image.addEventListener('error', () => URL.revokeObjectURL(objectUrl), { once: true });
      image.src = objectUrl;
    } catch (_) {
      image.replaceWith(document.createTextNode('🐾'));
    }
  });
}
function petEmoji(species = '') {
  const value = species.toLocaleLowerCase('es');
  if (value.includes('gato')) return '🐱';
  if (value.includes('ave') || value.includes('pájaro')) return '🦜';
  if (value.includes('pez')) return '🐠';
  if (value.includes('conejo')) return '🐰';
  return '🐶';
}
function recurrenceLabel(task) {
  if (task.seriesId) return 'Serie';
  if (task.recurrence === 'DAILY') return 'Diaria';
  if (task.recurrence === 'WEEKLY') return 'Semanal';
  if (task.recurrence === 'INTERVAL') return `Cada ${task.recurrenceIntervalDays || 1} días`;
  return 'Una vez';
}
function emptyState(icon, title, text) { return `<div class="empty-state"><span class="material-symbols-rounded" aria-hidden="true">${icon}</span><strong>${title}</strong><p>${text}</p></div>`; }

function filterQuery() {
  const params = new URLSearchParams();
  Object.entries(state.filters).forEach(([key, value]) => { if (value) params.set(key, value); });
  const query = params.toString();
  return query ? `?${query}` : '';
}

function renderShell() {
  const links = navigation.map(([key, href, icon, label]) => `<a class="nav-item ${page === key ? 'active' : ''}" href="${href}" ${page === key ? 'aria-current="page"' : ''}><span class="material-symbols-rounded" aria-hidden="true">${icon}</span>${label}</a>`).join('');
  $('#app-root').innerHTML = `
    <section class="app-shell">
      <aside class="sidebar">
        <a class="brand-lockup brand-link" href="/dashboard.html"><span class="brand-symbol material-symbols-rounded" aria-hidden="true">pets</span><span>Taskora <small>PET CARE</small></span></a>
        <nav class="side-nav" aria-label="Navegación principal">${links}</nav>
        <button id="logout-btn" class="logout-btn"><span class="material-symbols-rounded" aria-hidden="true">logout</span>Cerrar sesión</button>
      </aside>
      <div class="app-main">
        <header class="mobile-topbar"><a class="brand-lockup compact brand-link" href="/dashboard.html"><span class="brand-symbol material-symbols-rounded" aria-hidden="true">pets</span><span>Taskora</span></a><button id="mobile-logout-btn" class="icon-btn quiet" aria-label="Cerrar sesión"><span class="material-symbols-rounded" aria-hidden="true">logout</span></button></header>
        <nav class="mobile-nav" aria-label="Navegación móvil">${links}</nav>
        <main class="page-content" id="page-content">${pageTemplate()}</main>
      </div>
    </section>`;
  $('#logout-btn').onclick = logout;
  $('#mobile-logout-btn').onclick = logout;
}

function pageTitle(eyebrow, title, description, action = '') {
  return `<section class="welcome-row page-heading"><div><span class="eyebrow">${eyebrow}</span><h1>${title}</h1><p class="muted">${description}</p></div>${action ? `<div class="welcome-actions">${action}</div>` : ''}</section>`;
}

function pageTemplate() {
  const templates = {
    dashboard: () => `${pageTitle('PANEL PRINCIPAL', 'Hola, <span id="welcome-name">qué alegría verte</span>.', '<span id="date-label">Tu resumen está listo.</span>', '<button id="refresh-btn" class="icon-btn quiet" aria-label="Actualizar información"><span class="material-symbols-rounded">refresh</span></button>')}<section class="overview-grid" aria-label="Resumen"><article class="next-care-card"><div class="next-care-top"><span class="eyebrow eyebrow-light">SIGUIENTE CUIDADO</span><span class="live-dot">Próximo</span></div><div class="next-care-content"><div class="next-care-icon"><span class="material-symbols-rounded">event_upcoming</span></div><div><span id="next-care-pet" class="next-care-kicker">Tu agenda</span><h2 id="next-care-title">Todo al día</h2><p id="next-care-meta">No tienes cuidados pendientes.</p></div></div><a class="light-btn link-btn" href="/agenda.html">Abrir agenda<span class="material-symbols-rounded">arrow_forward</span></a></article><div class="stats-grid"><article class="stat-card"><div class="stat-icon sage"><span class="material-symbols-rounded">pets</span></div><div><span>Mascotas activas</span><strong id="stat-pets">0</strong><small>perfiles registrados</small></div></article><article class="stat-card"><div class="stat-icon coral"><span class="material-symbols-rounded">pending_actions</span></div><div><span>Cuidados pendientes</span><strong id="stat-pending">0</strong><small>por completar</small></div></article><article class="stat-card full-stat"><div class="stat-icon sand"><span class="material-symbols-rounded">check_circle</span></div><div><span>Cuidados completados</span><strong id="stat-completed">0</strong><small>en el historial</small></div></article></div></section><section class="module-grid"><a class="module-card" href="/pets.html"><span class="material-symbols-rounded">pets</span><strong>Mascotas</strong><small>Perfiles y fotografías</small></a><a class="module-card" href="/agenda.html"><span class="material-symbols-rounded">calendar_month</span><strong>Agenda</strong><small>Tareas y evidencias</small></a><a class="module-card" href="/caregivers.html"><span class="material-symbols-rounded">group</span><strong>Cuidadores</strong><small>Accesos compartidos</small></a><a class="module-card" href="/notifications.html"><span class="material-symbols-rounded">notifications</span><strong>Canales</strong><small>Recordatorios por Telegram</small></a></section>`,
    agenda: () => `${pageTitle('CUIDADOS PROGRAMADOS', 'Agenda', 'Crea, modifica y completa las tareas de cuidado.', '<button id="new-task-btn" class="primary-btn"><span class="material-symbols-rounded">add</span>Nuevo cuidado</button>')}<section id="filter-bar" class="filter-bar" aria-label="Filtros"></section><section class="panel"><div class="task-table-header"><span>Fecha</span><span>Mascota y cuidado</span><span>Estado</span><span>Acciones</span></div><div id="tasks-list" class="task-list"></div></section>`,
    pets: () => `${pageTitle('TU FAMILIA', 'Mascotas', 'Cada mascota tiene su propio perfil, fotografía y observaciones.', '<button id="new-pet-btn" class="primary-btn"><span class="material-symbols-rounded">add</span>Nueva mascota</button>')}<section class="panel"><div id="pets-list" class="pets-grid"></div></section>`,
    caregivers: () => `${pageTitle('ACCESO COMPARTIDO', 'Cuidadores', 'Asigna una persona registrada a una mascota, elige su permiso y contrólalo cuando quieras.', '<button id="new-caregiver-btn" class="primary-btn"><span class="material-symbols-rounded">person_add</span>Asignar cuidador</button>')}<section class="panel"><div id="caregivers-list" class="channels-list"></div></section>`,
    recurrences: () => `${pageTitle('RUTINAS REPETIDAS', 'Series recurrentes', 'Crea series diarias, semanales o por intervalo, con varios horarios por día. Pausa, reanuda o edita la serie completa.', '<button id="new-series-btn" class="primary-btn"><span class="material-symbols-rounded">add</span>Nueva serie</button>')}<section class="panel"><div id="series-list" class="task-list"></div></section>`,
    notifications: () => `${pageTitle('SIEMPRE A TIEMPO', 'Canales de recordatorio', 'Recibe cada cuidado por Telegram y Gmail con la anticipación que elijas.', '<button id="connect-gmail-btn" class="secondary-btn"><span class="material-symbols-rounded">mail</span>Conectar Gmail</button><button id="connect-telegram-btn" class="primary-btn"><span class="material-symbols-rounded">send</span>Vincular Telegram</button>')}<section class="telegram-info"><span class="material-symbols-rounded" aria-hidden="true">verified_user</span><div><strong>Conexiones seguras</strong><p>Telegram usa un código temporal. Gmail usa OAuth de Google y Taskora Pet nunca conoce tu contraseña.</p></div></section><section class="panel"><div id="channels-list" class="channels-list"></div></section>`,
    history: () => `${pageTitle('TRAZABILIDAD', 'Historial de cuidados', 'Consulta actividades completadas u omitidas, quién las realizó y sus fotografías.')}<section id="filter-bar" class="filter-bar" aria-label="Filtros"></section><section class="panel"><div id="history-list" class="task-list"></div></section>`,
    reports: () => `${pageTitle('RESUMEN Y EXPORTACIÓN', 'Reportes', 'Indicadores básicos del cumplimiento de cuidados. Los filtros también aplican a la exportación.', '<button id="export-report-btn" class="primary-btn"><span class="material-symbols-rounded">download</span>Exportar CSV</button>')}<section id="filter-bar" class="filter-bar" aria-label="Filtros"></section><section class="report-grid"><article class="report-card"><span>Cuidados totales</span><strong id="report-total">0</strong></article><article class="report-card"><span>Realizados</span><strong id="report-completed">0</strong></article><article class="report-card"><span>No realizados</span><strong id="report-skipped">0</strong></article><article class="report-card"><span>Vencidos</span><strong id="report-overdue">0</strong></article><article class="report-card"><span>Cumplimiento</span><strong id="report-rate">0%</strong></article></section><section class="panel"><div class="panel-heading"><div><span class="eyebrow">POR MASCOTA</span><h2>Resumen de actividad</h2></div></div><div id="pet-report-list" class="report-list"></div></section>`,
    profile: () => `${pageTitle('CUENTA PERSONAL', 'Mi perfil', 'Actualiza tus datos o desactiva tu cuenta.')}<section class="panel profile-panel"><form id="profile-form" class="form-grid"><label class="full">Nombre completo<input name="fullName" required></label><label>Correo electrónico<input name="email" type="email" required></label><label>Teléfono / WhatsApp<input name="phoneNumber" type="tel"></label><div class="form-actions full"><button class="primary-btn" type="submit">Guardar cambios</button></div></form><div class="danger-zone"><div><strong>Desactivar cuenta</strong><p>Se cerrará tu sesión y no podrás volver a ingresar.</p></div><button id="delete-profile-btn" class="danger-btn wide">Desactivar</button></div></section>`
  };
  return (templates[page] || templates.dashboard)();
}

function renderFilterBar() {
  const bar = $('#filter-bar');
  if (!bar) return;
  const petOptions = ['<option value="">Todas las mascotas</option>']
    .concat(state.pets.map((pet) => `<option value="${pet.id}" ${String(state.filters.petId) === String(pet.id) ? 'selected' : ''}>${escapeHtml(pet.name)}</option>`)).join('');
  const statusChoices = page === 'history'
    ? [['', 'Cerradas'], ['COMPLETED', 'Realizados'], ['SKIPPED', 'No realizados']]
    : [['', 'Todos los estados'], ['PENDING', 'Pendientes'], ['COMPLETED', 'Realizados'], ['SKIPPED', 'No realizados']];
  const statusOptions = statusChoices.map(([value, label]) => `<option value="${value}" ${state.filters.status === value ? 'selected' : ''}>${label}</option>`).join('');
  const typeOptions = ['<option value="">Todos los tipos</option>']
    .concat(careTypes.map((type) => `<option value="${type}" ${state.filters.careType === type ? 'selected' : ''}>${type}</option>`)).join('');
  const caregiverField = page === 'reports'
    ? `<label>Realizado por<select data-filter="completedById"><option value="">Cualquier persona</option><option value="${state.profile.id}" ${String(state.filters.completedById) === String(state.profile.id) ? 'selected' : ''}>Yo (${escapeHtml(state.profile.fullName.split(/\s+/)[0])})</option>${[...new Map(state.caregivers.map((access) => [access.caregiverId, access])).values()].map((access) => `<option value="${access.caregiverId}" ${String(state.filters.completedById) === String(access.caregiverId) ? 'selected' : ''}>${escapeHtml(access.caregiverName)}</option>`).join('')}</select></label>`
    : '';
  bar.innerHTML = `
    <label>Mascota<select data-filter="petId">${petOptions}</select></label>
    <label>Estado<select data-filter="status">${statusOptions}</select></label>
    <label>Tipo de cuidado<select data-filter="careType">${typeOptions}</select></label>
    <label>Desde<input data-filter="from" type="date" value="${state.filters.from}"></label>
    <label>Hasta<input data-filter="to" type="date" value="${state.filters.to}"></label>
    ${caregiverField}
    <button type="button" id="clear-filters" class="outline-btn">Limpiar filtros</button>`;
  bar.querySelectorAll('[data-filter]').forEach((control) => {
    control.onchange = async () => {
      state.filters[control.dataset.filter] = control.value;
      if (state.filters.from && state.filters.to && state.filters.from > state.filters.to) {
        showToast('La fecha inicial no puede ser posterior a la final.');
        state.filters[control.dataset.filter] = '';
        control.value = '';
        return;
      }
      try { await loadPageData(); } catch (error) { showToast(error.message); }
    };
  });
  $('#clear-filters').onclick = async () => {
    state.filters = { petId: '', status: '', careType: '', from: '', to: '', completedById: '' };
    try { await loadPageData(); } catch (error) { showToast(error.message); }
  };
}

function openModal(title, body) {
  lastFocusedElement = document.activeElement;
  $('#modal-title').textContent = title;
  $('#modal-body').innerHTML = body;
  $('#modal').classList.remove('hidden');
  document.body.style.overflow = 'hidden';
  window.setTimeout(() => $('#modal').querySelector('input, select, textarea, button')?.focus(), 0);
}
function closeModal() { $('#modal').classList.add('hidden'); $('#modal-body').innerHTML = ''; document.body.style.overflow = ''; lastFocusedElement?.focus(); }
function logout() { localStorage.removeItem('taskora_token'); localStorage.removeItem('taskora_role'); window.location.replace('/index.html'); }

async function loadPageData() {
  state.profile = await api('/users/me');
  if (['dashboard', 'agenda', 'pets', 'caregivers', 'recurrences', 'reports', 'history'].includes(page)) state.pets = await api('/api/pets');
  if (['dashboard'].includes(page)) state.tasks = await api('/api/care-tasks');
  if (['agenda', 'history'].includes(page)) state.tasks = await api(`/api/care-tasks${filterQuery()}`);
  if (page === 'recurrences') state.series = await api('/api/series');
  if (page === 'notifications') state.channels = await api('/api/notifications/channels');
  if (['caregivers', 'reports'].includes(page)) state.caregivers = await api('/api/caregivers');
  if (page === 'reports') state.report = await api(`/api/reports/summary${filterQuery()}`);
  renderPage();
}

function renderPage() {
  renderFilterBar();
  if (page === 'dashboard') renderDashboard();
  if (page === 'agenda') { renderTasks(state.tasks, '#tasks-list', 'agenda'); $('#new-task-btn').onclick = () => openTaskModal(); }
  if (page === 'pets') { renderPets(); $('#new-pet-btn').onclick = () => openPetModal(); }
  if (page === 'caregivers') { renderCaregivers(); $('#new-caregiver-btn').onclick = openCaregiverModal; }
  if (page === 'recurrences') { renderSeries(); $('#new-series-btn').onclick = () => openSeriesModal(); }
  if (page === 'notifications') { renderChannels(); $('#connect-telegram-btn').onclick = openTelegramLinkModal; $('#connect-gmail-btn').onclick = openGmailLink; }
  if (page === 'history') renderTasks(state.tasks.filter((task) => task.status !== 'PENDING'), '#history-list', 'history');
  if (page === 'reports') renderReports();
  if (page === 'profile') renderProfile();
}

function renderDashboard() {
  const firstName = state.profile.fullName.trim().split(/\s+/)[0] || 'qué alegría verte';
  $('#welcome-name').textContent = firstName;
  $('#date-label').textContent = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date());
  const pending = state.tasks.filter((task) => task.active && task.status === 'PENDING').sort((a, b) => new Date(a.scheduledAt) - new Date(b.scheduledAt));
  const next = pending[0];
  $('#stat-pets').textContent = state.pets.length;
  $('#stat-pending').textContent = pending.length;
  $('#stat-completed').textContent = state.tasks.filter((task) => task.status === 'COMPLETED').length;
  if (next) { $('#next-care-pet').textContent = next.petName; $('#next-care-title').textContent = next.title; $('#next-care-meta').textContent = `${next.careType} · ${formatDate(next.scheduledAt)}`; }
  $('#refresh-btn').onclick = loadPageData;
}

function renderTasks(tasks, selector, mode) {
  const list = $(selector);
  const editable = mode === 'agenda';
  const visible = tasks.filter((task) => task.active).sort((a, b) => new Date(a.scheduledAt) - new Date(b.scheduledAt));
  if (!visible.length) { list.innerHTML = emptyState('event_available', editable ? 'Tu agenda está despejada' : 'Sin registros históricos', editable ? 'Programa el primer cuidado de tu mascota.' : 'Las tareas cerradas aparecerán aquí.'); return; }
  list.innerHTML = visible.map((task) => {
    const date = new Date(task.scheduledAt);
    const level = task.accessLevel || 'OWNER';
    const viewer = level === 'VIEWER';
    const canStatus = !viewer && task.status === 'PENDING';
    const statusClass = task.status === 'COMPLETED' ? 'completed' : task.status === 'SKIPPED' ? 'skipped' : task.overdue ? 'overdue' : '';
    const sharedChip = level !== 'OWNER' ? `<span class="access-chip">${level === 'EDITOR' ? 'Cuidador' : 'Solo lectura'}</span>` : '';
    const doneBy = task.completedByName && task.status !== 'PENDING' ? ` · por ${escapeHtml(task.completedByName)}` : '';
    const statusCell = canStatus
      ? `<select class="status-select ${statusClass}" data-status-task="${task.id}" aria-label="Estado de ${escapeHtml(task.title)}"><option value="PENDING" selected>${task.overdue ? 'Vencido' : 'Pendiente'}</option><option value="COMPLETED">Realizado</option><option value="SKIPPED">No realizado</option></select>`
      : `<span class="status-select ${statusClass}">${escapeHtml(task.displayStatus || (task.status === 'COMPLETED' ? 'Realizado' : task.status === 'SKIPPED' ? 'No realizado' : 'Pendiente'))}</span>`;
    const actions = [
      `<button class="task-action" data-task-images="${task.id}" aria-label="Fotos de ${escapeHtml(task.title)}" title="Fotos"><span class="material-symbols-rounded">photo_camera</span></button>`,
      `<button class="task-action" data-task-logs="${task.id}" aria-label="Actividad de ${escapeHtml(task.title)}" title="Actividad"><span class="material-symbols-rounded">manage_history</span></button>`,
      !viewer && task.status === 'PENDING' ? `<button class="task-action" data-reschedule-task="${task.id}" aria-label="Reprogramar ${escapeHtml(task.title)}" title="Reprogramar"><span class="material-symbols-rounded">schedule</span></button>` : '',
      editable && level === 'OWNER' ? `<button class="task-action" data-edit-task="${task.id}" aria-label="Editar ${escapeHtml(task.title)}" title="Editar"><span class="material-symbols-rounded">edit</span></button><button class="task-action danger" data-delete-task="${task.id}" aria-label="Cancelar ${escapeHtml(task.title)}" title="Cancelar"><span class="material-symbols-rounded">delete</span></button>` : ''
    ].join('');
    return `<article class="task-item"><div class="task-date"><strong>${date.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}</strong><span>${date.toLocaleDateString('es-CO', { day: '2-digit', month: 'short', year: 'numeric' })}</span></div><div class="task-main"><span class="task-avatar">🐾</span><div><strong>${escapeHtml(task.title)}${sharedChip}</strong><span class="task-meta">${escapeHtml(task.petName)} · ${escapeHtml(task.careType)} · ${recurrenceLabel(task)}${doneBy}</span>${task.warning ? `<span class="task-meta warning-text">${escapeHtml(task.warning)}</span>` : ''}</div></div><div class="task-status">${statusCell}</div><div class="task-actions">${actions}</div></article>`;
  }).join('');
  list.querySelectorAll('[data-status-task]').forEach((select) => { select.onchange = () => updateTaskStatus(select.dataset.statusTask, select.value); });
  list.querySelectorAll('[data-task-images]').forEach((button) => { button.onclick = () => openEvidenceModal(visible.find((task) => task.id == button.dataset.taskImages)); });
  list.querySelectorAll('[data-task-logs]').forEach((button) => { button.onclick = () => openLogsModal(visible.find((task) => task.id == button.dataset.taskLogs)); });
  list.querySelectorAll('[data-reschedule-task]').forEach((button) => { button.onclick = () => openRescheduleModal(visible.find((task) => task.id == button.dataset.rescheduleTask)); });
  list.querySelectorAll('[data-edit-task]').forEach((button) => { button.onclick = () => openTaskModal(visible.find((task) => task.id == button.dataset.editTask)); });
  list.querySelectorAll('[data-delete-task]').forEach((button) => { button.onclick = () => deleteTask(button.dataset.deleteTask); });
}

function renderPets() {
  const list = $('#pets-list');
  if (!state.pets.length) { list.innerHTML = emptyState('pets', 'Tu familia empieza aquí', 'Agrega el perfil de tu primera mascota.'); return; }
  list.innerHTML = state.pets.map((pet) => {
    const level = pet.accessLevel || 'OWNER';
    const sharedLabel = level === 'EDITOR' ? '<span class="access-chip">Compartida · Editor</span>' : level === 'VIEWER' ? '<span class="access-chip">Compartida · Solo lectura</span>' : '';
    const actions = level === 'OWNER' ? `<div class="card-actions"><button class="small-btn" data-edit-pet="${pet.id}">Editar</button><button class="danger-btn" data-delete-pet="${pet.id}" aria-label="Archivar a ${escapeHtml(pet.name)}"><span class="material-symbols-rounded">archive</span></button></div>` : '';
    return `<article class="pet-profile-card"><div class="pet-profile-image">${pet.photoUrl ? `<img data-auth-image="${escapeHtml(pet.photoUrl)}" alt="Foto de ${escapeHtml(pet.name)}" loading="lazy">` : `<span>${petEmoji(pet.species)}</span>`}</div><div class="pet-profile-copy"><span class="eyebrow">${escapeHtml(pet.species)}</span><h2>${escapeHtml(pet.name)} ${sharedLabel}</h2><p>${pet.breed ? escapeHtml(pet.breed) : 'Sin raza registrada'}${pet.color ? ` · ${escapeHtml(pet.color)}` : ''}${pet.notes ? ` · ${escapeHtml(pet.notes)}` : ''}</p></div>${actions}</article>`;
  }).join('');
  hydrateProtectedImages(list);
  list.querySelectorAll('[data-edit-pet]').forEach((button) => { button.onclick = () => openPetModal(state.pets.find((pet) => pet.id == button.dataset.editPet)); });
  list.querySelectorAll('[data-delete-pet]').forEach((button) => { button.onclick = () => deletePet(button.dataset.deletePet); });
}

function openPetModal(pet = null) {
  const currentSpecies = pet?.species || '';
  const speciesOptions = [...petSpecies];
  if (currentSpecies && !speciesOptions.some((species) => simpleText(species) === simpleText(currentSpecies))) {
    speciesOptions.unshift(currentSpecies);
  }
  openModal(pet ? 'Editar mascota' : 'Nueva mascota', `
    <form id="pet-form" class="form-grid">
      ${pet?.photoUrl ? `<div class="pet-photo-preview full"><img data-auth-image="${escapeHtml(pet.photoUrl)}" alt="Foto actual de ${escapeHtml(pet.name)}"><div><strong>Foto actual</strong><small>Puedes reemplazarla o eliminarla.</small></div><button type="button" class="danger-btn" id="remove-pet-image"><span class="material-symbols-rounded">delete</span></button></div>` : ''}
      <label>Nombre<input name="name" required value="${escapeHtml(pet?.name || '')}"></label>
      <label>Especie
        <select id="pet-species" name="species" required>
          <option value="">Selecciona una especie</option>
          ${speciesOptions.map((species) => `<option value="${escapeHtml(species)}" ${simpleText(species) === simpleText(currentSpecies) ? 'selected' : ''}>${escapeHtml(species)}</option>`).join('')}
        </select>
      </label>
      <label>Raza
        <select id="pet-breed" name="breed"></select>
      </label>
      <label>Color<input name="color" list="pet-color-list" value="${escapeHtml(pet?.color || '')}" placeholder="Ej. Blanco y café"><datalist id="pet-color-list">${petColors.map((color) => `<option value="${escapeHtml(color)}"></option>`).join('')}</datalist></label>
      <label>Fecha de nacimiento<input name="birthDate" type="date" value="${pet?.birthDate || ''}"></label>
      <label>Sexo<select name="sex"><option value="">Sin especificar</option><option value="hembra" ${simpleText(pet?.sex) === 'hembra' ? 'selected' : ''}>Hembra</option><option value="macho" ${simpleText(pet?.sex) === 'macho' ? 'selected' : ''}>Macho</option></select></label>
      <label class="upload-field">Fotografía<input name="image" type="file" accept="image/jpeg,image/png,image/webp"><small>JPG, PNG o WebP. Máximo 5 MB.</small></label>
      <label class="full">Observaciones<textarea name="notes">${escapeHtml(pet?.notes || '')}</textarea></label>
      <div class="form-actions full"><button type="button" class="outline-btn" data-close>Cancelar</button><button class="primary-btn" type="submit">Guardar</button></div>
    </form>`);
  hydrateProtectedImages($('#modal-body'));
  const speciesInput = $('#pet-species');
  const breedSelect = $('#pet-breed');
  updateBreedOptions(speciesInput, breedSelect, pet?.breed || '');
  speciesInput.addEventListener('change', () => updateBreedOptions(speciesInput, breedSelect));
  $('[data-close]').onclick = closeModal;
  if ($('#remove-pet-image')) $('#remove-pet-image').onclick = async () => { if (!confirm('¿Eliminar la foto actual?')) return; try { await api(`/api/pets/${pet.id}/image`, { method: 'DELETE' }); closeModal(); await loadPageData(); showToast('Foto eliminada.'); } catch (error) { showToast(error.message); } };
  $('#pet-form').onsubmit = async (event) => {
    event.preventDefault();
    const data = formData(event.target); const image = event.target.elements.image.files[0]; delete data.image;
    if (!data.birthDate) delete data.birthDate;
    if (image && image.size > 5 * 1024 * 1024) return showToast('La imagen no puede superar 5 MB.');
    try { const saved = await api(pet ? `/api/pets/${pet.id}` : '/api/pets', { method: pet ? 'PUT' : 'POST', body: JSON.stringify(data) }); if (image) { const upload = new FormData(); upload.append('file', image); await api(`/api/pets/${saved.id}/image`, { method: 'POST', body: upload }); } closeModal(); await loadPageData(); showToast('Mascota guardada.'); } catch (error) { showToast(error.message); }
  };
}

function openTaskModal(task = null) {
  const availablePets = task ? state.pets : state.pets.filter((pet) => (pet.accessLevel || 'OWNER') === 'OWNER');
  if (!availablePets.length) return showToast('Como cuidador puedes completar o reprogramar cuidados compartidos. Solo el propietario puede crear nuevos cuidados.');
  const now = new Date(Date.now() + 60 * 60 * 1000);
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  const defaultDateTime = now.toISOString().slice(0, 16);
  const defaultDate = defaultDateTime.slice(0, 10);
  const selectedPet = availablePets.find((pet) => pet.id === task?.petId) || availablePets[0];
  const taskCareTypes = careTypesForPet(selectedPet);
  if (task?.careType && !taskCareTypes.includes(task.careType)) taskCareTypes.unshift(task.careType);
  openModal(task ? 'Editar cuidado' : 'Nuevo cuidado', `
    <form id="task-form" class="form-grid">
      ${task?.seriesId ? '<div class="pet-photo-preview full"><span class="material-symbols-rounded" aria-hidden="true">repeat</span><div><strong>Esta ocurrencia pertenece a una serie</strong><small>Para cambiar la rutina completa, edita la serie en la página Recurrencias.</small></div></div>' : ''}
      <label class="full">Título<input name="title" required value="${escapeHtml(task?.title || (task ? '' : 'Dar comida'))}" placeholder="Ej. Dar comida"></label>
      <label>Tipo<select name="careType" id="care-type">${taskCareTypes.map((type) => `<option ${task?.careType === type ? 'selected' : ''}>${escapeHtml(type)}</option>`).join('')}</select></label>
      <label>Mascota<select id="task-pet" name="petId" ${task ? 'disabled' : ''}>${availablePets.map((pet) => `<option value="${pet.id}" ${selectedPet?.id === pet.id ? 'selected' : ''}>${escapeHtml(pet.name)}</option>`).join('')}</select></label>
      <label>Prioridad<select name="priority"><option value="LOW" ${task?.priority === 'LOW' ? 'selected' : ''}>Baja</option><option value="MEDIUM" ${!task || task.priority === 'MEDIUM' ? 'selected' : ''}>Media</option><option value="HIGH" ${task?.priority === 'HIGH' ? 'selected' : ''}>Alta</option></select></label>
      ${task ? '' : '<label>Programación<select name="scheduleMode" id="schedule-mode"><option value="DAILY_SLOTS">Rutina diaria · varios horarios</option><option value="SINGLE">Una hora o repetición simple</option></select></label>'}

      ${task ? '' : `<section id="feeding-routine" class="routine-builder full">
        <div class="routine-heading"><div><span class="eyebrow">PLAN DIARIO EDITABLE</span><h3 id="routine-title">Comidas distribuidas durante el día</h3><p>Se creará una serie recurrente diaria con los horarios que definas. Podrás pausarla, reanudarla o editarla en Recurrencias.</p></div><span class="routine-count" id="routine-count">3 horarios</span></div>
        <div class="routine-dates">
          <label>Comenzar el<input name="startDate" type="date" value="${defaultDate}" required></label>
          <label>Finalizar el<input name="recurrenceEndDate" type="date"><small>Opcional</small></label>
        </div>
        <div id="routine-slots" class="routine-slots">
          ${routineSlot('Mañana', '08:00')}
          ${routineSlot('Tarde', '14:00')}
          ${routineSlot('Noche', '20:00')}
        </div>
        <button type="button" id="add-routine-slot" class="outline-btn routine-add"><span class="material-symbols-rounded">add</span>Agregar otro horario</button>
      </section>`}

      <div id="single-schedule" class="form-subgrid full ${!task ? 'hidden' : ''}">
        <label>Fecha y hora<input name="scheduledAt" type="datetime-local" value="${task?.scheduledAt?.slice(0, 16) || defaultDateTime}" required ${task && task.status !== 'PENDING' ? 'disabled' : ''}></label>
        ${task ? '' : '<label>Repetición<select name="recurrence"><option value="NONE" selected>Una vez</option><option value="DAILY">Diaria (serie)</option><option value="WEEKLY">Semanal (serie)</option><option value="INTERVAL">Por intervalo (serie)</option></select></label><label>Intervalo en días<input name="recurrenceIntervalDays" type="number" min="1" placeholder="Ej. 3"></label>'}
      </div>
      <label class="full">Descripción<textarea name="description" placeholder="Porción, alimento o indicaciones importantes">${escapeHtml(task?.description || '')}</textarea></label>
      <div class="form-actions full"><button type="button" class="outline-btn" data-close>Cancelar</button><button class="primary-btn" type="submit">${task ? 'Guardar cambios' : 'Crear cuidados'}</button></div>
    </form>`);
  $('[data-close]').onclick = closeModal;
  let routineTouched = false;
  const routinePresets = {
    Alimentación: [['Mañana', '08:00'], ['Tarde', '14:00'], ['Noche', '20:00']],
    Paseo: [['Mañana', '07:00'], ['Tarde', '13:00'], ['Noche', '19:00']],
    Agua: [['Mañana', '08:00'], ['Noche', '20:00']],
    Higiene: [['Mañana', '09:00']],
    Cita: [['Recordatorio', '09:00']],
    Tratamiento: [['Mañana', '08:00'], ['Noche', '20:00']]
  };
  const applyRoutinePreset = (careType) => {
    if (task) return;
    const preset = routinePresets[careType] || [['Recordatorio', '09:00']];
    $('#routine-slots').innerHTML = preset.map(([label, time]) => routineSlot(label, time)).join('');
    $('#routine-title').textContent = careType === 'Alimentación' ? 'Comidas distribuidas durante el día' : careType === 'Paseo' ? 'Paseos distribuidos durante el día' : `Rutina diaria de ${careType.toLocaleLowerCase('es')}`;
    updateRoutineCount();
  };
  const updateRoutineMode = () => {
    if (task) return;
    const routineMode = $('#schedule-mode').value === 'DAILY_SLOTS';
    $('#feeding-routine').classList.toggle('hidden', !routineMode);
    $('#single-schedule').classList.toggle('hidden', routineMode);
    $('#feeding-routine').querySelectorAll('input').forEach((input) => { input.disabled = !routineMode; });
    $('#single-schedule').querySelectorAll('input, select').forEach((input) => { input.disabled = routineMode; });
  };
  if (!task) {
    $('#schedule-mode').onchange = updateRoutineMode;
    $('#routine-slots').oninput = () => { routineTouched = true; };
    updateRoutineMode();
    $('#add-routine-slot').onclick = () => {
      const slots = $('#routine-slots').querySelectorAll('.routine-slot');
      if (slots.length >= 6) return showToast('Puedes configurar máximo 6 horarios diarios.');
      $('#routine-slots').insertAdjacentHTML('beforeend', routineSlot('Otro horario', '18:00'));
      updateRoutineCount();
    };
    $('#routine-slots').onclick = (event) => {
      const removeButton = event.target.closest('[data-remove-slot]');
      if (!removeButton) return;
      const slots = $('#routine-slots').querySelectorAll('.routine-slot');
      if (slots.length <= 1) return showToast('La rutina debe conservar al menos un horario.');
      removeButton.closest('.routine-slot').remove();
      updateRoutineCount();
    };
  }
  const updateTaskTitle = () => {
    const careType = $('#care-type').value;
    const pet = state.pets.find((item) => item.id == $('#task-pet').value);
    $('#task-form').elements.title.value = careTitleDefaults[careType] || `${careType} de ${pet?.name || 'la mascota'}`;
  };
  $('#task-pet').onchange = () => {
    const previousDefault = Object.values(careTitleDefaults).includes($('#task-form').elements.title.value) ||
      $('#task-form').elements.title.value.endsWith(' de la mascota');
    updateCareTypeOptions($('#task-pet'), $('#care-type'));
    if (previousDefault) updateTaskTitle();
    if (!routineTouched) applyRoutinePreset($('#care-type').value);
  };
  $('#care-type').onchange = () => {
    const previousDefault = Object.values(careTitleDefaults).includes($('#task-form').elements.title.value) ||
      $('#task-form').elements.title.value.endsWith(' de la mascota');
    if (previousDefault) updateTaskTitle();
    if (!routineTouched) applyRoutinePreset($('#care-type').value);
  };
  $('#task-form').onsubmit = async (event) => {
    event.preventDefault();
    const data = formData(event.target);
    try {
      if (task) {
        const payload = { title: data.title, careType: data.careType, priority: data.priority, description: data.description };
        if (data.scheduledAt && data.scheduledAt.slice(0, 16) !== String(task.scheduledAt).slice(0, 16)) payload.scheduledAt = data.scheduledAt;
        await api(`/api/care-tasks/${task.id}`, { method: 'PUT', body: JSON.stringify(payload) });
        showToast('Cuidado guardado.');
      } else if (data.scheduleMode === 'DAILY_SLOTS') {
        const slots = [...$('#routine-slots').querySelectorAll('.routine-slot')].map((slot) => slot.querySelector('[data-slot-time]').value);
        const payload = {
          petId: Number(data.petId), title: data.title, description: data.description,
          careType: data.careType, priority: data.priority, frequency: 'DAILY',
          timesOfDay: slots, startDate: data.startDate, endDate: data.recurrenceEndDate || null
        };
        await api('/api/series', { method: 'POST', body: JSON.stringify(payload) });
        showToast(`Serie diaria creada con ${slots.length} horarios. Adminístrala en Recurrencias.`);
      } else if (data.recurrence && data.recurrence !== 'NONE') {
        const payload = {
          petId: Number(data.petId), title: data.title, description: data.description,
          careType: data.careType, priority: data.priority, frequency: data.recurrence,
          intervalDays: data.recurrenceIntervalDays ? Number(data.recurrenceIntervalDays) : null,
          timesOfDay: [data.scheduledAt.slice(11, 16)], startDate: data.scheduledAt.slice(0, 10)
        };
        await api('/api/series', { method: 'POST', body: JSON.stringify(payload) });
        showToast('Serie recurrente creada. Adminístrala en Recurrencias.');
      } else {
        const payload = {
          petId: Number(data.petId), title: data.title, description: data.description,
          careType: data.careType, priority: data.priority, scheduledAt: data.scheduledAt
        };
        await api('/api/care-tasks', { method: 'POST', body: JSON.stringify(payload) });
        showToast('Cuidado guardado.');
      }
      closeModal();
      await loadPageData();
    } catch (error) { showToast(error.message); }
  };
}

function routineSlot(label, time) {
  return `<div class="routine-slot"><label>Momento<input data-slot-label required maxlength="40" value="${label}"></label><label>Hora<input data-slot-time type="time" required value="${time}"></label><button type="button" class="task-action danger" data-remove-slot aria-label="Quitar horario"><span class="material-symbols-rounded">close</span></button></div>`;
}

function updateRoutineCount() {
  const count = $('#routine-slots').querySelectorAll('.routine-slot').length;
  $('#routine-count').textContent = `${count} ${count === 1 ? 'horario' : 'horarios'}`;
}

async function updateTaskStatus(id, status) { const reason = status === 'SKIPPED' ? prompt('¿Por qué no se realizó este cuidado?') : null; if (status === 'SKIPPED' && !reason) { await loadPageData(); return; } try { await api(`/api/care-tasks/${id}`, { method: 'PUT', body: JSON.stringify({ status, reason }) }); await loadPageData(); showToast(status === 'COMPLETED' ? 'Cuidado marcado como realizado.' : 'Cuidado marcado como no realizado.'); } catch (error) { showToast(error.message); await loadPageData(); } }
async function deleteTask(id) { const reason = prompt('¿Por qué ya no es necesario este cuidado?'); if (!reason) return; try { await api(`/api/care-tasks/${id}/cancel`, { method: 'POST', body: JSON.stringify({ reason }) }); await loadPageData(); showToast('Cuidado cancelado.'); } catch (error) { showToast(error.message); } }
async function deletePet(id) { if (!confirm('¿Archivar esta mascota?')) return; try { await api(`/api/pets/${id}`, { method: 'DELETE' }); await loadPageData(); showToast('Mascota archivada.'); } catch (error) { showToast(error.message); } }

function openRescheduleModal(task) {
  const value = String(task.scheduledAt).slice(0, 16);
  openModal(`Reprogramar · ${task.title}`, `<form id="reschedule-form" class="form-grid"><label class="full">Nueva fecha y hora<input name="scheduledAt" type="datetime-local" required value="${value}"></label><label class="full">Motivo opcional<textarea name="reason" placeholder="Ej. Cambio de horario"></textarea></label><div class="form-actions full"><button type="button" class="outline-btn" data-close>Cancelar</button><button class="primary-btn" type="submit">Reprogramar</button></div></form>`);
  $('[data-close]').onclick = closeModal;
  $('#reschedule-form').onsubmit = async (event) => {
    event.preventDefault();
    try {
      await api(`/api/care-tasks/${task.id}/reschedule`, { method: 'POST', body: JSON.stringify(formData(event.target)) });
      closeModal(); await loadPageData(); showToast('Cuidado reprogramado.');
    } catch (error) { showToast(error.message); }
  };
}

async function openLogsModal(task) {
  try {
    const logs = await api(`/api/care-tasks/${task.id}/logs`);
    const actionLabels = { COMPLETED: 'Completó el cuidado', SKIPPED: 'Omitió el cuidado', RESCHEDULED: 'Reprogramó el cuidado', CANCELLED: 'Canceló el cuidado' };
    const rows = logs.length ? logs.map((log) => `<article class="channel-card"><div class="channel-avatar"><span class="material-symbols-rounded">${log.action === 'COMPLETED' ? 'check_circle' : log.action === 'SKIPPED' ? 'skip_next' : log.action === 'RESCHEDULED' ? 'schedule' : 'cancel'}</span></div><div><strong>${escapeHtml(actionLabels[log.action] || log.action)}</strong><small>${escapeHtml(log.actorName)} · ${formatDate(log.createdAt)}${log.detail ? ` · ${escapeHtml(log.detail)}` : ''}</small></div></article>`).join('') : emptyState('manage_history', 'Sin actividad registrada', 'Las acciones sobre este cuidado aparecerán aquí.');
    openModal(`Actividad · ${task.title}`, `<div class="channels-list">${rows}</div><div class="form-actions"><button type="button" class="outline-btn" data-close>Cerrar</button></div>`);
    $('[data-close]').onclick = closeModal;
  } catch (error) { showToast(error.message); }
}

async function openEvidenceModal(task) {
  try {
    const images = await api(`/api/care-tasks/${task.id}/images`);
    const canUpload = task.accessLevel !== 'VIEWER';
    const gallery = images.length ? images.map((item) => `<article class="evidence-card"><img data-auth-image="${escapeHtml(item.imageUrl)}" alt="Evidencia enviada por ${escapeHtml(item.uploaderName)}"><div class="evidence-info"><strong>${escapeHtml(item.uploaderName)}</strong><small>${formatDate(item.createdAt)}</small>${item.note ? `<p>${escapeHtml(item.note)}</p>` : ''}</div>${item.canDelete && canUpload ? `<button class="danger-btn evidence-delete" data-delete-evidence="${item.id}"><span class="material-symbols-rounded">delete</span></button>` : ''}</article>`).join('') : emptyState('photo_camera', 'Aún no hay fotos', 'El propietario o cuidador puede enviar la primera evidencia.');
    const form = canUpload
      ? `<form id="evidence-form" class="evidence-form"><label class="upload-field">Nueva foto<input name="file" type="file" accept="image/jpeg,image/png,image/webp" required><small>JPG, PNG o WebP. Máximo 5 MB.</small></label><label>Nota opcional<textarea name="note" maxlength="500"></textarea></label><div class="form-actions"><button type="button" class="outline-btn" data-close>Cerrar</button><button class="primary-btn" type="submit">Enviar foto</button></div></form>`
      : `<div class="form-actions"><p class="muted" style="margin-right:auto">Tu permiso de solo lectura no permite subir evidencias.</p><button type="button" class="outline-btn" data-close>Cerrar</button></div>`;
    openModal(`Fotos · ${task.title}`, `<section class="evidence-gallery" aria-label="Evidencias del cuidado">${gallery}</section>${form}`);
    hydrateProtectedImages($('#modal-body'));
    $('[data-close]').onclick = closeModal;
    document.querySelectorAll('[data-delete-evidence]').forEach((button) => { button.onclick = async () => { if (!confirm('¿Eliminar esta evidencia?')) return; try { await api(`/api/care-tasks/${task.id}/images/${button.dataset.deleteEvidence}`, { method: 'DELETE' }); await openEvidenceModal(task); showToast('Evidencia eliminada.'); } catch (error) { showToast(error.message); } }; });
    if (canUpload) $('#evidence-form').onsubmit = async (event) => { event.preventDefault(); const file = event.target.elements.file.files[0]; if (file.size > 5 * 1024 * 1024) return showToast('La imagen no puede superar 5 MB.'); try { await api(`/api/care-tasks/${task.id}/images`, { method: 'POST', body: new FormData(event.target) }); await openEvidenceModal(task); showToast('Foto enviada.'); } catch (error) { showToast(error.message); } };
  } catch (error) { showToast(error.message); }
}

function renderCaregivers() {
  const list = $('#caregivers-list');
  if (!state.caregivers.length) { list.innerHTML = emptyState('group', 'No hay cuidadores asignados', 'Invita a una persona que ya tenga cuenta en Taskora Pet.'); return; }
  list.innerHTML = state.caregivers.map((access) => `<article class="channel-card"><div class="channel-avatar"><span class="material-symbols-rounded">person</span></div><div><strong>${escapeHtml(access.caregiverName)}</strong><small>${escapeHtml(access.caregiverEmail)} · ${escapeHtml(access.petName)}</small></div><div class="card-actions"><select class="status-select" data-permission-caregiver="${access.id}" aria-label="Permiso de ${escapeHtml(access.caregiverName)}"><option value="EDITOR" ${access.permission === 'EDITOR' ? 'selected' : ''}>Editor</option><option value="VIEWER" ${access.permission === 'VIEWER' ? 'selected' : ''}>Solo lectura</option></select><button class="danger-btn" data-delete-caregiver="${access.id}" aria-label="Revocar acceso"><span class="material-symbols-rounded">person_remove</span></button></div></article>`).join('');
  list.querySelectorAll('[data-permission-caregiver]').forEach((select) => {
    select.onchange = async () => {
      try {
        await api(`/api/caregivers/${select.dataset.permissionCaregiver}`, { method: 'PUT', body: JSON.stringify({ permission: select.value }) });
        await loadPageData(); showToast('Permiso actualizado.');
      } catch (error) { showToast(error.message); await loadPageData(); }
    };
  });
  list.querySelectorAll('[data-delete-caregiver]').forEach((button) => { button.onclick = async () => { if (!confirm('¿Revocar el acceso de este cuidador?')) return; try { await api(`/api/caregivers/${button.dataset.deleteCaregiver}`, { method: 'DELETE' }); await loadPageData(); showToast('Acceso revocado.'); } catch (error) { showToast(error.message); } }; });
}
function openCaregiverModal() {
  const ownPets = state.pets.filter((pet) => (pet.accessLevel || 'OWNER') === 'OWNER');
  if (!ownPets.length) return showToast('Solo puedes asignar cuidadores a tus propias mascotas.');
  openModal('Asignar cuidador', `<form id="caregiver-form" class="form-grid"><label class="full">Correo del cuidador<input name="caregiverEmail" type="email" required placeholder="cuidador@correo.com"><small class="muted">La persona debe tener una cuenta registrada en Taskora Pet.</small></label><label>Mascota<select name="petId">${ownPets.map((pet) => `<option value="${pet.id}">${escapeHtml(pet.name)}</option>`).join('')}</select></label><label>Permiso<select name="permission"><option value="EDITOR">Editor · puede completar cuidados</option><option value="VIEWER">Solo lectura</option></select></label><div class="form-actions full"><button type="button" class="outline-btn" data-close>Cancelar</button><button class="primary-btn" type="submit">Asignar</button></div></form>`);
  $('[data-close]').onclick = closeModal;
  $('#caregiver-form').onsubmit = async (event) => { event.preventDefault(); const data = formData(event.target); data.petId = Number(data.petId); try { await api('/api/caregivers', { method: 'POST', body: JSON.stringify(data) }); closeModal(); await loadPageData(); showToast('Cuidador asignado.'); } catch (error) { showToast(error.message); } };
}

function seriesFrequencyLabel(series) {
  if (series.frequency === 'DAILY') return 'Diaria';
  if (series.frequency === 'WEEKLY') {
    const labels = series.daysOfWeek.map((day) => (weekdays.find(([value]) => value === day) || [])[1]).filter(Boolean);
    return labels.length ? `Semanal · ${labels.join(', ')}` : 'Semanal';
  }
  return `Cada ${series.intervalDays || 1} días`;
}

function renderSeries() {
  const list = $('#series-list');
  const visible = state.series.filter((series) => series.status !== 'CANCELLED');
  if (!visible.length) { list.innerHTML = emptyState('repeat', 'Sin series recurrentes', 'Crea una serie para repetir cuidados diarios, semanales o por intervalo.'); return; }
  const statusChip = (status) => status === 'ACTIVE' ? '<span class="status-chip">Activa</span>' : status === 'PAUSED' ? '<span class="status-chip paused">Pausada</span>' : '<span class="status-chip cancelled">Cancelada</span>';
  list.innerHTML = visible.map((series) => {
    const times = series.timesOfDay.map((time) => time.slice(0, 5)).join(' · ');
    const actions = series.canManage ? [
      `<button class="task-action" data-edit-series="${series.id}" aria-label="Editar serie" title="Editar"><span class="material-symbols-rounded">edit</span></button>`,
      series.status === 'ACTIVE' ? `<button class="task-action" data-pause-series="${series.id}" aria-label="Pausar serie" title="Pausar"><span class="material-symbols-rounded">pause</span></button>` : '',
      series.status === 'PAUSED' ? `<button class="task-action" data-resume-series="${series.id}" aria-label="Reanudar serie" title="Reanudar"><span class="material-symbols-rounded">play_arrow</span></button>` : '',
      `<button class="task-action danger" data-delete-series="${series.id}" aria-label="Eliminar serie" title="Eliminar"><span class="material-symbols-rounded">delete</span></button>`
    ].join('') : '<span class="access-chip">Compartida</span>';
    return `<article class="task-item"><div class="task-date"><strong>${seriesFrequencyLabel(series)}</strong><span>${series.endDate ? `Hasta ${series.endDate}` : 'Sin fecha final'}</span></div><div class="task-main"><span class="task-avatar"><span class="material-symbols-rounded">repeat</span></span><div><strong>${escapeHtml(series.title)}</strong><span class="task-meta">${escapeHtml(series.petName)} · ${escapeHtml(series.careType)} · ${times}</span><span class="task-meta">${series.pendingOccurrences} pendientes · ${series.completedOccurrences} completadas</span></div></div><div class="task-status">${statusChip(series.status)}</div><div class="task-actions">${actions}</div></article>`;
  }).join('');
  list.querySelectorAll('[data-edit-series]').forEach((button) => { button.onclick = () => openSeriesModal(state.series.find((series) => series.id == button.dataset.editSeries)); });
  list.querySelectorAll('[data-pause-series]').forEach((button) => { button.onclick = () => seriesAction(button.dataset.pauseSeries, 'pause', '¿Pausar esta serie? Las ocurrencias pendientes futuras se retirarán de la agenda.'); });
  list.querySelectorAll('[data-resume-series]').forEach((button) => { button.onclick = () => seriesAction(button.dataset.resumeSeries, 'resume', '¿Reanudar esta serie?'); });
  list.querySelectorAll('[data-delete-series]').forEach((button) => {
    button.onclick = async () => {
      if (!confirm('¿Eliminar la serie completa? El historial de cuidados completados se conserva.')) return;
      try { await api(`/api/series/${button.dataset.deleteSeries}`, { method: 'DELETE' }); await loadPageData(); showToast('Serie eliminada. El historial se conserva.'); } catch (error) { showToast(error.message); }
    };
  });
}

async function seriesAction(id, action, message) {
  if (!confirm(message)) return;
  try {
    await api(`/api/series/${id}/${action}`, { method: 'POST' });
    await loadPageData();
    showToast(action === 'pause' ? 'Serie pausada.' : 'Serie reanudada.');
  } catch (error) { showToast(error.message); }
}

function seriesTimeRow(value = '08:00') {
  return `<div class="series-time-row"><input data-series-time type="time" required value="${value}"><button type="button" class="task-action danger" data-remove-time aria-label="Quitar horario"><span class="material-symbols-rounded">close</span></button></div>`;
}

function openSeriesModal(series = null) {
  const availablePets = series ? state.pets : state.pets.filter((pet) => (pet.accessLevel || 'OWNER') === 'OWNER');
  if (!series && !availablePets.length) return showToast('Solo el propietario puede crear una serie para sus mascotas.');
  const today = new Date().toISOString().slice(0, 10);
  const selectedPet = availablePets.find((pet) => pet.id === series?.petId) || availablePets[0];
  const typeOptions = careTypesForPet(selectedPet);
  if (series?.careType && !typeOptions.includes(series.careType)) typeOptions.unshift(series.careType);
  openModal(series ? 'Editar serie' : 'Nueva serie recurrente', `
    <form id="series-form" class="form-grid">
      <label class="full">Título<input name="title" required maxlength="120" value="${escapeHtml(series?.title || '')}" placeholder="Ej. Alimentar a Luna"></label>
      ${series ? '' : `<label>Mascota<select id="series-pet" name="petId">${availablePets.map((pet) => `<option value="${pet.id}" ${selectedPet?.id === pet.id ? 'selected' : ''}>${escapeHtml(pet.name)}</option>`).join('')}</select></label>`}
      <label>Tipo<select id="series-care-type" name="careType">${typeOptions.map((type) => `<option ${series?.careType === type ? 'selected' : ''}>${escapeHtml(type)}</option>`).join('')}</select></label>
      <label>Prioridad<select name="priority"><option value="LOW" ${series?.priority === 'LOW' ? 'selected' : ''}>Baja</option><option value="MEDIUM" ${!series || series.priority === 'MEDIUM' ? 'selected' : ''}>Media</option><option value="HIGH" ${series?.priority === 'HIGH' ? 'selected' : ''}>Alta</option></select></label>
      <label>Frecuencia<select name="frequency" id="series-frequency"><option value="DAILY" ${!series || series.frequency === 'DAILY' ? 'selected' : ''}>Diaria</option><option value="WEEKLY" ${series?.frequency === 'WEEKLY' ? 'selected' : ''}>Semanal</option><option value="INTERVAL" ${series?.frequency === 'INTERVAL' ? 'selected' : ''}>Cada cierto intervalo</option></select></label>
      <label id="series-interval-field" class="${series?.frequency === 'INTERVAL' ? '' : 'hidden'}">Intervalo en días<input name="intervalDays" type="number" min="1" value="${series?.intervalDays || ''}" placeholder="Ej. 3"></label>
      <div id="series-days-field" class="full ${series?.frequency === 'WEEKLY' ? '' : 'hidden'}"><span class="eyebrow">DÍAS DE LA SEMANA</span><div class="weekday-picker" id="series-days">${weekdays.map(([value, label]) => `<label><input type="checkbox" value="${value}" ${series?.daysOfWeek?.includes(value) ? 'checked' : ''}><span>${label}</span></label>`).join('')}</div></div>
      <div class="full"><span class="eyebrow">HORARIOS DEL DÍA (MÁXIMO 6)</span><div id="series-times" class="series-times" style="margin-top:.5rem">${(series?.timesOfDay?.length ? series.timesOfDay.map((time) => seriesTimeRow(time.slice(0, 5))) : [seriesTimeRow('08:00')]).join('')}</div><button type="button" id="add-series-time" class="outline-btn" style="margin-top:.55rem"><span class="material-symbols-rounded">add</span>Agregar horario</button></div>
      ${series ? '' : `<label>Comenzar el<input name="startDate" type="date" required value="${today}"></label>`}
      <label>Finalizar el<input name="endDate" type="date" value="${series?.endDate || ''}"><small class="muted">Opcional</small></label>
      ${series ? `<label>Aplicar cambios desde<input name="applyFrom" type="date" required value="${today}"><small class="muted">Las ocurrencias anteriores no se modifican.</small></label>` : ''}
      <label class="full">Descripción<textarea name="description" maxlength="2000" placeholder="Indicaciones de la rutina">${escapeHtml(series?.description || '')}</textarea></label>
      <div class="form-actions full"><button type="button" class="outline-btn" data-close>Cancelar</button><button class="primary-btn" type="submit">${series ? 'Guardar cambios' : 'Crear serie'}</button></div>
    </form>`);
  $('[data-close]').onclick = closeModal;
  if (!series) {
    $('#series-pet').onchange = () => updateCareTypeOptions($('#series-pet'), $('#series-care-type'));
  }
  const syncFrequencyFields = () => {
    const frequency = $('#series-frequency').value;
    $('#series-interval-field').classList.toggle('hidden', frequency !== 'INTERVAL');
    $('#series-days-field').classList.toggle('hidden', frequency !== 'WEEKLY');
  };
  $('#series-frequency').onchange = syncFrequencyFields;
  $('#add-series-time').onclick = () => {
    if ($('#series-times').querySelectorAll('[data-series-time]').length >= 6) return showToast('Máximo 6 horarios por día.');
    $('#series-times').insertAdjacentHTML('beforeend', seriesTimeRow('12:00'));
  };
  $('#series-times').onclick = (event) => {
    const removeButton = event.target.closest('[data-remove-time]');
    if (!removeButton) return;
    if ($('#series-times').querySelectorAll('[data-series-time]').length <= 1) return showToast('La serie necesita al menos un horario.');
    removeButton.closest('.series-time-row').remove();
  };
  $('#series-form').onsubmit = async (event) => {
    event.preventDefault();
    const data = formData(event.target);
    const times = [...$('#series-times').querySelectorAll('[data-series-time]')].map((input) => input.value);
    if (new Set(times).size !== times.length) return showToast('Los horarios no pueden repetirse.');
    const days = [...$('#series-days').querySelectorAll('input:checked')].map((input) => input.value);
    if (data.frequency === 'WEEKLY' && !days.length) return showToast('Selecciona al menos un día de la semana.');
    const payload = {
      title: data.title, description: data.description, careType: data.careType,
      priority: data.priority, frequency: data.frequency,
      intervalDays: data.frequency === 'INTERVAL' && data.intervalDays ? Number(data.intervalDays) : null,
      daysOfWeek: data.frequency === 'WEEKLY' ? days : null,
      timesOfDay: times, endDate: data.endDate || null,
      clearEndDate: Boolean(series && !data.endDate)
    };
    try {
      if (series) {
        payload.applyFrom = data.applyFrom || today;
        await api(`/api/series/${series.id}`, { method: 'PUT', body: JSON.stringify(payload) });
        showToast('Serie actualizada.');
      } else {
        payload.petId = Number(data.petId);
        payload.startDate = data.startDate;
        await api('/api/series', { method: 'POST', body: JSON.stringify(payload) });
        showToast('Serie creada. Sus ocurrencias ya están en la agenda.');
      }
      closeModal();
      await loadPageData();
    } catch (error) { showToast(error.message); }
  };
}

function renderChannels() {
  const list = $('#channels-list');
  if (!state.channels.length) { list.innerHTML = emptyState('notifications_active', 'No hay canales activos', 'Conecta Telegram o Gmail para recibir los cuidados programados.'); return; }
  const channelName = (type) => type === 'TELEGRAM' ? 'Telegram' : type === 'WHATSAPP' ? 'WhatsApp (manual)' : 'Gmail';
  const channelIcon = (type) => type === 'TELEGRAM' ? 'send' : type === 'WHATSAPP' ? 'chat' : 'mail';
  list.innerHTML = state.channels.map((channel) => `<article class="channel-card"><div class="channel-avatar ${channel.type === 'TELEGRAM' ? 'telegram-avatar' : ''}"><span class="material-symbols-rounded">${channelIcon(channel.type)}</span></div><div><strong>${channelName(channel.type)}</strong><small>${escapeHtml(channel.label || channel.destination)} · ${channel.verified ? 'Verificado' : 'Pendiente'} · Aviso ${channel.reminderMinutesBefore || 0} min antes</small></div><div class="card-actions">${channel.type !== 'WHATSAPP' && channel.verified ? '<button class="small-btn" data-test-channel="' + channel.id + '">Probar</button>' : ''}${channel.type !== 'WHATSAPP' ? '<button class="small-btn" data-edit-channel="' + channel.id + '">Configurar</button>' : ''}<button class="danger-btn" data-delete-channel="${channel.id}" aria-label="Eliminar canal"><span class="material-symbols-rounded">delete</span></button></div></article>`).join('');
  list.querySelectorAll('[data-test-channel]').forEach((button) => { button.onclick = async () => { try { await api(`/api/notifications/channels/${button.dataset.testChannel}/test`, { method: 'POST' }); showToast('Mensaje de prueba enviado.'); } catch (error) { showToast(error.message); } }; });
  list.querySelectorAll('[data-edit-channel]').forEach((button) => { button.onclick = () => openChannelModal(state.channels.find((channel) => channel.id == button.dataset.editChannel)); });
  list.querySelectorAll('[data-delete-channel]').forEach((button) => { button.onclick = async () => { if (!confirm('¿Eliminar este canal?')) return; try { await api(`/api/notifications/channels/${button.dataset.deleteChannel}`, { method: 'DELETE' }); await loadPageData(); showToast('Canal eliminado.'); } catch (error) { showToast(error.message); } }; });
}

async function openGmailLink() {
  try {
    showToast('Preparando la autorización segura de Google...');
    const link = await api('/api/notifications/channels/gmail/link', { method: 'POST' });
    window.location.assign(link.authorizationUrl);
  } catch (error) { showToast(error.message); }
}

async function openTelegramLinkModal() {
  try {
    showToast('Preparando la conexión con Telegram...');
    const link = await api('/api/notifications/channels/telegram/link', { method: 'POST' });
    openModal('Vincular Telegram', `<section class="telegram-link"><span class="material-symbols-rounded telegram-mark" aria-hidden="true">send</span><p>Primero copia el comando. Después abre Telegram Web, pégalo en el chat del bot y envíalo. El código caduca en 15 minutos.</p><div class="link-code"><span>Comando de vinculación</span><strong class="start-command">${escapeHtml(link.startCommand)}</strong><small>Este código solo sirve para vincular tu cuenta de Taskora Pet.</small><button type="button" class="small-btn copy-command" id="copy-telegram-command"><span class="material-symbols-rounded">content_copy</span>Copiar comando</button></div><a id="open-telegram-web" class="primary-btn link-btn" href="${escapeHtml(link.webBotUrl)}" target="_blank" rel="noopener noreferrer"><span class="material-symbols-rounded">open_in_new</span>Abrir Telegram Web</a><div class="form-actions"><button type="button" class="outline-btn" data-close>Cancelar</button><button type="button" class="primary-btn" id="check-telegram-btn">Ya lo envié</button></div></section>`);
    $('[data-close]').onclick = closeModal;
    $('#copy-telegram-command').onclick = async () => { await copyToClipboard(link.startCommand); showToast('Comando copiado. Pégalo en el chat del bot.'); };
    $('#open-telegram-web').onclick = () => { copyToClipboard(link.startCommand); showToast('Comando copiado. Pégalo y envíalo en Telegram.'); };
    $('#check-telegram-btn').onclick = async () => {
      try {
        await api(`/api/notifications/channels/telegram/link/${encodeURIComponent(link.code)}/confirm`, { method: 'POST' });
        state.channels = await api('/api/notifications/channels');
        closeModal(); renderChannels(); showToast('Telegram quedó vinculado correctamente.');
      } catch (error) { showToast(error.message); }
    };
  } catch (error) { showToast(error.message); }
}
function openChannelModal(channel) {
  openModal('Configurar recordatorio', `<form id="channel-form" class="form-grid"><label class="full">Avisar antes<select name="reminderMinutesBefore"><option value="0" ${channel.reminderMinutesBefore === 0 ? 'selected' : ''}>A la hora exacta</option><option value="5" ${channel.reminderMinutesBefore === 5 ? 'selected' : ''}>5 minutos antes</option><option value="10" ${channel.reminderMinutesBefore === 10 ? 'selected' : ''}>10 minutos antes</option><option value="30" ${channel.reminderMinutesBefore === 30 ? 'selected' : ''}>30 minutos antes</option><option value="60" ${channel.reminderMinutesBefore === 60 ? 'selected' : ''}>1 hora antes</option><option value="1440" ${channel.reminderMinutesBefore === 1440 ? 'selected' : ''}>1 día antes</option></select></label><label class="full">Etiqueta<input name="label" value="${escapeHtml(channel.label || '')}" maxlength="100"></label><div class="form-actions full"><button type="button" class="outline-btn" data-close>Cancelar</button><button class="primary-btn" type="submit">Guardar</button></div></form>`);
  $('[data-close]').onclick = closeModal;
  $('#channel-form').onsubmit = async (event) => { event.preventDefault(); const data = formData(event.target); data.reminderMinutesBefore = Number(data.reminderMinutesBefore); try { await api(`/api/notifications/channels/${channel.id}`, { method: 'PUT', body: JSON.stringify(data) }); closeModal(); await loadPageData(); showToast('Recordatorio actualizado.'); } catch (error) { showToast(error.message); } };
}

function renderReports() {
  const report = state.report || { total: 0, pending: 0, overdue: 0, completed: 0, skipped: 0, complianceRate: 0, perPet: [] };
  $('#report-total').textContent = report.total;
  $('#report-completed').textContent = report.completed;
  $('#report-skipped').textContent = report.skipped;
  $('#report-overdue').textContent = report.overdue;
  $('#report-rate').textContent = `${report.complianceRate}%`;
  $('#pet-report-list').innerHTML = report.perPet.length
    ? report.perPet.map((row) => `<article class="report-row"><div><strong>${escapeHtml(row.petName)}</strong><small>${row.complianceRate}% de cumplimiento</small></div><span>${row.pending} pendientes · ${row.overdue} vencidos</span><span>${row.completed} realizados</span><span>${row.skipped} no realizados</span></article>`).join('')
    : emptyState('assessment', 'Sin datos para reportar', 'Registra cuidados o ajusta los filtros para generar indicadores.');
  $('#export-report-btn').onclick = exportReport;
}

async function exportReport() {
  try {
    const response = await fetch(`/api/reports/export${filterQuery()}`, { headers: { Authorization: `Bearer ${state.token}` } });
    if (!response.ok) throw new Error('No fue posible generar el reporte.');
    const blob = await response.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'taskora-pet-reporte.csv';
    link.click();
    URL.revokeObjectURL(link.href);
    showToast('Reporte exportado con los filtros aplicados.');
  } catch (error) { showToast(error.message); }
}

function renderProfile() {
  const form = $('#profile-form'); form.elements.fullName.value = state.profile.fullName; form.elements.email.value = state.profile.email; form.elements.phoneNumber.value = state.profile.phoneNumber || '';
  form.onsubmit = async (event) => { event.preventDefault(); try { state.profile = await api('/users/me', { method: 'PUT', body: JSON.stringify(formData(event.target)) }); showToast('Perfil actualizado.'); } catch (error) { showToast(error.message); } };
  $('#delete-profile-btn').onclick = async () => { if (!confirm('¿Seguro que deseas desactivar tu cuenta?')) return; try { await api('/users/me', { method: 'DELETE' }); logout(); } catch (error) { showToast(error.message); } };
}

document.addEventListener('DOMContentLoaded', async () => {
  if (!state.token) { window.location.replace('/index.html'); return; }
  renderShell();
  $('#modal-close').onclick = closeModal;
  $('#modal').onclick = (event) => { if (event.target.id === 'modal') closeModal(); };
  document.addEventListener('keydown', (event) => { if (event.key === 'Escape' && !$('#modal').classList.contains('hidden')) closeModal(); });
  try {
    await loadPageData();
    if (page === 'notifications') {
      const gmailStatus = new URLSearchParams(window.location.search).get('gmail');
      if (gmailStatus === 'connected') showToast('Gmail quedó conectado correctamente.');
      if (gmailStatus === 'cancelled') showToast('La conexión con Gmail fue cancelada.');
      if (gmailStatus === 'error') showToast('No fue posible completar la conexión con Gmail.');
      if (gmailStatus) window.history.replaceState({}, '', '/notifications.html');
    }
  } catch (error) { showToast(error.message); }
});
