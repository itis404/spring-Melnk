const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
const savedTheme = localStorage.getItem('studymarket-theme') || 'light';

document.documentElement.dataset.theme = savedTheme;

function csrfHeaders() {
    return csrfToken && csrfHeader ? {[csrfHeader]: csrfToken} : {};
}

function showToast(message, type = 'success') {
    const stack = document.querySelector('.flash-stack') || document.body.appendChild(Object.assign(document.createElement('section'), {
        className: 'flash-stack'
    }));
    const toast = document.createElement('div');
    toast.className = `flash ${type}`;
    toast.textContent = message;
    stack.appendChild(toast);
    window.setTimeout(() => toast.remove(), 3600);
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.reveal').forEach((element) => observer.observe(element));
    wireThemeToggle();
    wireFavoriteButtons();
    wireAjaxChat();
    wireSubmitOnce();
});

const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
        if (entry.isIntersecting) {
            entry.target.classList.add('is-visible');
            observer.unobserve(entry.target);
        }
    });
}, {threshold: 0.12});

function wireFavoriteButtons() {
    document.querySelectorAll('.favorite-button').forEach((button) => {
        button.addEventListener('click', async () => {
            const productId = button.dataset.productId;
            button.disabled = true;
            try {
                const response = await fetch(`/ajax/products/${productId}/favorite`, {
                    method: 'POST',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        ...csrfHeaders()
                    }
                });
                const payload = await response.json();
                if (!response.ok) {
                    throw new Error(payload.message || 'Не удалось обновить избранное');
                }
                button.classList.toggle('active', payload.active);
                button.querySelector('.favorite-count').textContent = payload.count;
            } catch (error) {
                showToast(error.message, 'danger');
            } finally {
                button.disabled = false;
            }
        });
    });
}

function wireThemeToggle() {
    document.querySelectorAll('[data-theme-toggle]').forEach((button) => {
        syncThemeButton(button);
        button.addEventListener('click', () => {
            const nextTheme = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.theme = nextTheme;
            localStorage.setItem('studymarket-theme', nextTheme);
            document.querySelectorAll('[data-theme-toggle]').forEach(syncThemeButton);
        });
    });
}

function syncThemeButton(button) {
    const dark = document.documentElement.dataset.theme === 'dark';
    button.querySelector('span').textContent = dark ? '☀' : '☾';
    button.title = dark ? 'Включить светлую тему' : 'Включить темную тему';
    button.setAttribute('aria-label', button.title);
}

function wireAjaxChat() {
    document.querySelectorAll('.ajax-chat-form').forEach((form) => {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const button = form.querySelector('button[type="submit"]');
            const textarea = form.querySelector('textarea');
            button.disabled = true;
            try {
                const response = await fetch(form.action, {
                    method: 'POST',
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest',
                        'Content-Type': 'application/x-www-form-urlencoded',
                        ...csrfHeaders()
                    },
                    body: new URLSearchParams(new FormData(form))
                });
                const payload = await response.json();
                if (!response.ok) {
                    throw new Error(payload.message || 'Сообщение не отправлено');
                }
                appendMessage(payload);
                textarea.value = '';
                showToast('Сообщение отправлено');
            } catch (error) {
                showToast(error.message, 'danger');
            } finally {
                button.disabled = false;
            }
        });
    });
}

function appendMessage(payload) {
    const list = document.querySelector('[data-message-list]');
    if (!list) {
        return;
    }
    const message = document.createElement('article');
    const initial = payload.sender.trim().charAt(0).toUpperCase() || 'Я';
    message.className = 'message own is-visible';
    message.innerHTML = `<span class="message-avatar"><span></span></span><div><strong></strong><p></p><small></small></div>`;
    message.querySelector('.message-avatar span').textContent = initial;
    message.querySelector('strong').textContent = payload.sender;
    message.querySelector('p').textContent = payload.body;
    message.querySelector('small').textContent = `${payload.sentAt} · отправлено`;
    list.appendChild(message);
    message.scrollIntoView({behavior: 'smooth', block: 'nearest'});
}

function wireSubmitOnce() {
    document.querySelectorAll('form[data-submit-once]').forEach((form) => {
        form.addEventListener('submit', () => {
            const button = form.querySelector('button[type="submit"]');
            if (button) {
                button.disabled = true;
                button.dataset.originalText = button.textContent;
                button.textContent = 'Отправляем...';
            }
        });
    });
}
