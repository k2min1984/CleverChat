(function () {
    'use strict';
    var root = document.querySelector('[data-crawl-detail]');
    if (!root) return;
    var tabs = Array.from(root.querySelectorAll('[role="tab"]'));
    var status = root.querySelector('[data-refresh-status]');
    var refreshing = false;
    var timer;
    function selectTab(hash) {
        var panelId = hash.replace('#', '');
        if (panelId === 'crawl-runs' || panelId === 'crawl-coverages') panelId = 'crawl-jobs';
        if (!tabs.some(function (tab) { return tab.getAttribute('aria-controls') === panelId; })) panelId = 'crawl-documents';
        tabs.forEach(function (tab) {
            var selected = tab.getAttribute('aria-controls') === panelId;
            tab.setAttribute('aria-selected', String(selected));
            tab.tabIndex = selected ? 0 : -1;
            document.getElementById(tab.getAttribute('aria-controls')).hidden = !selected;
        });
        if (hash === '#crawl-runs' || hash === '#crawl-coverages') document.getElementById(hash.substring(1)).open = true;
    }
    tabs.forEach(function (tab, index) {
        tab.addEventListener('click', function (event) {
            event.preventDefault();
            history.replaceState(null, '', tab.getAttribute('href'));
            selectTab(tab.getAttribute('href'));
        });
        tab.addEventListener('keydown', function (event) {
            var next;
            if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
            if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
            if (event.key === 'Home') next = 0;
            if (event.key === 'End') next = tabs.length - 1;
            if (next !== undefined) { event.preventDefault(); tabs[next].focus(); tabs[next].click(); }
        });
    });
    window.addEventListener('hashchange', function () { selectTab(location.hash); });
    selectTab(location.hash);
    function schedule() {
        clearTimeout(timer);
        if (root.dataset.active === 'true') timer = setTimeout(function () {
            if (document.hidden || root.querySelector('input:focus')) { schedule(); return; }
            refresh();
        }, 5000);
    }
    function refresh() {
        if (refreshing) return;
        refreshing = true;
        var button = root.querySelector('[data-refresh-crawl]');
        button.disabled = true;
        CleverChat.fetch(location.pathname + location.search, {credentials: 'same-origin', cache: 'no-store', headers: {'Accept': 'text/html'}})
            .then(function (response) { if (!response.ok) throw new Error(); return response.text(); })
            .then(function (html) {
                var next = new DOMParser().parseFromString(html, 'text/html');
                var detail = next.querySelector('[data-crawl-detail]');
                if (!detail) { clearTimeout(timer); root.dataset.active = 'false'; throw new Error(); }
                var openDetails = Array.from(root.querySelectorAll('details[open][data-detail-key]')).map(function (item) { return item.dataset.detailKey; });
                ['crawl-summary', 'crawl-documents', 'crawl-jobs', 'crawl-settings'].forEach(function (id) {
                    var current = document.getElementById(id);
                    var replacement = next.getElementById(id);
                    if (current && replacement) current.innerHTML = replacement.innerHTML;
                });
                root.querySelectorAll('details[data-detail-key]').forEach(function (item) { item.open = openDetails.indexOf(item.dataset.detailKey) !== -1; });
                root.dataset.active = detail.dataset.active;
                var run = root.querySelector('[data-crawl-run]');
                var nextRun = detail.querySelector('[data-crawl-run]');
                if (run && nextRun) { run.disabled = nextRun.disabled; run.textContent = nextRun.textContent; }
                status.textContent = root.dataset.active === 'true' ? '수집 중 · 5초마다 자동 갱신' : '최신 상태로 갱신했습니다.';
            }).catch(function () { status.textContent = '상태를 불러오지 못했습니다. 연결 또는 로그인 상태를 확인하세요.'; })
            .finally(function () { refreshing = false; button.disabled = false; schedule(); });
    }
    root.querySelector('[data-refresh-crawl]').addEventListener('click', refresh);
    schedule();
})();
