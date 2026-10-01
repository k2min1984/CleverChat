(function () {
    var root = document.querySelector('[data-admin-manage-root]');
    if (!root) return;

    var status = document.getElementById('manageStatus');

    function setStatus(message) {
        if (status) status.textContent = message || '';
    }

    function csrfHeaders(headers) {
        headers = headers || {};
        var token = document.querySelector('meta[name="csrfToken"]');
        var formId = document.querySelector('meta[name="csrfFormId"]');
        if (token && token.content) headers['X-CSRF-Token'] = token.content;
        if (formId && formId.content) headers['X-CSRF-FormId'] = formId.content;
        return headers;
    }

    function parseJson(response) {
        if (window.ADM && typeof ADM.updateCsrfMetaValue === 'function') {
            ADM.updateCsrfMetaValue(response.headers.get('X-CSRF-Token'), response.headers.get('X-CSRF-FormId'));
        }
        return response.json().catch(function () { return null; }).then(function (body) {
            if (!response.ok || !body || body.success === false) {
                throw new Error(body && body.error ? body.error.message : 'Request failed.');
            }
            return body.data;
        });
    }

    function nullableNumber(value) {
        return value === null || value === undefined || value === '' ? null : Number(value);
    }

    function nullableString(value) {
        return value === null || value === undefined || value.trim() === '' ? null : value.trim();
    }

    function payloadFromCode(scope) {
        return {
            parentId: nullableNumber(scope.querySelector('[name="parentId"]').value),
            code: scope.querySelector('[name="code"]').value,
            name: scope.querySelector('[name="name"]').value,
            value: nullableString(scope.querySelector('[name="value"]').value),
            description: nullableString(scope.querySelector('[name="description"]') ? scope.querySelector('[name="description"]').value : null),
            sortOrder: Number(scope.querySelector('[name="sortOrder"]').value || 100),
            enabled: scope.querySelector('[name="enabled"]').checked
        };
    }

    function payloadFromMenu(scope) {
        return {
            parentId: nullableNumber(scope.querySelector('[name="parentId"]').value),
            menuKey: scope.querySelector('[name="menuKey"]') ? nullableString(scope.querySelector('[name="menuKey"]').value) : null,
            title: scope.querySelector('[name="title"]').value,
            url: nullableString(scope.querySelector('[name="url"]').value),
            sortOrder: Number(scope.querySelector('[name="sortOrder"]').value || 100),
            enabled: scope.querySelector('[name="enabled"]').checked,
            visible: scope.querySelector('[name="visible"]').checked
        };
    }

    function request(url, method, payload) {
        setStatus('Saving...');
        return CleverChat.fetch(url, {
            method: method,
            credentials: 'same-origin',
            headers: csrfHeaders({
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }),
            body: payload === undefined ? undefined : JSON.stringify(payload)
        }).then(parseJson).then(function (data) {
            setStatus('Saved.');
            window.location.reload();
            return data;
        }).catch(function (error) {
            setStatus(error.message);
        });
    }

    var codeCreateForm = document.getElementById('codeCreateForm');
    if (codeCreateForm) {
        codeCreateForm.addEventListener('submit', function (event) {
            event.preventDefault();
            request('/admin/api/manage/codes', 'POST', payloadFromCode(codeCreateForm));
        });
    }

    root.querySelectorAll('[data-create-code]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            request('/admin/api/manage/codes', 'POST', payloadFromCode(form));
        });
    });

    root.querySelectorAll('[data-save-code-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-save-code-id');
            request('/admin/api/manage/codes/' + encodeURIComponent(id), 'PUT', payloadFromCode(button.closest('tr')));
        });
    });

    root.querySelectorAll('[data-disable-code-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-disable-code-id');
            request('/admin/api/manage/codes/' + encodeURIComponent(id), 'DELETE');
        });
    });

    var menuCreateForm = document.getElementById('menuCreateForm');
    if (menuCreateForm) {
        menuCreateForm.addEventListener('submit', function (event) {
            event.preventDefault();
            request('/admin/api/manage/menus', 'POST', payloadFromMenu(menuCreateForm));
        });
    }

    root.querySelectorAll('[data-create-menu]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            request('/admin/api/manage/menus', 'POST', payloadFromMenu(form));
        });
    });

    root.querySelectorAll('[data-save-menu-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-save-menu-id');
            request('/admin/api/manage/menus/' + encodeURIComponent(id), 'PUT', payloadFromMenu(button.closest('tr')));
        });
    });

    root.querySelectorAll('[data-disable-menu-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-disable-menu-id');
            request('/admin/api/manage/menus/' + encodeURIComponent(id), 'DELETE');
        });
    });

    var permissionForm = document.getElementById('permissionForm');
    if (permissionForm) {
        permissionForm.querySelectorAll('[name="menuIds"]').forEach(function (checkbox) {
            checkbox.addEventListener('change', function () {
                var row = checkbox.closest('.mtg-row');
                if (!row) return;
                row.querySelectorAll('.perm-check').forEach(function (permCheck) {
                    permCheck.checked = checkbox.checked;
                });
            });
        });

        permissionForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var roleCode = root.getAttribute('data-role-code');
            var ids = Array.prototype.slice.call(permissionForm.querySelectorAll('[name="menuIds"]:checked'))
                .map(function (checkbox) { return Number(checkbox.value); });
            request('/admin/api/manage/permissions/' + encodeURIComponent(roleCode), 'PUT', { menuIds: ids });
        });
    }
})();
