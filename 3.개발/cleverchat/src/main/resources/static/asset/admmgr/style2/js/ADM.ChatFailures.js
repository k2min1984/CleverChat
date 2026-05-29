(function () {
    var root = document.querySelector('[data-chat-failure-root]');
    if (!root) return;

    var status = document.getElementById('failureStatus');

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

    root.querySelectorAll('.failure-review-form').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            var id = form.getAttribute('data-failure-id');
            var input = form.querySelector('input[name="comment"]');
            var button = form.querySelector('button[type="submit"]');
            button.disabled = true;
            setStatus('Saving review...');
            fetch('/admin/api/chat/failures/' + encodeURIComponent(id) + '/review', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify({ comment: input ? input.value : '' })
            }).then(function (response) {
                ADM.updateCsrfMetaValue(response.headers.get('X-CSRF-Token'), response.headers.get('X-CSRF-FormId'));
                return response.json().catch(function () { return null; }).then(function (body) {
                    if (!response.ok || !body || body.success === false) {
                        throw new Error(body && body.error ? body.error.message : 'Review failed.');
                    }
                    var row = form.closest('tr');
                    if (row) {
                        row.querySelectorAll('td')[6].textContent = 'Reviewed';
                        var cell = row.querySelectorAll('td')[7];
                        cell.textContent = input && input.value ? input.value : '-';
                    }
                    setStatus('Review saved.');
                });
            }).catch(function (error) {
                button.disabled = false;
                setStatus(error.message);
            });
        });
    });
})();
