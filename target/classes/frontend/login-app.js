class NetflixApp {
    constructor() {
        this.apiBaseUrl = window.netflixApiBaseUrl || '/api';
        this.currentUser = null;
        this.profiles = [];
        this.pendingProfile = null;
        this.genreOptions = [];
        this.selectedGenres = new Set();
    this.currentSubscription = null;
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.showAuthForm('login');
        this.showPage('auth-page');
    }

    setupEventListeners() {
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (event) => {
                event.preventDefault();
                this.handleLogin();
            });
        }

        const adminLoginForm = document.getElementById('adminLoginForm');
        if (adminLoginForm) {
            adminLoginForm.addEventListener('submit', (event) => {
                event.preventDefault();
                this.handleAdminLogin();
            });
        }

        const registerForm = document.getElementById('registerForm');
        if (registerForm) {
            registerForm.addEventListener('submit', (event) => {
                event.preventDefault();
                this.handleRegister();
            });
        }

        const showRegister = document.getElementById('showRegister');
        if (showRegister) {
            showRegister.addEventListener('click', (event) => {
                event.preventDefault();
                this.showAuthForm('register');
            });
        }

        const showLogin = document.getElementById('showLogin');
        if (showLogin) {
            showLogin.addEventListener('click', (event) => {
                event.preventDefault();
                this.showAuthForm('login');
            });
        }

        const adminLoginLink = document.getElementById('adminLoginLink');
        if (adminLoginLink) {
            adminLoginLink.addEventListener('click', (event) => {
                event.preventDefault();
                this.showAuthForm('admin-login');
            });
        }

        const backToUserLogin = document.getElementById('backToUserLogin');
        if (backToUserLogin) {
            backToUserLogin.addEventListener('click', (event) => {
                event.preventDefault();
                this.showAuthForm('login');
            });
        }

        const saveGenrePreferences = document.getElementById('saveGenrePreferences');
        if (saveGenrePreferences) {
            saveGenrePreferences.addEventListener('click', () => this.saveUserGenrePreferences());
        }

        const skipGenrePreferences = document.getElementById('skipGenrePreferences');
        if (skipGenrePreferences) {
            skipGenrePreferences.addEventListener('click', () => this.skipGenreSelection());
        }
    }

    showAuthForm(formKey) {
        document.querySelectorAll('.auth-form').forEach((form) => {
            form.classList.add('d-none');
            form.classList.remove('active');
        });

        const target = document.getElementById(`${formKey}-form`);
        if (target) {
            target.classList.remove('d-none');
            target.classList.add('active');
        }
    }

    showPage(pageId) {
        document.querySelectorAll('.page').forEach((page) => {
            page.classList.add('d-none');
            page.classList.remove('active');
        });

        const target = document.getElementById(pageId);
        if (target) {
            target.classList.remove('d-none');
            target.classList.add('active');
        }
    }

    async handleLogin() {
        const email = document.getElementById('loginEmail')?.value.trim();
        const password = document.getElementById('loginPassword')?.value.trim();

        if (!email || !password) {
            this.showToast('Please fill in all fields', 'error');
            return;
        }

        try {
            const response = await this.postJson('/users/login', { email, password });
            const { user, profiles = [], subscription = null } = response || {};

            await this.persistUser(user, subscription);

            this.currentSubscription = subscription || null;
            this.profiles = Array.isArray(profiles) ? profiles : [];

            if (!this.profiles.length) {
                const fallbackName = user.username || (user.email ? user.email.split('@')[0] : 'Profile');
                const defaultProfile = await this.createProfile(user.userId, fallbackName);
                this.profiles = defaultProfile ? [defaultProfile] : [];
            }

            this.showToast('Login successful!', 'success');
            this.showProfileSelectionPage();
        } catch (error) {
            this.showToast(error.message || 'Unable to login', 'error');
        }
    }

    async handleAdminLogin() {
        const username = document.getElementById('adminUsername')?.value.trim();
        const password = document.getElementById('adminPassword')?.value.trim();

        if (!username || !password) {
            this.showToast('Please fill in all fields', 'error');
            return;
        }

        try {
            await this.postJson('/admin/login', { username, password });
            localStorage.setItem('isAdmin', 'true');
            this.showToast('Admin login successful!', 'success');
            setTimeout(() => {
                window.location.href = 'admin.html';
            }, 800);
        } catch (error) {
            this.showToast(error.message || 'Invalid admin credentials', 'error');
        }
    }

    async handleRegister() {
        const email = document.getElementById('registerEmail')?.value.trim();
        const username = document.getElementById('registerUsername')?.value.trim();
        const password = document.getElementById('registerPassword')?.value.trim();
        const dateOfBirth = document.getElementById('registerDateOfBirth')?.value;

        if (!email || !username || !password || !dateOfBirth) {
            this.showToast('Please fill in all fields', 'error');
            return;
        }

        try {
            const response = await this.postJson('/users/register', {
                email,
                username,
                password,
                dateOfBirth
            });

            const user = response?.user;
            await this.persistUser(user);

            const defaultProfile = await this.createProfile(
                user.userId,
                username || (email ? email.split('@')[0] : 'Profile')
            );

            this.profiles = defaultProfile ? [defaultProfile] : [];
            this.pendingProfile = defaultProfile || null;

            this.showToast('Registration successful!', 'success');

            await this.prepareGenreSelection();
            this.showGenreSelectionPage();
        } catch (error) {
            this.showToast(error.message || 'Registration failed', 'error');
        }
    }

    async persistUser(user, subscription = undefined) {
        if (!user) {
            throw new Error('User information missing from response');
        }
        this.currentUser = user;
        localStorage.setItem('netflixUser', JSON.stringify(user));
        localStorage.removeItem('selectedProfile');
        await this.refreshSubscription(user.userId, subscription);
    }

    async refreshSubscription(userId, preloaded = undefined) {
        if (!userId) {
            this.currentSubscription = null;
            localStorage.removeItem('netflixSubscription');
            return null;
        }

        let subscriptionData = preloaded;
        if (subscriptionData === undefined) {
            try {
                const response = await this.getJson(`/subscriptions/user/${userId}`);
                subscriptionData = response?.activeSubscription ?? null;
            } catch (error) {
                console.error('Failed to refresh subscription', error);
                this.showToast('Unable to refresh subscription status', 'error');
                subscriptionData = null;
            }
        }

        this.currentSubscription = subscriptionData || null;
        if (this.currentSubscription) {
            localStorage.setItem('netflixSubscription', JSON.stringify(this.currentSubscription));
        } else {
            localStorage.removeItem('netflixSubscription');
        }
        return this.currentSubscription;
    }

    async showProfileSelectionPage() {
        if (!this.currentUser) {
            this.showToast('Please log in first', 'error');
            return;
        }

        if (!this.profiles.length) {
            const profiles = await this.getJson(`/profiles/user/${this.currentUser.userId}`);
            this.profiles = Array.isArray(profiles) ? profiles : [];
        }

        this.showPage('profile-page');
        this.renderProfileCards();

        const addProfileBtn = document.getElementById('addProfileBtn');
        if (addProfileBtn) {
            addProfileBtn.onclick = () => this.promptAddProfile();
        }
    }

    renderProfileCards() {
        const profilesGrid = document.getElementById('profilesGrid');
        if (!profilesGrid) {
            return;
        }

        if (!this.profiles.length) {
            profilesGrid.innerHTML = '<p class="text-muted">No profiles yet. Create one to get started.</p>';
            return;
        }

        profilesGrid.innerHTML = this.profiles.map((profile) => `
            <div class="col-6 col-md-4">
                <div class="profile-card text-center p-4 cursor-pointer" data-profile-id="${profile.profileId}" style="transition: all 0.3s;">
                    <div class="profile-avatar mx-auto mb-3 d-flex align-items-center justify-content-center" style="width: 100px; height: 100px; background: linear-gradient(135deg, #e50914, #b20710); border-radius: 12px; font-size: 2rem; font-weight: bold;">
                        ${profile.profileName?.charAt(0)?.toUpperCase() || 'N'}
                    </div>
                    <h5>${profile.profileName}</h5>
                </div>
            </div>
        `).join('');

        profilesGrid.querySelectorAll('.profile-card').forEach((card) => {
            card.addEventListener('click', () => {
                const profileId = Number(card.getAttribute('data-profile-id'));
                this.handleProfileSelection(profileId);
            });
        });
    }

    async handleProfileSelection(profileId) {
        const profile = this.profiles.find((p) => p.profileId === profileId);
        if (!profile) {
            this.showToast('Profile not found', 'error');
            return;
        }

        try {
            const hasPreferences = await this.hasGenrePreferences(profile.profileId);
            if (hasPreferences) {
                this.completeProfileSelection(profile);
            } else {
                this.pendingProfile = profile;
                this.selectedGenres.clear();
                await this.prepareGenreSelection();
                this.showGenreSelectionPage();
            }
        } catch (error) {
            this.showToast(error.message || 'Unable to verify preferences', 'error');
        }
    }

    async promptAddProfile() {
        if (!this.currentUser) {
            this.showToast('Please log in first', 'error');
            return;
        }

        const profileName = window.prompt('Enter a profile name:');
        if (!profileName) {
            return;
        }

        try {
            const profile = await this.createProfile(this.currentUser.userId, profileName.trim());
            if (profile) {
                this.profiles.push(profile);
                this.renderProfileCards();
                this.showToast('Profile created', 'success');
            }
        } catch (error) {
            this.showToast(error.message || 'Unable to create profile', 'error');
        }
    }

    async createProfile(userId, profileName, maturityRatingOverride = null) {
        const response = await this.postJson('/profiles', {
            userId,
            profileName,
            maturityRatingOverride
        });

        const profile = response?.profile;
        if (!profile) {
            throw new Error(response?.message || 'Failed to create profile');
        }
        return profile;
    }

    async hasGenrePreferences(profileId) {
        const response = await this.getJson(`/recommendations/preferences/${profileId}`);
        if (!response) {
            return false;
        }
        if (typeof response.hasPreferences === 'boolean') {
            return response.hasPreferences;
        }
        const ids = Array.isArray(response.genreIds) ? response.genreIds : [];
        return ids.length > 0;
    }

    async prepareGenreSelection() {
        if (!this.genreOptions.length) {
            const response = await this.getJson('/genres');
            this.genreOptions = Array.isArray(response)
                ? response.map((item) => ({
                    id: item.genre_id ?? item.genreId ?? item.id,
                    name: item.name
                })).filter((item) => item.id && item.name)
                : [];
        }

        if (!this.genreOptions.length) {
            this.showToast('No genres available. Please add genres in the admin portal.', 'error');
        }
    }

    showGenreSelectionPage() {
        this.showPage('genre-selection-page');
        this.populateGenreSelection();
        const continueBtn = document.getElementById('saveGenrePreferences');
        if (continueBtn) {
            continueBtn.disabled = this.selectedGenres.size === 0;
        }
    }

    populateGenreSelection() {
        const genreGrid = document.getElementById('genreGrid');
        if (!genreGrid) {
            return;
        }

        if (!this.genreOptions.length) {
            genreGrid.innerHTML = '<p class="text-muted">No genres found in the database.</p>';
            return;
        }

        genreGrid.innerHTML = this.genreOptions.map((genre) => `
            <div class="col-6 col-md-4 col-lg-3">
                <div class="genre-card p-4 border rounded-3 text-center h-100 cursor-pointer ${this.selectedGenres.has(String(genre.id)) ? 'selected' : ''}"
                     data-genre-id="${genre.id}"
                     style="background: rgba(255,255,255,0.1); border-color: rgba(255,255,255,0.2) !important; transition: all 0.3s;">
                    <i class="fas fa-film mb-3" style="font-size: 2rem; color: #e50914;"></i>
                    <h5>${genre.name}</h5>
                </div>
            </div>
        `).join('');

        genreGrid.querySelectorAll('.genre-card').forEach((card) => {
            card.addEventListener('click', () => {
                const id = card.getAttribute('data-genre-id');
                if (this.selectedGenres.has(id)) {
                    this.selectedGenres.delete(id);
                    card.classList.remove('selected');
                } else {
                    this.selectedGenres.add(id);
                    card.classList.add('selected');
                }

                const continueBtn = document.getElementById('saveGenrePreferences');
                if (continueBtn) {
                    continueBtn.disabled = this.selectedGenres.size === 0;
                }
            });
        });
    }

    async saveUserGenrePreferences() {
        if (!this.pendingProfile) {
            this.showToast('Select a profile first', 'error');
            return;
        }

        if (!this.selectedGenres.size) {
            this.showToast('Please choose at least one genre', 'error');
            return;
        }

        const genreIds = Array.from(this.selectedGenres).map((id) => Number(id));

        try {
            await this.postJson(`/recommendations/preferences/${this.pendingProfile.profileId}`, {
                genreIds
            });

            this.showToast('Preferences saved!', 'success');
            this.completeProfileSelection(this.pendingProfile);
        } catch (error) {
            this.showToast(error.message || 'Unable to save preferences', 'error');
        }
    }

    skipGenreSelection() {
        if (!this.pendingProfile) {
            this.showToast('Select a profile first', 'error');
            return;
        }
        this.showToast('Skipped personalization – you can update it later.', 'info');
        this.completeProfileSelection(this.pendingProfile);
    }

    completeProfileSelection(profile) {
        if (!profile) {
            return;
        }

        const maturityOverride = profile.maturityRatingOverride ?? profile.maturity_rating_override ?? null;
        const normalizedOverride = typeof maturityOverride === 'string' && maturityOverride.trim()
            ? maturityOverride.trim().toUpperCase()
            : null;
        const normalizedProfile = {
            ...profile,
            maturityRatingOverride: normalizedOverride,
            maturity_rating_override: normalizedOverride
        };

        localStorage.setItem('selectedProfile', JSON.stringify(normalizedProfile));
        this.selectedGenres.clear();
        this.pendingProfile = null;

        this.showToast(`Welcome back, ${profile.profileName}!`, 'success');
        setTimeout(() => {
            window.location.href = '02-user-dashboard.html';
        }, 600);
    }

    async getJson(path) {
        const response = await fetch(this.buildUrl(path), {
            method: 'GET',
            headers: {
                'Accept': 'application/json'
            }
        });
        return this.handleJsonResponse(response);
    }

    async postJson(path, payload, options = {}) {
        const response = await fetch(this.buildUrl(path), {
            method: options.method || 'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            body: payload !== undefined ? JSON.stringify(payload) : undefined
        });
        return this.handleJsonResponse(response);
    }

    buildUrl(path) {
        if (!path) {
            return this.apiBaseUrl;
        }
        if (path.startsWith('http://') || path.startsWith('https://')) {
            return path;
        }
        return `${this.apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`;
    }

    async handleJsonResponse(response) {
        if (response.ok) {
            const contentType = response.headers.get('Content-Type') || '';
            if (contentType.includes('application/json')) {
                return response.json();
            }
            return null;
        }

        const message = await this.parseError(response);
        throw new Error(message || `Request failed with status ${response.status}`);
    }

    async parseError(response) {
        try {
            const data = await response.json();
            if (typeof data === 'string') {
                return data;
            }
            if (data?.message) {
                return data.message;
            }
            return JSON.stringify(data);
        } catch (error) {
            return response.statusText || 'Unknown error';
        }
    }

    showToast(message, type = 'success') {
        const toastElement = document.getElementById('liveToast');
        const toastMessage = document.getElementById('toastMessage');

        if (!toastElement || !toastMessage || !window.bootstrap) {
            console.log(`[${type}] ${message}`);
            return;
        }

        toastElement.className = `toast align-items-center text-white ${type === 'error' ? 'bg-danger' : type === 'info' ? 'bg-info' : 'bg-success'}`;
        toastMessage.textContent = message;

        const toast = bootstrap.Toast.getOrCreateInstance(toastElement);
        toast.show();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.netflixApp = new NetflixApp();
});