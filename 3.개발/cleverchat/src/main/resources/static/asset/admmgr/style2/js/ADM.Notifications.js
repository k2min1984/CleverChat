(function () {
    var root = document.querySelector('[data-notifications-root]');
    if (!root) return;

    var status = document.getElementById('notificationStatus');

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

    function payloadFrom(scope) {
        return {
            name: scope.querySelector('[name="name"]').value,
            type: scope.querySelector('[name="type"]').value,
            useYn: scope.querySelector('[name="enabled"]').checked ? 'Y' : 'N',
            endpointEnvKey: scope.querySelector('[name="endpointEnvKey"]').value,
            previousEndpointEnvKey: scope.querySelector('[name="previousEndpointEnvKey"]').value || null,
            rateLimitPerHour: Number(scope.querySelector('[name="rateLimitPerHour"]').value || 60)
        };
    }

    var createForm = document.getElementById('notificationCreateForm');
    if (createForm) {
        createForm.addEventListener('submit', function (event) {
            event.preventDefault();
            setStatus('Creating notification channel...');
            fetch('/admin/api/notifications/channels', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify(payloadFrom(createForm))
            }).then(parseJson).then(function () {
                setStatus('Notification channel created.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }

    root.querySelectorAll('[data-save-channel-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-save-channel-id');
            var row = button.closest('tr');
            setStatus('Saving notification channel #' + id + '...');
            fetch('/admin/api/notifications/channels/' + encodeURIComponent(id), {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify(payloadFrom(row))
            }).then(parseJson).then(function () {
                setStatus('Notification channel saved.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    });

    root.querySelectorAll('[data-test-channel-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-test-channel-id');
            setStatus('Sending test notification for channel #' + id + '...');
            fetch('/admin/api/notifications/channels/' + encodeURIComponent(id) + '/test', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({ 'Accept': 'application/json' })
            }).then(parseJson).then(function (data) {
                setStatus(data.sent ? 'Test notification sent.' : 'Test notification failed: ' + data.message);
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    });

    root.querySelectorAll('[data-review-event-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-review-event-id');
            setStatus('Reviewing notification event #' + id + '...');
            fetch('/admin/api/notifications/events/' + encodeURIComponent(id) + '/review', {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders({ 'Accept': 'application/json' })
            }).then(parseJson).then(function () {
                setStatus('Notification event reviewed.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    });
})();
