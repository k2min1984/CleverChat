(function () {
    'use strict';
    var root = document.querySelector('[data-settings-page]');
    if (!root) return;
    var tabs = Array.from(root.querySelectorAll('[role="tab"]'));
    var dirty = false;
    var dirtyForms = new Set();
    var submitting = false;
    function select(hash) {
        var selected = tabs.find(function (tab) { return tab.hash === hash; }) || tabs[0];
        tabs.forEach(function (tab) {
            var active = tab === selected;
            tab.setAttribute('aria-selected', String(active));
            tab.tabIndex = active ? 0 : -1;
            document.getElementById(tab.getAttribute('aria-controls')).hidden = !active;
        });
    }
    tabs.forEach(function (tab, index) {
        tab.addEventListener('click', function (event) {
            event.preventDefault();
            history.replaceState(null, '', tab.hash);
            select(tab.hash);
        });
        tab.addEventListener('keydown', function (event) {
            var next = event.key === 'ArrowRight' ? (index + 1) % tabs.length : event.key === 'ArrowLeft' ? (index + tabs.length - 1) % tabs.length : event.key === 'Home' ? 0 : event.key === 'End' ? tabs.length - 1 : null;
            if (next !== null) { event.preventDefault(); tabs[next].focus(); tabs[next].click(); }
        });
    });
    root.addEventListener('input', function (event) {
        var form = event.target.closest('form');
        if (form) { dirty = true; dirtyForms.add(form); }
    });
    root.addEventListener('submit', function (event) {
        var otherEdits = Array.from(dirtyForms).some(function (form) { return form !== event.target; });
        if (otherEdits && !window.confirm('다른 항목에 저장하지 않은 변경이 있습니다. 현재 항목만 저장하고 다른 변경은 취소할까요?')) {
            event.preventDefault(); return;
        }
        submitting = true;
    });
    window.addEventListener('beforeunload', function (event) {
        if (dirty && !submitting) { event.preventDefault(); event.returnValue = ''; }
    });
    window.addEventListener('hashchange', function () { select(location.hash); });
    select(location.hash || '#' + root.dataset.activeTab);
})();
