class AdminApp {
    constructor() {
        this.apiBaseUrl = window.netflixApiBaseUrl || '/api';
        this.state = {
            stats: null,
            titles: [],
            filteredTitles: [],
            genres: [],
            countries: [],
            ratings: [],
            users: [],
            profiles: [],
            actors: [],
            directors: [],
            subscriptionPlans: []
        };
        this.state.titlePagination = { page: 1, size: 50, totalItems: 0, totalPages: 0 };
        this.filters = {
            search: '',
            types: new Set(),
            genres: new Set(),
            countries: new Set(),
            ratings: new Set(),
            yearMin: null,
            yearMax: null
        };
    this.editingTitleId = null;
    this.editingUserId = null;
    this.activeUserDraft = null;
        this.chartInstances = {};
        this.toastTimeout = null;

        if (localStorage.getItem('isAdmin') !== 'true') {
            window.location.href = 'login.html';
            return;
        }

        this.init();
    }

    async init() {
        this.cacheDom();
        this.setupEventListeners();
        await this.loadInitialData();
        this.renderActiveSection();
    }

    cacheDom() {
        this.dom = {
            navItems: Array.from(document.querySelectorAll('.admin-nav-item')),
            sections: Array.from(document.querySelectorAll('.admin-section')),
            sectionTitle: document.getElementById('adminSectionTitle'),
            stats: {
                totalTitles: document.getElementById('totalTitles'),
                totalUsers: document.getElementById('totalUsers'),
                totalRecommendations: document.getElementById('totalRecommendations'),
                userEngagement: document.getElementById('userEngagement')
            },
            charts: {
                dashboard: document.getElementById('dashboardChart'),
                recommendation: document.getElementById('recommendationChart')
            },
            titles: {
                tableContainer: document.getElementById('titlesTableContainer'),
                appliedFilters: document.getElementById('appliedFilters'),
                typeFilters: document.getElementById('typeFilters'),
                genreFilters: document.getElementById('genreFilters'),
                countryFilters: document.getElementById('countryFilters'),
                ratingFilters: document.getElementById('ratingFilters'),
                searchInput: document.getElementById('titleSearchInput'),
                yearMin: document.getElementById('yearMin'),
                yearMax: document.getElementById('yearMax'),
                pagination: document.getElementById('titlesPagination')
            },
            tables: {
                users: document.getElementById('usersTableContainer'),
                actors: document.getElementById('actorsTableContainer'),
                directors: document.getElementById('directorsTableContainer')
            },
            modals: {
                title: document.getElementById('titleModal'),
                user: document.getElementById('userModal')
            },
            forms: {
                title: document.getElementById('titleForm'),
                user: document.getElementById('userForm')
            },
            buttons: {
                logout: document.getElementById('adminLogout'),
                clearFilters: document.getElementById('clearAllFilters'),
                addTitle: document.getElementById('addTitleBtn'),
                exportTitles: document.getElementById('exportTitles'),
                saveSettings: document.getElementById('saveRecommendationSettings'),
                addUser: document.getElementById('addUserBtn'),
                refreshUsers: document.getElementById('refreshUsersBtn')
            },
            inputs: {
                userEmail: document.getElementById('userEmail'),
                userUsername: document.getElementById('userUsername'),
                userPassword: document.getElementById('userPassword'),
                userPasswordHelp: document.getElementById('userPasswordHelp'),
                userDateOfBirth: document.getElementById('userDateOfBirth'),
                userSubscriptionPlan: document.getElementById('userSubscriptionPlan')
            }
        };
    }

    setupEventListeners() {
        this.dom.navItems.forEach((item) => {
            item.addEventListener('click', (event) => {
                event.preventDefault();
                const section = item.getAttribute('data-section');
                this.showSection(section);
            });
        });

        if (this.dom.buttons.logout) {
            this.dom.buttons.logout.addEventListener('click', () => {
                localStorage.removeItem('isAdmin');
                window.location.href = 'login.html';
            });
        }

        if (this.dom.buttons.addTitle) {
            this.dom.buttons.addTitle.addEventListener('click', () => this.showTitleModal());
        }

        if (this.dom.buttons.exportTitles) {
            this.dom.buttons.exportTitles.addEventListener('click', () => this.exportTitles());
        }

        if (this.dom.buttons.addUser) {
            this.dom.buttons.addUser.addEventListener('click', () => this.showUserModal());
        }

        if (this.dom.buttons.refreshUsers) {
            this.dom.buttons.refreshUsers.addEventListener('click', () => this.refreshUsers());
        }

        if (this.dom.titles.searchInput) {
            this.dom.titles.searchInput.addEventListener('input', (event) => {
                const raw = event.target.value.trim();
                this.filters.search = raw.toLowerCase();
                this.scheduleTitleRefresh({ page: 1, showLoader: true, delay: 250 });
            });
        }

        ['yearMin', 'yearMax'].forEach((key) => {
            const input = this.dom.titles[key];
            if (input) {
                input.addEventListener('change', () => {
                    const value = input.value ? Number(input.value) : null;
                    this.filters[key === 'yearMin' ? 'yearMin' : 'yearMax'] = Number.isFinite(value) ? value : null;
                    this.scheduleTitleRefresh({ page: 1, showLoader: true, delay: 0 });
                });
            }
        });

        if (this.dom.buttons.clearFilters) {
            this.dom.buttons.clearFilters.addEventListener('click', () => this.clearAllFilters());
        }

        document.querySelectorAll('.modal-close').forEach((btn) => {
            btn.addEventListener('click', () => {
                const modalKey = btn.dataset.modal;
                this.closeModal(modalKey);
            });
        });

        if (this.dom.forms.title) {
            this.dom.forms.title.addEventListener('submit', (event) => {
                event.preventDefault();
                this.saveTitleForm();
            });
        }

        if (this.dom.forms.user) {
            this.dom.forms.user.addEventListener('submit', (event) => {
                event.preventDefault();
                this.saveUserForm();
            });
        }

        const modal = this.dom.modals.title;
        if (modal) {
            modal.addEventListener('click', (event) => {
                if (event.target === modal) {
                    this.closeModal('title');
                }
            });
        }

        const userModal = this.dom.modals.user;
        if (userModal) {
            userModal.addEventListener('click', (event) => {
                if (event.target === userModal) {
                    this.closeModal('user');
                }
            });
        }

        this.setupWeightSliders();

        if (this.dom.buttons.saveSettings) {
            this.dom.buttons.saveSettings.addEventListener('click', () => this.saveRecommendationSettings());
        }
    }

    setupWeightSliders() {
        const sliderIds = ['genreWeight', 'historyWeight', 'ratingWeight', 'popularityWeight'];
        let savedWeights = null;
        try {
            savedWeights = JSON.parse(localStorage.getItem('adminRecommendationWeights'));
        } catch (error) {
            savedWeights = null;
        }

        sliderIds.forEach((id) => {
            const input = document.getElementById(id);
            if (!input) {
                return;
            }

            if (savedWeights && typeof savedWeights[id] === 'number') {
                input.value = savedWeights[id];
            }

            const valueDisplay = input.parentElement?.querySelector('.weight-value');
            const updateDisplay = () => {
                if (valueDisplay) {
                    valueDisplay.textContent = `${Math.round(Number(input.value) * 100)}%`;
                }
            };

            updateDisplay();
            input.addEventListener('input', updateDisplay);
        });
    }

    async loadInitialData() {
        this.setTitlesLoading(true);
        try {
            const [stats, genres, countries, ratings, users, profiles, actors, directors, plans] = await Promise.all([
                this.getJson('/admin/stats'),
                this.getJson('/genres'),
                this.getJson('/countries'),
                this.getJson('/ratings'),
                this.getJson('/admin/users'),
                this.getJson('/admin/profiles'),
                this.getJson('/actors'),
                this.getJson('/directors'),
                this.getJson('/subscriptions/plans')
            ]);

            this.state.stats = stats || null;
            this.state.genres = Array.isArray(genres)
                ? genres
                      .map((item) => ({ id: item.genreId ?? item.genre_id ?? item.id, name: item.name }))
                      .filter((genre) => genre.id && genre.name)
                : [];
            this.state.countries = Array.isArray(countries) ? countries : [];
            this.state.ratings = Array.isArray(ratings) ? ratings : [];
            this.state.users = Array.isArray(users) ? users : [];
            this.state.profiles = Array.isArray(profiles) ? profiles : [];
            this.state.actors = Array.isArray(actors) ? actors : [];
            this.state.directors = Array.isArray(directors) ? directors : [];
            this.state.subscriptionPlans = Array.isArray(plans) ? plans : [];

            await this.fetchTitles({ page: 1, showLoader: false });
            this.populateFilters();
            this.renderDashboard();
            this.renderUsers();
            this.renderActors();
            this.renderDirectors();
            this.renderRecommendationChart();
            this.renderAnalytics();

            const planMode = this.editingUserId ? 'edit' : 'create';
            this.populateUserPlanOptions(planMode, this.activeUserDraft?.subscription || null);
        } catch (error) {
            this.showToast(error.message || 'Failed to load admin data', 'error');
        } finally {
            this.setTitlesLoading(false);
        }
    }

    normalizeTitle(raw) {
        return {
            titleId: raw.title_id ?? raw.titleId,
            showId: raw.show_id ?? raw.showId,
            title: raw.title,
            type: raw.type,
            description: raw.description || '',
            dateAdded: raw.date_added ?? raw.dateAdded ?? '',
            releaseYear: raw.release_year ?? raw.releaseYear ?? null,
            rating: raw.rating || 'NR',
            duration: raw.duration || '',
            countries: raw.countries || [],
            genres: raw.genres || [],
            directors: raw.directors || [],
            cast: raw.cast || []
        };
    }

    async fetchTitles({ page, size, showLoader = true } = {}) {
        const currentPage = Number.isFinite(page) && page > 0 ? Number(page) : (this.state.titlePagination?.page ?? 1);
        const pageSize = Number.isFinite(size) && size > 0 ? Number(size) : (this.state.titlePagination?.size ?? 50);

        const params = new URLSearchParams();
        params.set('page', currentPage);
        params.set('size', pageSize);

        if (this.filters.search) {
            params.set('search', this.filters.search);
        }

        const addSetParam = (key, set) => {
            if (set instanceof Set && set.size) {
                params.set(key, Array.from(set).join(','));
            }
        };

        addSetParam('types', this.filters.types);
        addSetParam('ratings', this.filters.ratings);
        addSetParam('countries', this.filters.countries);
        addSetParam('genres', this.filters.genres);

        if (this.filters.yearMin != null) {
            params.set('yearMin', this.filters.yearMin);
        }
        if (this.filters.yearMax != null) {
            params.set('yearMax', this.filters.yearMax);
        }

        const requestToken = Symbol('titlesRequest');
        this.pendingTitlesRequest = requestToken;

        if (showLoader) {
            this.setTitlesLoading(true);
        }

        try {
            const response = await this.getJson(`/admin/titles?${params.toString()}`);
            if (this.pendingTitlesRequest !== requestToken) {
                return;
            }

            const items = Array.isArray(response?.items) ? response.items : [];
            const normalized = items.map((title) => this.normalizeTitle(title));

            this.state.titles = normalized;
            this.state.filteredTitles = [...normalized];
            this.state.titlePagination = {
                page: response?.page ?? currentPage,
                size: response?.size ?? pageSize,
                totalItems: response?.totalItems ?? normalized.length,
                totalPages: response?.totalPages ?? 1
            };

            this.renderTitles();
            this.renderAppliedFilters();
            this.renderTitlesPagination();
        } catch (error) {
            if (this.pendingTitlesRequest === requestToken) {
                this.showToast(error.message || 'Failed to load titles', 'error');
            }
        } finally {
            if (this.pendingTitlesRequest === requestToken && showLoader) {
                this.setTitlesLoading(false);
            }
        }
    }

    scheduleTitleRefresh(options = {}) {
        const delay = Number.isFinite(options.delay) ? Math.max(0, options.delay) : 200;
        if (this.titleRefreshTimeout) {
            clearTimeout(this.titleRefreshTimeout);
        }
        const args = { ...options };
        this.titleRefreshTimeout = setTimeout(() => {
            this.applyTitleFilters(args);
            this.titleRefreshTimeout = null;
        }, delay);
    }

    populateFilters() {
        const typeSet = new Set(['Movie', 'TV Show']);
        this.state.titles.forEach((title) => {
            if (title?.type) {
                typeSet.add(title.type);
            }
        });
        this.dom.titles.typeFilters.innerHTML = Array.from(typeSet)
            .sort()
            .map((type) => this.renderFilterCheckbox('types', type, type))
            .join('');

        this.dom.titles.genreFilters.innerHTML = this.state.genres
            .sort((a, b) => a.name.localeCompare(b.name))
            .map((genre) => this.renderFilterCheckbox('genres', genre.id, genre.name))
            .join('');

        this.dom.titles.countryFilters.innerHTML = this.state.countries
            .sort()
            .map((country) => this.renderFilterCheckbox('countries', country, country))
            .join('');

        this.dom.titles.ratingFilters.innerHTML = this.state.ratings
            .sort()
            .map((rating) => this.renderFilterCheckbox('ratings', rating, rating))
            .join('');

        this.bindFilterCheckboxes();
    }

    renderFilterCheckbox(group, value, label) {
        const id = `${group}-${String(value).toLowerCase().replace(/[^a-z0-9]+/g, '-')}`;
        return `
            <label class="filter-option" for="${id}">
                <input type="checkbox" id="${id}" value="${value}" data-filter-group="${group}">
                <span>${label}</span>
            </label>
        `;
    }

    bindFilterCheckboxes() {
        document.querySelectorAll('input[data-filter-group]').forEach((checkbox) => {
            checkbox.addEventListener('change', (event) => {
                const group = event.target.dataset.filterGroup;
                const value = event.target.value;
                if (!group) return;

                if (event.target.checked) {
                    this.filters[group].add(value);
                } else {
                    this.filters[group].delete(value);
                }
                this.scheduleTitleRefresh({ page: 1, showLoader: true, delay: 0 });
            });
        });
    }

    applyTitleFilters(options = {}) {
        const { page, keepPage = false, showLoader } = options;
        const currentPage = this.state.titlePagination?.page ?? 1;
        const targetPage = Number.isFinite(page) ? Number(page) : (keepPage ? currentPage : 1);
        this.fetchTitles({ page: targetPage, showLoader });
    }

    renderAppliedFilters() {
        const chips = [];
        const addChip = (label, token) => {
            chips.push(`<span class="filter-chip">${label}<button type="button" class="chip-remove" data-remove="${token}">×</button></span>`);
        };

        if (this.filters.search) {
            addChip(`Search: ${this.filters.search}`, 'search');
        }

        this.filters.types.forEach((value) => addChip(`Type: ${value}`, `types:${value}`));
        this.filters.genres.forEach((value) => {
            const label = this.getGenreNameById(value);
            addChip(`Genre: ${label}`, `genres:${value}`);
        });
        this.filters.countries.forEach((value) => addChip(`Country: ${value}`, `countries:${value}`));
        this.filters.ratings.forEach((value) => addChip(`Rating: ${value}`, `ratings:${value}`));

        if (this.filters.yearMin) {
            addChip(`Year ≥ ${this.filters.yearMin}`, 'yearMin');
        }
        if (this.filters.yearMax) {
            addChip(`Year ≤ ${this.filters.yearMax}`, 'yearMax');
        }

        this.dom.titles.appliedFilters.innerHTML = chips.length ? chips.join('') : '<span class="text-muted">No filters applied</span>';
        this.dom.titles.appliedFilters.querySelectorAll('.chip-remove').forEach((button) => {
            button.addEventListener('click', (event) => this.removeFilterChip(event.target.dataset.remove));
        });
    }

    removeFilterChip(token) {
        if (!token) return;

        if (token === 'search') {
            this.filters.search = '';
            if (this.dom.titles.searchInput) {
                this.dom.titles.searchInput.value = '';
            }
        } else if (token === 'yearMin' || token === 'yearMax') {
            const key = token === 'yearMin' ? 'yearMin' : 'yearMax';
            this.filters[key] = null;
            if (this.dom.titles[key]) {
                this.dom.titles[key].value = '';
            }
        } else {
            const [group, value] = token.split(':');
            if (this.filters[group] instanceof Set) {
                this.filters[group].delete(value);
                const checkbox = document.querySelector(`input[data-filter-group="${group}"][value="${value}"]`);
                if (checkbox) {
                    checkbox.checked = false;
                }
            }
        }

        this.applyTitleFilters({ page: 1, showLoader: true });
    }

    renderTitles() {
        const container = this.dom.titles.tableContainer;
        if (!container) return;

        if (!this.state.filteredTitles.length) {
            container.innerHTML = '<div class="empty-state">No titles match the current filters.</div>';
            return;
        }

        const rows = this.state.filteredTitles
            .map((title) => `
                <tr data-title-id="${title.titleId}">
                    <td>${title.title}</td>
                    <td>${title.type || '—'}</td>
                    <td>${title.releaseYear ?? '—'}</td>
                    <td>${title.rating || 'NR'}</td>
                    <td>${(title.genres || []).join(', ')}</td>
                    <td>${(title.countries || []).join(', ')}</td>
                    <td class="table-actions">
                        <button class="btn btn--sm" data-action="edit">Edit</button>
                        <button class="btn btn--sm btn--danger" data-action="delete">Delete</button>
                    </td>
                </tr>
            `)
            .join('');

        container.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Title</th>
                        <th>Type</th>
                        <th>Year</th>
                        <th>Rating</th>
                        <th>Genres</th>
                        <th>Countries</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        `;

        container.querySelectorAll('button[data-action="edit"]').forEach((button) => {
            button.addEventListener('click', (event) => {
                const row = event.target.closest('tr');
                const titleId = Number(row.getAttribute('data-title-id'));
                this.showTitleModal(titleId);
            });
        });

        container.querySelectorAll('button[data-action="delete"]').forEach((button) => {
            button.addEventListener('click', (event) => {
                const row = event.target.closest('tr');
                const titleId = Number(row.getAttribute('data-title-id'));
                this.deleteTitle(titleId);
            });
        });
    }

    renderTitlesPagination() {
        const container = this.dom.titles.pagination;
        if (!container) return;

        const info = this.state.titlePagination || { page: 1, size: 50, totalItems: this.state.filteredTitles.length, totalPages: 1 };
        const { page, size, totalItems, totalPages } = info;

        if (!totalPages || totalPages <= 1) {
            container.innerHTML = totalItems ? `<span class="pagination-summary">Showing ${Math.min(totalItems, size)} of ${totalItems}</span>` : '';
            return;
        }

        const summaryStart = totalItems === 0 ? 0 : (page - 1) * size + 1;
        const summaryEnd = Math.min(totalItems, page * size);

        const buttons = [];
        const pushButton = (label, targetPage, disabled = false, active = false) => {
            const classes = ['pagination-btn'];
            if (active) classes.push('active');
            if (disabled) classes.push('disabled');
            buttons.push(`<button class="${classes.join(' ')}" data-page="${targetPage}" ${disabled ? 'disabled' : ''}>${label}</button>`);
        };

        pushButton('Prev', Math.max(1, page - 1), page === 1, false);

        const windowSize = 5;
        let start = Math.max(1, page - Math.floor(windowSize / 2));
        let end = Math.min(totalPages, start + windowSize - 1);
        if (end - start + 1 < windowSize) {
            start = Math.max(1, end - windowSize + 1);
        }

        if (start > 1) {
            pushButton('1', 1, false, page === 1);
            if (start > 2) {
                buttons.push('<span class="pagination-ellipsis">…</span>');
            }
        }

        for (let i = start; i <= end; i += 1) {
            pushButton(String(i), i, false, i === page);
        }

        if (end < totalPages) {
            if (end < totalPages - 1) {
                buttons.push('<span class="pagination-ellipsis">…</span>');
            }
            pushButton(String(totalPages), totalPages, false, page === totalPages);
        }

        pushButton('Next', Math.min(totalPages, page + 1), page === totalPages, false);

        container.innerHTML = `
            <div class="pagination-summary">Showing ${summaryStart}-${summaryEnd} of ${totalItems}</div>
            <div class="pagination-controls">${buttons.join('')}</div>
        `;

        container.querySelectorAll('button[data-page]').forEach((button) => {
            if (button.disabled) return;
            button.addEventListener('click', (event) => {
                const target = Number(event.currentTarget.dataset.page);
                if (Number.isFinite(target)) {
                    this.applyTitleFilters({ page: target, showLoader: true });
                }
            });
        });
    }

    renderUsers() {
        const container = this.dom.tables.users;
        if (!container) return;

        if (!this.state.users.length) {
            container.innerHTML = '<div class="empty-state">No users found.</div>';
            return;
        }

        const rows = this.state.users
            .map((user) => {
                const subscription = user.subscription || null;
                const planLabel = subscription
                    ? `${this.escapeHtml(subscription.planName || 'Plan')} • ${subscription.quality || ''}`.trim()
                    : 'No active plan';
                const statusLabel = subscription ? this.capitalize(subscription.status) : '—';
                const startLabel = subscription?.startDate ? this.formatDate(subscription.startDate, true) : '';
                const subscriptionMeta = subscription
                    ? `${statusLabel}${startLabel ? ` · since ${startLabel}` : ''}`
                    : 'Assign a plan to activate billing.';

                return `
                    <tr data-user-id="${user.userId}">
                        <td>${user.userId}</td>
                        <td>
                            <div class="user-cell">
                                <div class="user-name">${this.escapeHtml(user.username || user.email || '—')}</div>
                                <div class="user-email">${this.escapeHtml(user.email || '—')}</div>
                            </div>
                        </td>
                        <td>${user.dateOfBirth || '—'}</td>
                        <td>
                            <div class="subscription-cell">
                                <div class="subscription-plan">${planLabel}</div>
                                <div class="subscription-meta">${this.escapeHtml(subscriptionMeta)}</div>
                            </div>
                        </td>
                        <td>${user.profileCount ?? 0}</td>
                        <td>${user.createdAt ? this.formatDate(user.createdAt, true) : '—'}</td>
                        <td class="table-actions">
                            <button class="btn btn--ghost btn--sm" data-action="edit" data-user-id="${user.userId}">Edit</button>
                            <button class="btn btn--danger btn--sm" data-action="delete" data-user-id="${user.userId}">Delete</button>
                        </td>
                    </tr>
                `;
            })
            .join('');

        container.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>User</th>
                        <th>Date of Birth</th>
                        <th>Subscription</th>
                        <th>Profiles</th>
                        <th>Joined</th>
                        <th></th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        `;

        container.querySelectorAll('button[data-action="edit"]').forEach((button) => {
            button.addEventListener('click', (event) => {
                const targetId = Number(event.currentTarget.dataset.userId);
                this.showUserModal(targetId);
            });
        });

        container.querySelectorAll('button[data-action="delete"]').forEach((button) => {
            button.addEventListener('click', (event) => {
                const targetId = Number(event.currentTarget.dataset.userId);
                this.deleteUserAccount(targetId);
            });
        });
    }

    renderActors() {
        const container = this.dom.tables.actors;
        if (!container) return;

        if (!this.state.actors.length) {
            container.innerHTML = '<div class="empty-state">No actors available.</div>';
            return;
        }

        const rows = this.state.actors
            .map((actor) => `
                <tr>
                    <td>${actor.actor_id ?? actor.actorId}</td>
                    <td>${actor.full_name ?? actor.fullName}</td>
                </tr>
            `)
            .join('');

        container.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        `;
    }

    renderDirectors() {
        const container = this.dom.tables.directors;
        if (!container) return;

        if (!this.state.directors.length) {
            container.innerHTML = '<div class="empty-state">No directors available.</div>';
            return;
        }

        const rows = this.state.directors
            .map((director) => `
                <tr>
                    <td>${director.director_id ?? director.directorId}</td>
                    <td>${director.full_name ?? director.fullName}</td>
                </tr>
            `)
            .join('');

        container.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                    </tr>
                </thead>
                <tbody>${rows}</tbody>
            </table>
        `;
    }

    renderDashboard() {
        if (!this.state.stats) {
            Object.values(this.dom.stats).forEach((el) => {
                if (el) el.textContent = '—';
            });
            return;
        }

        const stats = this.state.stats;
        if (this.dom.stats.totalTitles) {
            this.dom.stats.totalTitles.textContent = stats.totalTitles ?? stats.titleCount ?? 0;
        }
        if (this.dom.stats.totalUsers) {
            this.dom.stats.totalUsers.textContent = stats.totalUsers ?? 0;
        }
        if (this.dom.stats.totalRecommendations) {
            this.dom.stats.totalRecommendations.textContent = stats.totalRecommendationRequests ?? stats.totalRatingInteractions ?? 0;
        }
        if (this.dom.stats.userEngagement) {
            const totalViews = stats.totalWatchHistoryEntries || 0;
            const totalRatings = stats.totalRatingInteractions || 0;
            const engagement = stats.totalUsers ? Math.round((totalRatings / Math.max(totalViews, 1)) * 100) : 0;
            this.dom.stats.userEngagement.textContent = `${Math.min(100, Math.max(0, engagement))}%`;
        }

        this.renderDashboardChart();
    }

    renderDashboardChart() {
        const canvas = this.dom.charts.dashboard;
        if (!canvas || !window.Chart || !this.state.stats) {
            return;
        }

        const data = {
            labels: ['Movies', 'TV Shows'],
            datasets: [
                {
                    data: [this.state.stats.movieCount || 0, this.state.stats.tvShowCount || 0],
                    backgroundColor: ['#e50914', '#b20710'],
                    borderWidth: 0
                }
            ]
        };

        this.destroyChart('dashboard');
        this.chartInstances.dashboard = new Chart(canvas, {
            type: 'doughnut',
            data,
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 16
                        }
                    }
                }
            }
        });
    }

    destroyChart(key) {
        if (this.chartInstances[key]) {
            this.chartInstances[key].destroy();
            delete this.chartInstances[key];
        }
    }

    showSection(section) {
        this.dom.sections.forEach((el) => el.classList.remove('active'));
        const target = document.getElementById(`admin-${section}`);
        if (target) {
            target.classList.add('active');
        }

        this.dom.navItems.forEach((item) => item.classList.toggle('active', item.getAttribute('data-section') === section));

        if (this.dom.sectionTitle) {
            const label = this.dom.navItems.find((item) => item.getAttribute('data-section') === section)?.textContent?.trim();
            this.dom.sectionTitle.textContent = label || 'Dashboard';
        }

        if (section === 'dashboard') {
            this.renderDashboard();
        } else if (section === 'titles') {
            this.applyTitleFilters({ keepPage: true, showLoader: false });
        } else if (section === 'users') {
            this.renderUsers();
        } else if (section === 'actors') {
            this.renderActors();
        } else if (section === 'directors') {
            this.renderDirectors();
        } else if (section === 'recommendations-admin') {
            this.renderRecommendationChart();
        } else if (section === 'analytics') {
            this.renderAnalytics();
        }
    }

    renderActiveSection() {
        const activeNav = this.dom.navItems.find((item) => item.classList.contains('active'));
        const section = activeNav ? activeNav.getAttribute('data-section') : 'dashboard';
        this.showSection(section);
    }

    clearAllFilters() {
        this.filters.search = '';
        this.filters.types.clear();
        this.filters.genres.clear();
        this.filters.countries.clear();
        this.filters.ratings.clear();
        this.filters.yearMin = null;
        this.filters.yearMax = null;

        if (this.dom.titles.searchInput) this.dom.titles.searchInput.value = '';
        if (this.dom.titles.yearMin) this.dom.titles.yearMin.value = '';
        if (this.dom.titles.yearMax) this.dom.titles.yearMax.value = '';

        document.querySelectorAll('input[data-filter-group]').forEach((checkbox) => {
            checkbox.checked = false;
        });

        this.applyTitleFilters({ page: 1, showLoader: true });
    }

    setTitlesLoading(isLoading) {
        if (!this.dom.titles.tableContainer) return;
        if (isLoading) {
            this.dom.titles.tableContainer.innerHTML = '<div class="loading"><div class="loading-spinner"></div> Loading titles…</div>';
        } else if (this.dom.titles.tableContainer.querySelector('.loading-spinner')) {
            this.dom.titles.tableContainer.innerHTML = '';
        }
    }

    showTitleModal(titleId = null) {
        this.editingTitleId = titleId;
        const modal = this.dom.modals.title;
        if (!modal) return;

        const form = this.dom.forms.title;
        if (form) {
            form.reset();
        }

        const title = titleId ? this.state.titles.find((item) => item.titleId === titleId) : null;
        const titleHeader = document.getElementById('titleModalTitle');
        if (titleHeader) {
            titleHeader.textContent = title ? 'Edit Title' : 'Add New Title';
        }

        if (title && form) {
            const setValue = (selector, value = '') => {
                const field = form.querySelector(selector);
                if (field) field.value = value;
            };

            setValue('#titleShowId', title.showId || '');
            setValue('#titleName', title.title || '');
            setValue('#titleType', title.type || '');
            setValue('#titleYear', title.releaseYear || '');
            setValue('#titleRating', title.rating || '');
            setValue('#titleDuration', title.duration || '');
            setValue('#titleDescription', title.description || '');
            setValue('#titleGenres', (title.genres || []).join(', '));
            setValue('#titleCountries', (title.countries || []).join(', '));
            setValue('#titleDirectors', (title.directors || []).join(', '));
            setValue('#titleCast', (title.cast || []).join(', '));
            setValue('#titleDateAdded', title.dateAdded || '');
        }

        modal.classList.add('active');
    }

    closeModal(modalKey) {
        const targetKeys = modalKey ? [modalKey] : Object.keys(this.dom.modals || {});
        targetKeys.forEach((key) => {
            const modal = this.dom.modals?.[key];
            if (modal) {
                modal.classList.remove('active');
            }
        });

        if (!modalKey || modalKey === 'title') {
            this.editingTitleId = null;
        }

        if (!modalKey || modalKey === 'user') {
            this.editingUserId = null;
            this.activeUserDraft = null;
            this.resetUserForm();
        }
    }

    showUserModal(userId = null) {
        const modal = this.dom.modals.user;
        if (!modal) return;

        const numericId = Number(userId);
        const isEdit = Number.isFinite(numericId) && numericId > 0;
        const user = isEdit ? this.state.users.find((item) => item.userId === numericId) : null;

        if (isEdit && !user) {
            this.showToast('User not found.', 'error');
            return;
        }

        this.editingUserId = isEdit ? numericId : null;
        this.activeUserDraft = user ? { ...user } : null;

        const { inputs } = this.dom;
        if (inputs?.userEmail) {
            inputs.userEmail.value = user?.email || '';
        }
        if (inputs?.userUsername) {
            inputs.userUsername.value = user?.username || '';
        }
        if (inputs?.userDateOfBirth) {
            inputs.userDateOfBirth.value = user?.dateOfBirth || '';
        }
        if (inputs?.userPassword) {
            inputs.userPassword.value = '';
            inputs.userPassword.required = !isEdit;
            inputs.userPassword.placeholder = isEdit ? 'Leave blank to keep current password' : 'Initial password';
        }
        if (inputs?.userPasswordHelp) {
            inputs.userPasswordHelp.textContent = isEdit
                ? 'Leave blank to keep the existing password.'
                : 'Required when creating a user.';
        }

        this.populateUserPlanOptions(isEdit ? 'edit' : 'create', user?.subscription || null);

        const modalTitle = document.getElementById('userModalTitle');
        if (modalTitle) {
            modalTitle.textContent = isEdit ? `Edit ${user?.username || user?.email || 'User'}` : 'Add New User';
        }

        modal.classList.add('active');
    }

    resetUserForm() {
        const { inputs } = this.dom;
        if (!inputs) return;

        if (inputs.userEmail) inputs.userEmail.value = '';
        if (inputs.userUsername) inputs.userUsername.value = '';
        if (inputs.userPassword) {
            inputs.userPassword.value = '';
            inputs.userPassword.required = true;
            inputs.userPassword.placeholder = 'Initial password';
        }
        if (inputs.userPasswordHelp) {
            inputs.userPasswordHelp.textContent = 'Required when creating a user.';
        }
        if (inputs.userDateOfBirth) inputs.userDateOfBirth.value = '';

        this.populateUserPlanOptions('create');
    }

    populateUserPlanOptions(mode = 'create', currentSubscription = null) {
        const select = this.dom.inputs?.userSubscriptionPlan;
        if (!select) return;

        const plans = Array.isArray(this.state.subscriptionPlans) ? this.state.subscriptionPlans : [];
        const options = [];

        if (mode === 'edit') {
            options.push({ value: 'keep', label: 'Keep current plan' });
            options.push({ value: 'none', label: 'Cancel subscription' });
        } else {
            options.push({ value: 'none', label: 'No active subscription' });
        }

        plans.forEach((plan) => {
            const planId = plan.planId ?? plan.plan_id;
            const planName = plan.planName ?? plan.plan_name;
            if (!planId || !planName) return;

            const labelParts = [planName];
            if (plan.quality) {
                labelParts.push(`• ${plan.quality}`);
            }
            if (plan.screensAllowed ?? plan.screens_allowed) {
                labelParts.push(`• ${(plan.screensAllowed ?? plan.screens_allowed)} screens`);
            }
            if (plan.price != null) {
                labelParts.push(`• ${this.formatCurrency(plan.price)}`);
            }

            options.push({ value: String(planId), label: labelParts.join(' ') });
        });

        select.innerHTML = options.map((option) => `<option value="${option.value}">${option.label}</option>`).join('');

        if (mode === 'edit') {
            if (currentSubscription?.planId != null) {
                select.value = String(currentSubscription.planId);
            } else {
                select.value = 'none';
            }
        } else {
            select.value = 'none';
        }
    }

    async saveTitleForm() {
        const form = this.dom.forms.title;
        if (!form) return;

        const payload = {
            show_id: form.querySelector('#titleShowId')?.value.trim() || null,
            title: form.querySelector('#titleName')?.value.trim(),
            type: form.querySelector('#titleType')?.value,
            release_year: Number(form.querySelector('#titleYear')?.value) || null,
            rating: form.querySelector('#titleRating')?.value,
            duration: form.querySelector('#titleDuration')?.value.trim(),
            description: form.querySelector('#titleDescription')?.value.trim(),
            genres: this.parseList(form.querySelector('#titleGenres')?.value),
            countries: this.parseList(form.querySelector('#titleCountries')?.value),
            directors: this.parseList(form.querySelector('#titleDirectors')?.value),
            cast: this.parseList(form.querySelector('#titleCast')?.value),
            date_added: form.querySelector('#titleDateAdded')?.value || null
        };

        if (!payload.title || !payload.type || !payload.release_year || !payload.rating || !payload.duration) {
            this.showToast('Please fill all required fields.', 'error');
            return;
        }

        try {
            if (this.editingTitleId) {
                await this.putJson(`/titles/${this.editingTitleId}`, payload);
                this.showToast('Title updated successfully.', 'success');
            } else {
                await this.postJson('/titles', payload);
                this.showToast('Title created successfully.', 'success');
            }
            await this.loadInitialData();
            this.closeModal('title');
        } catch (error) {
            this.showToast(error.message || 'Unable to save title', 'error');
        }
    }

    async saveUserForm() {
        const { inputs } = this.dom;
        if (!inputs) return;

        const isEdit = Number.isFinite(this.editingUserId) && this.editingUserId > 0;
        const email = inputs.userEmail?.value.trim();
        const username = inputs.userUsername?.value.trim();
    const password = (inputs.userPassword?.value || '').trim();
        const rawDob = inputs.userDateOfBirth?.value || '';
        const planSelection = inputs.userSubscriptionPlan?.value;

        if (!email) {
            this.showToast('Email is required.', 'error');
            return;
        }
        if (!username) {
            this.showToast('Username is required.', 'error');
            return;
        }
        if (!isEdit && !password) {
            this.showToast('Password is required when creating a user.', 'error');
            return;
        }

        const payload = {
            email,
            username
        };

        if (!isEdit || password) {
            payload.password = password;
        }

        const originalDob = this.activeUserDraft?.dateOfBirth || '';
        if (!isEdit) {
            if (rawDob) {
                payload.dateOfBirth = rawDob;
            }
        } else if (rawDob !== originalDob) {
            payload.dateOfBirth = rawDob;
        }

        const originalPlanId = this.activeUserDraft?.subscription?.planId;
        if (!isEdit) {
            if (planSelection && planSelection !== 'none' && planSelection !== 'keep') {
                payload.subscriptionPlanId = Number(planSelection);
            }
        } else if (planSelection === 'none') {
            if (originalPlanId != null) {
                payload.cancelSubscription = true;
            }
        } else if (planSelection && planSelection !== 'keep') {
            const nextPlanId = Number(planSelection);
            if (!Number.isNaN(nextPlanId) && nextPlanId > 0 && nextPlanId !== Number(originalPlanId)) {
                payload.subscriptionPlanId = nextPlanId;
            }
        }

        try {
            const endpoint = isEdit ? `/admin/users/${this.editingUserId}` : '/admin/users';
            const result = isEdit ? await this.putJson(endpoint, payload) : await this.postJson(endpoint, payload);
            this.upsertUser(result);
            this.showToast(isEdit ? 'User updated successfully.' : 'User created successfully.', 'success');
            this.closeModal('user');
        } catch (error) {
            this.showToast(error.message || 'Unable to save user', 'error');
        }
    }

    upsertUser(user) {
        if (!user) return;
        const existingIndex = this.state.users.findIndex((item) => item.userId === user.userId);
        if (existingIndex >= 0) {
            this.state.users.splice(existingIndex, 1, user);
        } else {
            this.state.users.push(user);
        }
        this.state.users.sort((a, b) => (a.userId ?? 0) - (b.userId ?? 0));
        this.renderUsers();
    }

    removeUserFromState(userId) {
        this.state.users = this.state.users.filter((user) => user.userId !== userId);
        this.renderUsers();
    }

    async refreshUsers() {
        try {
            const users = await this.getJson('/admin/users');
            this.state.users = Array.isArray(users) ? users : [];
            this.renderUsers();
            this.showToast('Users refreshed.', 'success');
        } catch (error) {
            this.showToast(error.message || 'Unable to refresh users', 'error');
        }
    }

    async deleteUserAccount(userId) {
        if (!Number.isFinite(userId) || userId <= 0) return;

        const confirmed = window.confirm('Delete this user and all associated data?');
        if (!confirmed) return;

        try {
            await this.deleteJson(`/admin/users/${userId}`);
            this.removeUserFromState(userId);
            this.showToast('User deleted.', 'success');
        } catch (error) {
            this.showToast(error.message || 'Unable to delete user', 'error');
        }
    }

    parseList(value = '') {
        return value
            .split(',')
            .map((token) => token.trim())
            .filter(Boolean);
    }

    escapeHtml(value = '') {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        };
        return String(value ?? '').replace(/[&<>"']/g, (char) => map[char]);
    }

    capitalize(value = '') {
        if (!value) return '';
        return String(value).charAt(0).toUpperCase() + String(value).slice(1);
    }

    formatDate(value, includeTime = false) {
        if (!value) return '—';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return typeof value === 'string' ? value : '—';
        }

        const options = { year: 'numeric', month: 'short', day: '2-digit' };
        if (includeTime) {
            options.hour = '2-digit';
            options.minute = '2-digit';
        }
        return date.toLocaleString(undefined, options);
    }

    formatCurrency(value) {
        if (value == null) {
            return '';
        }
        const number = typeof value === 'number' ? value : Number(value);
        if (Number.isNaN(number)) {
            return `$${value}`;
        }
        return number.toLocaleString(undefined, { style: 'currency', currency: 'USD', minimumFractionDigits: 2 });
    }

    async deleteTitle(titleId) {
        if (!window.confirm('Delete this title?')) {
            return;
        }

        try {
            await this.deleteJson(`/titles/${titleId}`);
            this.showToast('Title deleted.', 'success');
            await this.loadInitialData();
        } catch (error) {
            this.showToast(error.message || 'Unable to delete title', 'error');
        }
    }

    exportTitles() {
        const rows = this.state.filteredTitles.map((title) => [
            title.title,
            title.type,
            title.releaseYear,
            title.rating,
            (title.genres || []).join('; '),
            (title.countries || []).join('; ')
        ]);

        const csv = [
            ['Title', 'Type', 'Year', 'Rating', 'Genres', 'Countries'],
            ...rows
        ]
            .map((row) => row.map((cell) => `"${(cell ?? '').toString().replace(/"/g, '""')}"`).join(','))
            .join('\n');

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'titles.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    saveRecommendationSettings() {
        const weights = {};
        const sliderIds = ['genreWeight', 'historyWeight', 'ratingWeight', 'popularityWeight'];

        sliderIds.forEach((id) => {
            const input = document.getElementById(id);
            if (input) {
                weights[id] = Number(input.value);
            }
        });

        const total = sliderIds.reduce((sum, id) => sum + (weights[id] || 0), 0);
        localStorage.setItem('adminRecommendationWeights', JSON.stringify(weights));

        if (Math.abs(total - 1) > 0.05) {
            this.showToast(`Settings saved, but weights sum to ${Math.round(total * 100)}%. Consider normalizing to 100%.`, 'info');
        } else {
            this.showToast('Recommendation settings saved.', 'success');
        }
    }

    getGenreCounts() {
        return this.state.titles.reduce((acc, title) => {
            (title.genres || []).forEach((genre) => {
                const key = genre;
                acc[key] = (acc[key] || 0) + 1;
            });
            return acc;
        }, {});
    }

    getRatingCounts() {
        return this.state.titles.reduce((acc, title) => {
            const key = title.rating || 'NR';
            acc[key] = (acc[key] || 0) + 1;
            return acc;
        }, {});
    }

    getGenreNameById(id) {
        const match = this.state.genres.find((genre) => String(genre.id) === String(id));
        return match ? match.name : id;
    }

    renderRecommendationChart() {
        const canvas = this.dom.charts.recommendation;
        if (!canvas || !window.Chart) {
            return;
        }

        if (!this.state.titles.length) {
            this.destroyChart('recommendation');
            const ctx = canvas.getContext('2d');
            if (ctx) {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            }
            return;
        }

        const genreCounts = this.getGenreCounts();
        const topGenres = Object.entries(genreCounts)
            .sort((a, b) => b[1] - a[1])
            .slice(0, 6);

        if (!topGenres.length) {
            this.destroyChart('recommendation');
            const ctx = canvas.getContext('2d');
            if (ctx) {
                ctx.clearRect(0, 0, canvas.width, canvas.height);
            }
            return;
        }

        const labels = topGenres.map(([genre]) => genre);
        const data = topGenres.map(([, count]) => count);

        this.destroyChart('recommendation');
        this.chartInstances.recommendation = new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [
                    {
                        data,
                        backgroundColor: '#e50914'
                    }
                ]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: { ticks: { color: '#fff' } },
                    y: {
                        beginAtZero: true,
                        ticks: { color: '#fff', precision: 0 }
                    }
                }
            }
        });
    }

    renderAnalytics() {
        const container = document.getElementById('analyticsContainer');
        if (!container) {
            return;
        }

        if (!this.state.stats) {
            container.innerHTML = '<div class="empty-state">Analytics data will appear once stats are available.</div>';
            return;
        }

        const stats = this.state.stats;
        const ratingCounts = Object.entries(this.getRatingCounts()).sort((a, b) => b[1] - a[1]);
        const topGenres = Object.entries(this.getGenreCounts()).sort((a, b) => b[1] - a[1]).slice(0, 10);

        const overviewCards = [
            { label: 'Profiles', value: stats.totalProfiles ?? 0 },
            { label: 'Actors', value: stats.totalActors ?? 0 },
            { label: 'Directors', value: stats.totalDirectors ?? 0 },
            { label: 'Watch Entries', value: stats.totalWatchHistoryEntries ?? 0 },
            { label: 'Rating Interactions', value: stats.totalRatingInteractions ?? 0 }
        ]
            .map(
                (card) => `
                <div class="analytics-card compact">
                    <div class="stat-number">${card.value}</div>
                    <div class="stat-label">${card.label}</div>
                </div>
            `
            )
            .join('');

        const ratingRows = ratingCounts
            .map(([rating, count]) => `<tr><td>${rating}</td><td>${count}</td></tr>`)
            .join('');

        const genreRows = topGenres
            .map(([genre, count]) => `<tr><td>${genre}</td><td>${count}</td></tr>`)
            .join('');

        container.innerHTML = `
            <div class="analytics-grid secondary">${overviewCards}</div>
            <div class="analytics-tables">
                <div class="analytics-panel">
                    <h3>Rating Distribution</h3>
                    <table class="data-table">
                        <thead><tr><th>Rating</th><th>Titles</th></tr></thead>
                        <tbody>${ratingRows || '<tr><td colspan="2">No ratings available</td></tr>'}</tbody>
                    </table>
                </div>
                <div class="analytics-panel">
                    <h3>Top Genres</h3>
                    <table class="data-table">
                        <thead><tr><th>Genre</th><th>Titles</th></tr></thead>
                        <tbody>${genreRows || '<tr><td colspan="2">No genres available</td></tr>'}</tbody>
                    </table>
                </div>
            </div>
        `;
    }

    showToast(message, type = 'success') {
        let toast = document.getElementById('adminToast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'adminToast';
            toast.className = 'toast hidden';
            toast.innerHTML = `
                <span id="adminToastMessage"></span>
                <button type="button" id="adminToastClose">×</button>
            `;
            document.body.appendChild(toast);
            document.getElementById('adminToastClose').addEventListener('click', () => this.hideToast());
        }

        const messageEl = document.getElementById('adminToastMessage');
        toast.className = `toast toast--${type}`;
        if (messageEl) {
            messageEl.textContent = message;
        }
        toast.classList.remove('hidden');

        clearTimeout(this.toastTimeout);
        this.toastTimeout = setTimeout(() => this.hideToast(), 4000);
    }

    hideToast() {
        const toast = document.getElementById('adminToast');
        if (toast) {
            toast.classList.add('hidden');
        }
    }

    async getJson(path) {
        const response = await fetch(this.buildUrl(path), {
            headers: { Accept: 'application/json' }
        });
        return this.handleResponse(response);
    }

    async postJson(path, payload) {
        const response = await fetch(this.buildUrl(path), {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: JSON.stringify(payload)
        });
        return this.handleResponse(response);
    }

    async putJson(path, payload) {
        const response = await fetch(this.buildUrl(path), {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json'
            },
            body: JSON.stringify(payload)
        });
        return this.handleResponse(response);
    }

    async deleteJson(path) {
        const response = await fetch(this.buildUrl(path), {
            method: 'DELETE',
            headers: { Accept: 'application/json' }
        });
        if (!response.ok) {
            throw new Error(await this.extractErrorMessage(response));
        }
        return true;
    }

    async handleResponse(response) {
        if (response.ok) {
            const contentType = response.headers.get('Content-Type') || '';
            if (contentType.includes('application/json')) {
                return response.json();
            }
            return null;
        }
        throw new Error(await this.extractErrorMessage(response));
    }

    async extractErrorMessage(response) {
        try {
            const data = await response.json();
            return data?.message || JSON.stringify(data);
        } catch (error) {
            return response.statusText || 'Request failed';
        }
    }

    buildUrl(path) {
        if (!path) return this.apiBaseUrl;
        if (path.startsWith('http')) return path;
        return `${this.apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const app = new AdminApp();
    window.adminApp = app;
    window.netflixApp = app; // Backwards compatibility with legacy handlers
});