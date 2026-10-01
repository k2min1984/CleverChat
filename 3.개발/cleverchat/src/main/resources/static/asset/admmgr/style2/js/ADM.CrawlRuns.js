(function () {
    var root = document.querySelector('[data-crawl-runs-root]');
    if (!root) return;

    var status = document.getElementById('crawlRunStatus');

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

    root.querySelectorAll('[data-review-run-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-review-run-id');
            setStatus('실패 실행 이력 #' + id + ' 검토 처리 중입니다...');
            button.disabled = true;
            CleverChat.fetch('/admin/api/crawl-runs/' + encodeURIComponent(id) + '/review', {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify({ comment: 'Reviewed from crawl run list.' })
            }).then(parseJson).then(function () {
                setStatus('실패 실행 이력을 검토 처리했습니다.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
                button.disabled = false;
            });
        });
    });

    var retentionForm = document.getElementById('crawlRetentionForm');
    if (retentionForm) {
        retentionForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var params = new URLSearchParams();
            params.set('runRetentionDays', retentionForm.querySelector('[name="runRetentionDays"]').value || '365');
            params.set('documentRetentionDays', retentionForm.querySelector('[name="documentRetentionDays"]').value || '90');
            params.set('dryRun', String(retentionForm.querySelector('[name="dryRun"]').checked));
            setStatus('만료된 크롤링 운영 데이터를 확인하는 중입니다...');
            CleverChat.fetch('/admin/api/crawl-runs/expired?' + params.toString(), {
                method: 'DELETE',
                credentials: 'same-origin',
                headers: csrfHeaders({ 'Accept': 'application/json' })
            }).then(parseJson).then(function (data) {
                setStatus('실행 로그: ' + data.deletedRunLogs + '/' + data.wouldDeleteRunLogs
                    + ', 수집 결과: ' + data.deletedDocuments + '/' + data.wouldDeleteDocuments + '.');
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }
})();
