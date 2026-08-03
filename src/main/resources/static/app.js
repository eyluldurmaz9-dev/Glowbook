const state = {
    token: null,
    role: null,
    customerId: null,
    employeeId: null,
    fullName: null,
    selectedServiceId: null,
    selectedOptionId: null,
    selectedSlot: null,
    selectedPackageId: null,
};

const api = {
    base: '/api',
    catalog: '/api/catalog',
    auth: '/api/auth',
    appointments: '/api/appointments'
};

const dom = {
    statusMessage: document.getElementById('status-message'),
    showLogin: document.getElementById('show-login'),
    showRegister: document.getElementById('show-register'),
    guestBooking: document.getElementById('guest-booking'),
    loginForm: document.getElementById('login-form'),
    registerForm: document.getElementById('register-form'),
    loggedInPanel: document.getElementById('logged-in-panel'),
    signedInName: document.getElementById('signed-in-name'),
    logoutButton: document.getElementById('logout-button'),
    bookingPanel: document.querySelector('.booking-panel'),
    serviceSelect: document.getElementById('service-select'),
    optionSelect: document.getElementById('option-select'),
    appointmentDate: document.getElementById('appointment-date'),
    loadSlotsButton: document.getElementById('load-slots'),
    slotList: document.getElementById('slots'),
    customerInfo: document.getElementById('customer-info'),
    packageSection: document.getElementById('package-section'),
    packageSelect: document.getElementById('package-select'),
    customerFirstname: document.getElementById('customer-firstname'),
    customerLastname: document.getElementById('customer-lastname'),
    customerPhone: document.getElementById('customer-phone'),
    bookAppointmentButton: document.getElementById('book-appointment'),
    resultPanel: document.querySelector('.result-panel'),
    resultOutput: document.getElementById('result-output')
};

function init() {
    bindActions();
    loadServices();
    const stored = localStorage.getItem('glowbookUser');
    if (stored) {
        const parsed = JSON.parse(stored);
        state.token = parsed.token;
        state.role = parsed.role;
        state.customerId = parsed.customerId;
        state.employeeId = parsed.employeeId;
        state.fullName = parsed.fullName;
        showLoggedIn();
    }
}

function bindActions() {
    dom.showLogin.addEventListener('click', () => toggleForms('login'));
    dom.showRegister.addEventListener('click', () => toggleForms('register'));
    dom.guestBooking.addEventListener('click', () => {
        state.token = null;
        state.customerId = null;
        state.role = null;
        state.fullName = null;
        updateStatus('Guest booking mode enabled');
        showBookingPanel();
    });
    dom.logoutButton.addEventListener('click', logout);
    dom.loginForm.addEventListener('submit', async event => {
        event.preventDefault();
        await login();
    });
    dom.registerForm.addEventListener('submit', async event => {
        event.preventDefault();
        await register();
    });
    dom.serviceSelect.addEventListener('change', onServiceChange);
    dom.loadSlotsButton.addEventListener('click', loadSlots);
    dom.bookAppointmentButton.addEventListener('click', bookAppointment);
}

function toggleForms(formType) {
    dom.loginForm.classList.toggle('hidden', formType !== 'login');
    dom.registerForm.classList.toggle('hidden', formType !== 'register');
    dom.bookAppointmentButton.textContent = 'Book Appointment';
    dom.resultPanel.classList.add('hidden');
    showBookingPanel(false);
}

function showLoggedIn() {
    dom.loggedInPanel.classList.remove('hidden');
    dom.loginForm.classList.add('hidden');
    dom.registerForm.classList.add('hidden');
    dom.bookingPanel.classList.remove('hidden');
    dom.signedInName.textContent = state.fullName;
    dom.packageSection.classList.remove('hidden');
    updateStatus(`Signed in as ${state.fullName}`);
    loadPackages();
}

function showBookingPanel(show = true) {
    dom.bookingPanel.classList.toggle('hidden', !show);
    dom.packageSection.classList.toggle('hidden', state.token == null ? false : !dom.packageSection.classList.contains('hidden'));
}

async function loadServices() {
    try {
        const response = await fetch(`${api.catalog}/services`);
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Could not load services');
        }

        dom.serviceSelect.innerHTML = '<option value="">Select service</option>';
        body.data.forEach(service => {
            const option = document.createElement('option');
            option.value = service.serviceId;
            option.textContent = service.serviceName;
            dom.serviceSelect.appendChild(option);
        });
    } catch (error) {
        updateStatus('Could not load services');
    }
}

async function onServiceChange() {
    const serviceId = dom.serviceSelect.value;
    state.selectedServiceId = serviceId || null;
    dom.optionSelect.innerHTML = '<option value="">Select option</option>';
    if (!serviceId) {
        dom.optionSelect.disabled = true;
        return;
    }

    await loadOptions(serviceId);
    if (state.token && state.customerId) {
        loadPackages();
    }
}

async function loadOptions(serviceId) {
    try {
        const response = await fetch(`${api.catalog}/services/${serviceId}/options`);
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Could not load options');
        }

        dom.optionSelect.innerHTML = '<option value="">Select option</option>';
        body.data.forEach(option => {
            const item = document.createElement('option');
            item.value = option.optionId;
            item.textContent = `${option.optionName} - ${formatCurrency(option.price)}`;
            dom.optionSelect.appendChild(item);
        });
        dom.optionSelect.disabled = false;
    } catch (error) {
        updateStatus('Could not load options');
    }
}

async function loadPackages() {
    if (!state.customerId) {
        return;
    }

    try {
        const response = await fetch(`${api.base}/customers/${state.customerId}/packages`, {
            headers: authHeader()
        });
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Could not load packages');
        }

        dom.packageSelect.innerHTML = '<option value="">No package</option>';
        body.data.forEach(pkg => {
            const item = document.createElement('option');
            item.value = pkg.customerPackageId;
            item.textContent = `${pkg.packageName} (${pkg.remainingSession} remaining)`;
            dom.packageSelect.appendChild(item);
        });
        dom.packageSection.classList.remove('hidden');
    } catch (error) {
        updateStatus('Could not load packages');
    }
}

async function loadSlots() {
    const serviceId = dom.serviceSelect.value;
    const date = dom.appointmentDate.value;
    if (!serviceId || !date) {
        return updateStatus('Please select service and date first');
    }

    try {
        const response = await fetch(`${api.appointments}/available-slots?serviceId=${serviceId}&date=${date}`);
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Could not load available slots');
        }

        renderSlots(body.data);
    } catch (error) {
        updateStatus('Could not load available slots');
    }
}

function renderSlots(slots) {
    dom.slotList.innerHTML = '';
    if (!slots.length) {
        dom.slotList.textContent = 'No available slots for selected date.';
        return;
    }

    slots.forEach(slot => {
        const card = document.createElement('div');
        card.className = 'slot-item';
        const left = document.createElement('div');
        const title = document.createElement('div');
        title.textContent = `${slot.employeeName} (${slot.employeeId})`;
        const date = document.createElement('div');
        date.textContent = `Date: ${slot.appointmentDate}`;
        left.appendChild(title);
        left.appendChild(date);

        const right = document.createElement('div');
        slot.availableTimes.forEach(time => {
            const button = document.createElement('button');
            button.textContent = time;
            button.addEventListener('click', () => selectSlot(slot.employeeId, time));
            right.appendChild(button);
        });
        card.appendChild(left);
        card.appendChild(right);
        dom.slotList.appendChild(card);
    });
}

function selectSlot(employeeId, time) {
    state.selectedSlot = {employeeId, time};
    updateStatus(`Selected slot: ${employeeId} at ${time}`);
}

async function bookAppointment() {
    const serviceId = dom.serviceSelect.value;
    const optionId = dom.optionSelect.value;
    const date = dom.appointmentDate.value;
    const slot = state.selectedSlot;
    if (!serviceId || !optionId || !date || !slot) {
        return updateStatus('Please select a service, option, date, and time slot');
    }

    const payload = {
        employeeId: slot.employeeId,
        serviceId: Number(serviceId),
        optionId: Number(optionId),
        appointmentDate: date,
        appointmentTime: slot.time,
        customerPackageId: dom.packageSelect.value ? Number(dom.packageSelect.value) : null,
        customerName: dom.customerFirstname.value.trim(),
        customerSurname: dom.customerLastname.value.trim(),
        phone: dom.customerPhone.value.trim()
    };

    const missingGuest = !state.customerId && (!payload.customerName || !payload.customerSurname || !payload.phone);
    if (missingGuest) {
        return updateStatus('Guest booking requires name, surname, and phone');
    }

    if (state.customerId) {
        payload.customerId = state.customerId;
        payload.customerName = payload.customerName || undefined;
        payload.customerSurname = payload.customerSurname || undefined;
        payload.phone = payload.phone || undefined;
    }

    try {
        const response = await fetch(api.appointments, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...authHeader()
            },
            body: JSON.stringify(payload)
        });
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Booking failed');
        }
        showResult(JSON.stringify(body.data, null, 2));
        updateStatus('Booking completed successfully');
    } catch (error) {
        updateStatus('Booking failed due to network error');
    }
}

async function login() {
    const phone = document.getElementById('login-phone').value.trim();
    const password = document.getElementById('login-password').value.trim();
    if (!phone || !password) {
        return updateStatus('Phone and password are required');
    }

    try {
        const response = await fetch(`${api.auth}/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({username: phone, password, role: 'CUSTOMER'})
        });
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Login failed');
        }
        state.token = body.data.token;
        state.role = body.data.role;
        state.customerId = body.data.customerId;
        state.fullName = body.data.fullName;
        localStorage.setItem('glowbookUser', JSON.stringify({
            token: state.token,
            role: state.role,
            customerId: state.customerId,
            fullName: state.fullName
        }));
        showLoggedIn();
    } catch (error) {
        updateStatus('Login failed');
    }
}

async function register() {
    const firstName = document.getElementById('register-firstname').value.trim();
    const lastName = document.getElementById('register-lastname').value.trim();
    const phone = document.getElementById('register-phone').value.trim();
    const password = document.getElementById('register-password').value.trim();
    const email = document.getElementById('register-email').value.trim();

    if (!firstName || !lastName || !phone || !password) {
        return updateStatus('Complete all required registration fields');
    }

    try {
        const response = await fetch(`${api.auth}/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({firstName, lastName, phone, password, email})
        });
        const body = await response.json();
        if (!body.success) {
            return updateStatus(body.message || 'Registration failed');
        }

        state.token = body.data.token;
        state.role = body.data.role;
        state.customerId = body.data.customerId;
        state.fullName = body.data.fullName;
        localStorage.setItem('glowbookUser', JSON.stringify({
            token: state.token,
            role: state.role,
            customerId: state.customerId,
            fullName: state.fullName
        }));
        showLoggedIn();
    } catch (error) {
        updateStatus('Registration failed');
    }
}

function logout() {
    state.token = null;
    state.role = null;
    state.customerId = null;
    state.employeeId = null;
    state.fullName = null;
    localStorage.removeItem('glowbookUser');
    dom.loggedInPanel.classList.add('hidden');
    toggleForms('login');
    updateStatus('Logged out');
}

function authHeader() {
    return state.token ? {Authorization: `Bearer ${state.token}`} : {};
}

function formatCurrency(value) {
    return !value ? '' : `${parseFloat(value).toFixed(2)} ₺`;
}

function updateStatus(message) {
    dom.statusMessage.textContent = message;
}

function showResult(text) {
    dom.resultOutput.textContent = text;
    dom.resultPanel.classList.remove('hidden');
}

window.addEventListener('DOMContentLoaded', init);
