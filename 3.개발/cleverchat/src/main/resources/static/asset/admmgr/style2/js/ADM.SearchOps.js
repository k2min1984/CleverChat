(function () {
    var root = document.querySelector('[data-search-root]');
    if (!root) return;

    var status = document.getElementById('searchStatus');
    var results = document.getElementById('searchResults');

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

    var testForm = document.getElementById('searchTestForm');
    if (testForm) {
        testForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var query = testForm.querySelector('[name="query"]').value;
            var limit = Number(testForm.querySelector('[name="limit"]').value || 5);
            setStatus('Running search...');
            if (results) results.textContent = '';
            fetch('/admin/api/search/test', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify({ query: query, limit: limit })
            }).then(parseJson).then(function (data) {
                setStatus('Search completed. ' + data.resultCount + ' result(s).');
                if (!results) return;
                data.results.forEach(function (item) {
                    var li = document.createElement('li');
                    li.textContent = item.scenarioTitle + ' [' + item.matchedField + ']';
                    results.appendChild(li);
                });
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }

    var rebuildForm = document.getElementById('popularRebuildForm');
    if (rebuildForm) {
        rebuildForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var statDate = rebuildForm.querySelector('[name="statDate"]').value;
            setStatus('Rebuilding popular searches...');
            fetch('/admin/api/search/popular/rebuild', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify({ statDate: statDate })
            }).then(parseJson).then(function (data) {
                setStatus('Rebuilt ' + data.rebuiltCount + ' row(s).');
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }

    var expiredForm = document.getElementById('expiredDeleteForm');
    if (expiredForm) {
        expiredForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var retentionDays = expiredForm.querySelector('[name="retentionDays"]').value || '90';
            var dryRun = expiredForm.querySelector('[name="dryRun"]').checked;
            var url = '/admin/api/search/logs/expired?retentionDays=' + encodeURIComponent(retentionDays)
                + '&dryRun=' + encodeURIComponent(String(dryRun));
            setStatus(dryRun ? 'Checking expired logs...' : 'Deleting expired logs...');
            fetch(url, {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: csrfHeaders({ 'Accept': 'application/json' })
            }).then(parseJson).then(function (data) {
                setStatus('Search logs: ' + data.deletedSearchLogs + '/' + data.wouldDeleteSearchLogs
                    + ', block logs: ' + data.deletedBlockLogs + '/' + data.wouldDeleteBlockLogs + '.');
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }
})();
