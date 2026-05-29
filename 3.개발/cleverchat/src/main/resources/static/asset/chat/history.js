(function () {
    var root = document.querySelector('[data-history-root]');
    if (!root) return;

    var statusLine = document.getElementById('historyStatus');
    var errorLine = document.getElementById('historyError');
    var historyList = document.getElementById('historyList');

    function setStatus(text) {
        statusLine.textContent = text || '';
        errorLine.hidden = true;
        errorLine.textContent = '';
    }

    function setError(text) {
        statusLine.textContent = '';
        errorLine.textContent = text || 'History could not be loaded.';
        errorLine.hidden = false;
    }

    function api(path) {
        return fetch(path, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        }).then(function (response) {
            return response.json().catch(function () {
                return null;
            }).then(function (body) {
                if (!response.ok || !body || body.success === false) {
                    var message = body && body.error ? body.error.message : 'History could not be loaded.';
                    throw new Error(message);
                }
                return body.data;
            });
        });
    }

    function render(items) {
        historyList.innerHTML = '';
        if (!items || items.length === 0) {
            historyList.textContent = 'No chat history from the last 90 days.';
            setStatus('');
            return;
        }
        items.forEach(function (item) {
            var card = document.createElement('article');
            card.className = 'history-item';
            card.setAttribute('aria-label', 'Chat history for ' + item.scenarioTitle);

            var title = document.createElement('h2');
            title.textContent = item.scenarioTitle || 'Scenario';
            card.appendChild(title);

            var meta = document.createElement('p');
            meta.textContent = item.state + ' · ' + item.messageCount + ' messages';
            card.appendChild(meta);

            var preview = document.createElement('p');
            preview.textContent = item.lastMessage || 'No message preview.';
            card.appendChild(preview);

            var link = document.createElement('a');
            link.className = 'button-link secondary';
            link.href = '/chat/api/sessions/' + item.sessionId + '/history';
            link.textContent = 'API history';
            link.setAttribute('aria-label', 'Open API history for ' + (item.scenarioTitle || 'scenario'));
            card.appendChild(link);

            historyList.appendChild(card);
        });
        setStatus(items.length + ' history items loaded.');
    }

    api('/chat/api/history').then(render).catch(function (error) {
        setError(error.message);
    });
})();
