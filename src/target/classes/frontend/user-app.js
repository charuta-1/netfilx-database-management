class UserApp {
    constructor() {
        this.apiBaseUrl = window.netflixApiBaseUrl || '/api';
        this.currentUser = this.safeParse(localStorage.getItem('netflixUser'));
        this.currentProfile = this.normalizeProfileRecord(this.safeParse(localStorage.getItem('selectedProfile')));
        this.subscription = this.safeParse(localStorage.getItem('netflixSubscription'));
        this.state = {
            titles: [],
            titleMap: new Map(),
            recommendations: [],
            watchlist: new Set(),
            watchHistory: [],
            ratings: new Map(),
            continueWatching: [],
            genres: [],
            countries: [],
            subscriptionPlans: [],
            activeSubscriptionId: null
        };
        this.filters = { genre: '', type: '', rating: '', country: '', search: '' };
        this.updateBrowseGridDebounced = this.debounce(() => this.renderBrowseGrid(), 200);
        this.currentModalTitleId = null;
        this.isInitialized = false;
        this.ratingHierarchy = [
            'G',
            'TV-Y',
            'TV-Y7',
            'TV-G',
            'PG',
            'TV-PG',
            'PG-13',
            'TV-14',
            'R',
            'TV-MA',
            'NC-17'
        ];
        this.ageContext = this.buildAgeContext();

        if (!this.currentUser || !this.currentProfile) {
            window.location.href = 'login.html';
            return;
        }

        this.init();
    }

    safeParse(json) {
        try {
            return json ? JSON.parse(json) : null;
        } catch (error) {
            console.error('Failed to parse JSON from storage', error);
            return null;
        }
    }

    debounce(fn, wait = 200) {
        let timeoutId;
        return (...args) => {
            clearTimeout(timeoutId);
            timeoutId = setTimeout(() => fn.apply(this, args), wait);
        };
    }

    async init() {
        try {
            this.setupEventListeners();
            await this.loadInitialData();
            this.updateProfileUI();
            this.ageContext = this.buildAgeContext();
            this.renderHome();
            this.updateSubscriptionBadge();
            this.updateAgeNotice();
            this.showSection('home');
            this.isInitialized = true;
        } catch (error) {
            this.showToast(error.message || 'Failed to load dashboard', 'error');
        }
    }

    setupEventListeners() {
        document.querySelectorAll('.nav-link').forEach((link) => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                const section = link.getAttribute('data-section');
                this.showSection(section);
                this.updateActiveNav(link);
            });
        });

        const logoutBtn = document.getElementById('logout');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', (event) => {
                event.preventDefault();
                this.logout();
            });
        }

        const refreshRecommendations = document.getElementById('refreshRecommendations');
        if (refreshRecommendations) {
            refreshRecommendations.addEventListener('click', () => this.refreshRecommendations());
        }

        const profileTrigger = document.getElementById('currentProfile');
        if (profileTrigger) {
            profileTrigger.addEventListener('click', () => this.toggleProfileDropdown());
        }

    const genreFilter = document.getElementById('genreFilter');
        const typeFilter = document.getElementById('typeFilter');
        const ratingFilter = document.getElementById('ratingFilter');
        const countryFilter = document.getElementById('countryFilter');
        const clearBrowseFilters = document.getElementById('clearBrowseFilters');
        const browseSearch = document.getElementById('browseSearchInput');
        if (genreFilter) {
            genreFilter.addEventListener('change', (event) => {
                this.filters.genre = event.target.value;
                this.updateBrowseGridDebounced();
            });
        }
        if (typeFilter) {
            typeFilter.addEventListener('change', (event) => {
                this.filters.type = event.target.value;
                this.updateBrowseGridDebounced();
            });
        }
        if (ratingFilter) {
            ratingFilter.addEventListener('change', (event) => {
                this.filters.rating = event.target.value;
                this.updateBrowseGridDebounced();
            });
        }
        if (countryFilter) {
            countryFilter.addEventListener('change', (event) => {
                this.filters.country = event.target.value;
                this.updateBrowseGridDebounced();
            });
        }
        if (clearBrowseFilters) {
            clearBrowseFilters.addEventListener('click', () => {
                this.filters = { genre: '', type: '', rating: '', country: '', search: '' };
                if (genreFilter) genreFilter.value = '';
                if (typeFilter) typeFilter.value = '';
                if (ratingFilter) ratingFilter.value = '';
                if (countryFilter) countryFilter.value = '';
                if (browseSearch) browseSearch.value = '';
                this.updateBrowseGridDebounced();
            });
        }
        if (browseSearch) {
            browseSearch.addEventListener('input', (event) => {
                this.filters.search = event.target.value || '';
                this.updateBrowseGridDebounced();
            });
        }

        document.querySelectorAll('.modal-close').forEach((btn) => btn.addEventListener('click', () => this.hideModal()));
        document.querySelectorAll('.modal').forEach((modal) => {
            modal.addEventListener('click', (event) => {
                if (event.target === modal) {
                    this.hideModal();
                }
            });
        });

        const subscriptionModalDone = document.getElementById('subscriptionModalDone');
        if (subscriptionModalDone) {
            subscriptionModalDone.addEventListener('click', () => this.hideModal());
        }

        const toastClose = document.getElementById('toastClose');
        if (toastClose) {
            toastClose.addEventListener('click', () => {
                const toast = document.getElementById('toast');
                if (toast) {
                    toast.classList.add('hidden');
                }
            });
        }

        const thumbsUpBtn = document.getElementById('thumbsUpBtn');
        const thumbsDownBtn = document.getElementById('thumbsDownBtn');
        const notInterestedBtn = document.getElementById('notInterestedBtn');
        if (thumbsUpBtn) {
            thumbsUpBtn.addEventListener('click', () => {
                if (this.currentModalTitleId) {
                    this.rateTitle(this.currentModalTitleId, 'thumbs_up');
                }
            });
        }
        if (thumbsDownBtn) {
            thumbsDownBtn.addEventListener('click', () => {
                if (this.currentModalTitleId) {
                    this.rateTitle(this.currentModalTitleId, 'thumbs_down');
                }
            });
        }
        if (notInterestedBtn) {
            notInterestedBtn.addEventListener('click', () => {
                if (this.currentModalTitleId) {
                    this.rateTitle(this.currentModalTitleId, 'thumbs_down');
                }
            });
        }

        document.addEventListener('click', (event) => {
            const trigger = event.target.closest('[data-title-action]');
            if (!trigger) {
                return;
            }

            const action = trigger.getAttribute('data-title-action');
            if (!action) {
                return;
            }

            if (action === 'view-subscription') {
                this.showSubscriptionDetails();
                return;
            }

            const rawTitleId = trigger.getAttribute('data-title-id');
            if (!rawTitleId) {
                return;
            }

            const titleId = this.normalizeTitleIdValue(rawTitleId);
            if (titleId === null || titleId === undefined) {
                return;
            }

            switch (action) {
                case 'open-modal':
                    this.showTitleModal(titleId);
                    break;
                case 'log-watch':
                    this.logWatchEvent(titleId);
                    break;
                case 'toggle-watchlist':
                    this.toggleWatchlist(titleId);
                    break;
                case 'rate-up':
                    this.rateTitle(titleId, 'thumbs_up');
                    break;
                case 'rate-down':
                    this.rateTitle(titleId, 'thumbs_down');
                    break;
                default:
                    break;
            }
        });
    }

    async loadInitialData() {
        this.toggleLoading(true);
        try {
            this.ageContext = this.buildAgeContext();
            const [titles, watchlist, continueWatching, recommendations, ratings, genres, subscriptionInfo, plans] = await Promise.all([
                this.getJson('/titles'),
                this.getJson(`/watchlist/${this.currentProfile.profileId}`),
                this.getJson(`/watch-history/${this.currentProfile.profileId}/continue-watching`),
                this.getJson(`/recommendations/${this.currentProfile.profileId}?limit=20`),
                this.getJson(`/ratings/profile/${this.currentProfile.profileId}`),
                this.getJson('/genres'),
                this.getJson(`/subscriptions/user/${this.currentUser.userId}`),
                this.getJson('/subscriptions/plans')
            ]);

            const normalizedTitles = Array.isArray(titles)
                ? titles
                    .map((title) => this.normalizeTitle(title))
                    .filter((normalized) => normalized?.titleId !== null && normalized?.titleId !== undefined)
                : [];
            this.state.titles = normalizedTitles;
            this.state.titleMap = new Map(normalizedTitles.map((title) => [this.normalizeTitleIdForMap(title.titleId), title]));
            this.state.countries = this.extractCountries(normalizedTitles);

            if (Array.isArray(watchlist)) {
                this.state.watchlist = new Set(
                    watchlist
                        .map((entry) => this.normalizeTitleIdForMap(this.normalizeTitleIdValue(entry?.titleId ?? entry?.title_id)))
                        .filter((id) => id !== null && id !== undefined)
                );
            }

            this.state.continueWatching = Array.isArray(continueWatching)
                ? continueWatching
                    .map((entry) => this.normalizeHistoryEntry(entry))
                    .filter(Boolean)
                : [];
            this.state.watchHistory = this.state.continueWatching.map((entry) => ({ ...entry }));

            const preparedRecommendations = this.prepareRecommendations(recommendations, normalizedTitles);
            this.state.recommendations = preparedRecommendations;
            preparedRecommendations.forEach((title) => {
                const key = this.normalizeTitleIdForMap(title?.titleId);
                if (title && key !== null && key !== undefined && !this.state.titleMap.has(key)) {
                    this.state.titleMap.set(key, title);
                }
            });

            if (Array.isArray(ratings)) {
                this.state.ratings = new Map(
                    ratings
                        .map((rating) => [
                            this.normalizeTitleIdForMap(this.normalizeTitleIdValue(rating?.titleId ?? rating?.title_id)),
                            rating?.ratingValue ?? rating?.rating_value
                        ])
                        .filter(([titleId]) => titleId !== null && titleId !== undefined)
                );
            }

            if (Array.isArray(genres)) {
                this.state.genres = genres.map((genre) => ({
                    genreId: genre.genreId ?? genre.genre_id,
                    name: genre.name || genre.genreName || 'Unknown'
                })).sort((a, b) => a.name.localeCompare(b.name));
            }

            const activeSubscription = subscriptionInfo?.activeSubscription ?? null;
            this.subscription = activeSubscription || null;
            this.state.activeSubscriptionId = activeSubscription?.subscriptionId ?? activeSubscription?.subscription_id ?? null;
            if (subscriptionInfo && Object.prototype.hasOwnProperty.call(subscriptionInfo, 'activeSubscription')) {
                if (this.subscription) {
                    localStorage.setItem('netflixSubscription', JSON.stringify(this.subscription));
                } else {
                    localStorage.removeItem('netflixSubscription');
                }
            }

            this.state.subscriptionPlans = Array.isArray(plans)
                ? plans.map((plan) => this.normalizePlan(plan))
                : [];

            this.populateGenreFilter();
            this.populateCountryFilter();
            await this.populateProfileDropdown();
            this.renderBrowseGrid();
            this.updateSubscriptionBadge();
            this.renderSubscriptionPlans();
        } finally {
            this.toggleLoading(false);
        }
    }

    normalizeTitle(raw) {
        const ratingCode = this.getRatingCodeFromRaw(raw);
        const normalizedId = this.normalizeTitleIdValue(raw?.title_id ?? raw?.titleId ?? raw?.show_id ?? raw?.showId);
        if (normalizedId === null || normalizedId === undefined) {
            return null;
        }
        const fallback = {
            titleId: normalizedId,
            showId: this.normalizeTitleIdValue(raw?.show_id ?? raw?.showId ?? normalizedId),
            title: raw.title,
            type: raw.type,
            description: raw.description || 'Description coming soon.',
            dateAdded: raw.date_added ?? raw.dateAdded,
            releaseYear: raw.release_year ?? raw.releaseYear ?? '—',
            rating: ratingCode || 'NR',
            ratingCode: ratingCode || 'NR',
            duration: raw.duration || '—',
            countries: this.normalizeCountries(raw.countries ?? raw.country ?? raw.country_list),
            genres: raw.genres || [],
            directors: raw.directors || [],
            cast: raw.cast || []
        };
        return fallback;
    }

    normalizeHistoryEntry(entry = {}) {
        const normalizedId = this.normalizeTitleIdValue(entry?.titleId ?? entry?.title_id);
        if (normalizedId === null || normalizedId === undefined) {
            return null;
        }
        return {
            titleId: normalizedId,
            watchedAt: entry?.watchedAt ?? entry?.watched_at ?? entry?.updatedAt ?? entry?.updated_at ?? new Date().toISOString(),
            completed: Boolean(entry?.completed ?? entry?.isCompleted ?? entry?.completedFlag ?? true)
        };
    }

    normalizeCountries(value) {
        if (!value) {
            return [];
        }
        if (Array.isArray(value)) {
            return value.map((entry) => String(entry).trim()).filter(Boolean);
        }
        if (typeof value === 'string') {
            return value
                .split(/[,;|]/)
                .map((entry) => entry.trim())
                .filter(Boolean);
        }
        return [];
    }

    normalizeMaturityOverride(value) {
        if (typeof value !== 'string') {
            return null;
        }
        const trimmed = value.trim();
        return trimmed ? trimmed.toUpperCase() : null;
    }

    normalizeProfileRecord(rawProfile) {
        if (!rawProfile || typeof rawProfile !== 'object') {
            return null;
        }

        const rawId = rawProfile.profileId ?? rawProfile.profile_id ?? null;
        let profileId = rawId;
        if (rawId !== null && rawId !== undefined) {
            const numericId = Number(rawId);
            if (!Number.isNaN(numericId)) {
                profileId = numericId;
            }
        }
        const normalizedOverride = this.normalizeMaturityOverride(
            rawProfile.maturityRatingOverride ?? rawProfile.maturity_rating_override ?? null
        );

        return {
            ...rawProfile,
            profileId,
            profile_id: profileId,
            maturityRatingOverride: normalizedOverride,
            maturity_rating_override: normalizedOverride
        };
    }

    getProfileMaturityLabel(profile) {
        if (!profile) {
            return '';
        }
        const override = this.normalizeMaturityOverride(
            profile.maturityRatingOverride ?? profile.maturity_rating_override ?? null
        );
        if (override) {
            return override;
        }
    return 'Auto (Age-Based)';
    }

    updateProfileUI() {
        const profileNameEl = document.querySelector('#currentProfile .profile-name');
        if (profileNameEl) {
            const profileName = this.currentProfile?.profileName || this.currentProfile?.profile_name || 'Profile';
            profileNameEl.textContent = profileName;
        }

        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) {
            dropdown.classList.add('hidden');
        }

        const profileTrigger = document.getElementById('currentProfile');
        if (profileTrigger) {
            const label = this.getProfileMaturityLabel(this.currentProfile);
            profileTrigger.setAttribute('title', `Maturity: ${label || 'Auto (age-based)'}`);
        }

        const manageProfilesLink = document.getElementById('manageProfiles');
        if (manageProfilesLink) {
            manageProfilesLink.onclick = (event) => {
                event.preventDefault();
                this.showToast('Profile management is coming soon.', 'info');
            };
        }

        const genrePreferencesLink = document.getElementById('genrePreferences');
        if (genrePreferencesLink) {
            genrePreferencesLink.onclick = (event) => {
                event.preventDefault();
                this.showToast('Genre preferences will be available shortly.', 'info');
            };
        }
    }

    async populateProfileDropdown() {
        const listContainer = document.getElementById('dropdownProfiles');
        if (!listContainer) {
            return;
        }

        listContainer.innerHTML = '<p class="text-muted">Loading profiles…</p>';

        try {
            const profiles = await this.getJson(`/profiles/user/${this.currentUser.userId}`);
            const normalizedProfiles = Array.isArray(profiles)
                ? profiles
                    .map((profile) => this.normalizeProfileRecord(profile))
                    .filter((profile) => profile && profile.profileId !== undefined && profile.profileId !== null)
                : [];

            if (!normalizedProfiles.length) {
                listContainer.innerHTML = '<p class="text-muted">No profiles yet.</p>';
                return;
            }

            listContainer.innerHTML = normalizedProfiles.map((profile) => {
                const profileId = profile.profileId;
                const profileName = profile.profileName ?? profile.profile_name ?? 'Profile';
                const maturityLabel = this.getProfileMaturityLabel(profile);
                const profileKey = this.encodeAttribute(String(profileId));
                const nameMarkup = this.encodeAttribute(profileName);
                const metaMarkup = maturityLabel
                    ? `<span class="dropdown-profile__meta">${this.encodeAttribute(maturityLabel)}</span>`
                    : '';
                return `
                    <button type="button" class="dropdown-profile" data-profile-key="${profileKey}">
                        <span class="dropdown-profile__name">${nameMarkup}</span>
                        ${metaMarkup}
                    </button>
                `;
            }).join('');

            listContainer.querySelectorAll('.dropdown-profile').forEach((button) => {
                button.addEventListener('click', () => {
                    const profileKey = button.getAttribute('data-profile-key');
                    const selectedProfile = normalizedProfiles.find((profile) => this.encodeAttribute(String(profile.profileId)) === profileKey);
                    if (selectedProfile) {
                        const normalizedSelection = this.normalizeProfileRecord(selectedProfile);
                        localStorage.setItem('selectedProfile', JSON.stringify(normalizedSelection));
                        window.location.reload();
                    }
                });
            });
        } catch (error) {
            console.warn('Failed to load profiles', error);
            listContainer.innerHTML = '<p class="text-muted">Unable to load profiles</p>';
        }
    }

    renderHome() {
        this.renderHeroTitle();
        this.renderRecommendationsRow();
        this.renderContinueWatchingRow();
        this.renderWatchlistRow();
        this.renderGenreRows();
        this.renderPopularRow();
        this.renderSubscriptionPlans();
    }

    renderHeroTitle() {
        const heroBanner = document.getElementById('heroBanner');
        if (!heroBanner) {
            return;
        }

        const allowedRecommendations = this.filterTitlesForAge(this.state.recommendations);
        const allowedTitles = this.filterTitlesForAge(this.state.titles);
        const title = allowedRecommendations[0] || allowedTitles[0];
        if (!title) {
            heroBanner.classList.add('hidden');
            return;
        }

    heroBanner.classList.remove('hidden');
    const accent = this.getAccentColor(title.titleId || title.title);
    heroBanner.style.setProperty('--hero-accent', accent);
        heroBanner.style.removeProperty('background-image');
        heroBanner.style.removeProperty('background');

        const heroTitle = heroBanner.querySelector('.hero-title');
        const heroDescription = heroBanner.querySelector('.hero-description');
        if (heroTitle) heroTitle.textContent = title.title;
        if (heroDescription) heroDescription.textContent = title.description;

        const playBtn = document.getElementById('logWatchBtn');
        const infoBtn = document.getElementById('infoBtn');
        if (playBtn) {
            playBtn.onclick = () => this.logWatchEvent(title.titleId);
        }
        if (infoBtn) {
            infoBtn.onclick = () => this.showTitleModal(title.titleId);
        }
    }

    renderRecommendationsRow() {
        const grid = document.getElementById('recommendationsGrid');
        if (!grid) {
            return;
        }

        const items = this.filterTitlesForAge(this.state.recommendations).map((title) => this.renderTitleCard(title)).join('');
        grid.innerHTML = items || '<div class="empty-state">Recommendations will appear here once available.</div>';
    }

    renderContinueWatchingRow() {
        const grid = document.getElementById('continueWatchingGrid');
        if (!grid) {
            return;
        }

        const titles = this.state.continueWatching
                .map((item) => this.state.titleMap.get(this.normalizeTitleIdForMap(item?.titleId ?? item?.title_id)))
            .filter(Boolean)
            .slice(0, 12);

        grid.innerHTML = titles.length
            ? titles.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">Log a title to begin building your watch history.</div>';
    }

    renderWatchlistRow() {
        const grid = document.getElementById('myListGrid');
        if (!grid) {
            return;
        }

        const titles = Array.from(this.state.watchlist)
                .map((titleId) => this.state.titleMap.get(this.normalizeTitleIdForMap(titleId)))
            .filter(Boolean);

        grid.innerHTML = titles.length
            ? titles.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">Save titles you want to remember for later.</div>';
    }

    renderGenreRows() {
        const container = document.getElementById('genreBasedRows');
        if (!container) {
            return;
        }

        if (!this.state.genres.length || !this.state.recommendations.length) {
            container.innerHTML = this.state.recommendations.length
                ? ''
                : '<div class="empty-state">We will personalize these rows once we have recommendations.</div>';
            return;
        }

        const genreSections = this.state.genres.slice(0, 3).map((genre) => {
            const titlesForGenre = this.state.recommendations
                .filter((title) => Array.isArray(title.genres) && title.genres.some((g) => g.toLowerCase() === genre.name.toLowerCase()))
                .slice(0, 12);

            if (!titlesForGenre.length) {
                return '';
            }

            return `
                <div class="content-row">
                    <h2>${genre.name}</h2>
                    <div class="row-slider">
                        <div class="title-grid">
                            ${titlesForGenre.map((title) => this.renderTitleCard(title)).join('')}
                        </div>
                    </div>
                </div>
            `;
        }).filter(Boolean).join('');

        container.innerHTML = genreSections;
    }

    populateGenreFilter() {
        const filter = document.getElementById('genreFilter');
        if (!filter) {
            return;
        }

        const options = ['<option value="">All Genres</option>']
            .concat(this.state.genres.map((genre) => `<option value="${genre.name}">${genre.name}</option>`));

        filter.innerHTML = options.join('');
    }

    populateCountryFilter() {
        const filter = document.getElementById('countryFilter');
        if (!filter) {
            return;
        }

        const options = ['<option value="">All Countries</option>']
            .concat(this.state.countries.map((country) => `<option value="${country}">${country}</option>`));

        filter.innerHTML = options.join('');
    }

    renderPopularRow() {
        const grid = document.getElementById('popularGrid');
        if (!grid) {
            return;
        }

        const titles = [...this.state.recommendations]
            .sort((a, b) => (b.releaseYear || 0) - (a.releaseYear || 0))
            .slice(0, 12);

        grid.innerHTML = titles.length
            ? titles.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">No titles found.</div>';
    }

    renderTitleCard(title) {
    const mapKey = this.normalizeTitleIdForMap(title.titleId);
    const inWatchlist = this.state.watchlist.has(mapKey);
    const rating = this.state.ratings.get(mapKey);
        const ratingCode = this.getTitleRatingCode(title);
        const description = this.truncateText(title.description || 'Description coming soon.', 110);
    const accent = this.getAccentColor(title.titleId || title.title);
        const monogram = this.getTitleMonogram(title.title);
        const titleIdAttr = this.encodeAttribute(title.titleId ?? title.title_id ?? '');

        return `
            <article class="title-card" data-title-id="${titleIdAttr}">
                <button class="title-card__poster" type="button" data-title-action="open-modal" data-title-id="${titleIdAttr}" style="--poster-accent:${accent}">
                    <span class="title-card__poster-initial">${monogram}</span>
                    <span class="sr-only">View details for ${title.title}</span>
                    <span class="title-card__badge">${ratingCode || 'NR'}</span>
                </button>
                <div class="title-card__info">
                    <h3 class="title-card__title">${title.title}</h3>
                    <div class="title-card__meta">
                        <span>${title.releaseYear || '—'}</span>
                        <span>&bull;</span>
                        <span>${title.type}</span>
                        <span>&bull;</span>
                        <span>${title.duration || '—'}</span>
                    </div>
                    <p class="title-card__description">${description}</p>
                    <div class="title-card__actions">
                        <button class="icon-btn" title="Mark as watched" data-title-action="log-watch" data-title-id="${titleIdAttr}">
                            <i class="fas fa-clipboard-check"></i>
                        </button>
                        <button class="icon-btn ${inWatchlist ? 'active' : ''}" title="${inWatchlist ? 'Remove from My List' : 'Add to My List'}" data-title-action="toggle-watchlist" data-title-id="${titleIdAttr}">
                            <i class="fas ${inWatchlist ? 'fa-check' : 'fa-plus'}"></i>
                        </button>
                        <button class="icon-btn ${rating === 'thumbs_up' ? 'active' : ''}" title="Thumbs up" data-title-action="rate-up" data-title-id="${titleIdAttr}">
                            <i class="fas fa-thumbs-up"></i>
                        </button>
                        <button class="icon-btn ${rating === 'thumbs_down' ? 'active' : ''}" title="Thumbs down" data-title-action="rate-down" data-title-id="${titleIdAttr}">
                            <i class="fas fa-thumbs-down"></i>
                        </button>
                        <button class="icon-btn" title="View subscription plan" data-title-action="view-subscription">
                            <i class="fas fa-layer-group"></i>
                        </button>
                    </div>
                </div>
            </article>
        `;
    }

    async refreshRecommendations() {
        try {
            this.toggleLoading(true);
            const recommendations = await this.getJson(`/recommendations/${this.currentProfile.profileId}?limit=20`);
            this.state.recommendations = this.prepareRecommendations(recommendations, this.state.titles);
            this.state.recommendations.forEach((title) => {
                if (title && !this.state.titleMap.has(title.titleId)) {
                    this.state.titleMap.set(title.titleId, title);
                }
            });
            this.renderHeroTitle();
            this.renderRecommendationsRow();
            this.renderRecommendationsGrid();
            this.showToast('Recommendations updated', 'success');
        } catch (error) {
            this.showToast(error.message || 'Unable to refresh recommendations', 'error');
        } finally {
            this.toggleLoading(false);
        }
    }

    async logWatchEvent(titleId) {
        try {
            await this.postJson('/watch-history', {
                profileId: this.currentProfile.profileId,
                titleId,
                completed: true
            });
            this.showToast('Title logged to your watch history', 'success');

            const targetKey = this.normalizeTitleIdForMap(titleId);
            this.state.watchHistory = this.state.watchHistory.filter((item) => this.normalizeTitleIdForMap(item?.titleId) !== targetKey);
            const historyEntry = this.normalizeHistoryEntry({
                titleId,
                watchedAt: new Date().toISOString(),
                completed: true
            });
            if (historyEntry) {
                this.state.watchHistory.unshift(historyEntry);
            }
            this.state.continueWatching = this.state.watchHistory.slice(0, 12);
            this.renderContinueWatchingRow();
            this.renderHistoryGrid();
        } catch (error) {
            this.showToast(error.message || 'Unable to log watch activity', 'error');
        }
    }

    async toggleWatchlist(titleId) {
        try {
            const response = await this.postJson('/watchlist/toggle', {
                profileId: this.currentProfile.profileId,
                titleId
            });

            const normalizedId = this.normalizeTitleIdForMap(titleId);
            const inWatchlist = !!response?.inWatchlist;
            if (inWatchlist) {
                this.state.watchlist.add(normalizedId);
                this.showToast('Saved to your list', 'success');
            } else {
                this.state.watchlist.delete(normalizedId);
                this.showToast('Removed from your list', 'info');
            }

            if (this.isInitialized) {
                this.renderWatchlistRow();
                this.renderWatchlistGrid();
                this.renderRecommendationsRow();
                this.renderRecommendationsGrid();
                this.renderBrowseGrid();
                this.renderPopularRow();
                this.renderGenreRows();
                this.renderContinueWatchingRow();
            }
        } catch (error) {
            this.showToast(error.message || 'Unable to update My List', 'error');
        }
    }

    async rateTitle(titleId, ratingValue) {
        try {
            const response = await this.postJson('/ratings/toggle', {
                profileId: this.currentProfile.profileId,
                titleId,
                ratingValue
            });

            const normalizedId = this.normalizeTitleIdForMap(titleId);
            const action = response?.action;
            if (action === 'removed') {
                this.state.ratings.delete(normalizedId);
            } else {
                this.state.ratings.set(normalizedId, ratingValue);
            }

            this.showToast(response?.message || 'Rating saved', 'success');
            this.renderRecommendationsRow();
            this.renderRecommendationsGrid();
            this.renderBrowseGrid();
            this.renderPopularRow();
            this.renderGenreRows();
            if (this.normalizeTitleIdForMap(this.currentModalTitleId) === normalizedId) {
                this.showTitleModal(titleId);
            }
        } catch (error) {
            this.showToast(error.message || 'Unable to rate title', 'error');
        }
    }

    showTitleModal(titleId) {
        const key = this.normalizeTitleIdForMap(titleId);
        const title = this.state.titleMap.get(key) || this.state.recommendations.find((item) => this.normalizeTitleIdForMap(item?.titleId) === key);
        if (!title) {
            this.showToast('Title not found', 'error');
            return;
        }

        this.currentModalTitleId = title.titleId;

    const modal = document.getElementById('titleModal');
        if (!modal) {
            return;
        }

        modal.querySelector('#modalTitle').textContent = title.title;
        modal.querySelector('#modalYear').textContent = title.releaseYear || '—';
        const ratingCode = this.getTitleRatingCode(title) || 'NR';
        const modalRating = modal.querySelector('#modalRating');
        if (modalRating) {
            modalRating.textContent = ratingCode;
        }
        modal.querySelector('#modalDuration').textContent = title.duration || '—';
        modal.querySelector('#modalDescription').textContent = title.description || '';
        modal.querySelector('#modalCast').textContent = title.cast.length ? title.cast.join(', ') : 'Not available';
        modal.querySelector('#modalGenres').textContent = title.genres.length ? title.genres.join(', ') : 'Not available';
        modal.querySelector('#modalDirectors').textContent = title.directors.length ? title.directors.join(', ') : 'Not available';

        const recommendationReason = document.getElementById('recommendationReason');
        if (recommendationReason) {
            recommendationReason.textContent = '';
            recommendationReason.classList.add('hidden');
        }

        const watchlistBtn = document.getElementById('modalWatchlistBtn');
        if (watchlistBtn) {
            const inWatchlist = this.state.watchlist.has(key);
            watchlistBtn.classList.toggle('active', inWatchlist);
            watchlistBtn.innerHTML = `<i class="fas ${inWatchlist ? 'fa-check' : 'fa-plus'}"></i> ${inWatchlist ? 'Saved' : 'Save to List'}`;
            watchlistBtn.onclick = () => this.toggleWatchlist(title.titleId);
        }

        const playBtn = document.getElementById('modalLogWatchBtn');
        if (playBtn) {
            playBtn.onclick = () => this.logWatchEvent(titleId);
        }

        const thumbsUpBtn = document.getElementById('thumbsUpBtn');
        const thumbsDownBtn = document.getElementById('thumbsDownBtn');
        const notInterestedBtn = document.getElementById('notInterestedBtn');
    const rating = this.state.ratings.get(key);
        if (thumbsUpBtn) {
            thumbsUpBtn.classList.toggle('active', rating === 'thumbs_up');
        }
        if (thumbsDownBtn) {
            thumbsDownBtn.classList.toggle('active', rating === 'thumbs_down');
        }
        if (notInterestedBtn) {
            notInterestedBtn.classList.remove('active');
        }

        this.toggleAgeWarning(ratingCode);
        modal.classList.remove('hidden');
    }

    hideModal() {
        document.querySelectorAll('.modal').forEach((modal) => modal.classList.add('hidden'));
        this.currentModalTitleId = null;
    }

    showSection(section) {
        document.querySelectorAll('.admin-section, .content-section').forEach((sectionEl) => sectionEl.classList.remove('active'));

        const target = document.querySelector(`#${section}-section`) || document.getElementById(`user-${section}`);
        if (target) {
            target.classList.add('active');
        }

        switch (section) {
            case 'recommendations':
                this.renderRecommendationsGrid();
                break;
            case 'my-list':
                this.renderWatchlistGrid();
                break;
            case 'history':
                this.renderHistoryGrid();
                break;
            case 'browse':
                this.renderBrowseGrid();
                break;
            default:
                break;
        }
    }

    renderRecommendationsGrid() {
        const grid = document.getElementById('recommendationsContentGrid');
        if (!grid) {
            return;
        }

        const items = this.filterTitlesForAge(this.state.recommendations);
        grid.innerHTML = items.length
            ? items.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">We will have new recommendations soon.</div>';
    }

    renderWatchlistGrid() {
        const grid = document.getElementById('myListContentGrid');
        if (!grid) {
            return;
        }

        const titles = Array.from(this.state.watchlist)
            .map((titleId) => this.state.titleMap.get(titleId))
            .filter(Boolean);

        grid.innerHTML = titles.length
            ? titles.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">You haven\'t saved any titles yet.</div>';
    }

    renderHistoryGrid() {
        const grid = document.getElementById('historyGrid');
        if (!grid) {
            return;
        }

        const titles = this.state.watchHistory
                .map((item) => this.state.titleMap.get(this.normalizeTitleIdForMap(item?.titleId ?? item?.title_id)))
            .filter(Boolean);

        grid.innerHTML = titles.length
            ? titles.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">Log a title to build your history.</div>';
    }

    renderBrowseGrid() {
        const grid = document.getElementById('browseGrid');
        if (!grid) {
            return;
        }

        const pool = this.filterTitlesForAge(this.state.titles);
        if (!pool.length) {
            grid.innerHTML = '<div class="empty-state">No titles available yet. Check back soon!</div>';
            return;
        }
        const searchTerm = this.filters.search.trim().toLowerCase();
        const filtered = pool.filter((title) => {
            const matchesGenre = !this.filters.genre || (title.genres || []).some((genre) => genre.toLowerCase() === this.filters.genre.toLowerCase());
            const matchesType = !this.filters.type || title.type === this.filters.type;
            const matchesRating = !this.filters.rating || title.rating === this.filters.rating;
            const matchesCountry = !this.filters.country || (title.countries || []).some((country) => country.toLowerCase() === this.filters.country.toLowerCase());
            const matchesSearch = !searchTerm || [
                title.title,
                title.description,
                (title.countries || []).join(' '),
                (title.genres || []).join(' ')
            ].some((value) => typeof value === 'string' && value.toLowerCase().includes(searchTerm));
            return matchesGenre && matchesType && matchesRating && matchesCountry && matchesSearch;
        });

        grid.innerHTML = filtered.length
            ? filtered.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">No titles match your filters yet.</div>';
    }

    renderSubscriptionPlans() {
        const section = document.getElementById('subscriptionPlansSection');
        const grid = document.getElementById('subscriptionPlansGrid');
        const statusLabel = document.getElementById('subscriptionPlansStatus');
        if (!section || !grid || !statusLabel) {
            return;
        }

        const plans = this.state.subscriptionPlans || [];
        if (!plans.length) {
            section.classList.add('hidden');
            return;
        }

        section.classList.remove('hidden');
        const activeId = this.state.activeSubscriptionId;
        const hasActive = Boolean(this.subscription);
        statusLabel.textContent = hasActive
            ? `Current plan: ${this.subscription?.plan?.planName || this.subscription?.plan?.name || 'Active'}`
            : 'No active plan yet.';

        grid.innerHTML = plans.map((plan) => {
            const isActive = this.isPlanActive(plan.planId);
            const buttonLabel = isActive ? 'Current Plan' : 'Choose Plan';
            const buttonClass = isActive ? 'btn btn--secondary' : 'btn btn--primary';
            const badge = isActive ? '<span class="subscription-plan__badge">Active</span>' : '';
            const actionAttrs = isActive ? 'disabled' : `onclick="userApp.subscribeToPlan(${plan.planId})"`;

            return `
                <article class="subscription-plan ${isActive ? 'subscription-plan--active' : ''}">
                    <div class="subscription-plan__header">
                        <h3 class="subscription-plan__name">${plan.name}</h3>
                        ${badge}
                    </div>
                    <div class="subscription-plan__price">${plan.priceLabel}</div>
                    <div class="subscription-plan__meta">
                        <span>${plan.quality}</span>
                        <span>${plan.screensAllowed} screens at once</span>
                        <span>${plan.description}</span>
                    </div>
                    <div class="subscription-plan__actions">
                        <button class="${buttonClass}" ${actionAttrs}>
                            ${buttonLabel}
                        </button>
                    </div>
                </article>
            `;
        }).join('');
    }

    isPlanActive(planId) {
        if (!planId) {
            return false;
        }
        const activeId = this.state.activeSubscriptionId;
        return activeId !== null && Number(activeId) === Number(planId);
    }

    normalizePlan(raw = {}) {
        const planId = raw.planId ?? raw.plan_id ?? raw.id;
        const name = raw.planName ?? raw.name ?? 'Plan';
        const quality = raw.quality ?? 'HD streaming';
        const screensAllowed = raw.screensAllowed ?? raw.screens_allowed ?? 1;
        const price = raw.price ?? raw.monthlyPrice ?? raw.monthly_price ?? 0;
        const priceLabel = Number.isFinite(Number(price))
            ? this.formatCurrency(Number(price)) + '/mo'
            : `${price}`;
        const description = raw.description || 'Stream on your favorite devices without limits.';

        return {
            planId,
            name,
            quality,
            screensAllowed,
            price,
            priceLabel,
            description
        };
    }

    async subscribeToPlan(planId) {
        if (!planId) {
            return;
        }
        if (this.isPlanActive(planId)) {
            this.showToast('This is already your active plan.', 'info');
            return;
        }

        try {
            this.toggleLoading(true);
            const response = await this.postJson(`/subscriptions/user/${this.currentUser.userId}/subscribe`, { planId });
            const subscription = response?.subscription ?? response?.activeSubscription ?? null;
            if (subscription) {
                this.subscription = subscription;
                this.state.activeSubscriptionId = subscription.subscriptionId ?? subscription.subscription_id ?? planId;
            } else {
                await this.refreshSubscriptionStatus();
            }
            this.updateSubscriptionBadge();
            this.renderSubscriptionPlans();
            this.showToast('Subscription updated successfully.', 'success');
        } catch (error) {
            this.showToast(error.message || 'Unable to update subscription right now.', 'error');
        } finally {
            this.toggleLoading(false);
        }
    }

    async refreshSubscriptionStatus() {
        try {
            const info = await this.getJson(`/subscriptions/user/${this.currentUser.userId}`);
            const active = info?.activeSubscription ?? null;
            this.subscription = active || null;
            this.state.activeSubscriptionId = active?.subscriptionId ?? active?.subscription_id ?? null;
            this.updateSubscriptionBadge();
        } catch (error) {
            console.warn('Failed to refresh subscription status', error);
        }
    }

    updateActiveNav(activeLink) {
        document.querySelectorAll('.nav-link').forEach((link) => link.classList.remove('active'));
        activeLink.classList.add('active');
    }

    toggleProfileDropdown() {
        const dropdown = document.getElementById('profileDropdown');
        if (dropdown) {
            dropdown.classList.toggle('hidden');
        }
    }

    handleSearch(query) {
        const trimmed = query.trim();
        const grid = document.getElementById('searchResultsGrid');
        const section = document.getElementById('search-results');

        if (!section || !grid) {
            return;
        }

        if (!trimmed) {
            section.classList.remove('active');
            return;
        }

        const results = this.state.titles.filter((title) => {
            const haystack = [
                title.title,
                title.description,
                ...(title.genres || []),
                ...(title.cast || [])
            ].join(' ').toLowerCase();
            return haystack.includes(trimmed.toLowerCase());
        });

        section.classList.add('active');
        grid.innerHTML = results.length
            ? results.map((title) => this.renderTitleCard(title)).join('')
            : '<div class="empty-state">No titles match your search right now.</div>';
    }

    logout() {
        localStorage.removeItem('netflixUser');
        localStorage.removeItem('selectedProfile');
        localStorage.removeItem('netflixSubscription');
        window.location.href = 'login.html';
    }

    toggleLoading(isLoading) {
        const overlay = document.getElementById('loadingOverlay');
        if (!overlay) {
            return;
        }
        overlay.classList.toggle('hidden', !isLoading);
    }

    async getJson(path) {
        const response = await fetch(this.buildUrl(path), { headers: { Accept: 'application/json' } });
        return this.handleResponse(response);
    }

    async postJson(path, payload, options = {}) {
        const response = await fetch(this.buildUrl(path), {
            method: options.method || 'POST',
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
                ...(options.headers || {})
            },
            body: JSON.stringify(payload)
        });
        return this.handleResponse(response);
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

    buildAgeContext() {
        const age = this.calculateAge(this.currentUser?.dateOfBirth || this.currentUser?.date_of_birth);
        const override = this.getProfileRatingOverride();
        const allowedRatings = this.getAllowedRatingsForAge(age, override);
        const maxRating = this.getMaxAllowedRating(allowedRatings);
        const isRestricted = allowedRatings.length > 0 && allowedRatings.length < this.ratingHierarchy.length;

        return {
            age,
            allowedRatings,
            maxRating,
            override,
            isRestricted
        };
    }

    calculateAge(dateString) {
        if (!dateString) {
            return 0;
        }
        const parsed = new Date(dateString);
        if (Number.isNaN(parsed.getTime())) {
            return 0;
        }
        const today = new Date();
        let age = today.getFullYear() - parsed.getFullYear();
        const m = today.getMonth() - parsed.getMonth();
        if (m < 0 || (m === 0 && today.getDate() < parsed.getDate())) {
            age--;
        }
        return Math.max(age, 0);
    }

    getProfileRatingOverride() {
        if (!this.currentProfile) {
            return null;
        }
        return (
            this.currentProfile.maturityRatingOverride ||
            this.currentProfile.maturity_rating_override ||
            null
        );
    }

    getAllowedRatingsForAge(age, override) {
        if (override && typeof override === 'string' && override.trim()) {
            return this.getUpToRating(override.trim().toUpperCase());
        }

        if (typeof age !== 'number') {
            return [];
        }

        if (age < 7) {
            return ['G', 'TV-Y', 'TV-Y7'];
        }
        if (age < 13) {
            return ['G', 'PG', 'TV-Y', 'TV-Y7', 'TV-G', 'TV-PG'];
        }
        if (age < 17) {
            return ['G', 'PG', 'PG-13', 'TV-Y', 'TV-Y7', 'TV-G', 'TV-PG', 'TV-14'];
        }
        return [...this.ratingHierarchy];
    }

    getUpToRating(maxRating) {
        const normalized = maxRating.toUpperCase();
        const order = ['G', 'TV-Y', 'TV-Y7', 'TV-G', 'PG', 'TV-PG', 'PG-13', 'TV-14', 'R', 'TV-MA', 'NC-17'];
        const index = order.indexOf(normalized);
        if (index === -1) {
            return [...this.ratingHierarchy];
        }
        return order.slice(0, index + 1);
    }

    getMaxAllowedRating(allowedRatings = []) {
        let maxRating = null;
        allowedRatings.forEach((rating) => {
            const normalized = rating?.toUpperCase();
            const idx = this.ratingHierarchy.indexOf(normalized);
            if (idx === -1) {
                return;
            }
            if (maxRating === null) {
                maxRating = normalized;
                return;
            }
            const currentIdx = this.ratingHierarchy.indexOf(maxRating);
            if (idx > currentIdx) {
                maxRating = normalized;
            }
        });
        return maxRating;
    }

    isRatingAllowed(ratingCode) {
        const { allowedRatings } = this.ageContext || {};
        if (!allowedRatings || !allowedRatings.length) {
            return true;
        }
        if (!ratingCode) {
            return true;
        }
        return allowedRatings.includes(ratingCode.toUpperCase());
    }

    filterTitlesForAge(titles = []) {
        if (!Array.isArray(titles)) {
            return [];
        }
        return titles.filter((title) => this.isRatingAllowed(this.getTitleRatingCode(title)));
    }

    prepareRecommendations(rawRecommendations, fallbackPool = []) {
        const normalized = Array.isArray(rawRecommendations)
            ? rawRecommendations.map((title) => this.normalizeTitle(title))
            : [];
        const filtered = this.filterTitlesForAge(normalized);
        if (filtered.length) {
            return filtered;
        }
        const fallback = this.filterTitlesForAge(Array.isArray(fallbackPool) ? fallbackPool : []);
        return fallback.slice(0, 12);
    }

    updateAgeNotice() {
        const ageNotice = document.getElementById('ageNotice');
        if (!ageNotice) {
            return;
        }

        const content = ageNotice.querySelector('.age-notice-content span');
        const { age, allowedRatings, maxRating, isRestricted, override } = this.ageContext || {};

        if (!allowedRatings || !allowedRatings.length || !isRestricted) {
            ageNotice.classList.add('hidden');
            return;
        }

        let message = 'Content filtered for age-appropriate viewing';
        if (override) {
            message = `Content filtered up to ${override}`;
        } else if (age > 0) {
            const displayRating = maxRating || allowedRatings[allowedRatings.length - 1];
            message = `Content filtered for viewers aged ${age}+ (up to ${displayRating})`;
        } else {
            const displayRating = maxRating || allowedRatings[allowedRatings.length - 1];
            message = `Content filtered to kid-friendly titles (up to ${displayRating})`;
        }

        if (content) {
            content.textContent = message;
        }
        ageNotice.classList.remove('hidden');
    }

    updateSubscriptionBadge() {
        const badge = document.getElementById('subscriptionBadge');
        if (!badge) {
            return;
        }

        const subscription = this.subscription;
        if (!subscription || !subscription.plan) {
            badge.textContent = 'No active plan';
            badge.title = 'Subscribe to unlock full access';
            badge.classList.remove('hidden');
            badge.classList.add('subscription-badge--inactive');
            return;
        }

        const plan = subscription.plan || {};
        const planName = plan.planName || 'Plan';
        const quality = plan.quality || '';
        const rawPrice = plan.price;
        const screens = plan.screensAllowed;
        const status = (subscription.status || '').toLowerCase();
        const isActive = status === 'active' || status === '';

        let priceLabel = '';
        if (rawPrice !== undefined && rawPrice !== null && rawPrice !== '') {
            const numericPrice = Number(rawPrice);
            if (Number.isFinite(numericPrice)) {
                priceLabel = `$${numericPrice.toFixed(2)}/mo`;
            } else {
                priceLabel = `${rawPrice}`;
            }
        }

        const fragments = [
            `<span class="subscription-badge__plan">${planName}</span>`
        ];

        if (quality) {
            fragments.push('<span class="subscription-badge__divider">•</span>');
            fragments.push(`<span class="subscription-badge__quality">${quality}</span>`);
        }

        if (screens) {
            fragments.push('<span class="subscription-badge__divider">•</span>');
            fragments.push(`<span class="subscription-badge__screens">${screens} screens</span>`);
        }

        if (priceLabel) {
            fragments.push('<span class="subscription-badge__divider">•</span>');
            fragments.push(`<span class="subscription-badge__price">${priceLabel}</span>`);
        }

        if (!isActive) {
            fragments.push('<span class="subscription-badge__status">(inactive)</span>');
        }

        badge.innerHTML = fragments.join(' ');
        badge.classList.remove('hidden');
        badge.classList.toggle('subscription-badge--inactive', !isActive);
        badge.title = isActive
            ? `Current plan: ${planName}`
            : 'Subscription inactive';
    }

    toggleAgeWarning(ratingCode) {
        const warning = document.getElementById('ageWarning');
        if (!warning) {
            return;
        }

        const { allowedRatings, maxRating } = this.ageContext || {};
        if (!ratingCode || !allowedRatings || !allowedRatings.length) {
            warning.classList.add('hidden');
            return;
        }

        const normalized = ratingCode.toUpperCase();
        if (allowedRatings.includes(normalized)) {
            warning.classList.add('hidden');
            return;
        }

        const messageSpan = warning.querySelector('span');
        if (messageSpan) {
            messageSpan.textContent = `This title is rated ${normalized}, above your profile limit (${maxRating || 'PG'}).`;
        }
        warning.classList.remove('hidden');
    }

    getRatingCodeFromRaw(raw) {
        if (!raw) {
            return null;
        }
        const candidates = [
            raw.rating?.code,
            raw.ratingCode,
            raw.rating_code,
            raw.rating,
            raw.ratingId ? raw.ratingId : null
        ];
        const code = candidates.find((value) => typeof value === 'string' && value.trim());
        return code ? code.trim().toUpperCase() : null;
    }

    getTitleRatingCode(title) {
        if (!title) {
            return null;
        }
        const candidates = [
            title.ratingCode,
            title.rating,
            title.rating_code,
            title.rating?.code
        ];
        const code = candidates.find((value) => typeof value === 'string' && value.trim());
        return code ? code.trim().toUpperCase() : null;
    }

    truncateText(text, maxLength = 160) {
        if (!text || typeof text !== 'string') {
            return '';
        }
        if (text.length <= maxLength) {
            return text;
        }
        return `${text.slice(0, maxLength - 1).trim()}…`;
    }

    formatPeopleList(list, fallback = 'Not available') {
        if (!list) {
            return fallback;
        }
        if (Array.isArray(list)) {
            if (!list.length) {
                return fallback;
            }
            return list.slice(0, 4).join(', ');
        }
        if (typeof list === 'string' && list.trim()) {
            return list;
        }
        return fallback;
    }

    getTitleMonogram(title) {
        if (!title || typeof title !== 'string') {
            return '?';
        }
        const trimmed = title.trim();
        if (!trimmed) {
            return '?';
        }
        const words = trimmed.split(/\s+/).slice(0, 2);
        const initials = words.map((word) => word.charAt(0).toUpperCase()).join('');
        return initials || trimmed.charAt(0).toUpperCase();
    }

    getAccentColor(seed) {
        if (seed === undefined || seed === null) {
            return 'hsl(355, 45%, 42%)';
        }
        let numericSeed;
        if (Number.isFinite(Number(seed))) {
            numericSeed = Number(seed);
        } else {
            const str = String(seed);
            numericSeed = str.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
        }
        const hue = Math.abs(numericSeed * 37) % 360;
        return `hsl(${hue}, 40%, 45%)`;
    }

    encodeAttribute(value) {
        if (value === null || value === undefined) {
            return '';
        }
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#x27;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    normalizeTitleIdValue(rawId) {
        if (rawId === null || rawId === undefined) {
            return null;
        }
        if (typeof rawId === 'number' && Number.isFinite(rawId)) {
            return rawId;
        }
        const str = String(rawId).trim();
        if (!str) {
            return null;
        }
        const numeric = Number(str);
        if (!Number.isNaN(numeric) && Number.isFinite(numeric)) {
            return numeric;
        }
        return str;
    }

    normalizeTitleIdForMap(rawId) {
        const normalized = this.normalizeTitleIdValue(rawId);
        if (normalized === null || normalized === undefined) {
            return null;
        }
        if (typeof normalized === 'number') {
            return `num:${normalized}`;
        }
        return `str:${normalized}`;
    }

    extractCountries(titles = []) {
        const set = new Set();
        titles.forEach((title) => {
            (title.countries || []).forEach((country) => {
                if (country) {
                    set.add(country);
                }
            });
        });
        return Array.from(set).sort((a, b) => a.localeCompare(b));
    }

    showSubscriptionDetails() {
        const subscription = this.subscription;
        if (!subscription) {
            this.showToast('No subscription information available yet.', 'info');
            return;
        }

        const modal = document.getElementById('subscriptionModal');
        const summary = document.getElementById('subscriptionSummary');
        if (!modal || !summary) {
            this.showToast('Subscription details are unavailable right now.', 'error');
            return;
        }

        summary.innerHTML = this.renderSubscriptionSummary(subscription);
        modal.classList.remove('hidden');
    }

    renderSubscriptionSummary(subscription) {
        const plan = subscription.plan || {};
        const planName = plan.planName || plan.name || 'Plan';
        const tier = plan.quality || 'Standard';
        const screens = plan.screensAllowed ?? plan.screens_allowed ?? '—';
        const price = plan.price ?? plan.monthlyPrice ?? null;
        const status = (subscription.status || 'active').toUpperCase();
        const started = this.formatDateLabel(subscription.startDate || subscription.start_date);
        const ends = this.formatDateLabel(subscription.endDate || subscription.end_date) || 'Current';

        const priceLabel = price !== null && price !== undefined
            ? this.formatCurrency(price)
            : '—';

        return `
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Plan</span>
                <span class="subscription-summary__value">${planName}</span>
            </div>
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Status</span>
                <span class="subscription-summary__value subscription-summary__value--${status.toLowerCase()}">${status}</span>
            </div>
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Quality</span>
                <span class="subscription-summary__value">${tier}</span>
            </div>
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Screens</span>
                <span class="subscription-summary__value">${screens}</span>
            </div>
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Monthly Price</span>
                <span class="subscription-summary__value">${priceLabel}</span>
            </div>
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Started</span>
                <span class="subscription-summary__value">${started || '—'}</span>
            </div>
            <div class="subscription-summary__row">
                <span class="subscription-summary__label">Ends</span>
                <span class="subscription-summary__value">${ends}</span>
            </div>
        `;
    }

    formatCurrency(value) {
        const numeric = Number(value);
        if (!Number.isFinite(numeric)) {
            return `${value}`;
        }
        return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(numeric);
    }

    formatDateLabel(value) {
        if (!value) {
            return '';
        }
        const parsed = new Date(value);
        if (Number.isNaN(parsed.getTime())) {
            return '';
        }
        return parsed.toLocaleDateString(undefined, {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    buildUrl(path) {
        if (!path) {
            return this.apiBaseUrl;
        }
        if (path.startsWith('http')) {
            return path;
        }
        return `${this.apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`;
    }

    showToast(message, type = 'success') {
        const toast = document.getElementById('toast');
        const toastMessage = document.getElementById('toastMessage');
        const toastIcon = document.getElementById('toastIcon');

        if (!toast || !toastMessage || !toastIcon) {
            console.log(`[${type}] ${message}`);
            return;
        }

        toast.className = `toast toast--${type}`;
        toastMessage.textContent = message;
        toast.classList.remove('hidden');

        clearTimeout(this.toastTimeout);
        this.toastTimeout = setTimeout(() => toast.classList.add('hidden'), 4000);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    window.userApp = new UserApp();
});