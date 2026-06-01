(function () {
    var root = document.querySelector('[data-notices-root]');
    if (!root) return;

    var status = document.getElementById('noticeStatus');

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

    function normalizeDateTime(value) {
        return value ? value + ':00+09:00' : null;
    }

    function payloadFrom(scope) {
        return {
            title: scope.querySelector('[name="title"]').value,
            content: scope.querySelector('[name="content"]').value,
            useYn: scope.querySelector('[name="enabled"]').checked ? 'Y' : 'N',
            startsAt: normalizeDateTime(scope.querySelector('[name="startsAt"]').value),
            endsAt: normalizeDateTime(scope.querySelector('[name="endsAt"]').value),
            priority: Number(scope.querySelector('[name="priority"]').value || 100)
        };
    }

    var createForm = document.getElementById('noticeCreateForm');
    if (createForm) {
        createForm.addEventListener('submit', function (event) {
            event.preventDefault();
            setStatus('Creating notice...');
            fetch('/admin/api/notices', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify(payloadFrom(createForm))
            }).then(parseJson).then(function () {
                setStatus('Notice created.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }

    root.querySelectorAll('[data-save-notice-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-save-notice-id');
            var row = button.closest('tr');
            setStatus('Saving notice #' + id + '...');
            fetch('/admin/api/notices/' + encodeURIComponent(id), {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify(payloadFrom(row))
            }).then(parseJson).then(function () {
                setStatus('Notice saved.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    });

    root.querySelectorAll('[data-disable-notice-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-disable-notice-id');
            setStatus('Disabling notice #' + id + '...');
            fetch('/admin/api/notices/' + encodeURIComponent(id), {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: csrfHeaders({ 'Accept': 'application/json' })
            }).then(parseJson).then(function () {
                setStatus('Notice disabled.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    });
})();
