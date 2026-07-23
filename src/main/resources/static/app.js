const token = localStorage.getItem('taskora_token');
const $ = (selector) => document.querySelector(selector);
let toastTimer;

function showToast(message) {
  const toast = $('#toast');
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add('show');
  toastTimer = window.setTimeout(() => toast.classList.remove('show'), 3200);
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(options.headers || {}) }
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.message || 'No fue posible completar la operación.');
  return data;
}

function formData(form) { return Object.fromEntries(new FormData(form).entries()); }

document.addEventListener('DOMContentLoaded', () => {
  if (token) {
    const role = localStorage.getItem('taskora_role') || readTokenRole(token);
    window.location.replace(role === 'ADMIN' ? '/admin.html' : '/dashboard.html');
    return;
  }
  document.querySelectorAll('[data-auth-tab]').forEach((tab) => {
    tab.onclick = () => {
      const register = tab.dataset.authTab === 'register';
      document.querySelectorAll('[data-auth-tab]').forEach((item) => {
        const selected = item === tab;
        item.classList.toggle('active', selected);
        item.setAttribute('aria-selected', String(selected));
      });
      $('#login-form').classList.toggle('hidden', register);
      $('#register-form').classList.toggle('hidden', !register);
      $('#auth-helper').textContent = register ? 'Crea un espacio para organizar el bienestar de tus mascotas.' : 'Ingresa para revisar la rutina de tus compañeros.';
    };
  });
  $('#login-form').onsubmit = async (event) => {
    event.preventDefault();
    try {
      const data = await api('/auth/login', { method: 'POST', body: JSON.stringify(formData(event.target)) });
      localStorage.setItem('taskora_token', data.accessToken);
      localStorage.setItem('taskora_role', data.role);
      window.location.replace(data.redirectTo || '/dashboard.html');
    } catch (error) { showToast(error.message); }
  };
  $('#register-form').onsubmit = async (event) => {
    event.preventDefault();
    try {
      await api('/auth/register', { method: 'POST', body: JSON.stringify(formData(event.target)) });
      showToast('Cuenta creada. Ya puedes iniciar sesión.');
      $('#login-tab').click();
      event.target.reset();
    } catch (error) { showToast(error.message); }
  };
  $('#forgot-link').onclick = () => {
    $('#login-form').classList.add('hidden');
    $('#forgot-form').classList.remove('hidden');
    $('#auth-helper').textContent = 'Recupera el acceso a tu cuenta en dos pasos.';
  };
  $('#back-to-login').onclick = () => {
    $('#forgot-form').classList.add('hidden');
    $('#login-form').classList.remove('hidden');
    $('#auth-helper').textContent = 'Ingresa para revisar la rutina de tus compañeros.';
  };
  $('#forgot-form').onsubmit = async (event) => {
    event.preventDefault();
    try {
      const data = await api('/auth/forgot-password', { method: 'POST', body: JSON.stringify(formData(event.target)) });
      showToast(data.message || 'Si el correo está registrado, recibirás un enlace.');
      event.target.reset();
    } catch (error) { showToast(error.message); }
  };
});

function readTokenRole(value) {
  try {
    return JSON.parse(atob(value.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).role;
  } catch (_) {
    return 'USER';
  }
}
