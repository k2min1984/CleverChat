(function () {
    var root = document.querySelector('[data-chat-root]');
    if (!root) return;

    var state = {
        sessionId: null,
        selectedScenarioId: null,
        busy: false
    };

    var statusLine = document.getElementById('statusLine');
    var errorLine = document.getElementById('errorLine');
    var scenarioList = document.getElementById('scenarioList');
    var scenarioCount = document.getElementById('scenarioCount');
    var selectedScenarioName = document.getElementById('selectedScenarioName');
    var sessionStateBadge = document.getElementById('sessionStateBadge');
    var recommendList = document.getElementById('recommendList');
    var messageList = document.getElementById('messageList');
    var optionList = document.getElementById('optionList');
    var form = document.getElementById('chatForm');
    var input = document.getElementById('freeText');

    function setStatus(text) {
        statusLine.textContent = text || '';
        if (errorLine) {
            errorLine.hidden = true;
            errorLine.textContent = '';
        }
    }

    function setError(text) {
        if (errorLine) {
            errorLine.textContent = text || 'The request could not be processed.';
            errorLine.hidden = false;
        }
        statusLine.textContent = '';
    }

    function setBusy(busy) {
        state.busy = busy;
        input.disabled = busy || !state.sessionId;
        form.querySelector('button[type="submit"]').disabled = busy || !state.sessionId;
    }

    function setSessionState(text) {
        if (sessionStateBadge) {
            sessionStateBadge.textContent = text || '대기';
        }
    }

    function selectedScenarioTitle(scenarioId) {
        var selected = scenarioList.querySelector('[data-scenario-id="' + scenarioId + '"] .scenario-title');
        return selected ? selected.textContent : '선택한 상담';
    }

    function markScenarioActive(scenarioId) {
        scenarioList.querySelectorAll('[data-scenario-id]').forEach(function (button) {
            var active = String(scenarioId) === button.getAttribute('data-scenario-id');
            button.classList.toggle('is-active', active);
            button.setAttribute('aria-current', active ? 'true' : 'false');
        });
    }

    function api(path, options) {
        options = options || {};
        options.headers = options.headers || {};
        options.headers.Accept = 'application/json';
        if (options.body) {
            options.headers['Content-Type'] = 'application/json';
        }
        options.credentials = 'same-origin';
        return fetch(path, options).then(function (response) {
            return response.json().catch(function () {
                return null;
            }).then(function (body) {
                if (!response.ok || !body || body.success === false) {
                    var message = body && body.error ? body.error.message : 'The request could not be processed.';
                    throw new Error(message);
                }
                return body.data;
            });
        });
    }

    function renderMessages(messages) {
        messageList.innerHTML = '';
        if (!messages || messages.length === 0) {
            var empty = document.createElement('div');
            empty.className = 'empty-state';
            empty.setAttribute('data-empty-state', '');
            var title = document.createElement('strong');
            title.textContent = '상담 주제를 선택하면 대화가 시작됩니다.';
            var text = document.createElement('span');
            text.textContent = '왼쪽 목록에서 테스트할 주제를 선택해 주세요.';
            empty.appendChild(title);
            empty.appendChild(text);
            messageList.appendChild(empty);
            return;
        }
        messages.forEach(function (message) {
            var row = document.createElement('div');
            row.className = 'message-row ' + (message.direction === 'USER' ? 'user' : 'bot');
            var bubble = document.createElement('p');
            bubble.className = 'message ' + (message.direction === 'USER' ? 'user' : 'bot');
            bubble.textContent = message.content || '';
            bubble.setAttribute('aria-label', (message.direction === 'USER' ? 'User message: ' : 'Chat message: ') + (message.content || ''));
            row.appendChild(bubble);
            messageList.appendChild(row);
            if (message.direction === 'BOT' && message.id) {
                messageList.appendChild(feedbackControls(message.id));
            }
        });
        messageList.scrollTop = messageList.scrollHeight;
    }

    function feedbackControls(messageId) {
        var wrapper = document.createElement('div');
        wrapper.className = 'feedback-controls';
        wrapper.setAttribute('aria-label', 'Rate chat answer');
        [
            { rating: 'UP', label: '도움됨', title: '이 답변이 도움됨', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M7 11v10H4a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2h3Z"/><path d="M7 11l5-8 1.8 1.2a3 3 0 0 1 1.1 3.4L14 11h5.4a2 2 0 0 1 2 2.3l-1 6a2 2 0 0 1-2 1.7H7V11Z"/></svg>' },
            { rating: 'DOWN', label: '도움 안 됨', title: '이 답변이 도움되지 않음', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M7 13V3H4a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2h3Z"/><path d="M7 13l5 8 1.8-1.2a3 3 0 0 0 1.1-3.4L14 13h5.4a2 2 0 0 0 2-2.3l-1-6A2 2 0 0 0 18.4 3H7v10Z"/></svg>' }
        ].forEach(function (item) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'feedback-button';
            button.innerHTML = item.svg;
            button.title = item.label;
            button.setAttribute('aria-label', item.title);
            button.addEventListener('click', function () {
                submitFeedback(messageId, item.rating, null);
            });
            wrapper.appendChild(button);
        });
        return wrapper;
    }

    function renderOptions(options) {
        optionList.innerHTML = '';
        (options || []).forEach(function (option) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'secondary';
            button.textContent = option.label;
            button.setAttribute('role', 'listitem');
            button.setAttribute('aria-label', 'Choose answer: ' + option.label);
            button.addEventListener('click', function () {
                selectOption(option.id);
            });
            optionList.appendChild(button);
        });
    }

    function renderSession(session) {
        state.sessionId = session.sessionId;
        renderMessages(session.messages);
        renderOptions(session.options);
        setSessionState(session.state === 'ACTIVE' ? '진행 중' : '완료');
        setStatus(session.state === 'ACTIVE' ? '상담이 진행 중입니다.' : '상담이 완료되었습니다.');
        setBusy(false);
    }

    function loadScenarios() {
        return api('/chat/api/scenarios').then(function (scenarios) {
            scenarioList.innerHTML = '';
            if (scenarioCount) {
                scenarioCount.textContent = scenarios ? String(scenarios.length) : '0';
            }
            if (!scenarios || scenarios.length === 0) {
                scenarioList.textContent = '상담 주제가 없습니다.';
                return;
            }
            scenarios.forEach(function (scenario) {
                var button = document.createElement('button');
                button.type = 'button';
                button.className = 'secondary';
                button.setAttribute('data-scenario-id', scenario.id);
                var title = document.createElement('span');
                title.className = 'scenario-title';
                title.textContent = scenario.title;
                button.appendChild(title);
                if (scenario.description) {
                    var description = document.createElement('span');
                    description.className = 'scenario-description';
                    description.textContent = scenario.description;
                    button.appendChild(description);
                }
                button.title = scenario.description || scenario.title;
                button.setAttribute('role', 'listitem');
                button.setAttribute('aria-label', 'Choose scenario topic: ' + scenario.title);
                button.addEventListener('click', function () {
                    startSession(scenario.id);
                });
                scenarioList.appendChild(button);
            });
            setStatus('상담 주제를 선택해 주세요.');
        });
    }

    function loadRecommendations() {
        return api('/chat/api/recommendations').then(function (recommendations) {
            recommendList.innerHTML = '';
            if (!recommendations || recommendations.length === 0) {
                recommendList.textContent = '추천 질문이 없습니다.';
                return;
            }
            recommendations.forEach(function (recommendation) {
                var button = document.createElement('button');
                button.type = 'button';
                button.className = 'secondary';
                button.textContent = recommendation.label;
                button.setAttribute('role', 'listitem');
                button.setAttribute('aria-label', 'Choose recommended question: ' + recommendation.label);
                button.addEventListener('click', function () {
                    startSession(recommendation.scenarioId);
                });
                recommendList.appendChild(button);
            });
        });
    }

    function startSession(scenarioId) {
        setBusy(true);
        setStatus('상담을 시작하는 중입니다.');
        state.selectedScenarioId = scenarioId;
        markScenarioActive(scenarioId);
        if (selectedScenarioName) {
            selectedScenarioName.textContent = selectedScenarioTitle(scenarioId);
        }
        setSessionState('시작 중');
        api('/chat/api/sessions', {
            method: 'POST',
            body: JSON.stringify({ scenarioId: scenarioId })
        }).then(renderSession).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function selectOption(optionId) {
        if (!state.sessionId || state.busy) return;
        setBusy(true);
        api('/chat/api/sessions/' + state.sessionId + '/select-option', {
            method: 'POST',
            body: JSON.stringify({ optionId: optionId })
        }).then(renderSession).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function sendFreeText(text) {
        if (!state.sessionId || state.busy) return;
        setBusy(true);
        api('/chat/api/sessions/' + state.sessionId + '/free-text', {
            method: 'POST',
            body: JSON.stringify({ text: text })
        }).then(function (session) {
            input.value = '';
            renderSession(session);
        }).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function submitFeedback(messageId, rating, comment) {
        api('/chat/api/messages/' + messageId + '/feedback', {
            method: 'POST',
            body: JSON.stringify({ rating: rating, comment: comment })
        }).then(function () {
            setStatus('Feedback saved.');
        }).catch(function (error) {
            setError(error.message);
        });
    }

    form.addEventListener('submit', function (event) {
        event.preventDefault();
        var text = input.value.trim();
        if (!text) {
            setError('질문을 입력해 주세요.');
            return;
        }
        sendFreeText(text);
    });

    document.getElementById('newChatBtn').addEventListener('click', function () {
        state.sessionId = null;
        state.selectedScenarioId = null;
        markScenarioActive(null);
        if (selectedScenarioName) {
            selectedScenarioName.textContent = '주제를 선택하세요';
        }
        setSessionState('대기');
        messageList.innerHTML = '';
        renderMessages([]);
        optionList.innerHTML = '';
        setStatus('상담 주제를 선택해 주세요.');
        setBusy(false);
    });

    setBusy(true);
    Promise.all([loadScenarios(), loadRecommendations()])
        .catch(function (error) {
            setError(error.message);
        })
        .finally(function () {
            setBusy(false);
        });
})();
