// Enhanced Netflix User Management Application - Connected to MySQL Backend via REST API
// Backend API Base URL
const API_BASE_URL = 'http://localhost:8082/api';

class NetflixApp {
    constructor() {
        this.currentUser = null;
        this.currentProfile = null;
        this.currentPage = 'auth';
        this.currentSection = 'home';
        this.currentAdminSection = 'dashboard';
        this.searchActive = false;
        this.currentModalTitle = null;
        this.selectedGenres = new Set();
        this.isAdmin = false;
        this.charts = {};
        this.currentFilters = {};
        this.currentTitlePage = 1;
        this.itemsPerPage = 10;

        // Cache for loaded data from backend
        this.cache = {
            titles: [],
            genres: [],
            countries: [],
            actors: [],
            directors: [],
            ratings: [],
            watchlist: [],
            watchHistory: [],
            userRatings: [],
            recommendations: [],
            genrePreferences: {},
            filterCounts: {}
        };

        this.init();
    }

    init() {
        console.log('Enhanced Netflix app initializing with backend API...');
        this.setupEventListeners();
        this.showPage('auth');
    }

    // ===================== API HELPER METHODS =====================

    async apiCall(endpoint, options = {}) {
        const url = `${API_BASE_URL}${endpoint}`;
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
            },
            ...options
        };

        try {
            console.log(`API Call: ${options.method || 'GET'} ${url}`);
            const response = await fetch(url, defaultOptions);

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP ${response.status}: ${response.statusText}`);
            }

            return await response.json();
        } catch (error) {
            console.error(`API Error [${endpoint}]:`, error);
            throw error;
        }
    }

    // ===================== AUTHENTICATION METHODS =====================

    setupEventListeners() {
        console.log('Setting up event listeners...');

        setTimeout(() => {
            this.bindAuthEvents();
            this.bindAdminEvents();
            this.bindUserEvents();
            this.bindModalEvents();
            this.bindUtilityEvents();
        }, 100);

        console.log('Event listeners setup complete');
    }

    bindAuthEvents() {
        const loginForm = document.getElementById('loginForm');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleLogin();
            });
        }

        const adminLoginForm = document.getElementById('adminLoginForm');
        if (adminLoginForm) {
            adminLoginForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleAdminLogin();
            });
        }

        const registerForm = document.getElementById('registerForm');
        if (registerForm) {
            registerForm.addEventListener('submit', (e) => {
                e.preventDefault();
                this.handleRegister();
            });
        }

        const showRegister = document.getElementById('showRegister');
        if (showRegister) {
            showRegister.addEventListener('click', (e) => {
                e.preventDefault();
                this.showAuthForm('register');
            });
        }

        const showLogin = document.getElementById('showLogin');
        if (showLogin) {
            showLogin.addEventListener('click', (e) => {
                e.preventDefault();
                this.showAuthForm('login');
            });
        }

        const adminLoginLink = document.getElementById('adminLoginLink');
        if (adminLoginLink) {
            adminLoginLink.addEventListener('click', (e) => {
                e.preventDefault();
                this.showAuthForm('admin-login');
            });
        }

        const backToUserLogin = document.getElementById('backToUserLogin');
        if (backToUserLogin) {
            backToUserLogin.addEventListener('click', (e) => {
                e.preventDefault();
                this.showAuthForm('login');
            });
        }
    }

    async handleLogin() {
        const email = document.getElementById('loginEmail').value.trim();
        const password = document.getElementById('loginPassword').value;

        if (!email || !password) {
            this.showToast('Please fill in all fields', 'error');
            return;
        }

        try {
            const response = await this.apiCall('/users/login', {
                method: 'POST',
                body: JSON.stringify({ email, password })
            });

            if (response.success) {
                this.currentUser = response.user;
                const profiles = response.profiles || [];

                if (profiles.length === 0) {
                    this.showToast('No profiles found. Please contact support.', 'error');
                    return;
                }

                this.showProfileSelection(profiles);
                this.showToast('Login successful!', 'success');
            } else {
                this.showToast(response.message || 'Login failed', 'error');
            }
        } catch (error) {
            this.showToast(error.message || 'Login failed. Please try again.', 'error');
        }
    }

    async handleRegister() {
        const email = document.getElementById('registerEmail').value.trim();
        const username = document.getElementById('registerUsername').value.trim();
        const password = document.getElementById('registerPassword').value;
        const confirmPassword = document.getElementById('registerConfirmPassword').value;
        const dob = document.getElementById('registerDateOfBirth').value; 

        if (!email || !username || !password || !confirmPassword) {
            this.showToast('Please fill in all fields', 'error');
            return;
        }

        if (password !== confirmPassword) {
            this.showToast('Passwords do not match', 'error');
            return;
        }

        if (password.length < 6) {
            this.showToast('Password must be at least 6 characters', 'error');
            return;
        }

        try {
            const response = await this.apiCall('/users/register', {
                method: 'POST',
                body: JSON.stringify({
                    email,
                    username,
                    password,
                    dateOfBirth: dob || null
                })
            });

            if (response.success) {
                this.showToast('Registration successful! Please login.', 'success');
                this.showAuthForm('login');
                document.getElementById('registerForm').reset();
            } else {
                this.showToast(response.message || 'Registration failed', 'error');
            }
        } catch (error) {
            this.showToast(error.message || 'Registration failed. Please try again.', 'error');
        }
    }

    async handleAdminLogin() {
        const username = document.getElementById('adminUsername').value.trim();
        const password = document.getElementById('adminPassword').value;

        if (!username || !password) {
            this.showToast('Please fill in all fields', 'error');
            return;
        }

        try {
            const response = await this.apiCall('/admin/login', {
                method: 'POST',
                body: JSON.stringify({ username, password })
            });

            if (response.success) {
                this.isAdmin = true;
                this.currentUser = { username: response.admin.username, role: 'admin' };
                await this.loadAdminData();
                this.showPage('admin');
                this.showAdminSection('dashboard');
                this.showToast('Admin login successful!', 'success');
            } else {
                this.showToast(response.message || 'Invalid credentials', 'error');
            }
        } catch (error) {
            this.showToast(error.message || 'Admin login failed', 'error');
        }
    }

    showAuthForm(formType) {
        document.querySelectorAll('.auth-form').forEach(form => form.classList.remove('active'));

        const formMap = {
            'login': 'login-form',
            'register': 'register-form',
            'admin-login': 'admin-login-form'
        };

        const targetForm = document.getElementById(formMap[formType]);
        if (targetForm) {
            targetForm.classList.add('active');
        }
    }

    showProfileSelection(profiles) {
        this.showPage('profile-selection');
        const profilesGrid = document.getElementById('profilesGrid');

        if (profilesGrid) {
            profilesGrid.innerHTML = profiles.map(profile => `
                <div class="profile-card" onclick="window.app.selectProfile(${profile.profileId})">
                    <div class="profile-avatar">
                        <i class="fas fa-user"></i>
                    </div>
                    <div class="profile-name">${this.escapeHtml(profile.profileName)}</div>
                </div>
            `).join('');

            // Add "Add Profile" option if user has less than 5 profiles
            if (profiles.length < 5) {
                profilesGrid.innerHTML += `
                    <div class="profile-card add-profile" onclick="window.app.showAddProfileModal()">
                        <div class="profile-avatar">
                            <i class="fas fa-plus"></i>
                        </div>
                        <div class="profile-name">Add Profile</div>
                    </div>
                `;
            }
        }
    }

    async selectProfile(profileId) {
        try {
            const response = await this.apiCall(`/profiles/${profileId}`);
            this.currentProfile = response;

            // Load all necessary data for the profile
            await this.loadUserData();

            this.showPage('main');
            this.showSection('home');
            this.updateCurrentProfileDisplay();
            this.loadHeroBanner();
            this.loadContentRows();
            this.showToast(`Welcome, ${this.currentProfile.profileName}!`, 'success');
        } catch (error) {
            this.showToast('Failed to load profile. Please try again.', 'error');
        }
    }

    async showAddProfileModal() {
        const profileName = prompt('Enter profile name:');
        if (profileName && profileName.trim()) {
            try {
                const response = await this.apiCall('/profiles', {
                    method: 'POST',
                    body: JSON.stringify({
                        userId: this.currentUser.userId,
                        profileName: profileName.trim()
                    })
                });

                if (response.success) {
                    this.showToast('Profile created successfully!', 'success');
                    // Reload profile selection
                    const loginResponse = await this.apiCall('/users/login', {
                        method: 'POST',
                        body: JSON.stringify({
                            email: this.currentUser.email,
                            password: this.currentUser.passwordHash
                        })
                    });
                    this.showProfileSelection(loginResponse.profiles);
                }
            } catch (error) {
                this.showToast('Failed to create profile', 'error');
            }
        }
    }

    // ===================== DATA LOADING METHODS =====================

    async loadUserData() {
        try {
            console.log('Loading user data from backend...');

            // Load all data in parallel
            const [
                titles,
                genres,
                countries,
                watchlist,
                watchHistory,
                ratings,
                recommendations
            ] = await Promise.all([
                this.apiCall('/titles'),
                this.apiCall('/genres'),
                this.apiCall('/countries'),
                this.apiCall(`/watchlist/${this.currentProfile.profileId}`),
                this.apiCall(`/watch-history/${this.currentProfile.profileId}`),
                this.apiCall(`/ratings/profile/${this.currentProfile.profileId}`),
                this.apiCall(`/recommendations/${this.currentProfile.profileId}?limit=20`)
            ]);

            // Store in cache
            this.cache.titles = titles || [];
            this.cache.genres = genres || [];
            this.cache.countries = countries || [];
            this.cache.watchlist = watchlist || [];
            this.cache.watchHistory = watchHistory || [];
            this.cache.userRatings = ratings || [];
            this.cache.recommendations = recommendations || [];

            console.log('User data loaded successfully:', {
                titles: this.cache.titles.length,
                watchlist: this.cache.watchlist.length,
                watchHistory: this.cache.watchHistory.length,
                recommendations: this.cache.recommendations.length
            });

            // Load genre preferences
            await this.loadGenrePreferences();

        } catch (error) {
            console.error('Error loading user data:', error);
            this.showToast('Some data failed to load', 'warning');
        }
    }

    async loadGenrePreferences() {
        try {
            const prefs = await this.apiCall(`/recommendations/preferences/${this.currentProfile.profileId}`);
            this.cache.genrePreferences = prefs || {};
        } catch (error) {
            console.log('No genre preferences found, will use defaults');
            this.cache.genrePreferences = {};
        }
    }

    async loadAdminData() {
        try {
            console.log('Loading admin data...');

            const [titles, actors, directors, genres, countries, ratings, health] = await Promise.all([
                this.apiCall('/titles'),
                this.apiCall('/actors'),
                this.apiCall('/directors'),
                this.apiCall('/genres'),
                this.apiCall('/countries'),
                this.apiCall('/ratings'),
                this.apiCall('/health')
            ]);

            this.cache.titles = titles || [];
            this.cache.actors = actors || [];
            this.cache.directors = directors || [];
            this.cache.genres = genres || [];
            this.cache.countries = countries || [];
            this.cache.ratings = ratings || [];

            // Calculate filter counts from loaded data
            this.calculateFilterCounts();

            console.log('Admin data loaded:', health);
        } catch (error) {
            console.error('Error loading admin data:', error);
            this.showToast('Failed to load admin data', 'error');
        }
    }

    calculateFilterCounts() {
        const titles = this.cache.titles;

        // Count by type
        this.cache.filterCounts.types = {};
        titles.forEach(title => {
            const type = title.type || 'Unknown';
            this.cache.filterCounts.types[type] = (this.cache.filterCounts.types[type] || 0) + 1;
        });

        // Count by genre
        this.cache.filterCounts.genres = {};
        titles.forEach(title => {
            if (title.genres && Array.isArray(title.genres)) {
                title.genres.forEach(genre => {
                    this.cache.filterCounts.genres[genre] = (this.cache.filterCounts.genres[genre] || 0) + 1;
                });
            }
        });

        // Count by country
        this.cache.filterCounts.countries = {};
        titles.forEach(title => {
            if (title.countries && Array.isArray(title.countries)) {
                title.countries.forEach(country => {
                    this.cache.filterCounts.countries[country] = (this.cache.filterCounts.countries[country] || 0) + 1;
                });
            }
        });

        // Count by rating
        this.cache.filterCounts.ratings = {};
        titles.forEach(title => {
            const rating = title.rating || 'Unrated';
            this.cache.filterCounts.ratings[rating] = (this.cache.filterCounts.ratings[rating] || 0) + 1;
        });
    }

    // ===================== CONTENT RENDERING METHODS =====================

    loadHeroBanner() {
        const heroBanner = document.getElementById('heroBanner');
        if (!heroBanner || this.cache.titles.length === 0) return;

        // Get a featured title (first one or random)
        const featuredTitle = this.cache.titles[Math.floor(Math.random() * Math.min(10, this.cache.titles.length))];

        heroBanner.style.backgroundImage = featuredTitle.imageUrl
            ? `url(${featuredTitle.imageUrl})`
            : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';

        const heroTitle = heroBanner.querySelector('.hero-title');
        const heroDescription = heroBanner.querySelector('.hero-description');

        if (heroTitle) heroTitle.textContent = featuredTitle.title;
        if (heroDescription) heroDescription.textContent = featuredTitle.description || 'No description available';
    }

    loadContentRows() {
        console.log('Loading content rows...');
        this.loadPersonalizedRecommendations();
        this.loadContinueWatching();
        this.loadMyList();
        this.loadGenreBasedRows();
        this.loadPopularContent();
    }

    loadPersonalizedRecommendations() {
        const recommendations = this.cache.recommendations || [];
        this.renderTitleGrid('recommendationsGrid', recommendations.slice(0, 6));

        const recommendationsRow = document.getElementById('personalizedRecommendations');
        if (recommendationsRow) {
            recommendationsRow.style.display = recommendations.length > 0 ? 'block' : 'none';
        }
    }

    loadContinueWatching() {
        const continueWatching = this.cache.watchHistory
            .filter(w => !w.isCompleted)
            .map(w => this.cache.titles.find(t => t.title_id === w.titleId))
            .filter(t => t != null)
            .slice(0, 6);

        this.renderTitleGrid('continueWatchingGrid', continueWatching);

        const continueWatchingRow = document.getElementById('continueWatchingRow');
        if (continueWatchingRow) {
            continueWatchingRow.style.display = continueWatching.length > 0 ? 'block' : 'none';
        }
    }

    loadMyList() {
        const myList = this.cache.watchlist
            .map(w => this.cache.titles.find(t => t.title_id === w.titleId))
            .filter(t => t != null)
            .slice(0, 6);

        this.renderTitleGrid('myListGrid', myList);

        const myListRow = document.getElementById('myListRow');
        if (myListRow) {
            myListRow.style.display = myList.length > 0 ? 'block' : 'none';
        }
    }

    loadGenreBasedRows() {
        const genreBasedRows = document.getElementById('genreBasedRows');
        if (!genreBasedRows) return;

        genreBasedRows.innerHTML = '';

        // Get top genres from preferences or use default popular genres
        const topGenres = this.getTopGenres();

        topGenres.forEach(genreName => {
            const genreContent = this.cache.titles.filter(title =>
                title.genres && title.genres.includes(genreName)
            ).slice(0, 6);

            if (genreContent.length > 0) {
                const rowHtml = `
                    <div class="content-row">
                        <div class="row-header">
                            <h2>${this.escapeHtml(genreName)}</h2>
                        </div>
                        <div class="content-grid" id="genre-${genreName.replace(/\s+/g, '-')}"></div>
                    </div>
                `;
                genreBasedRows.innerHTML += rowHtml;

                setTimeout(() => {
                    this.renderTitleGrid(`genre-${genreName.replace(/\s+/g, '-')}`, genreContent);
                }, 0);
            }
        });
    }

    getTopGenres() {
        const preferences = this.cache.genrePreferences;

        if (preferences && Object.keys(preferences).length > 0) {
            return Object.entries(preferences)
                .sort((a, b) => b[1] - a[1])
                .slice(0, 5)
                .map(([genre]) => genre);
        }

        // Default popular genres
        return ['Action', 'Drama', 'Comedy', 'Thriller', 'Sci-Fi'].filter(genre =>
            this.cache.titles.some(t => t.genres && t.genres.includes(genre))
        );
    }

    loadPopularContent() {
        // Sort by popularity if available, otherwise just take first titles
        const popular = [...this.cache.titles]
            .sort((a, b) => (b.popularityScore || 0) - (a.popularityScore || 0))
            .slice(0, 6);

        this.renderTitleGrid('popularGrid', popular);
    }

    renderTitleGrid(gridId, titles) {
        const grid = document.getElementById(gridId);
        if (!grid) return;

        if (!titles || titles.length === 0) {
            grid.innerHTML = '<p class="no-content">No content available</p>';
            return;
        }

        grid.innerHTML = titles.map(title => {
            const titleId = title.title_id || title.titleId;
            const imageUrl = title.imageUrl || title.image_url || 'https://via.placeholder.com/300x450?text=No+Image';
            const titleName = title.title || 'Untitled';
            const titleType = title.type || 'Unknown';

            return `
                <div class="title-card" onclick="window.app.showTitleModal(${titleId})">
                    <img src="${imageUrl}" alt="${this.escapeHtml(titleName)}" onerror="this.src='https://via.placeholder.com/300x450?text=No+Image'">
                    <div class="title-card-info">
                        <h3>${this.escapeHtml(titleName)}</h3>
                        <span class="title-type">${this.escapeHtml(titleType)}</span>
                    </div>
                </div>
            `;
        }).join('');
    }

    // ===================== RECOMMENDATION LOGIC (Enhanced) =====================

    async generatePersonalizedRecommendations(profileId, limit = 20) {
        try {
            // Try to get recommendations from backend
            const recommendations = await this.apiCall(`/recommendations/${profileId}?limit=${limit}`);
            return recommendations || [];
        } catch (error) {
            console.log('Generating client-side recommendations...');
            return this.generateClientSideRecommendations(limit);
        }
    }

    generateClientSideRecommendations(limit = 20) {
        const watchedTitles = this.cache.watchHistory.map(w => w.titleId);
        const ratedTitles = this.cache.userRatings.map(r => r.titleId);
        const watchlistTitles = this.cache.watchlist.map(w => w.titleId);

        // Get genre preferences from watched/rated content
        const genreScores = {};

        // Analyze watched content
        watchedTitles.forEach(titleId => {
            const title = this.cache.titles.find(t => t.title_id === titleId);
            if (title && title.genres) {
                title.genres.forEach(genre => {
                    genreScores[genre] = (genreScores[genre] || 0) + 1;
                });
            }
        });

        // Get unwatched titles
        const unwatched = this.cache.titles.filter(title =>
            !watchedTitles.includes(title.title_id) &&
            !watchlistTitles.includes(title.title_id)
        );

        // Score each unwatched title
        const scored = unwatched.map(title => {
            let score = 0;

            // Genre match score
            if (title.genres) {
                title.genres.forEach(genre => {
                    score += genreScores[genre] || 0;
                });
            }

            // Popularity score
            score += (title.popularityScore || 0) * 0.1;

            return { title, score };
        });

        // Sort by score and return top results
        return scored
            .sort((a, b) => b.score - a.score)
            .slice(0, limit)
            .map(item => item.title);
    }

    async refreshRecommendations() {
        try {
            const recommendations = await this.apiCall(`/recommendations/${this.currentProfile.profileId}?limit=20`);
            this.cache.recommendations = recommendations || [];
            this.loadPersonalizedRecommendations();
            this.showToast('Recommendations refreshed!', 'success');
        } catch (error) {
            this.showToast('Failed to refresh recommendations', 'error');
        }
    }

    async setGenrePreferences(genres) {
        try {
            await this.apiCall(`/recommendations/preferences/${this.currentProfile.profileId}`, {
                method: 'POST',
                body: JSON.stringify({ genres })
            });

            await this.loadGenrePreferences();
            await this.refreshRecommendations();
            this.showToast('Genre preferences updated!', 'success');
        } catch (error) {
            this.showToast('Failed to update preferences', 'error');
        }
    }

    // ===================== MODAL METHODS =====================

    async showTitleModal(titleId) {
        try {
            const title = this.cache.titles.find(t => t.title_id === titleId || t.titleId === titleId);
            if (!title) {
                this.showToast('Title not found', 'error');
                return;
            }

            this.currentModalTitle = title;

            const modal = document.getElementById('titleModal');
            if (!modal) return;

            // Populate modal with title details
            const modalTitle = modal.querySelector('.modal-title');
            const modalDescription = modal.querySelector('.modal-description');
            const modalMeta = modal.querySelector('.modal-meta');
            const modalGenres = modal.querySelector('.modal-genres');
            const modalCast = modal.querySelector('.modal-cast');

            if (modalTitle) modalTitle.textContent = title.title;
            if (modalDescription) modalDescription.textContent = title.description || 'No description available';

            if (modalMeta) {
                const rating = title.rating || 'Not Rated';
                const year = title.release_year || title.releaseYear || 'Unknown';
                const duration = title.duration || 'Unknown';
                modalMeta.innerHTML = `
                    <span class="meta-item">${this.escapeHtml(rating)}</span>
                    <span class="meta-item">${year}</span>
                    <span class="meta-item">${this.escapeHtml(duration)}</span>
                `;
            }

            if (modalGenres && title.genres) {
                modalGenres.innerHTML = title.genres.map(genre =>
                    `<span class="genre-tag">${this.escapeHtml(genre)}</span>`
                ).join('');
            }

            if (modalCast && title.cast) {
                modalCast.textContent = `Cast: ${title.cast.slice(0, 5).join(', ')}`;
            }

            // Update action buttons
            this.updateModalButtons(title);

            modal.classList.add('active');
        } catch (error) {
            console.error('Error showing title modal:', error);
            this.showToast('Failed to load title details', 'error');
        }
    }

    updateModalButtons(title) {
        const titleId = title.title_id || title.titleId;
        const isInWatchlist = this.cache.watchlist.some(w => w.titleId === titleId);
        const userRating = this.cache.userRatings.find(r => r.titleId === titleId);

        const watchlistBtn = document.querySelector('.btn-watchlist');
        if (watchlistBtn) {
            watchlistBtn.innerHTML = isInWatchlist
                ? '<i class="fas fa-check"></i> In My List'
                : '<i class="fas fa-plus"></i> Add to List';
            watchlistBtn.onclick = () => isInWatchlist ? this.removeFromWatchlist(titleId) : this.addToWatchlist(titleId);
        }

        const thumbsUpBtn = document.querySelector('.btn-thumbs-up');
        const thumbsDownBtn = document.querySelector('.btn-thumbs-down');

        if (thumbsUpBtn) {
            thumbsUpBtn.classList.toggle('active', userRating?.ratingValue === 'THUMBS_UP');
            thumbsUpBtn.onclick = () => this.rateTitle(titleId, 'THUMBS_UP');
        }

        if (thumbsDownBtn) {
            thumbsDownBtn.classList.toggle('active', userRating?.ratingValue === 'THUMBS_DOWN');
            thumbsDownBtn.onclick = () => this.rateTitle(titleId, 'THUMBS_DOWN');
        }
    }

    closeModal() {
        const modal = document.getElementById('titleModal');
        if (modal) {
            modal.classList.remove('active');
        }
        this.currentModalTitle = null;
    }

    bindModalEvents() {
        const closeModalBtn = document.querySelector('.close-modal');
        if (closeModalBtn) {
            closeModalBtn.addEventListener('click', () => this.closeModal());
        }

        const modal = document.getElementById('titleModal');
        if (modal) {
            modal.addEventListener('click', (e) => {
                if (e.target === modal) {
                    this.closeModal();
                }
            });
        }

        const playBtn = document.querySelector('.btn-play');
        if (playBtn) {
            playBtn.addEventListener('click', () => this.playTitle());
        }
    }

    async playTitle() {
        if (!this.currentModalTitle) return;

        const titleId = this.currentModalTitle.title_id || this.currentModalTitle.titleId;

        try {
            await this.apiCall('/watch-history', {
                method: 'POST',
                body: JSON.stringify({
                    profileId: this.currentProfile.profileId,
                    titleId: titleId,
                    completed: false
                })
            });

            this.showToast(`Playing ${this.currentModalTitle.title}...`, 'success');
            this.closeModal();

            // Reload watch history
            const watchHistory = await this.apiCall(`/watch-history/${this.currentProfile.profileId}`);
            this.cache.watchHistory = watchHistory || [];
            this.loadContinueWatching();
        } catch (error) {
            this.showToast('Failed to start playback', 'error');
        }
    }

    // ===================== WATCHLIST METHODS =====================

    async addToWatchlist(titleId) {
        try {
            await this.apiCall('/watchlist', {
                method: 'POST',
                body: JSON.stringify({
                    profileId: this.currentProfile.profileId,
                    titleId: titleId
                })
            });

            // Reload watchlist
            const watchlist = await this.apiCall(`/watchlist/${this.currentProfile.profileId}`);
            this.cache.watchlist = watchlist || [];

            if (this.currentModalTitle) {
                this.updateModalButtons(this.currentModalTitle);
            }

            this.loadMyList();
            this.showToast('Added to My List', 'success');
        } catch (error) {
            this.showToast(error.message || 'Failed to add to watchlist', 'error');
        }
    }

    async removeFromWatchlist(titleId) {
        try {
            await this.apiCall(`/watchlist/${this.currentProfile.profileId}/${titleId}`, {
                method: 'DELETE'
            });

            // Reload watchlist
            const watchlist = await this.apiCall(`/watchlist/${this.currentProfile.profileId}`);
            this.cache.watchlist = watchlist || [];

            if (this.currentModalTitle) {
                this.updateModalButtons(this.currentModalTitle);
            }

            this.loadMyList();
            this.showToast('Removed from My List', 'success');
        } catch (error) {
            this.showToast('Failed to remove from watchlist', 'error');
        }
    }

    // ===================== RATING METHODS =====================

    async rateTitle(titleId, ratingValue) {
        try {
            await this.apiCall('/ratings', {
                method: 'POST',
                body: JSON.stringify({
                    profileId: this.currentProfile.profileId,
                    titleId: titleId,
                    ratingValue: ratingValue
                })
            });

            // Reload ratings
            const ratings = await this.apiCall(`/ratings/profile/${this.currentProfile.profileId}`);
            this.cache.userRatings = ratings || [];

            if (this.currentModalTitle) {
                this.updateModalButtons(this.currentModalTitle);
            }

            this.showToast('Rating saved', 'success');
        } catch (error) {
            this.showToast('Failed to save rating', 'error');
        }
    }

    // ===================== SEARCH METHODS =====================

    bindUserEvents() {
        const searchBtn = document.querySelector('.nav-search');
        if (searchBtn) {
            searchBtn.addEventListener('click', () => this.toggleSearch());
        }

        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => this.handleSearch(e.target.value));
        }

        const logoutBtn = document.querySelector('.logout-btn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.logout());
        }

        // Navigation
        const navHome = document.querySelector('[data-section="home"]');
        const navMovies = document.querySelector('[data-section="movies"]');
        const navTvShows = document.querySelector('[data-section="tv-shows"]');
        const navMyList = document.querySelector('[data-section="my-list"]');

        if (navHome) navHome.addEventListener('click', () => this.showSection('home'));
        if (navMovies) navMovies.addEventListener('click', () => this.showSection('movies'));
        if (navTvShows) navTvShows.addEventListener('click', () => this.showSection('tv-shows'));
        if (navMyList) navMyList.addEventListener('click', () => this.showSection('my-list'));

        // Profile dropdown
        const profileToggle = document.querySelector('.current-profile');
        if (profileToggle) {
            profileToggle.addEventListener('click', () => this.toggleProfileDropdown());
        }
    }

    toggleSearch() {
        this.searchActive = !this.searchActive;
        const searchBar = document.getElementById('searchBar');

        if (searchBar) {
            searchBar.classList.toggle('active', this.searchActive);
            if (this.searchActive) {
                document.getElementById('searchInput')?.focus();
            } else {
                this.clearSearch();
            }
        }
    }

    handleSearch(query) {
        if (!query || query.trim() === '') {
            this.clearSearch();
            return;
        }

        const searchQuery = query.toLowerCase().trim();
        const results = this.cache.titles.filter(title =>
            title.title.toLowerCase().includes(searchQuery) ||
            (title.description && title.description.toLowerCase().includes(searchQuery)) ||
            (title.genres && title.genres.some(g => g.toLowerCase().includes(searchQuery))) ||
            (title.cast && title.cast.some(c => c.toLowerCase().includes(searchQuery)))
        );

        this.showSearchResults(results);
    }

    showSearchResults(results) {
        const searchResults = document.getElementById('searchResults');
        if (!searchResults) return;

        searchResults.classList.add('active');

        if (results.length === 0) {
            searchResults.innerHTML = '<p class="no-results">No results found</p>';
            return;
        }

        searchResults.innerHTML = `
            <h2>Search Results (${results.length})</h2>
            <div class="content-grid" id="searchResultsGrid"></div>
        `;

        this.renderTitleGrid('searchResultsGrid', results.slice(0, 20));
    }

    clearSearch() {
        const searchInput = document.getElementById('searchInput');
        const searchResults = document.getElementById('searchResults');

        if (searchInput) searchInput.value = '';
        if (searchResults) searchResults.classList.remove('active');
    }

    // ===================== NAVIGATION METHODS =====================

    showPage(pageId) {
        document.querySelectorAll('.page').forEach(page => page.classList.remove('active'));
        const targetPage = document.getElementById(`${pageId}-page`);
        if (targetPage) {
            targetPage.classList.add('active');
        }
        this.currentPage = pageId;
    }

    showSection(sectionId) {
        document.querySelectorAll('.main-content-section').forEach(section =>
            section.classList.remove('active')
        );

        const targetSection = document.getElementById(`${sectionId}-section`);
        if (targetSection) {
            targetSection.classList.add('active');
        }

        // Update active nav item
        document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
        const activeNavItem = document.querySelector(`[data-section="${sectionId}"]`);
        if (activeNavItem) {
            activeNavItem.classList.add('active');
        }

        this.currentSection = sectionId;

        // Load section-specific content
        if (sectionId === 'movies') {
            this.loadMovies();
        } else if (sectionId === 'tv-shows') {
            this.loadTVShows();
        } else if (sectionId === 'my-list') {
            this.loadMyListSection();
        }
    }

    loadMovies() {
        const movies = this.cache.titles.filter(t => t.type === 'Movie');
        const moviesGrid = document.getElementById('moviesGrid');

        if (moviesGrid) {
            this.renderTitleGrid('moviesGrid', movies);
        }
    }

    loadTVShows() {
        const tvShows = this.cache.titles.filter(t => t.type === 'TV Show');
        const tvShowsGrid = document.getElementById('tvShowsGrid');

        if (tvShowsGrid) {
            this.renderTitleGrid('tvShowsGrid', tvShows);
        }
    }

    loadMyListSection() {
        const myListTitles = this.cache.watchlist
            .map(w => this.cache.titles.find(t => t.title_id === w.titleId))
            .filter(t => t != null);

        const myListFullGrid = document.getElementById('myListFullGrid');
        if (myListFullGrid) {
            this.renderTitleGrid('myListFullGrid', myListTitles);
        }
    }

    updateCurrentProfileDisplay() {
        const profileName = document.querySelector('.current-profile .profile-name');
        if (profileName && this.currentProfile) {
            profileName.textContent = this.currentProfile.profileName;
        }
    }

    toggleProfileDropdown() {
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) {
            dropdown.classList.toggle('active');
            if (dropdown.classList.contains('active')) {
                this.loadProfileDropdown();
            }
        }
    }

    async loadProfileDropdown() {
        try {
            const response = await this.apiCall(`/users/${this.currentUser.userId}/profiles`);
            const profiles = response || [];

            const dropdownProfiles = document.getElementById('dropdownProfiles');
            if (dropdownProfiles) {
                dropdownProfiles.innerHTML = profiles.map(profile => `
                    <a href="#" class="dropdown-profile" onclick="window.app.switchProfile(${profile.profileId}); return false;">
                        ${this.escapeHtml(profile.profileName)}
                    </a>
                `).join('');
            }
        } catch (error) {
            console.error('Failed to load profiles:', error);
        }
    }

    async switchProfile(profileId) {
        if (profileId === this.currentProfile?.profileId) {
            this.closeProfileDropdown();
            return;
        }

        await this.selectProfile(profileId);
        this.closeProfileDropdown();
    }

    closeProfileDropdown() {
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) {
            dropdown.classList.remove('active');
        }
    }

    // ===================== ADMIN METHODS =====================

    bindAdminEvents() {
        // Admin navigation
        const adminDashboard = document.querySelector('[data-admin-section="dashboard"]');
        const adminTitles = document.querySelector('[data-admin-section="titles"]');
        const adminRecommendations = document.querySelector('[data-admin-section="recommendations"]');

        if (adminDashboard) adminDashboard.addEventListener('click', () => this.showAdminSection('dashboard'));
        if (adminTitles) adminTitles.addEventListener('click', () => this.showAdminSection('titles'));
        if (adminRecommendations) adminRecommendations.addEventListener('click', () => this.showAdminSection('recommendations'));

        // Admin actions
        const addTitleBtn = document.getElementById('addTitleBtn');
        if (addTitleBtn) {
            addTitleBtn.addEventListener('click', () => this.showAddTitleModal());
        }
    }

    showAdminSection(sectionId) {
        document.querySelectorAll('.admin-section').forEach(section =>
            section.classList.remove('active')
        );

        const targetSection = document.getElementById(`admin-${sectionId}`);
        if (targetSection) {
            targetSection.classList.add('active');
        }

        // Update active nav
        document.querySelectorAll('.admin-nav-item').forEach(item =>
            item.classList.remove('active')
        );
        const activeNavItem = document.querySelector(`[data-admin-section="${sectionId}"]`);
        if (activeNavItem) {
            activeNavItem.classList.add('active');
        }

        this.currentAdminSection = sectionId;

        // Load section data
        if (sectionId === 'dashboard') {
            this.loadAdminDashboard();
        } else if (sectionId === 'titles') {
            this.loadAdminTitles();
        } else if (sectionId === 'recommendations') {
            this.loadAdminRecommendations();
        }
    }

    loadAdminDashboard() {
        const statsCards = document.getElementById('statsCards');
        if (!statsCards) return;

        const stats = {
            totalTitles: this.cache.titles.length,
            totalActors: this.cache.actors.length,
            totalDirectors: this.cache.directors.length,
            totalGenres: this.cache.genres.length
        };

        statsCards.innerHTML = `
            <div class="stat-card">
                <i class="fas fa-film"></i>
                <div class="stat-value">${stats.totalTitles}</div>
                <div class="stat-label">Total Titles</div>
            </div>
            <div class="stat-card">
                <i class="fas fa-user-tie"></i>
                <div class="stat-value">${stats.totalActors}</div>
                <div class="stat-label">Total Actors</div>
            </div>
            <div class="stat-card">
                <i class="fas fa-video"></i>
                <div class="stat-value">${stats.totalDirectors}</div>
                <div class="stat-label">Total Directors</div>
            </div>
            <div class="stat-card">
                <i class="fas fa-tags"></i>
                <div class="stat-value">${stats.totalGenres}</div>
                <div class="stat-label">Total Genres</div>
            </div>
        `;

        this.renderAdminCharts();
    }

    renderAdminCharts() {
        // This would render charts using Chart.js
        // Implementation can be added based on requirements
        console.log('Admin charts would be rendered here');
    }

    loadAdminTitles() {
        const adminTitlesTable = document.getElementById('adminTitlesTable');
        if (!adminTitlesTable) return;

        const titles = this.cache.titles.slice(0, 50); // Show first 50

        const tableHtml = `
            <table class="admin-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Title</th>
                        <th>Type</th>
                        <th>Year</th>
                        <th>Rating</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${titles.map(title => `
                        <tr>
                            <td>${title.title_id || title.titleId}</td>
                            <td>${this.escapeHtml(title.title)}</td>
                            <td>${this.escapeHtml(title.type)}</td>
                            <td>${title.release_year || title.releaseYear}</td>
                            <td>${this.escapeHtml(title.rating || 'N/A')}</td>
                            <td>
                                <button class="btn btn--small" onclick="window.app.editTitle(${title.title_id || title.titleId})">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button class="btn btn--small btn--danger" onclick="window.app.deleteTitle(${title.title_id || title.titleId})">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        adminTitlesTable.innerHTML = tableHtml;
    }

    loadAdminRecommendations() {
        const recommendationsSettings = document.getElementById('recommendationsSettings');
        if (!recommendationsSettings) return;

        recommendationsSettings.innerHTML = `
            <div class="settings-panel">
                <h3>Recommendation Settings</h3>
                <p>Manage recommendation algorithms and settings here.</p>
                <button class="btn btn--primary" onclick="window.app.updateRecommendationSettings()">
                    Update Settings
                </button>
            </div>
        `;
    }

    async showAddTitleModal() {
        this.showToast('Add Title feature - Connect to your backend endpoint', 'info');
    }

    async editTitle(titleId) {
        this.showToast(`Edit Title ${titleId} - Connect to your backend endpoint`, 'info');
    }

    async deleteTitle(titleId) {
        if (!confirm('Are you sure you want to delete this title?')) return;

        try {
            await this.apiCall(`/titles/${titleId}`, {
                method: 'DELETE'
            });

            await this.loadAdminData();
            this.loadAdminTitles();
            this.showToast('Title deleted successfully', 'success');
        } catch (error) {
            this.showToast('Failed to delete title', 'error');
        }
    }

    // ===================== UTILITY METHODS =====================

    bindUtilityEvents() {
        // Close dropdowns when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.profile-dropdown-container')) {
                this.closeProfileDropdown();
            }
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                this.closeModal();
                this.closeProfileDropdown();
                if (this.searchActive) {
                    this.toggleSearch();
                }
            }
        });
    }

    populateGenreFilter() {
        const genreFilter = document.getElementById('genreFilter');
        if (!genreFilter || this.cache.genres.length === 0) return;

        genreFilter.innerHTML = '<option value="">All Genres</option>' +
            this.cache.genres.map(genre =>
                `<option value="${genre.genreId || genre.genre_id}">${this.escapeHtml(genre.name)}</option>`
            ).join('');
    }

    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast--${type}`;
        toast.textContent = message;

        document.body.appendChild(toast);

        setTimeout(() => toast.classList.add('show'), 100);

        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    logout() {
        this.currentUser = null;
        this.currentProfile = null;
        this.isAdmin = false;
        this.cache = {
            titles: [],
            genres: [],
            countries: [],
            actors: [],
            directors: [],
            ratings: [],
            watchlist: [],
            watchHistory: [],
            userRatings: [],
            recommendations: [],
            genrePreferences: {},
            filterCounts: {}
        };
        this.selectedGenres.clear();
        this.closeProfileDropdown();
        this.showPage('auth');
        this.showAuthForm('login');
        this.showToast('Logged out successfully', 'success');
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM loaded, initializing Enhanced Netflix app with backend API...');
    window.app = new NetflixApp();
});

