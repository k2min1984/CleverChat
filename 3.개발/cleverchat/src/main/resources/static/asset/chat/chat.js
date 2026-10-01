(function () {
    var root = document.querySelector('[data-chat-root]');
    if (!root) return;

    var state = {
        sessionId: null,
        selectedScenarioId: null,
        busy: false
    };

    var scenarioList = document.getElementById('scenarioList');
    var scenarioCount = document.getElementById('scenarioCount');
    var selectedScenarioName = document.getElementById('selectedScenarioName');
    var sessionStateBadge = document.getElementById('sessionStateBadge');
    var recommendList = document.getElementById('recommendList');
    var messageList = document.getElementById('messageList');
    var optionList = document.getElementById('optionList');
    var form = document.getElementById('chatForm');
    var input = document.getElementById('freeText');
    var chatStatus = document.getElementById('statusLine');
    var chatError = document.getElementById('errorLine');

    function setStatus(text) {
        if (chatStatus) {
            chatStatus.textContent = text || '';
        }
        if (chatError) {
            chatError.textContent = '';
        }
        return text;
    }

    function setError(text) {
        var message = text || '요청을 처리할 수 없습니다.';
        if (chatStatus) {
            chatStatus.textContent = '';
        }
        if (chatError) {
            chatError.textContent = message;
        } else if (chatStatus) {
            chatStatus.textContent = message;
        }
        if (window.console && text) {
            console.warn(text);
        }
    }
    function setBusy(busy) {
        state.busy = busy;
        input.disabled = busy;
        form.querySelector('button[type="submit"]').disabled = busy;
        messageList.querySelectorAll('.inline-option-list button').forEach(function (button) {
            button.disabled = busy;
        });
    }

    function setSessionState(text) {
        if (sessionStateBadge) {
            sessionStateBadge.textContent = text || '대기';
        }
    }

    function selectedScenarioTitle(scenarioId) {
        if (!scenarioId) {
            return '\uAC80\uC0C9 \uACB0\uACFC';
        }
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
        return CleverChat.fetch(path, options).then(function (response) {
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

    function getLastBotMessageId(messages) {
        var lastId = null;
        (messages || []).forEach(function (message) {
            if (message.direction === 'BOT' && message.id) {
                lastId = message.id;
            }
        });
        return lastId;
    }

    function splitMessageContent(content) {
        var links = [];
        var lines = String(content || '').split('\n');
        var text = lines.map(function (line, index) {
            var lineWithoutUrl = line.replace(/https?:\/\/[^\s)]+/g, function (url) {
                links.push({
                    url: url.replace(/[.,;:]+$/, ''),
                    label: linkLabelFromLines(lines, index)
                });
                return '';
            });
            return lineWithoutUrl;
        }).join('\n');
        text = text
            .split('\n')
            .map(function (line) { return line.trimEnd(); })
            .join('\n')
            .replace(/\n{3,}/g, '\n\n')
            .trim();
        return {
            text: text,
            links: links
        };
    }

    function linkLabelFromLines(lines, urlLineIndex) {
        for (var index = urlLineIndex - 1; index >= 0; index--) {
            var line = (lines[index] || '').trim();
            if (!line) {
                continue;
            }
            line = line.replace(/^확인\s*경로\s*:\s*/i, '').trim();
            if (line) {
                return line + ' 바로가기';
            }
        }
        return '바로가기';
    }

    function structuredLinks(message) {
        return (message.links || []).map(function (link) {
            return {
                url: link.url,
                label: link.label || '바로가기'
            };
        }).filter(function (link) {
            return !!link.url;
        });
    }

    function messageBubble(message) {
        var content = splitMessageContent(message.content);
        var links = structuredLinks(message);
        if (links.length === 0) {
            links = content.links;
        }
        var bubble = document.createElement('div');
        bubble.className = 'message ' + (message.direction === 'USER' ? 'user' : 'bot');
        bubble.setAttribute('aria-label', (message.direction === 'USER' ? 'User message: ' : 'Chat message: ') + (message.content || ''));

        var text = document.createElement('p');
        text.className = 'message-text';
        text.textContent = content.text || '';
        bubble.appendChild(text);

        if (message.direction === 'BOT' && links.length > 0) {
            bubble.classList.add('has-links');
            var actions = document.createElement('div');
            actions.className = 'message-actions';
            links.forEach(function (linkInfo, index) {
                var link = document.createElement('a');
                link.className = 'chat-link-button';
                link.href = linkInfo.url;
                link.target = '_blank';
                link.rel = 'noopener noreferrer';
                link.title = linkInfo.label || '해당 페이지로 이동';
                link.setAttribute('aria-label', link.title + ' (새 창)');
                link.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><path d="M14 3h7v7"/><path d="M10 14 21 3"/><path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5"/></svg>';
                actions.appendChild(link);
            });
            bubble.appendChild(actions);
        }

        return bubble;
    }

    function optionButton(option, sourceNodeId, sourceMessageId) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'secondary';
        button.textContent = option.label;
        button.setAttribute('role', 'listitem');
        button.setAttribute('aria-label', 'Choose answer: ' + option.label);
        button.disabled = state.busy;
        button.addEventListener('click', function () {
            selectOption(option.id, sourceNodeId, sourceMessageId);
        });
        return button;
    }

    function visibleScenarioOptions(options) {
        return (options || []).filter(function (option) {
            return !/^(상위\s*메뉴|처음으로|이전)$/.test((option.label || '').trim());
        });
    }

    function inlineOptions(options) {
        var wrapper = document.createElement('div');
        wrapper.className = 'inline-option-list';
        wrapper.setAttribute('role', 'list');
        wrapper.setAttribute('aria-label', 'Available answers');
        visibleScenarioOptions(options).forEach(function (option) {
            wrapper.appendChild(optionButton(option));
        });
        return wrapper;
    }

    function searchOptionButton(option, sourceMessageId) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'secondary';
        var isDocument = option.optionType === 'DOCUMENT';
        var icon = document.createElement('span');
        icon.className = 'search-option-icon';
        icon.setAttribute('aria-hidden', 'true');
        icon.innerHTML = isDocument
            ? '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2h9l5 5v15H6z"/><path d="M14 2v6h6"/><path d="M9 13h8M9 17h8"/></svg>'
            : '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>';
        var label = document.createElement('span');
        label.textContent = option.label;
        button.appendChild(icon);
        button.appendChild(label);
        button.classList.add(isDocument ? 'is-document' : 'is-scenario');
        button.setAttribute('role', 'listitem');
        button.setAttribute('aria-label', 'Choose result: ' + option.label);
        button.disabled = state.busy;
        button.addEventListener('click', function () {
            selectSearchResult(option.crawlDocumentNo, option.scenarioNo, option.scenarioNodeNo, sourceMessageId);
        });
        return button;
    }

    function inlineSearchOptions(searchOptions) {
        var wrapper = document.createElement('div');
        wrapper.className = 'inline-option-list';
        wrapper.setAttribute('role', 'list');
        wrapper.setAttribute('aria-label', 'Search results');
        (searchOptions || []).forEach(function (option) {
            wrapper.appendChild(searchOptionButton(option));
        });
        return wrapper;
    }

    function appendSearchGroup(wrapper, title, options, sourceMessageId) {
        if (!options || options.length === 0) return;
        var group = document.createElement('div');
        group.className = 'inline-search-group';
        group.setAttribute('role', 'group');
        group.setAttribute('aria-label', title);
        var heading = document.createElement('strong');
        heading.className = 'inline-search-heading';
        heading.textContent = title;
        group.appendChild(heading);
        options.forEach(function (option) {
            group.appendChild(searchOptionButton(option, sourceMessageId));
        });
        wrapper.appendChild(group);
    }

    function backButton() {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'secondary';
        button.textContent = '이전';
        button.setAttribute('role', 'listitem');
        button.setAttribute('aria-label', 'Go back to previous step');
        button.disabled = state.busy;
        button.addEventListener('click', goBack);
        return button;
    }

    function searchMoreButton(count) {
        var button = document.createElement('button');
        button.type = 'button';
        button.className = 'secondary';
        button.textContent = '검색 결과 더 보기 (' + count + '건)';
        button.setAttribute('role', 'listitem');
        button.setAttribute('aria-label', 'Show more search results');
        button.disabled = state.busy;
        button.addEventListener('click', searchMore);
        return button;
    }

    function inlineSessionActions(options, searchOptions, canGoBack, searchMoreCount, sourceNodeId, backTargetType, sourceMessageId) {
        var wrapper = document.createElement('div');
        wrapper.className = 'inline-option-list';
        wrapper.setAttribute('role', 'list');
        wrapper.setAttribute('aria-label', 'Available actions');
        var choices = visibleScenarioOptions(options);
        var endOptions = choices.filter(function (option) {
            return (option.label || '').trim() === '안내 종료';
        });
        choices.filter(function (option) {
            return (option.label || '').trim() !== '안내 종료';
        }).forEach(function (option) {
            wrapper.appendChild(optionButton(option, sourceNodeId, sourceMessageId));
        });
        appendSearchGroup(wrapper, '상담 안내', (searchOptions || []).filter(function (option) {
            return option.optionType !== 'DOCUMENT';
        }), sourceMessageId);
        appendSearchGroup(wrapper, '관련 자료', (searchOptions || []).filter(function (option) {
            return option.optionType === 'DOCUMENT';
        }), sourceMessageId);
        if (searchMoreCount > 0) {
            wrapper.appendChild(searchMoreButton(searchMoreCount));
        }
        if (canGoBack) {
            wrapper.appendChild(backButton());
        }
        endOptions.forEach(function (option) {
            wrapper.appendChild(optionButton(option, sourceNodeId, sourceMessageId));
        });
        return wrapper;
    }

    function renderMessages(messages, options, searchOptions, canGoBack, searchMoreCount, backTargetType) {
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
        var lastBotMessageId = getLastBotMessageId(messages);
        messages.forEach(function (message) {
            var row = document.createElement('div');
            row.className = 'message-row ' + (message.direction === 'USER' ? 'user' : 'bot');
            row.appendChild(messageBubble(message));
            messageList.appendChild(row);
            if (message.direction === 'BOT') {
                var isLatest = message.id === lastBotMessageId;
                var messageOptions = message.options || (isLatest ? options : []);
                var messageSearchOptions = message.searchOptions || (isLatest ? searchOptions : []);
                var messageSearchMoreCount = isLatest ? searchMoreCount : 0;
                var messageCanGoBack = isLatest && canGoBack;
                var hasActions = messageOptions.length > 0
                    || messageSearchOptions.length > 0
                    || messageSearchMoreCount > 0
                    || messageCanGoBack;
                if (hasActions) {
                    messageList.appendChild(inlineSessionActions(
                        messageOptions,
                        messageSearchOptions,
                        messageCanGoBack,
                        messageSearchMoreCount,
                        message.nodeId,
                        isLatest ? backTargetType : null,
                        message.id
                    ));
                }
            }
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
        optionList.hidden = true;
    }

    function renderSession(session) {
        state.sessionId = session.sessionId;
        renderMessages(
            session.messages,
            session.options,
            session.searchOptions,
            session.canGoBack,
            session.searchMoreCount || 0,
            session.backTargetType
        );
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
        }).then(function (session) {
            renderSession(session);
            return session;
        }).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function selectOption(optionId, sourceNodeId, sourceMessageId) {
        if (!state.sessionId || state.busy) return;
        setBusy(true);
        api('/chat/api/sessions/' + state.sessionId + '/select-option', {
            method: 'POST',
            body: JSON.stringify({ optionId: optionId, sourceNodeId: sourceNodeId || null })
        }).then(renderSession).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function selectSearchResult(crawlDocumentNo, scenarioNo, scenarioNodeNo, sourceMessageId) {
        if (!state.sessionId || state.busy) return;
        setBusy(true);
        api('/chat/api/sessions/' + state.sessionId + '/select-search-result', {
            method: 'POST',
            body: JSON.stringify({ crawlDocumentNo: crawlDocumentNo, scenarioNo: scenarioNo, scenarioNodeNo: scenarioNodeNo, sourceMessageId: sourceMessageId })
        }).then(function (session) {
            state.selectedScenarioId = session.scenarioId;
            markScenarioActive(session.scenarioId);
            if (selectedScenarioName) {
                selectedScenarioName.textContent = selectedScenarioTitle(session.scenarioId);
            }
            renderSession(session);
        }).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function searchMore() {
        if (!state.sessionId || state.busy) return;
        setBusy(true);
        api('/chat/api/sessions/' + state.sessionId + '/search-more', {
            method: 'POST'
        }).then(renderSession).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function goBack() {
        if (!state.sessionId || state.busy) return;
        setBusy(true);
        api('/chat/api/sessions/' + state.sessionId + '/back', {
            method: 'POST'
        }).then(function (session) {
            state.selectedScenarioId = session.scenarioId;
            markScenarioActive(session.scenarioId);
            if (selectedScenarioName) {
                selectedScenarioName.textContent = selectedScenarioTitle(session.scenarioId);
            }
            renderSession(session);
        }).catch(function (error) {
            setError(error.message);
            setBusy(false);
        });
    }

    function sendFreeText(text) {
        if (state.busy) return;
        if (!state.sessionId) {
            setBusy(true);
            setStatus('질문과 맞는 상담 주제를 찾는 중입니다.');
            api('/chat/api/sessions/auto', {
                method: 'POST',
                body: JSON.stringify({ text: text })
            }).then(function (session) {
                state.selectedScenarioId = session.scenarioId;
                markScenarioActive(session.scenarioId);
                if (selectedScenarioName) {
                    selectedScenarioName.textContent = selectedScenarioTitle(session.scenarioId);
                }
                input.value = '';
                renderSession(session);
            }).catch(function (error) {
                setError(error.message);
                setBusy(false);
            });
            return;
        }
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
