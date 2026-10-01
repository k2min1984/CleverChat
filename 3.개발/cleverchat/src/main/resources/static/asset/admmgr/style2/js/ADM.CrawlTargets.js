(function () {
    var root = document.querySelector('[data-crawl-targets-root]');
    if (!root) return;

    var status = document.getElementById('crawlStatus');

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

    var form = document.getElementById('crawlTargetForm');

    function schedulePayload(scheduleForm) {
        return {
            scheduleEnabled: scheduleForm.querySelector('[name="scheduleEnabled"]').checked,
            scheduleIntervalMinutes: Number(scheduleForm.querySelector('[name="scheduleIntervalMinutes"]').value || 1440),
            scheduleMode: scheduleForm.querySelector('[name="scheduleMode"]').value,
            scheduleCron: scheduleForm.querySelector('[name="scheduleCron"]').value || null
        };
    }

    function renderPreview(scheduleForm, data) {
        var output = scheduleForm.querySelector('[data-schedule-preview]');
        if (!output) return;
        if (!data || !data.nextRunTimes || data.nextRunTimes.length === 0) {
            output.textContent = '예약된 자동 실행이 없습니다.';
            return;
        }
        output.textContent = '다음 실행: ' + data.nextRunTimes.slice(0, 5).map(function (value) {
            return String(value).replace('T', ' ').slice(0, 16);
        }).join(', ');
    }

    function previewSchedule(scheduleForm) {
        var output = scheduleForm.querySelector('[data-schedule-preview]');
        if (output) output.textContent = '스케줄을 확인하는 중입니다...';
        CleverChat.fetch('/admin/api/crawl-targets/schedule/preview', {
            method: 'POST',
            credentials: 'same-origin',
            headers: csrfHeaders({
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }),
            body: JSON.stringify(schedulePayload(scheduleForm))
        }).then(parseJson).then(function (data) {
            renderPreview(scheduleForm, data);
        }).catch(function (error) {
            if (output) output.textContent = error.message;
            setStatus(error.message);
        });
    }

    if (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            var payload = Object.assign({
                url: form.querySelector('[name="url"]').value,
                label: form.querySelector('[name="label"]').value,
                useYn: form.querySelector('[name="useYn"]').checked ? 'Y' : 'N'
            }, schedulePayload(form));
            setStatus('크롤링 대상을 등록하는 중입니다...');
            CleverChat.fetch('/admin/api/crawl-targets', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify(payload)
            }).then(parseJson).then(function () {
                setStatus('크롤링 대상이 등록되었습니다.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    }

    root.querySelectorAll('[data-preview-schedule]').forEach(function (button) {
        button.addEventListener('click', function () {
            var scheduleForm = button.closest('form');
            if (scheduleForm) previewSchedule(scheduleForm);
        });
    });

    root.querySelectorAll('[data-run-target-id]').forEach(function (button) {
        button.addEventListener('click', function () {
            var id = button.getAttribute('data-run-target-id');
            setStatus('크롤링 대상 #' + id + ' 실행 중입니다...');
            button.disabled = true;
            CleverChat.fetch('/admin/api/crawl-targets/' + encodeURIComponent(id) + '/run', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeaders({ 'Accept': 'application/json' })
            }).then(parseJson).then(function (data) {
                setStatus('실행이 완료되었습니다: ' + data.run.status + '.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
                button.disabled = false;
            });
        });
    });

    root.querySelectorAll('[data-schedule-target-id]').forEach(function (scheduleForm) {
        scheduleForm.addEventListener('submit', function (event) {
            event.preventDefault();
            var id = scheduleForm.getAttribute('data-schedule-target-id');
            var payload = schedulePayload(scheduleForm);
            setStatus('크롤링 대상 #' + id + ' 스케줄 저장 중입니다...');
            CleverChat.fetch('/admin/api/crawl-targets/' + encodeURIComponent(id) + '/schedule', {
                method: 'PUT',
                credentials: 'same-origin',
                headers: csrfHeaders({
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                }),
                body: JSON.stringify(payload)
            }).then(parseJson).then(function () {
                setStatus('스케줄이 저장되었습니다.');
                window.location.reload();
            }).catch(function (error) {
                setStatus(error.message);
            });
        });
    });
})();
