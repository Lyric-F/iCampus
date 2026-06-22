// ========== API 基础地址 ==========
window.API_BASE_URL = 'http://47.103.204.46:8080';

// ========== 主题配置库 ==========
const themeThemes = {
    pureWhiteBlue: {
        name: '纯净白·蓝',
        desc: '极浅灰蓝背景 · 清新蓝色',
        primary: '#007aff',
        primaryLight: '#3a8eff',
        glowPrimary: 'rgba(0, 122, 255, 0.2)',
        titleDark: '#1c1c1e',
        textDark: '#1c1c1e',
        textLight: '#5c5c5e',
        textMuted: '#8e8e93',
        textPlaceholder: '#aeaeb2',
        iconColor: '#007aff',
        navColor: '#8e8e93',
        badgeBg: '#e9f0fe',
        bgLight: '#fafcff',
        cateActiveBg: null
    },
    index10: {
        name: '原初·灵感',
        desc: '紫调 · 深色按钮',
        primary: '#5b42f3',
        primaryLight: '#826eff',
        glowPrimary: 'rgba(91, 66, 243, 0.2)',
        titleDark: '#2e3b6e',
        textDark: '#121826',
        textLight: '#5c6072',
        textMuted: '#8b8faa',
        textPlaceholder: '#9ca3af',
        iconColor: '#5b42f3',
        navColor: '#9095ae',
        badgeBg: '#e9eeff',
        bgLight: '#f6f8fe',
        cateActiveBg: '#3a2a8f'
    }
};

const solidThemes = {
    dawn: { name: '晨雾灰', desc: '雅致灰调 · 宁静', primary: '#6c7a8f', primaryLight: '#8d99ae', glowPrimary: 'rgba(108, 122, 143, 0.2)', titleDark: '#4a5568', textDark: '#2d3748', textLight: '#718096', textMuted: '#6c7a8f', textPlaceholder: '#a0aec0', iconColor: '#5a677b', navColor: '#8d9aae', badgeBg: '#edf2f7', bgLight: '#f9fafb', cateActiveBg: null },
    ocean: { name: '海洋蓝', desc: '清新蓝调 · 畅快', primary: '#2d8cff', primaryLight: '#64b5ff', glowPrimary: 'rgba(45, 140, 255, 0.2)', titleDark: '#1e3a5f', textDark: '#1e2a3a', textLight: '#5a7a9a', textMuted: '#5c7c9a', textPlaceholder: '#8ea9c4', iconColor: '#3c5a7a', navColor: '#8ba5c0', badgeBg: '#eaf4ff', bgLight: '#f0f7ff', cateActiveBg: null },
    mint: { name: '薄荷绿', desc: '舒缓绿意 · 治愈', primary: '#67b26f', primaryLight: '#8fc98f', glowPrimary: 'rgba(103, 178, 111, 0.2)', titleDark: '#2c5e2e', textDark: '#1e3a2a', textLight: '#5a8a5a', textMuted: '#5c8c6a', textPlaceholder: '#9ac09a', iconColor: '#3c6e3c', navColor: '#8ab88a', badgeBg: '#e8f5e8', bgLight: '#f0faf0', cateActiveBg: null },
    apricot: { name: '暖杏色', desc: '温柔杏色 · 暖意', primary: '#f4a261', primaryLight: '#f7b77c', glowPrimary: 'rgba(244, 162, 97, 0.2)', titleDark: '#9c6e3e', textDark: '#3a2a1e', textLight: '#8a6a4a', textMuted: '#8c725a', textPlaceholder: '#c4a27a', iconColor: '#b87a3c', navColor: '#c4a07a', badgeBg: '#fff0e0', bgLight: '#fff5ef', cateActiveBg: null },
    lavender: { name: '淡紫灰', desc: '优雅紫灰 · 浪漫', primary: '#a78bfa', primaryLight: '#c4b0ff', glowPrimary: 'rgba(167, 139, 250, 0.2)', titleDark: '#5a4a8a', textDark: '#2a2a3a', textLight: '#6a6a8a', textMuted: '#7a6a9a', textPlaceholder: '#b0a0d0', iconColor: '#6a5a9a', navColor: '#9a8ac0', badgeBg: '#f0eaff', bgLight: '#faf5ff', cateActiveBg: null },
    coral: { name: '珊瑚橙', desc: '活力橙调 · 热情', primary: '#ff7e5e', primaryLight: '#ff9e7e', glowPrimary: 'rgba(255, 126, 94, 0.2)', titleDark: '#b54a2a', textDark: '#3a2a1e', textLight: '#8a5a4a', textMuted: '#8c6a5a', textPlaceholder: '#c48a6a', iconColor: '#c45a3c', navColor: '#c48a6a', badgeBg: '#ffe6e0', bgLight: '#fff5f0', cateActiveBg: null }
};

// 全局主题状态
let currentThemeType = 'theme';
let currentThemeId = null;
let darkMode = false;

// 辅助函数：颜色亮度调整
function adjustColorBrightness(color, factor) {
    if (color.startsWith('#')) {
        let r = parseInt(color.slice(1,3), 16);
        let g = parseInt(color.slice(3,5), 16);
        let b = parseInt(color.slice(5,7), 16);
        r = Math.min(255, Math.floor(r * factor));
        g = Math.min(255, Math.floor(g * factor));
        b = Math.min(255, Math.floor(b * factor));
        return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
    }
    return color;
}

function applyTheme(themeObj, isDark) {
    const primary = isDark ? adjustColorBrightness(themeObj.primary, 1.2) : themeObj.primary;
    const primaryLight = isDark ? adjustColorBrightness(themeObj.primaryLight, 1.2) : themeObj.primaryLight;
    const bgLight = isDark ? '#0f121c' : themeObj.bgLight;
    const cardBg = isDark ? '#1a1f2e' : '#ffffff';
    const borderColor = isDark ? '#2c2e3a' : '#eef2f6';
    const textPrimary = isDark ? '#eef3fc' : themeObj.textDark;
    const textSecondary = isDark ? '#9aa9cc' : themeObj.textMuted;

    const root = document.documentElement;
    root.style.setProperty('--primary', primary);
    root.style.setProperty('--primary-light', primaryLight);
    root.style.setProperty('--glow-primary', themeObj.glowPrimary);
    root.style.setProperty('--text-primary', textPrimary);
    root.style.setProperty('--text-secondary', textSecondary);
    root.style.setProperty('--bg-body', bgLight);
    root.style.setProperty('--bg-card', cardBg);
    root.style.setProperty('--border-light', borderColor);
    root.style.setProperty('--cate-active-bg', themeObj.cateActiveBg || primary);

    document.body.style.backgroundColor = bgLight;
    document.body.style.color = textPrimary;

    // 注入全局样式覆盖
    const styleId = 'global-theme-style';
    let styleEl = document.getElementById(styleId);
    if (!styleEl) {
        styleEl = document.createElement('style');
        styleEl.id = styleId;
        document.head.appendChild(styleEl);
    }
    styleEl.textContent = `
        .card, .post-card, .post-item, .menu-item, .list-item, .top-bar, .tabs, .tab,
        .comment-item, .bottom-nav, .print-shop-card, .print-service-card, .search-history-card,
        .profile-header, .notification-item, .run-subitem, .job-detail-container, .job-publish-container,
        .job-manage-container, .func-card, .hot-section, .category-row {
            background-color: var(--bg-card) !important;
            border-color: var(--border-light) !important;
            color: var(--text-primary) !important;
        }
        body.dark .post-content, body.dark .card-info, body.dark .post-desc {
            color: #cbd5e6 !important;
        }
        .top-title, .post-name, .post-title, .card-title, .profile-name, .menu-left span {
            color: var(--text-primary) !important;
        }
        .post-footer, .comment-time, .job-detail-label, .print-shop-location, .print-shop-stats span {
            color: var(--text-secondary) !important;
        }
        .search-bar, .form-item input, .form-item textarea, .form-item select {
            background: ${isDark ? '#2a2e42' : '#f5f5f7'} !important;
            color: var(--text-primary) !important;
        }
        .btn, .publish-btn, .tabbar-publish, .cart-checkout {
            background: var(--primary) !important;
        }
        .action-icon.liked, .action-icon.disliked {
            color: #ff3b30 !important;
        }
    `;
}

function getCurrentThemeObject() {
    if (currentThemeType === 'theme') return themeThemes[currentThemeId];
    else return solidThemes[currentThemeId];
}

function applyThemeSetting() {
    const theme = getCurrentThemeObject();
    if (!theme) return;
    applyTheme(theme, darkMode);
    if (darkMode) {
        document.body.classList.add('dark');
    } else {
        document.body.classList.remove('dark');
    }
    // 保存到 localStorage
    localStorage.setItem('icampus_theme_id', currentThemeId);
    localStorage.setItem('icampus_theme_type', currentThemeType);
    localStorage.setItem('icampus_dark', darkMode);
}

function setDarkMode(enabled) {
    darkMode = enabled;
    window.darkMode = enabled;   // 确保这一行存在
    applyThemeSetting();
    updateNightModeButtonText();
    // 触发全局事件，供其他页面监听（可选）
    window.dispatchEvent(new Event('themechange'));
}

function setTheme(id, type) {
    currentThemeId = id;
    currentThemeType = type;
    window.currentThemeId = currentThemeId;   // 添加
    window.currentThemeType = currentThemeType; // 添加
    applyThemeSetting();
    window.dispatchEvent(new Event('themechange'));
}

function initTheme() {
    const savedThemeId = localStorage.getItem('icampus_theme_id');
    const savedThemeType = localStorage.getItem('icampus_theme_type');
    const savedDark = localStorage.getItem('icampus_dark') === 'true';

    if (savedThemeId && savedThemeType) {
        currentThemeId = savedThemeId;
        currentThemeType = savedThemeType;
    } else {
        currentThemeId = 'pureWhiteBlue';
        currentThemeType = 'theme';
    }
    darkMode = savedDark;

    // ========== 关键修复：同步 window 上的变量 ==========
    window.darkMode = darkMode;
    window.currentThemeId = currentThemeId;
    window.currentThemeType = currentThemeType;

    applyThemeSetting();
    updateNightModeButtonText();  // 添加这一行，确保按钮文字与当前模式一致

    // 监听 storage 事件，实现多标签页同步
    window.addEventListener('storage', (e) => {
        if (e.key === 'icampus_theme_id' || e.key === 'icampus_theme_type' || e.key === 'icampus_dark') {
            location.reload();
        }
    });
}

function updateNightModeButtonText() {
    const nightSpan = document.querySelector('#nightModeToggle span');
    const nightIcon = document.querySelector('#nightModeToggle i');
    if (nightSpan) nightSpan.innerText = darkMode ? '日间模式' : '夜间模式';
    if (nightIcon) nightIcon.className = darkMode ? 'fas fa-sun' : 'fas fa-moon';
}

// ========== 暴露全局变量和函数 ==========
window.themeThemes = themeThemes;
window.solidThemes = solidThemes;
window.currentThemeId = currentThemeId;
window.currentThemeType = currentThemeType;
window.darkMode = darkMode;
window.setDarkMode = setDarkMode;
window.setTheme = setTheme;
window.initTheme = initTheme;

// ========== WebSocket 实时消息 ==========
let ws = null;

/**
 * 连接 WebSocket（页面加载时自动调用）
 */
function connectWebSocket() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;

    if (ws && ws.readyState === WebSocket.OPEN) {
        return;
    }
    if (ws) {
        ws.close();
    }

    const wsUrl = `ws://47.103.204.46:8080/ws/notifications?studentId=${studentId}`;
    ws = new WebSocket(wsUrl);

    ws.onopen = function() {
        console.log('WebSocket 连接已建立');
        setInterval(() => {
            if (ws && ws.readyState === WebSocket.OPEN) {
                ws.send('ping');
            }
        }, 30000);
    };

    ws.onmessage = function(event) {
        console.log('收到新消息:', event.data);
        updateMsgBadge();   // 更新红点
        if (window.location.pathname.includes('msg.html')) {
            loadNotifications();
        }
    };

    ws.onclose = function() {
        console.log('WebSocket 连接关闭');
    };

    ws.onerror = function(error) {
        console.error('WebSocket 错误:', error);
    };
}

function closeWebSocket() {
    if (ws) {
        ws.close();
        ws = null;
    }
}

function initPageCommon() {
    const studentId = localStorage.getItem('studentId');
    if (studentId && (!ws || ws.readyState !== WebSocket.OPEN)) {
        connectWebSocket();
    }
}

// ========== 通用函数 ==========
function showToast(message, type = 'info') {
    let toast = document.querySelector('.toast');
    if (toast) toast.remove();
    toast = document.createElement('div');
    toast.className = 'toast';
    if (type === 'success') toast.classList.add('success');
    if (type === 'error') toast.classList.add('error');
    toast.innerText = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 2600);
}

function formatRelativeTime(isoString) {
    if (!isoString) return '';
    const now = new Date();
    const target = new Date(isoString);
    const diffSeconds = Math.floor((now - target) / 1000);
    if (diffSeconds < 0) return '刚刚';
    if (diffSeconds < 60) return `${diffSeconds}秒前`;
    if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)}分钟前`;
    if (diffSeconds < 86400) return `${Math.floor(diffSeconds / 3600)}小时前`;
    if (diffSeconds < 2592000) return `${Math.floor(diffSeconds / 86400)}天前`;
    return target.toLocaleDateString();
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>]/g, function(m) {
        if (m === '&') return '&amp;';
        if (m === '<') return '&lt;';
        if (m === '>') return '&gt;';
        return m;
    });
}

// ========== 页面跳转 ==========
window.goTo = function(url) {
    window.location.href = url;
};

window.goPage = function(page) {
    const pages = {
        index: 'index.html',
        help: 'help.html',
        publish: 'publish.html',
        msg: 'msg.html',
        mine: 'mine.html',
        job: 'job.html',
        print: 'print.html',
        run: 'run.html',
        dry: 'dry.html',
        'edit-profile': 'edit-profile.html',
        'my-posts': 'my-posts.html',
        'my-favorites': 'my-favorites.html',
        'my-orders': 'my-orders.html',
        login: 'login.html',
        register: 'register.html',
        search: 'search.html'
    };
    if (pages[page]) {
        window.location.href = pages[page];
    } else {
        console.warn('未知页面:', page);
    }
};

// ========== 红点管理（统一） ==========
async function updateMsgBadge() {
    const studentId = localStorage.getItem('studentId');
    const badge = document.getElementById('msgBadge');
    if (!badge) return;
    if (!studentId) {
        badge.style.display = 'none';
        return;
    }

    try {
        // 1. 获取通知未读数
        const notifyRes = await fetch(`${API_BASE_URL}/api/notification/unread-count?studentId=${studentId}`, {
            credentials: 'include'
        });
        const notifyData = await notifyRes.json();
        const unreadNotify = notifyData.count || 0;

        // 2. 获取私信未读数
        const msgRes = await fetch(`${API_BASE_URL}/api/message/unread-count?userId=${studentId}`);
        const msgData = await msgRes.json();
        const unreadMsg = msgData.count || 0;

        const total = unreadNotify + unreadMsg;

        if (total > 0) {
            badge.innerText = total > 99 ? '99+' : total;
            badge.style.display = 'flex';
        } else {
            badge.style.display = 'none';
        }
    } catch (e) {
        console.error('更新消息红点失败', e);
        badge.style.display = 'none';
    }
}

function hideAllBadges() {
    document.querySelectorAll('.bottom-nav .msg-dot, .tabbar-item .badge').forEach(badge => {
        badge.style.display = 'none';
    });
}

async function fetchUnreadCount() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return 0;
    try {
        const res = await fetch(`${API_BASE_URL}/api/notification/unread-count?studentId=${studentId}`, {
            credentials: 'include'
        });
        const data = await res.json();
        return data.count || 0;
    } catch (e) {
        return 0;
    }
}

// ========== 通知相关（与后端匹配） ==========
async function loadNotifications() {
    const container = document.getElementById('notificationList');
    if (!container) return;

    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        container.innerHTML = '<div class="empty-state">请先登录</div>';
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/api/notification/list?studentId=${studentId}`, {
            credentials: 'include'
        });
        const notifications = await res.json();
        if (!notifications || notifications.length === 0) {
            container.innerHTML = '<div class="empty-state">暂无消息</div>';
            await updateMsgBadge();
            return;
        }

        let html = `
            <div style="padding: 12px 20px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                    <span>消息列表</span>
                    <button id="markAllReadBtn" style="background: none; border: none; color: #0071e3;">一键已读</button>
                </div>
            </div>
        `;
        notifications.forEach(n => {
            const isUnread = !n.isRead;
            html += `
                <div class="notification-item ${isUnread ? 'unread' : ''}" data-id="${n.id}">
                    <div class="notification-content">
                        <div class="notification-message">${escapeHtml(n.message)}</div>
                        <div class="notification-footer">
                            <span class="notification-time">${formatRelativeTime(n.createTime)}</span>
                            ${isUnread ? `<button class="mark-read-btn" data-id="${n.id}">标为已读</button>` : ''}
                            <button class="delete-btn" data-id="${n.id}">删除</button>
                        </div>
                    </div>
                </div>
            `;
        });
        container.innerHTML = html;

        document.querySelectorAll('.mark-read-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const id = btn.getAttribute('data-id');
                await markNotificationAsRead(id);
            });
        });
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.stopPropagation();
                const id = btn.getAttribute('data-id');
                if (confirm('删除该通知？')) await deleteNotification(id);
            });
        });
        const markAllBtn = document.getElementById('markAllReadBtn');
        if (markAllBtn) markAllBtn.addEventListener('click', markAllAsRead);

        await updateMsgBadge();
    } catch (e) {
        console.error(e);
        container.innerHTML = '<div class="empty-state">加载失败，请重试</div>';
    }
}

async function markNotificationAsRead(notificationId) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    try {
        const formData = new URLSearchParams();
        formData.append('notificationId', notificationId);
        formData.append('studentId', studentId);
        const res = await fetch(API_BASE_URL + '/api/notification/read', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData,
            credentials: 'include'
        });
        const data = await res.json();
        if (data.success) {
            await loadNotifications();
            await updateMsgBadge();
            showToast('已标记为已读', 'success');
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    } catch (e) {
        showToast('操作失败', 'error');
    }
}

async function markAllAsRead() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    try {
        const formData = new URLSearchParams();
        formData.append('studentId', studentId);
        const res = await fetch(API_BASE_URL + '/api/notification/read-all', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData,
            credentials: 'include'
        });
        const data = await res.json();
        if (data.success) {
            await loadNotifications();
            await updateMsgBadge();
            showToast('已全部标记为已读', 'success');
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    } catch (e) {
        showToast('操作失败', 'error');
    }
}

async function deleteNotification(notificationId) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    try {
        const formData = new URLSearchParams();
        formData.append('notificationId', notificationId);
        formData.append('studentId', studentId);
        const res = await fetch(API_BASE_URL + '/api/notification/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData,
            credentials: 'include'
        });
        const data = await res.json();
        if (data.success) {
            await loadNotifications();
            await updateMsgBadge();
            showToast('删除成功', 'success');
        } else {
            showToast(data.message || '删除失败', 'error');
        }
    } catch (e) {
        showToast('删除失败', 'error');
    }
}

// ========== 个人中心相关 ==========
async function renderMineEnhanced() {
    const container = document.getElementById('mineContent');
    if (!container) return;
    const isLoggedIn = localStorage.getItem('loggedIn') === 'true';
    const studentId = localStorage.getItem('studentId') || '';
    if (!isLoggedIn || !studentId) {
        container.innerHTML = `<div class="card" style="text-align:center; padding:40px;"><div style="font-size:17px;color:#0071e3;cursor:pointer;" onclick="goTo('login.html')"><i class="fas fa-user-circle"></i> 点击登录 · 开启校园圈</div></div>`;
        return;
    }
    try {
        const userRes = await fetch(`${API_BASE_URL}/api/user/info?studentId=${studentId}`);
        const user = await userRes.json();
        const statsRes = await fetch(`${API_BASE_URL}/api/user/stats?studentId=${studentId}`);
        const stats = await statsRes.json();
        const avatarHtml = user.avatar ? `<img src="${user.avatar.startsWith('http') ? user.avatar : API_BASE_URL + user.avatar}" style="width:80px;height:80px;border-radius:80px;object-fit:cover;">` : `<i class="fas fa-user-graduate"></i>`;
        const profileHtml = `
            <div class="profile-header">
                <div class="profile-avatar">${avatarHtml}</div>
                <div class="profile-info">
                    <div class="profile-name">${escapeHtml(user.name || studentId)}</div>
                    <div class="profile-bio">${escapeHtml(user.bio || '这个人很懒，还没写签名~')}</div>
                    <div class="profile-stats">
                        <div class="stat-item"><div class="stat-number">${stats.postCount || 0}</div><div class="stat-label">帖子</div></div>
                        <div class="stat-item"><div class="stat-number">${stats.favoriteCount || 0}</div><div class="stat-label">收藏</div></div>
                        <div class="stat-item"><div class="stat-number">${stats.orderCount || 0}</div><div class="stat-label">订单</div></div>
                    </div>
                </div>
            </div>
            <div class="menu-item" onclick="goTo('my-posts.html')"><div class="menu-left"><i class="fas fa-file-alt"></i><span>我的帖子</span></div><div class="menu-right"><i class="fas fa-chevron-right"></i></div></div>
            <div class="menu-item" onclick="goTo('my-favorites.html')"><div class="menu-left"><i class="fas fa-heart"></i><span>我的收藏</span></div><div class="menu-right"><i class="fas fa-chevron-right"></i></div></div>
            <div class="menu-item" onclick="goTo('my-orders.html')"><div class="menu-left"><i class="fas fa-shopping-bag"></i><span>我的订单</span></div><div class="menu-right"><i class="fas fa-chevron-right"></i></div></div>
            <div class="menu-item" onclick="goTo('edit-profile.html')"><div class="menu-left"><i class="fas fa-edit"></i><span>编辑资料</span></div><div class="menu-right"><i class="fas fa-chevron-right"></i></div></div>
            <div class="menu-item" onclick="logout()"><div class="menu-left"><i class="fas fa-sign-out-alt"></i><span>退出登录</span></div></div>
            <div class="menu-item danger-item" onclick="showSettings()"><div class="menu-left"><i class="fas fa-trash-alt"></i><span>注销账号</span></div></div>
        `;
        container.innerHTML = profileHtml;
    } catch(e) {
        container.innerHTML = '<div class="card">加载失败，请重试</div>';
    }
}

async function loadProfileForEdit() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) { showToast('请先登录', 'error'); goTo('mine.html'); return; }
    const res = await fetch(`${API_BASE_URL}/api/user/info?studentId=${studentId}`);
    const user = await res.json();
    const nameInput = document.getElementById('editName');
    const studentIdInput = document.getElementById('editStudentId');
    const bioTextarea = document.getElementById('editBio');
    const avatarInput = document.getElementById('editAvatar');
    const nicknameInput = document.getElementById('editNickname');
    if (nicknameInput) nicknameInput.value = user.nickname || '';
    if (nameInput) nameInput.value = user.name || '';
    if (studentIdInput) studentIdInput.value = studentId;
    if (bioTextarea) bioTextarea.value = user.bio || '';
    if (avatarInput) avatarInput.value = user.avatar || '';
}

async function updateProfile() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        showToast('请先登录', 'error');
        return;
    }
    const name = document.getElementById('editName')?.value.trim() || '';
    const bio = document.getElementById('editBio')?.value.trim() || '';
    const nickname = document.getElementById('editNickname')?.value.trim() || '';
    const avatar = document.getElementById('editAvatar')?.value.trim() || '';

    const formBody = `studentId=${studentId}&name=${encodeURIComponent(name)}&bio=${encodeURIComponent(bio)}&avatar=${encodeURIComponent(avatar)}&nickname=${encodeURIComponent(nickname)}`;

    try {
        const res = await fetch(API_BASE_URL + '/api/user/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formBody
        });
        const data = await res.json();
        if (data.success) {
            showToast('资料更新成功', 'success');
            goTo('mine.html');
        } else {
            showToast(data.message || '更新失败', 'error');
        }
    } catch (e) {
        showToast('网络错误', 'error');
    }
}

async function loadMyPosts() {
    const studentId = localStorage.getItem('studentId');
    const container = document.getElementById('myPostsList');
    if (!container) return;
    if (!studentId) {
        container.innerHTML = '<div class="empty-state">请先登录</div>';
        return;
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/user/posts?studentId=${studentId}`);
        const posts = await res.json();
        if (!posts || posts.length === 0) {
            container.innerHTML = '<div class="empty-state">✨ 暂无发布内容</div>';
            return;
        }
        container.innerHTML = posts.map(p => `
            <div class="list-item" data-post-id="${p.id}">
                <div class="post-title">${escapeHtml(p.title)}</div>
                <div class="post-content">${escapeHtml(p.content.substring(0, 80))}${p.content.length > 80 ? '...' : ''}</div>
                <div class="post-footer" style="margin-top:12px; display: flex; justify-content: space-between; align-items: center;">
                    <span>#${escapeHtml(p.type)}</span>
                    <span>${formatRelativeTime(p.createTime)}</span>
                    <span class="action-icon delete-post-btn" style="color:#ff3b30;" onclick="deleteMyPost(${p.id})">
                        <i class="fas fa-trash-alt"></i> 删除
                    </span>
                </div>
            </div>
        `).join('');
    } catch(e) {
        container.innerHTML = '<div class="empty-state">加载失败，请重试</div>';
    }
}

async function deleteMyPost(postId) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        showToast('请先登录', 'error');
        return;
    }
    showConfirmDialog('确定要删除这条动态吗？删除后无法恢复。', async () => {
        try {
            const res = await fetch(API_BASE_URL + '/api/post/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: `studentId=${studentId}&postId=${postId}`
            });
            const data = await res.json();
            if (data.success) {
                showToast('删除成功', 'success');
                loadMyPosts();
                if (window.location.pathname.endsWith('index.html') || window.location.pathname === '/' || window.location.pathname === '') {
                    if (typeof loadPosts === 'function') loadPosts();
                }
            } else {
                showToast(data.message || '删除失败', 'error');
            }
        } catch (err) {
            showToast('网络错误', 'error');
        }
    });
}

async function loadMyFavorites() {
    const studentId = localStorage.getItem('studentId');
    const container = document.getElementById('myFavoritesList');
    if (!container) return;
    if (!studentId) { container.innerHTML = '<div class="empty-state">请先登录</div>'; return; }
    const res = await fetch(`${API_BASE_URL}/api/user/favorites?studentId=${studentId}`);
    const favorites = await res.json();
    if (!favorites || favorites.length === 0) {
        container.innerHTML = '<div class="empty-state">❤️ 暂无收藏内容</div>';
        return;
    }
    container.innerHTML = favorites.map(p => `
        <div class="list-item" onclick="goToPost(${p.id})">
            <div class="post-title">${escapeHtml(p.title)}</div>
            <div class="post-content">${escapeHtml(p.content.substring(0, 80))}</div>
            <div class="post-footer"><span>${escapeHtml(p.type)}</span><span><i class="far fa-heart"></i> 已收藏</span><span>${formatRelativeTime(p.createTime)}</span></div>
        </div>
    `).join('');
}

async function loadMyOrders() {
    const studentId = localStorage.getItem('studentId');
    const container = document.getElementById('myOrdersList');
    if (!container) return;
    if (!studentId) { container.innerHTML = '<div class="empty-state">请先登录</div>'; return; }
    const res = await fetch(`${API_BASE_URL}/api/user/orders?studentId=${studentId}`);
    const orders = await res.json();
    if (!orders || orders.length === 0) {
        container.innerHTML = '<div class="empty-state">📦 暂无订单，去打印/跑腿试试~</div>';
        return;
    }
    container.innerHTML = orders.map(o => `
        <div class="list-item">
            <div style="display:flex; justify-content:space-between;"><strong>${escapeHtml(o.orderType)}</strong><span>${o.status === 'PAID' ? '已支付' : '待支付'}</span></div>
            <div>金额: ¥${o.amount}</div>
            <div>创建时间: ${formatRelativeTime(o.createTime)}</div>
        </div>
    `).join('');
}

function goToPost(postId) {
    showToast('跳转帖子详情', 'info');
}

function logout() {
    closeWebSocket();
    localStorage.removeItem('loggedIn');
    localStorage.removeItem('studentId');
    // 如果需要保留主题设置，不要清除所有 localStorage
    // 可以只删除登录相关的 key
    // 或者保留主题配置
    const themeId = localStorage.getItem('icampus_theme_id');
    const darkMode = localStorage.getItem('icampus_dark');
    //localStorage.clear();  // 如果希望彻底清除，但会丢失主题
    if (themeId) localStorage.setItem('icampus_theme_id', themeId);
    if (darkMode) localStorage.setItem('icampus_dark', darkMode);
    showToast('已退出', 'success');
    setTimeout(() => goTo('index.html'), 1000);
}

function showSettings() {
    if (confirm('⚠️ 注销账号将永久删除您的所有数据，且不可恢复。确定要注销吗？')) {
        const studentId = localStorage.getItem('studentId');
        if (!studentId) { showToast('未登录', 'error'); return; }
        fetch(API_BASE_URL + '/api/user/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `studentId=${studentId}`
        }).then(res => res.json()).then(data => {
            if (data.success) {
                localStorage.removeItem('studentId');
                localStorage.removeItem('loggedIn');
                showToast('账号已注销', 'success');
                goTo('index.html');
                if (typeof renderMineEnhanced === 'function') renderMineEnhanced();
            } else {
                showToast(data.message || '注销失败', 'error');
            }
        }).catch(() => showToast('网络错误', 'error'));
    }
}

function showConfirmDialog(message, onConfirm) {
    const existingModal = document.querySelector('.ios-confirm-modal');
    if (existingModal) existingModal.remove();
    const modal = document.createElement('div');
    modal.className = 'ios-confirm-modal';
    modal.innerHTML = `
        <div class="ios-confirm-overlay"></div>
        <div class="ios-confirm-container">
            <div class="ios-confirm-message">${escapeHtml(message)}</div>
            <div class="ios-confirm-buttons">
                <button class="ios-confirm-btn cancel">取消</button>
                <button class="ios-confirm-btn confirm">删除</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
    setTimeout(() => modal.classList.add('show'), 10);
    const cancelBtn = modal.querySelector('.cancel');
    const confirmBtn = modal.querySelector('.confirm');
    const overlay = modal.querySelector('.ios-confirm-overlay');
    const close = () => {
        modal.classList.remove('show');
        setTimeout(() => modal.remove(), 200);
    };
    cancelBtn.onclick = close;
    overlay.onclick = close;
    confirmBtn.onclick = () => {
        close();
        if (onConfirm) onConfirm();
    };
}

// ========== 帖子相关功能 ==========
let currentCate = '最新';
let searchKeyword = '';

function loadPosts() {
    const container = document.getElementById('postList');
    if (!container) return;
    let category = currentCate === '最新' ? '' : currentCate;
    let keyword = searchKeyword;
    let studentId = localStorage.getItem('studentId') || '';
    let url = `/api/post/list?category=${encodeURIComponent(category)}&keyword=${encodeURIComponent(keyword)}&studentId=${encodeURIComponent(studentId)}`;
    fetch(API_BASE_URL + url)
        .then(res => res.json())
        .then(posts => renderPosts(posts))
        .catch(err => { container.innerHTML = '<div style="text-align:center; padding:40px;">加载失败</div>'; });
}

function renderPosts(posts) {
    const container = document.getElementById('postList');
    if (!container) return;
    if (!posts || posts.length === 0) {
        container.innerHTML = `<div style="text-align:center; padding:40px;">暂无动态</div>`;
        return;
    }
    container.innerHTML = posts.map(p => {
        const likeClass = p.userLiked ? 'action-icon like-icon liked' : 'action-icon like-icon';
        const dislikeClass = p.userDisliked ? 'action-icon dislike-icon disliked' : 'action-icon dislike-icon';
        return `
            <div class="post-item" data-post-id="${p.id}">
                <div class="post-header">
                    <div class="post-avatar">
                        ${p.avatar ? `<img src="${p.avatar.startsWith('http') ? p.avatar : API_BASE_URL + p.avatar}" style="width:44px;height:44px;border-radius:44px;object-fit:cover;">` : `<i class="fas fa-user-circle"></i>`}
                    </div>
                    <div class="post-name">${escapeHtml(p.nickname || p.studentId)}</div>
                    <div class="post-tag">#${escapeHtml(p.type)}</div>
                </div>
                <div class="post-title">${escapeHtml(p.title)}</div>
                <div class="post-content">${escapeHtml(p.content)}</div>
                <div class="post-footer">
                    <span><i class="far fa-calendar-alt"></i> ${formatRelativeTime(p.createTime)}</span>
                    <div class="post-actions">
                        <span class="${likeClass}" data-post-id="${p.id}" onclick="likePost(${p.id}, this)">
                            <i class="far fa-thumbs-up"></i> <span class="like-count">${p.likeCount || 0}</span>
                        </span>
                        <span class="${dislikeClass}" data-post-id="${p.id}" onclick="dislikePost(${p.id}, this)">
                            <i class="far fa-thumbs-down"></i> <span class="dislike-count">${p.dislikeCount || 0}</span>
                        </span>
                        <span class="action-icon comment-icon" data-post-id="${p.id}" onclick="toggleComments(${p.id})">
                            <i class="far fa-comment"></i> <span class="comment-count">${p.commentCount || 0}</span>
                        </span>
                    </div>
                </div>
                <div class="comment-section" id="comment-section-${p.id}" style="display:none;">
                    <div id="comments-list-${p.id}" class="comments-list"></div>
                    <div class="comment-input-area">
                        <input type="text" id="comment-input-${p.id}" placeholder="写下你的评论...">
                        <button onclick="submitComment(${p.id})">发布</button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function likePost(postId, btn) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) { showToast('请先登录', 'error'); goTo('login.html'); return; }
    fetch(API_BASE_URL + '/api/post/like', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `studentId=${studentId}&postId=${postId}`
    }).then(res => res.json()).then(data => { if (data.success) loadPosts(); else showToast(data.message || '操作失败', 'error'); });
}

function dislikePost(postId, btn) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) { showToast('请先登录', 'error'); goTo('login.html'); return; }
    fetch(API_BASE_URL + '/api/post/dislike', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `studentId=${studentId}&postId=${postId}`
    }).then(res => res.json()).then(data => { if (data.success) loadPosts(); else showToast(data.message || '操作失败', 'error'); });
}

function toggleComments(postId) {
    const section = document.getElementById(`comment-section-${postId}`);
    if (section) {
        if (section.style.display === 'none') {
            section.style.display = 'block';
            loadComments(postId);
        } else {
            section.style.display = 'none';
        }
    }
}

function loadComments(postId) {
    fetch(`${API_BASE_URL}/api/post/comments?postId=${postId}`)
        .then(res => res.json())
        .then(comments => {
            const container = document.getElementById(`comments-list-${postId}`);
            if (!container) return;
            if (!comments || comments.length === 0) {
                container.innerHTML = '<div style="padding:12px; color:#8e8e93;">暂无评论，来抢沙发～</div>';
                return;
            }
            container.innerHTML = comments.map(c => `<div class="comment-item"><span class="comment-author">${escapeHtml(c.studentId)}</span><span class="comment-time">${formatRelativeTime(c.createTime)}</span><div class="comment-content">${escapeHtml(c.content)}</div></div>`).join('');
        });
}

function submitComment(postId) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) { showToast('请先登录', 'error'); goTo('login.html'); return; }
    const input = document.getElementById(`comment-input-${postId}`);
    if (!input) return;
    const content = input.value.trim();
    if (!content) { showToast('评论内容不能为空', 'error'); return; }
    fetch(API_BASE_URL + '/api/post/comment', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `studentId=${studentId}&postId=${postId}&content=${encodeURIComponent(content)}`
    }).then(res => res.json()).then(data => {
        if (data.success) {
            showToast('评论成功', 'success');
            input.value = '';
            loadComments(postId);
            const commentCountSpan = document.querySelector(`.post-item[data-post-id="${postId}"] .comment-count`);
            if (commentCountSpan) commentCountSpan.innerText = data.commentCount;
        } else {
            showToast(data.message || '评论失败', 'error');
        }
    });
}

function publishPost() {
    const isLoggedIn = localStorage.getItem('loggedIn') === 'true';
    const studentId = localStorage.getItem('studentId');
    if (!isLoggedIn || !studentId) { showToast('请先登录', 'error'); goTo('login.html'); return; }
    const type = document.getElementById('postType')?.value;
    const title = document.getElementById('postTitle')?.value.trim();
    const content = document.getElementById('postContent')?.value.trim();
    if (!title || !content) { showToast('标题和内容不能为空', 'error'); return; }
    fetch(API_BASE_URL + '/api/post/publish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `studentId=${studentId}&type=${type}&title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}`
    }).then(res => res.json()).then(data => {
        if (data.success) {
            showToast('发布成功！', 'success');
            if (document.getElementById('postTitle')) document.getElementById('postTitle').value = '';
            if (document.getElementById('postContent')) document.getElementById('postContent').value = '';
            goTo('index.html');
        } else {
            showToast(data.message || '发布失败', 'error');
        }
    }).catch(() => showToast('网络错误', 'error'));
}

// ========== 登录注册 ==========
async function doLogin() {
    const studentId = document.getElementById('loginStudentId')?.value.trim();
    const password = document.getElementById('loginPassword')?.value.trim();
    if (!studentId || !password) {
        showToast('请填写学号和密码', 'error');
        return;
    }
    try {
        const res = await fetch(API_BASE_URL + '/api/user/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `studentId=${studentId}&password=${password}`
        });
        const data = await res.json();
        if (data.success) {
            // 改为 localStorage
            localStorage.setItem('loggedIn', 'true');
            localStorage.setItem('studentId', studentId);
            showToast('登录成功', 'success');
            await loadHomeSearchHistory();
            await updateMsgBadge();
            connectWebSocket();
            goTo('mine.html');
        } else {
            showToast(data.message || '登录失败', 'error');
        }
    } catch (error) {
        showToast('网络错误', 'error');
    }
}

function doRegister() {
    const studentId = document.getElementById('regStudentId')?.value.trim();
    const password = document.getElementById('regPassword')?.value.trim();
    const confirm = document.getElementById('regConfirm')?.value.trim();
    if (!studentId || !password) { showToast('请填写完整信息', 'error'); return; }
    if (password !== confirm) { showToast('两次密码不一致', 'error'); return; }
    fetch(API_BASE_URL + '/api/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `studentId=${studentId}&password=${password}`
    }).then(res => res.json()).then(data => {
        if (data.success) {
            showToast('注册成功，请登录', 'success');
            goTo('login.html');
        } else {
            showToast(data.message || '注册失败', 'error');
        }
    });
}

// ========== 搜索相关 ==========
function filterCate(cate) {
    currentCate = cate;
    const tabs = document.querySelectorAll('.cate-tab');
    tabs.forEach(el => el.classList.remove('active'));
    if (event && event.target) event.target.classList.add('active');
    loadPosts();
}

function doSearch() {
    const input = document.getElementById('searchInput');
    searchKeyword = input ? input.value.trim() : '';
    if (!searchKeyword) {
        const dropdown = document.getElementById('searchHistoryDropdown');
        if (dropdown) dropdown.style.display = 'none';
    }
    loadPosts();
}

async function loadHotPosts() {
    const hotScrollDiv = document.querySelector('.hot-scroll');
    if (!hotScrollDiv) return;
    try {
        const res = await fetch(API_BASE_URL + '/api/post/hot?limit=10');
        const posts = await res.json();
        if (posts && posts.length) {
            const itemsHtml = posts.map((post, idx) => `<div class="hot-item" onclick="searchKeywordDirectly('${escapeHtml(post.title)}')">${idx+1}. ${escapeHtml(post.title)}</div>`).join('');
            hotScrollDiv.innerHTML = itemsHtml + itemsHtml;
        } else {
            const fallback = ['校园卡丢失', '考研书籍出售', '运动会通知', '校园兼职', '雨伞丢失', '电动车出售', 'intj答疑', '男生年龄焦虑'];
            let fallbackHtml = '';
            for (let i = 0; i < fallback.length; i++) {
                fallbackHtml += `<div class="hot-item" onclick="searchKeywordDirectly('${fallback[i]}')">${i+1}. ${fallback[i]}</div>`;
            }
            hotScrollDiv.innerHTML = fallbackHtml + fallbackHtml;
        }
    } catch(e) { console.warn('热榜加载失败', e); }
}

async function saveSearchRecord(keyword) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId || !keyword.trim()) return;
    try {
        await fetch(API_BASE_URL + '/api/search/save', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `studentId=${studentId}&keyword=${encodeURIComponent(keyword)}`
        });
    } catch(e) {}
}

async function loadSearchHistory() {
    const studentId = localStorage.getItem('studentId');
    const container = document.getElementById('searchHistoryContainer');
    const listDiv = document.getElementById('historyList');
    if (!container || !listDiv) return;
    if (!studentId) {
        container.style.display = 'none';
        return;
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/search/list?studentId=${studentId}`);
        const records = await res.json();
        if (records && records.length) {
            container.style.display = 'block';
            listDiv.innerHTML = records.map(rec => `<div class="history-item" data-id="${rec.id}"><span onclick="searchKeywordDirectly('${escapeHtml(rec.keyword)}')">${escapeHtml(rec.keyword)}</span><i class="fas fa-times-circle history-delete" onclick="deleteSearchRecord('${rec.id}', event)"></i></div>`).join('');
        } else {
            container.style.display = 'none';
        }
    } catch(e) { container.style.display = 'none'; }
}

window.deleteSearchRecord = async function(recordId, event) {
    event.stopPropagation();
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    try {
        await fetch(API_BASE_URL + '/api/search/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `studentId=${studentId}&recordId=${recordId}`
        });
        loadSearchHistory();
    } catch(e) {}
};

document.getElementById('manageHistoryBtn')?.addEventListener('click', async () => {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    if (confirm('清空所有搜索记录？')) {
        await fetch(API_BASE_URL + '/api/search/clear', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: `studentId=${studentId}`
        });
        loadSearchHistory();
    }
});

async function performSearch(keyword) {
    if (!keyword.trim()) return;
    const resultContainer = document.getElementById('searchResultList');
    if (!resultContainer) return;
    resultContainer.innerHTML = '<div style="text-align:center; padding:40px;"><i class="fas fa-spinner fa-pulse"></i> 搜索中...</div>';
    try {
        const res = await fetch(`${API_BASE_URL}/api/post/search?keyword=${encodeURIComponent(keyword)}`);
        const posts = await res.json();
        if (!posts || posts.length === 0) {
            resultContainer.innerHTML = '<div class="search-result-empty">✨ 这里什么都没有呢~</div>';
            return;
        }
        resultContainer.innerHTML = posts.map(p => `
            <div class="post-item">
                <div class="post-header"><div class="post-avatar"><i class="fas fa-user-circle"></i></div><div class="post-name">${escapeHtml(p.nickname || p.studentId)}</div><div class="post-tag">#${escapeHtml(p.type)}</div></div>
                <div class="post-title">${escapeHtml(p.title)}</div>
                <div class="post-content">${escapeHtml(p.content)}</div>
                <div class="post-footer"><span><i class="far fa-calendar-alt"></i> ${formatRelativeTime(p.createTime)}</span><div class="post-actions"><span class="action-icon"><i class="far fa-thumbs-up"></i> ${p.likeCount || 0}</span><span class="action-icon"><i class="far fa-comment"></i> ${p.commentCount || 0}</span></div></div>
            </div>
        `).join('');
    } catch(e) { resultContainer.innerHTML = '<div class="search-result-empty">网络开小差了，请重试</div>'; }
}

window.searchKeywordDirectly = function(keyword) {
    const searchInput = document.getElementById('searchInputPage');
    if (searchInput) searchInput.value = keyword;
    performSearch(keyword);
    goTo('search.html');
};

function bindHomeSearch() {
    const homeInput = document.getElementById('searchInput');
    if (!homeInput) return;
    homeInput.addEventListener('focus', () => homeInput.placeholder = '');
    homeInput.addEventListener('blur', () => { if (!homeInput.value) homeInput.placeholder = '搜失物、二手、公告'; });
    homeInput.addEventListener('keypress', async (e) => {
        if (e.key === 'Enter') {
            const kw = homeInput.value.trim();
            if (kw) {
                await saveSearchRecord(kw);
                const searchInputPage = document.getElementById('searchInputPage');
                if (searchInputPage) searchInputPage.value = kw;
                performSearch(kw);
                goTo('search.html');
            }
        }
    });
}

function bindSearchPageEvents() {
    const searchInput = document.getElementById('searchInputPage');
    const clearBtn = document.getElementById('clearSearchPage');
    if (!searchInput) return;
    searchInput.addEventListener('input', () => {
        if (searchInput.value) {
            if (clearBtn) clearBtn.style.display = 'block';
        } else {
            if (clearBtn) clearBtn.style.display = 'none';
        }
    });
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            searchInput.value = '';
            clearBtn.style.display = 'none';
            const resultContainer = document.getElementById('searchResultList');
            if (resultContainer) resultContainer.innerHTML = '';
            loadSearchHistory();
            searchInput.focus();
        });
    }
    searchInput.addEventListener('keypress', async (e) => {
        if (e.key === 'Enter') {
            const keyword = searchInput.value.trim();
            if (!keyword) return;
            await saveSearchRecord(keyword);
            loadSearchHistory();
            performSearch(keyword);
        }
    });
}

let currentSearchHistory = [];

async function loadHomeSearchHistory() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        currentSearchHistory = [];
        renderHomeHistoryDropdown();
        return;
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/search/list?studentId=${studentId}`);
        const records = await res.json();
        currentSearchHistory = records || [];
        renderHomeHistoryDropdown();
    } catch(e) {
        console.warn('加载历史失败', e);
    }
}

function renderHomeHistoryDropdown() {
    const dropdown = document.getElementById('searchHistoryDropdown');
    const listContainer = document.getElementById('homeHistoryList');
    if (!dropdown || !listContainer) return;
    if (!currentSearchHistory.length) {
        dropdown.style.display = 'none';
        return;
    }
    listContainer.innerHTML = currentSearchHistory.map(rec => `
        <div class="history-item" data-id="${rec.id}">
            <span onclick="searchKeywordDirectly('${escapeHtml(rec.keyword)}')">${escapeHtml(rec.keyword)}</span>
            <i class="fas fa-times-circle" onclick="deleteHomeHistoryRecord('${rec.id}', event)"></i>
        </div>
    `).join('');
    dropdown.style.display = 'block';
}

async function deleteHomeHistoryRecord(recordId, event) {
    event.stopPropagation();
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    await fetch(API_BASE_URL + '/api/search/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `studentId=${studentId}&recordId=${recordId}`
    });
    await loadHomeSearchHistory();
}

function bindHomeSearchClick() {
    const searchInput = document.getElementById('searchInput');
    const dropdown = document.getElementById('searchHistoryDropdown');
    if (!searchInput) return;
    searchInput.addEventListener('click', function(e) {
        e.stopPropagation();
        loadHomeSearchHistory();
    });
    document.addEventListener('click', function() {
        if (dropdown) dropdown.style.display = 'none';
    });
    if (dropdown) {
        dropdown.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    }
    const manageBtn = document.getElementById('homeHistoryManageBtn');
    if (manageBtn) {
        manageBtn.addEventListener('click', async () => {
            const studentId = localStorage.getItem('studentId');
            if (!studentId) return;
            if (confirm('清空所有搜索记录？')) {
                await fetch(API_BASE_URL + '/api/search/clear', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: `studentId=${studentId}`
                });
                await loadHomeSearchHistory();
            }
        });
    }
}

function bindHomeSearchClear() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchHome');
    if (!searchInput || !clearBtn) return;
    searchInput.addEventListener('input', function() {
        if (this.value) {
            clearBtn.style.display = 'block';
        } else {
            clearBtn.style.display = 'none';
            const dropdown = document.getElementById('searchHistoryDropdown');
            if (dropdown) dropdown.style.display = 'none';
        }
    });
    clearBtn.addEventListener('click', function() {
        searchInput.value = '';
        clearBtn.style.display = 'none';
        doSearch();
        const dropdown = document.getElementById('searchHistoryDropdown');
        if (dropdown) dropdown.style.display = 'none';
        searchInput.focus();
    });
}

// ========== 图片压缩 ==========
async function compressImage(file, maxWidth = 800, quality = 0.7) {
    return new Promise((resolve) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = (e) => {
            const img = new Image();
            img.src = e.target.result;
            img.onload = () => {
                const canvas = document.createElement('canvas');
                let width = img.width;
                let height = img.height;
                if (width > maxWidth) {
                    height = (height * maxWidth) / width;
                    width = maxWidth;
                }
                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);
                canvas.toBlob((blob) => {
                    resolve(new File([blob], file.name, { type: file.type }));
                }, file.type, quality);
            };
        };
    });
}

function previewAvatar(file) {
    const reader = new FileReader();
    reader.onload = function(e) {
        const preview = document.getElementById('avatarPreview');
        if (preview) {
            preview.src = e.target.result;
            preview.style.display = 'block';
        }
    };
    reader.readAsDataURL(file);
}

async function uploadAvatar() {
    const fileInput = document.getElementById('avatarFile');
    const file = fileInput?.files[0];
    if (!file) {
        showToast('请选择图片文件', 'error');
        return;
    }
    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        showToast('请先登录', 'error');
        return;
    }
    showToast('正在压缩并上传...', 'info');
    let compressedFile;
    try {
        compressedFile = await compressImage(file);
    } catch (e) {
        showToast('图片处理失败，将使用原图上传', 'info');
        compressedFile = file;
    }
    const formData = new FormData();
    formData.append('file', compressedFile);
    formData.append('studentId', studentId);
    try {
        const xhr = new XMLHttpRequest();
        xhr.open('POST', API_BASE_URL + '/api/user/upload-avatar', true);
        xhr.upload.addEventListener('progress', (e) => {
            if (e.lengthComputable) {
                const percent = Math.round((e.loaded / e.total) * 100);
                showToast(`上传中 ${percent}%`, 'info');
            }
        });
        xhr.onload = () => {
            if (xhr.status === 200) {
                const data = JSON.parse(xhr.responseText);
                if (data.success) {
                    showToast('头像上传成功', 'success');
                    const preview = document.getElementById('avatarPreview');
                    if (preview) preview.src = data.avatarUrl + '?t=' + new Date().getTime();
                    const avatarInput = document.getElementById('editAvatar');
                    if (avatarInput) avatarInput.value = data.avatarUrl;
                } else {
                    showToast(data.message || '上传失败', 'error');
                }
            } else {
                showToast('上传失败', 'error');
            }
        };
        xhr.onerror = () => {
            showToast('网络错误', 'error');
        };
        xhr.send(formData);
    } catch (e) {
        showToast('网络错误', 'error');
    }
}

// ========== 校园兼职模块 ==========
let allJobs = [];

async function loadJobs() {
    const container = document.getElementById('jobList');
    if (!container) return;
    try {
        const res = await fetch(API_BASE_URL + '/api/job/list');
        allJobs = await res.json();
        renderJobList(allJobs);
    } catch(e) {
        container.innerHTML = '<div class="empty-state">加载失败，请重试</div>';
    }
}

function renderJobList(jobs) {
    const container = document.getElementById('jobList');
    if (!container) return;
    if (!jobs || jobs.length === 0) {
        container.innerHTML = '<div class="empty-state">✨ 暂无兼职信息</div>';
        return;
    }
    container.innerHTML = jobs.map(job => `
        <div class="post-item" data-job-id="${job.id}">
            <div class="post-header">
                <div class="post-avatar"><i class="fas fa-briefcase"></i></div>
                <div class="post-name">${escapeHtml(job.title)}</div>
                <div class="post-tag">#兼职</div>
            </div>
            <div class="post-content">
                ${job.workTime ? `<i class="fas fa-clock"></i> ${escapeHtml(job.workTime)}<br>` : ''}
                ${job.location ? `<i class="fas fa-map-marker-alt"></i> ${escapeHtml(job.location)}` : ''}
            </div>
            <div class="post-footer">
                <span><i class="far fa-calendar-alt"></i> ${formatRelativeTime(job.createTime)}</span>
                <div class="post-actions">
                    <span class="action-icon" onclick="showJobDetail(${job.id})"><i class="fas fa-eye"></i> 详情</span>
                </div>
            </div>
        </div>
    `).join('');
}

function filterJobs() {
    const keyword = document.getElementById('jobSearchInput')?.value.trim().toLowerCase();
    if (!keyword) {
        renderJobList(allJobs);
        return;
    }
    const filtered = allJobs.filter(job =>
        job.title.toLowerCase().includes(keyword) ||
        (job.location && job.location.toLowerCase().includes(keyword))
    );
    renderJobList(filtered);
}

async function showJobDetail(jobId) {
    try {
        const res = await fetch(`${API_BASE_URL}/api/job/${jobId}`);
        const job = await res.json();
        const detailTitle = document.getElementById('detailTitle');
        const detailWorkTime = document.getElementById('detailWorkTime');
        const detailLocation = document.getElementById('detailLocation');
        const detailPeopleCount = document.getElementById('detailPeopleCount');
        const detailRequirements = document.getElementById('detailRequirements');
        const detailDescription = document.getElementById('detailDescription');
        const detailTime = document.getElementById('detailTime');
        const detailPhone = document.getElementById('detailPhone');
        if (detailTitle) detailTitle.innerText = job.title;
        if (detailWorkTime) detailWorkTime.innerText = job.workTime || '未填写';
        if (detailLocation) detailLocation.innerText = job.location || '未填写';
        if (detailPeopleCount) detailPeopleCount.innerText = job.peopleCount || '未填写';
        if (detailRequirements) detailRequirements.innerText = job.requirements || '无特殊要求';
        if (detailDescription) detailDescription.innerText = job.description || '暂无详细描述';
        if (detailTime) detailTime.innerText = formatRelativeTime(job.createTime);
        if (detailPhone) detailPhone.innerText = job.phone || '未提供';
        const modal = document.getElementById('jobDetailModal');
        if (modal) modal.style.display = 'flex';
    } catch(e) {
        showToast('加载详情失败', 'error');
    }
}

async function publishJob() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        showToast('请先登录', 'error');
        goTo('login.html');
        return;
    }
    const title = document.getElementById('jobTitle')?.value.trim();
    if (!title) {
        showToast('标题不能为空', 'error');
        return;
    }
    const phone = document.getElementById('jobPhone')?.value.trim();
    if (!phone) {
        showToast('请填写联系电话', 'error');
        return;
    }
    const workTime = document.getElementById('jobWorkTime')?.value.trim();
    const location = document.getElementById('jobLocation')?.value.trim();
    const peopleCount = document.getElementById('jobPeopleCount')?.value.trim();
    const requirements = document.getElementById('jobRequirements')?.value.trim();
    const description = document.getElementById('jobDescription')?.value.trim();
    const formData = new URLSearchParams();
    formData.append('studentId', studentId);
    formData.append('title', title);
    formData.append('phone', phone);
    if (workTime) formData.append('workTime', workTime);
    if (location) formData.append('location', location);
    if (peopleCount) formData.append('peopleCount', peopleCount);
    if (requirements) formData.append('requirements', requirements);
    if (description) formData.append('description', description);
    try {
        const res = await fetch(API_BASE_URL + '/api/job/publish', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        });
        const data = await res.json();
        if (data.success) {
            showToast('发布成功', 'success');
            closePublishModal();
            loadJobs();
        } else {
            showToast(data.message || '发布失败', 'error');
        }
    } catch(e) {
        showToast('网络错误', 'error');
    }
}

async function loadMyJobs() {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) {
        showToast('请先登录', 'error');
        goTo('login.html');
        return;
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/job/my?studentId=${studentId}`);
        const myJobs = await res.json();
        const container = document.getElementById('myJobsList');
        if (!container) return;
        if (!myJobs || myJobs.length === 0) {
            container.innerHTML = '<div class="empty-state" style="padding:40px;">暂无发布</div>';
        } else {
            container.innerHTML = myJobs.map(job => `
                <div class="manage-job-item">
                    <div class="manage-job-info">
                        <div class="manage-job-title">${escapeHtml(job.title)}</div>
                        <div class="manage-job-time">${formatRelativeTime(job.createTime)}</div>
                    </div>
                    <div class="manage-job-delete" onclick="deleteJob(${job.id})"><i class="fas fa-trash-alt"></i></div>
                </div>
            `).join('');
        }
        const modal = document.getElementById('manageJobsModal');
        if (modal) modal.style.display = 'flex';
    } catch(e) {
        showToast('加载失败', 'error');
    }
}

async function deleteJob(jobId) {
    const studentId = localStorage.getItem('studentId');
    if (!studentId) return;
    showConfirmDialog('确定删除该兼职吗？', async () => {
        try {
            const formData = new URLSearchParams();
            formData.append('studentId', studentId);
            formData.append('jobId', jobId);
            const res = await fetch(API_BASE_URL + '/api/job/delete', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData
            });
            const data = await res.json();
            if (data.success) {
                showToast('删除成功', 'success');
                loadMyJobs();
                loadJobs();
            } else {
                showToast(data.message || '删除失败', 'error');
            }
        } catch(e) {
            showToast('网络错误', 'error');
        }
    });
}

function openPublishModal() {
    const modal = document.getElementById('publishJobModal');
    if (modal) modal.style.display = 'flex';
}

function closePublishModal() {
    const modal = document.getElementById('publishJobModal');
    if (modal) modal.style.display = 'none';
    const inputs = ['jobTitle', 'jobWorkTime', 'jobLocation', 'jobPeopleCount', 'jobRequirements', 'jobDescription'];
    inputs.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
}

function closeManageModal() {
    const modal = document.getElementById('manageJobsModal');
    if (modal) modal.style.display = 'none';
}

function closeDetailModal() {
    const modal = document.getElementById('jobDetailModal');
    if (modal) modal.style.display = 'none';
}

function bindJobEvents() {
    const menuBtn = document.getElementById('jobMenuBtn');
    const dropdown = document.getElementById('jobMenuDropdown');
    if (menuBtn && dropdown) {
        menuBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            dropdown.style.display = dropdown.style.display === 'none' ? 'block' : 'none';
        });
        document.addEventListener('click', () => {
            if (dropdown) dropdown.style.display = 'none';
        });
        dropdown.addEventListener('click', (e) => e.stopPropagation());
    }
    const publishBtn = document.getElementById('publishJobBtn');
    if (publishBtn) publishBtn.addEventListener('click', openPublishModal);
    const manageBtn = document.getElementById('manageMyJobsBtn');
    if (manageBtn) manageBtn.addEventListener('click', () => {
        if (dropdown) dropdown.style.display = 'none';
        loadMyJobs();
    });
    const closePublish = document.getElementById('closePublishBtn');
    if (closePublish) closePublish.addEventListener('click', closePublishModal);
    const cancelBtn = document.querySelector('.job-publish-cancel');
    if (cancelBtn) cancelBtn.addEventListener('click', closePublishModal);
    const submitBtn = document.querySelector('.job-publish-submit');
    if (submitBtn) submitBtn.addEventListener('click', publishJob);
    const closeManage = document.getElementById('closeManageBtn');
    if (closeManage) closeManage.addEventListener('click', closeManageModal);
    const closeDetail = document.getElementById('closeDetailBtn');
    if (closeDetail) closeDetail.addEventListener('click', closeDetailModal);
    document.querySelectorAll('.job-detail-overlay, .job-publish-overlay, .job-manage-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) {
                closeDetailModal();
                closePublishModal();
                closeManageModal();
            }
        });
    });
}

// ========== 页面加载完成后执行全局初始化 ==========
document.addEventListener('DOMContentLoaded', function() {
    initTheme();
    initPageCommon();
    updateMsgBadge();
});

function bindCompassAnimation() {
    const compassItems = document.querySelectorAll('.tabbar-item[onclick*="help.html"]');
    compassItems.forEach(item => {
        const originalClick = item.getAttribute('onclick');
        if (!originalClick) return;
        item.removeAttribute('onclick');
        item.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const svgIcon = item.querySelector('.compass-icon');
            if (svgIcon && !svgIcon.classList.contains('compass-animate')) {
                svgIcon.classList.add('compass-animate');
                svgIcon.addEventListener('animationend', () => {
                    svgIcon.classList.remove('compass-animate');
                    if (originalClick) {
                        const urlMatch = originalClick.match(/goTo\(['"]([^'"]+)['"]\)/);
                        if (urlMatch && urlMatch[1]) {
                            window.location.href = urlMatch[1];
                        }
                    }
                }, { once: true });
            } else {
                if (originalClick) {
                    const urlMatch = originalClick.match(/goTo\(['"]([^'"]+)['"]\)/);
                    if (urlMatch && urlMatch[1]) window.location.href = urlMatch[1];
                }
            }
        });
    });
}

document.addEventListener('DOMContentLoaded', bindCompassAnimation);