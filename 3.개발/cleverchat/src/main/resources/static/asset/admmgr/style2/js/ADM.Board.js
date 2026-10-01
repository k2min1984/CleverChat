(function () {
    'use strict';
    var root = document.querySelector('.board-page');
    if (!root) return;

    root.querySelectorAll('[data-board-open]').forEach(function (button) {
        var panel = document.getElementById(button.dataset.boardOpen);
        if (!panel) return;
        panel.classList.add('board-create-collapse');
        button.hidden = false;
        button.addEventListener('click', function () {
            panel.open = !panel.open;
            if (panel.open) {
                var field = panel.querySelector('input:not([type=hidden]), select, textarea');
                if (field) field.focus();
            }
        });
        panel.addEventListener('toggle', function () { button.setAttribute('aria-expanded', String(panel.open)); });
    });

    root.querySelectorAll('[data-board-pagination]').forEach(function (table) {
        var rows = Array.from(table.tBodies[0].rows).filter(function (row) { return !row.classList.contains('td-empty'); });
        var page = 1;
        var pageSize = 20;
        var totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
        var footer = document.createElement('div');
        footer.className = 'table-footer board-table-footer';
        var count = document.createElement('span');
        count.className = 'board-page-count';
        count.setAttribute('role', 'status');
        count.setAttribute('aria-live', 'polite');
        var nav = document.createElement('nav');
        nav.className = 'board-pagination';
        nav.setAttribute('aria-label', (table.caption ? table.caption.textContent : '목록') + ' 페이지');
        footer.append(count, nav);
        table.closest('.table-card').appendChild(footer);

        function button(label, target, disabled, current) {
            var item = document.createElement('button');
            item.type = 'button';
            item.className = 'page-btn' + (current ? ' active' : '');
            item.textContent = label;
            item.disabled = disabled;
            if (current) item.setAttribute('aria-current', 'page');
            item.addEventListener('click', function () {
                page = target;
                render();
                var active = nav.querySelector('[aria-current="page"]');
                if (active) active.focus({ preventScroll: true });
                table.closest('.table-card').scrollIntoView({ block: 'start', behavior: 'auto' });
            });
            nav.appendChild(item);
        }
        function render() {
            rows.forEach(function (row, index) { row.hidden = index < (page - 1) * pageSize || index >= page * pageSize; });
            count.textContent = rows.length ? '조회된 ' + rows.length + '건 중 ' + ((page - 1) * pageSize + 1) + '–' + Math.min(page * pageSize, rows.length) + '건' : '조회된 항목이 없습니다.';
            nav.replaceChildren();
            nav.hidden = totalPages < 2;
            if (totalPages < 2) return;
            button('이전', page - 1, page === 1, false);
            var first = Math.max(1, Math.min(page - 2, totalPages - 4));
            for (var n = first; n <= Math.min(totalPages, first + 4); n++) button(String(n), n, false, n === page);
            button('다음', page + 1, page === totalPages, false);
        }
        render();
    });

    root.querySelectorAll('.board-editable-table').forEach(function (table) {
        table.querySelectorAll('tbody tr:not(.td-empty)').forEach(function (row) {
            var actions = row.querySelector('.row-actions');
            if (!actions) return;
            var fields = Array.from(row.querySelectorAll('input, select, textarea'));
            var original = fields.map(function (field) { return { value: field.value, checked: field.checked }; });
            fields.forEach(function (field) {
                var value = document.createElement('span');
                value.className = 'board-cell-value';
                if (field.tagName === 'TEXTAREA') value.classList.add('is-long');
                value.textContent = field.type === 'checkbox' ? (field.checked ? '사용' : '미사용') : field.tagName === 'SELECT' ? (field.selectedOptions[0] ? field.selectedOptions[0].text : '-') : (field.value.replace(/(\d{4}-\d{2}-\d{2})T/, '$1 ') || '-');
                if (field.name === 'startsAt' || field.name === 'endsAt') value.textContent = (field.name === 'startsAt' ? '시작 ' : '종료 ') + (field.value ? value.textContent : '제한 없음');
                if (field.tagName === 'TEXTAREA') value.title = field.value;
                if (field.type === 'checkbox') {
                    if (!field.hasAttribute('aria-label')) field.setAttribute('aria-label', '사용 여부');
                    Array.from(field.parentElement.childNodes).forEach(function (node) { if (node.nodeType === Node.TEXT_NODE) node.textContent = ''; });
                }
                field.insertAdjacentElement('afterend', value);
            });
            var edit = document.createElement('button');
            edit.type = 'button';
            edit.className = 'btn btn-xs btn-secondary board-edit-start';
            edit.textContent = '수정';
            edit.addEventListener('click', function () { row.classList.add('is-editing'); if (fields[0]) fields[0].focus(); });
            var cancel = document.createElement('button');
            cancel.type = 'button';
            cancel.className = 'btn btn-xs btn-secondary board-edit-cancel';
            cancel.textContent = '취소';
            cancel.addEventListener('click', function () {
                fields.forEach(function (field, index) { field.value = original[index].value; field.checked = original[index].checked; });
                row.classList.remove('is-editing');
                edit.focus();
            });
            actions.prepend(edit);
            actions.append(cancel);
        });
        table.classList.add('board-enhanced');
    });
})();
