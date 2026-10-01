/**************************************************
 * @desc : Scenario graph edit view (DRAFT only)
 **************************************************/
(function () {
    var root = document.querySelector('.content-area[data-graph-load-url], .admin-content[data-graph-load-url]');
    if (!root) return;

    var loadUrl = root.getAttribute('data-graph-load-url');
    var saveUrl = root.getAttribute('data-graph-save-url');

    var nodeNav = document.getElementById('nodeNav');
    var nodeEditor = document.getElementById('nodeEditor');
    var emptyHelp = document.getElementById('nodeEmptyHelp');
    var selectHelp = document.getElementById('nodeSelectHelp');
    var msgBox = document.getElementById('graphMsg');
    var startStatus = document.getElementById('startNodeStatus');
    var nodeCountStatus = document.getElementById('nodeCountStatus');

    var dirty = false;
    var startNodeKey = '';
    var selectedUid = '';
    var uidSeq = 0;
    var dragUid = '';
    var commonCodes = window.CLEVERCHAT_COMMON_CODES || {};

    function commonCodeOptions(groupCode, fallback) {
        var options = commonCodes[groupCode];
        if (!Array.isArray(options) || options.length === 0) options = fallback;
        return options.map(function (option) {
            return '<option value="' + escapeHtml(option.value) + '">' + escapeHtml(option.name) + '</option>';
        }).join('');
    }

    function commonCodeLabel(groupCode, value) {
        var options = commonCodes[groupCode] || [];
        for (var i = 0; i < options.length; i++) {
            if (options[i].value === value) return options[i].name;
        }
        return value;
    }

    function nextUid() {
        uidSeq += 1;
        return 'node-' + uidSeq;
    }

    function cards() {
        return Array.prototype.slice.call(nodeEditor.querySelectorAll('[data-role="node"]'));
    }

    function selectedCard() {
        return nodeEditor.querySelector('[data-role="node"][data-uid="' + selectedUid + '"]');
    }

    function showMsg(text, isError) {
        msgBox.hidden = false;
        msgBox.textContent = text;
        msgBox.setAttribute('role', isError ? 'alert' : 'status');
        msgBox.classList.toggle('is-error', !!isError);
        msgBox.style.color = isError ? '#dc2626' : '#16a34a';
    }

    function clearMsg() {
        msgBox.hidden = true;
        msgBox.textContent = '';
        msgBox.setAttribute('role', 'status');
        msgBox.classList.remove('is-error');
    }

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function nodeTypeLabel(value) {
        return commonCodeLabel('SCENARIO_NODE_TYPE', value) || '노드';
    }

    function normalizeKey(value) {
        return (value || '')
            .trim()
            .toLowerCase()
            .replace(/[^a-z0-9_-]+/g, '-')
            .replace(/^-+|-+$/g, '')
            .slice(0, 60);
    }

    function nodeKeyMap() {
        var map = {};
        cards().forEach(function (card) {
            var key = card.querySelector('[data-field="nodeKey"]').value.trim();
            if (key) map[key] = card;
        });
        return map;
    }

    function uniqueNodeKey(seed) {
        var base = normalizeKey(seed) || 'node';
        var map = nodeKeyMap();
        var key = base;
        var index = 1;
        while (map[key]) {
            index += 1;
            key = base + '-' + index;
        }
        return key;
    }

    function nodeOptions(currentValue) {
        var map = nodeKeyMap();
        var html = '<option value="">상담 종료</option>';
        if (currentValue && !map[currentValue]) {
            html += '<option value="' + escapeHtml(currentValue) + '" selected>' +
                escapeHtml(currentValue + ' (삭제된 노드)') +
                '</option>';
        }
        cards().forEach(function (card) {
            var key = card.querySelector('[data-field="nodeKey"]').value.trim();
            var title = card.querySelector('[data-field="title"]').value.trim();
            if (!key) return;
            html += '<option value="' + escapeHtml(key) + '"' + (key === currentValue ? ' selected' : '') + '>' +
                escapeHtml(title ? title + ' (' + key + ')' : key) +
                '</option>';
        });
        return html;
    }

    function titleForKey(key) {
        if (!key) return '';
        var card = nodeKeyMap()[key];
        if (!card) return key + ' (없는 노드)';
        var title = card.querySelector('[data-field="title"]').value.trim();
        return title ? title + ' (' + key + ')' : key;
    }

    function nodeDisplay(card) {
        var key = card.querySelector('[data-field="nodeKey"]').value.trim();
        var title = card.querySelector('[data-field="title"]').value.trim();
        return title ? title + (key ? ' (' + key + ')' : '') : key || '새 노드';
    }

    function optionLabel(row) {
        return row.querySelector('[data-field="label"]').value.trim() || '선택지';
    }

    function pathsFromNode(card, visited, depth) {
        var key = card.querySelector('[data-field="nodeKey"]').value.trim();
        var type = card.querySelector('[data-field="nodeType"]').value;
        var options = Array.prototype.slice.call(card.querySelectorAll('[data-role="option"]'));
        if (type === 'END') return [[]];
        if (options.length === 0) return [['상담 종료']];
        if (depth > 12) return [['경로가 너무 깁니다']];
        return options.reduce(function (paths, row) {
            return paths.concat(pathsFromOption(row, visited, depth));
        }, []);
    }

    function pathsFromOption(row, visited, depth) {
        var label = optionLabel(row);
        var nextKey = row.querySelector('[data-field="nextNodeKey"]').value.trim();
        if (!nextKey) return [[label, '상담 종료']];
        var target = nodeKeyMap()[nextKey];
        if (!target) return [[label, nextKey + ' (삭제된 노드)']];
        if (visited[nextKey]) return [[label, nodeDisplay(target), '순환 연결 감지']];
        var nextVisited = Object.assign({}, visited);
        nextVisited[nextKey] = true;
        return pathsFromNode(target, nextVisited, depth + 1).map(function (suffix) {
            return [label, nodeDisplay(target)].concat(suffix);
        });
    }

    function renderOptionPath(row) {
        var pathBox = row.querySelector('[data-role="connectionPath"]');
        if (!pathBox) return;
        var owner = row.closest('[data-role="node"]');
        var ownerKey = owner ? owner.querySelector('[data-field="nodeKey"]').value.trim() : '';
        var visited = {};
        if (ownerKey) visited[ownerKey] = true;
        var paths = pathsFromOption(row, visited, 0);
        pathBox.innerHTML =
            '<strong>전체 경로</strong>' +
            '<ul>' +
            paths.map(function (path) {
                return '<li>' + path.map(escapeHtml).join(' <span aria-hidden="true">→</span> ') + '</li>';
            }).join('') +
            '</ul>';
    }

    function updateOptionConnectionHint(row) {
        var nextKey = row.querySelector('[data-field="nextNodeKey"]').value.trim();
        var hint = row.querySelector('[data-role="connectionHint"]');
        var missing = !!nextKey && !nodeKeyMap()[nextKey];
        row.classList.toggle('is-terminal-option', !nextKey);
        row.classList.toggle('is-missing-option', missing);
        if (!hint) return;
        if (missing) {
            hint.textContent = '연결된 노드를 찾을 수 없습니다. 다른 노드를 선택하거나 새 노드를 만드세요.';
        } else if (!nextKey) {
            hint.textContent = '이 선택지를 누르면 상담을 종료합니다.';
        } else {
            hint.textContent = '선택한 노드로 이어집니다.';
        }
        renderOptionPath(row);
    }

    function parseMetadataRows(metadata) {
        if (!metadata) return [];
        try {
            var parsed = JSON.parse(metadata);
            if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') return [];
            return Object.keys(parsed).map(function (key) {
                var value = parsed[key];
                return {
                    key: key,
                    value: typeof value === 'string' ? value : JSON.stringify(value)
                };
            });
        } catch (e) {
            return [];
        }
    }

    function metadataRow(item) {
        var row = document.createElement('div');
        row.className = 'graph-metadata-row';
        row.dataset.role = 'metadata';
        row.innerHTML =
            '<input type="text" data-field="metadataKey" maxlength="80" aria-label="메타데이터 항목명" placeholder="항목명">' +
            '<input type="text" data-field="metadataValue" maxlength="500" aria-label="메타데이터 값" placeholder="값">' +
            '<button type="button" class="btn btn-xs btn-secondary" data-action="removeMetadata" aria-label="메타데이터 삭제">삭제</button>';
        row.querySelector('[data-field="metadataKey"]').value = item && item.key ? item.key : '';
        row.querySelector('[data-field="metadataValue"]').value = item && item.value ? item.value : '';
        return row;
    }

    function linkRow(link) {
        var row = document.createElement('div');
        row.className = 'graph-node-link-row';
        row.dataset.role = 'link';
        row.innerHTML =
            '<label>버튼명<input type="text" data-field="linkLabel" maxlength="150" aria-label="바로가기 버튼명" placeholder="예: 송배전 바로가기"></label>' +
            '<label>URL<input type="text" data-field="linkUrl" maxlength="1000" aria-label="바로가기 URL" placeholder="https:// 또는 / 로 시작"></label>' +
            '<label>유형<select data-field="linkType" aria-label="바로가기 유형">' +
                commonCodeOptions('SCENARIO_LINK_TYPE', [
                    { value: 'EXTERNAL', name: '외부 링크' },
                    { value: 'INTERNAL', name: '내부 링크' },
                    { value: 'DOWNLOAD', name: '파일 다운로드' }
                ]) +
            '</select></label>' +
            '<label>정렬<input type="number" data-field="linkSortOrder" min="0" step="1" aria-label="바로가기 정렬 순서"></label>' +
            '<label class="graph-check"><input type="checkbox" data-field="linkUseYn" aria-label="바로가기 사용 여부"> 사용</label>' +
            '<button type="button" class="btn btn-xs btn-secondary red" data-action="removeLink" aria-label="바로가기 삭제">삭제</button>';
        row.querySelector('[data-field="linkLabel"]').value = link && link.label ? link.label : '';
        row.querySelector('[data-field="linkUrl"]').value = link && link.url ? link.url : '';
        row.querySelector('[data-field="linkType"]').value = link && link.linkType ? link.linkType : 'EXTERNAL';
        row.querySelector('[data-field="linkSortOrder"]').value = link && Number.isFinite(link.sortOrder) ? link.sortOrder : 0;
        row.querySelector('[data-field="linkUseYn"]').checked = !link || link.useYn !== 'N';
        return row;
    }

    function optionRow(option) {
        var row = document.createElement('div');
        row.className = 'graph-option-row';
        row.dataset.role = 'option';
        row.innerHTML =
            '<div class="graph-option-main">' +
                '<label class="graph-option-label">버튼 문구<input type="text" data-field="label" maxlength="150" aria-label="선택지 버튼 문구" placeholder="예: 여권 발급"></label>' +
                '<label class="graph-next-node-field">연결 방식<select data-field="nextNodeKey" aria-label="다음 노드 선택"></select></label>' +
                '<div class="graph-option-actions">' +
                    '<button type="button" class="btn btn-xs btn-secondary" data-action="createLinkedNode" aria-label="새 노드를 만들고 연결">새 노드 만들기</button>' +
                    '<button type="button" class="btn btn-xs btn-secondary red" data-action="removeOption" aria-label="선택지 삭제">삭제</button>' +
                '</div>' +
            '</div>' +
            '<span class="graph-option-hint" data-role="connectionHint"></span>' +
            '<details class="graph-option-advanced">' +
                '<summary>옵션</summary>' +
                '<div class="graph-option-advanced-grid">' +
                    '<label>조건식<input type="text" data-field="conditionExpr" maxlength="500" aria-label="선택지 조건식" placeholder="선택"></label>' +
                    '<label>정렬<input type="number" data-field="sortOrder" min="1" step="1" aria-label="선택지 정렬 순서"></label>' +
                    '<label class="graph-check"><input type="checkbox" data-field="useYn" aria-label="선택지 사용 여부"> 사용</label>' +
                '</div>' +
            '</details>' +
            '<div class="graph-option-path" data-role="connectionPath" aria-live="polite"></div>';
        row.querySelector('[data-field="label"]').value = option.label || '';
        row.querySelector('[data-field="nextNodeKey"]').innerHTML = nodeOptions(option.nextNodeKey || '');
        row.querySelector('[data-field="conditionExpr"]').value = option.conditionExpr || '';
        row.querySelector('[data-field="sortOrder"]').value = option.sortOrder || 1;
        row.querySelector('[data-field="useYn"]').checked = option.useYn !== 'N';
        updateOptionConnectionHint(row);
        return row;
    }

    function nextSortOrder() {
        return cards().length + 1;
    }

    function nodeCard(node) {
        var card = document.createElement('article');
        card.className = 'graph-node-card';
        card.dataset.role = 'node';
        card.dataset.uid = nextUid();
        card.innerHTML =
            '<header class="graph-node-card-header">' +
                '<div class="graph-node-title-wrap">' +
                    '<span class="graph-node-index" data-role="nodeIndex">1</span>' +
                    '<div>' +
                        '<strong data-role="nodeTitle">새 노드</strong>' +
                        '<div class="graph-node-badges">' +
                            '<span class="badge badge-default" data-role="nodeTypeBadge">질문</span>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
                '<div class="graph-node-actions">' +
                    '<button type="button" class="btn btn-xs btn-secondary" data-action="setStartNode" aria-label="시작 노드로 지정">시작으로 지정</button>' +
                    '<button type="button" class="btn btn-xs btn-secondary red" data-action="removeNode" aria-label="노드 삭제">삭제</button>' +
                '</div>' +
            '</header>' +
            '<div class="graph-field-grid">' +
                '<label class="graph-field">관리키<input type="text" data-field="nodeKey" maxlength="80" aria-label="노드 키" placeholder="예: start"></label>' +
                '<label class="graph-field">노드 유형<select data-field="nodeType" aria-label="노드 유형">' +
                    commonCodeOptions('SCENARIO_NODE_TYPE', [
                        { value: 'QUESTION', name: '질문' },
                        { value: 'ANSWER', name: '답변' },
                        { value: 'BRANCH', name: '분기' },
                        { value: 'END', name: '종료' }
                    ]) +
                '</select></label>' +
                '<label class="graph-field">정렬 순서<input type="number" data-field="sortOrder" min="1" step="1" aria-label="노드 정렬 순서"></label>' +
                '<label class="graph-field graph-field-wide">제목<input type="text" data-field="title" maxlength="150" aria-label="노드 제목" placeholder="관리자가 구분할 제목"></label>' +
                '<label class="graph-field graph-field-wide">챗봇 문구<textarea data-field="content" maxlength="10000" rows="5" aria-label="노드 내용" placeholder="사용자에게 보여줄 질문 또는 답변 문구"></textarea></label>' +
            '</div>' +
            '<section class="graph-links-box" aria-label="바로가기 링크">' +
                '<div class="graph-subheader">' +
                    '<div><strong>바로가기 링크</strong><p class="help-text">답변 하단에 버튼으로 노출할 링크를 등록합니다.</p></div>' +
                    '<button type="button" class="btn btn-xs btn-secondary" data-action="addLink" aria-label="바로가기 추가">링크 추가</button>' +
                '</div>' +
                '<div data-role="links" class="graph-node-link-list"></div>' +
            '</section>' +
            '<section class="graph-options-box" aria-label="선택지 목록">' +
                '<div class="graph-subheader">' +
                    '<div><strong>선택지와 연결</strong><p class="help-text">사용자가 누를 버튼과 이어질 다음 노드를 선택합니다.</p></div>' +
                    '<button type="button" class="btn btn-xs btn-secondary" data-action="addOption" aria-label="선택지 추가">선택지 추가</button>' +
                '</div>' +
                '<div data-role="connectionSummary" class="graph-connection-summary"></div>' +
                '<div data-role="options" class="graph-option-list"></div>' +
            '</section>' +
            '<details class="graph-metadata-box">' +
                '<summary>고급 설정: 메타데이터</summary>' +
                '<p class="help-text">기본 상담 흐름에는 직접 노출되지 않는 확장 정보입니다. 필요한 경우에만 입력하세요.</p>' +
                '<div data-role="metadataRows" class="graph-metadata-list"></div>' +
                '<button type="button" class="btn btn-xs btn-secondary" data-action="addMetadata" aria-label="메타데이터 추가">항목 추가</button>' +
            '</details>';

        card.querySelector('[data-field="nodeKey"]').value = node.nodeKey || '';
        card.querySelector('[data-field="nodeType"]').value = node.nodeType || 'QUESTION';
        card.querySelector('[data-field="title"]').value = node.title || '';
        card.querySelector('[data-field="content"]').value = node.content || '';
        card.querySelector('[data-field="sortOrder"]').value = node.sortOrder || nextSortOrder();
        parseMetadataRows(node.metadata).forEach(function (item) {
            card.querySelector('[data-role="metadataRows"]').appendChild(metadataRow(item));
        });
        (node.links || []).forEach(function (link) {
            card.querySelector('[data-role="links"]').appendChild(linkRow(link));
        });
        (node.options || []).forEach(function (option) {
            card.querySelector('[data-role="options"]').appendChild(optionRow(option));
        });
        return card;
    }

    function connectionSummary(card) {
        var rows = [];
        var map = nodeKeyMap();
        Array.prototype.forEach.call(card.querySelectorAll('[data-role="option"]'), function (row) {
            var label = row.querySelector('[data-field="label"]').value.trim() || '선택지';
            var nextKey = row.querySelector('[data-field="nextNodeKey"]').value.trim();
            var missing = !!nextKey && !map[nextKey];
            var cssClass = 'graph-link-pill' + (!nextKey ? ' graph-link-pill-end' : '') + (missing ? ' graph-link-pill-missing' : '');
            var target = nextKey ? titleForKey(nextKey) : '상담 종료';
            rows.push('<span class="' + cssClass + '">' + escapeHtml(label) + ' → ' + escapeHtml(target) + '</span>');
        });
        return rows.length ? rows.join('') : '<span class="graph-link-empty">연결된 선택지가 없습니다.</span>';
    }

    function ensureNodeKey(card) {
        var input = card.querySelector('[data-field="nodeKey"]');
        if (input.value.trim()) return input.value.trim();
        var title = card.querySelector('[data-field="title"]').value.trim();
        input.value = uniqueNodeKey(title || 'node');
        return input.value.trim();
    }

    function refreshNodeCard(card, index) {
        var key = card.querySelector('[data-field="nodeKey"]').value.trim();
        var title = card.querySelector('[data-field="title"]').value.trim();
        var type = card.querySelector('[data-field="nodeType"]').value;
        card.hidden = card.dataset.uid !== selectedUid;
        card.classList.toggle('is-start', !!key && key === startNodeKey);
        card.querySelector('[data-role="nodeIndex"]').textContent = String(index + 1);
        card.querySelector('[data-role="nodeTitle"]').textContent = title || key || '새 노드';
        var nodeTypeBadge = card.querySelector('[data-role="nodeTypeBadge"]');
        nodeTypeBadge.textContent = nodeTypeLabel(type);
        nodeTypeBadge.className = 'badge graph-node-type';
        nodeTypeBadge.setAttribute('data-type', type);
        card.querySelector('[data-action="setStartNode"]').hidden = !!key && key === startNodeKey;
        card.querySelector('[data-role="connectionSummary"]').innerHTML = connectionSummary(card);
        card.querySelector('.graph-options-box').hidden = type === 'END';
    }

    function updateSortOrdersFromDom() {
        cards().forEach(function (card, index) {
            card.querySelector('[data-field="sortOrder"]').value = index + 1;
        });
    }

    function renderNodeNav() {
        var html = '';
        cards().forEach(function (card, index) {
            var key = card.querySelector('[data-field="nodeKey"]').value.trim();
            var title = card.querySelector('[data-field="title"]').value.trim() || key || '새 노드';
            var type = card.querySelector('[data-field="nodeType"]').value;
            var linkCount = card.querySelectorAll('[data-role="link"]').length;
            var optionCount = card.querySelectorAll('[data-role="option"]').length;
            var isSelected = card.dataset.uid === selectedUid;
            var isStart = key && key === startNodeKey;
            html += '<button type="button" class="graph-node-nav-item' + (isSelected ? ' is-selected' : '') + (isStart ? ' is-start' : '') + '" data-action="selectNode" data-uid="' + card.dataset.uid + '" draggable="true" role="listitem" aria-pressed="' + (isSelected ? 'true' : 'false') + '">' +
                '<span class="graph-node-drag-handle" aria-label="드래그 정렬" title="드래그해서 순서 변경">☰</span>' +
                '<span class="graph-node-nav-index">' + (index + 1) + '</span>' +
                '<span class="graph-node-nav-main">' +
                    '<strong>' + escapeHtml(title) + '</strong>' +
                    '<span>' + escapeHtml(key || '관리키 없음') + '</span>' +
                '</span>' +
                '<span class="graph-node-nav-meta">' +
                      '<b class="graph-node-type" data-type="' + escapeHtml(type) + '">' + escapeHtml(nodeTypeLabel(type)) + '</b>' +
                    '<small class="graph-node-link-count">링크 ' + linkCount + '</small>' +
                    '<small class="graph-node-opt-count">선택지 ' + optionCount + '</small>' +
                  '</span>' +
            '</button>';
        });
        nodeNav.innerHTML = html;
    }

    function refreshOptionSelects() {
        Array.prototype.forEach.call(nodeEditor.querySelectorAll('[data-field="nextNodeKey"]'), function (select) {
            var value = select.value;
            select.innerHTML = nodeOptions(value);
            select.value = value;
            updateOptionConnectionHint(select.closest('[data-role="option"]'));
        });
    }

    function refreshStartStatus() {
        if (startStatus) startStatus.textContent = startNodeKey ? '시작 노드: ' + titleForKey(startNodeKey) : '시작 노드가 없습니다.';
        if (nodeCountStatus) nodeCountStatus.textContent = '노드 ' + cards().length + '개';
    }

    function refreshAll() {
        var all = cards();
        if (!selectedUid && all.length) selectedUid = all[0].dataset.uid;
        if (!startNodeKey && all.length) startNodeKey = ensureNodeKey(all[0]);
        if (startNodeKey && !nodeKeyMap()[startNodeKey] && all.length) startNodeKey = ensureNodeKey(all[0]);
        updateSortOrdersFromDom();
        refreshOptionSelects();
        all.forEach(refreshNodeCard);
        renderNodeNav();
        refreshStartStatus();
        emptyHelp.hidden = all.length > 0;
        selectHelp.hidden = all.length > 0;
    }

    function collectMetadata(card) {
        var metadata = {};
        Array.prototype.forEach.call(card.querySelectorAll('[data-role="metadata"]'), function (row) {
            var key = row.querySelector('[data-field="metadataKey"]').value.trim();
            var value = row.querySelector('[data-field="metadataValue"]').value.trim();
            if (key) metadata[key] = value;
        });
        return Object.keys(metadata).length ? JSON.stringify(metadata) : '{}';
    }

    function render(graph) {
        var nodes = graph.nodes || [];
        startNodeKey = graph.startNodeKey || '';
        selectedUid = '';
        nodeEditor.innerHTML = '';
        nodes.forEach(function (node) {
            nodeEditor.appendChild(nodeCard(node));
        });
        refreshAll();
        dirty = false;
    }

    function collect() {
        var all = cards();
        all.forEach(ensureNodeKey);
        if (!startNodeKey && all.length) startNodeKey = ensureNodeKey(all[0]);
        var nodes = all.map(function (card) {
            var links = [];
            Array.prototype.forEach.call(card.querySelectorAll('[data-role="link"]'), function (row) {
                links.push({
                    label: row.querySelector('[data-field="linkLabel"]').value.trim(),
                    url: row.querySelector('[data-field="linkUrl"]').value.trim(),
                    linkType: row.querySelector('[data-field="linkType"]').value || 'EXTERNAL',
                    sortOrder: parseInt(row.querySelector('[data-field="linkSortOrder"]').value, 10) || 0,
                    useYn: row.querySelector('[data-field="linkUseYn"]').checked ? 'Y' : 'N'
                });
            });
            var options = [];
            if (card.querySelector('[data-field="nodeType"]').value !== 'END') {
                Array.prototype.forEach.call(card.querySelectorAll('[data-role="option"]'), function (row) {
                    options.push({
                        label: row.querySelector('[data-field="label"]').value.trim(),
                        nextNodeKey: row.querySelector('[data-field="nextNodeKey"]').value.trim() || null,
                        conditionExpr: row.querySelector('[data-field="conditionExpr"]').value.trim() || null,
                        sortOrder: parseInt(row.querySelector('[data-field="sortOrder"]').value, 10) || 1,
                        useYn: row.querySelector('[data-field="useYn"]').checked ? 'Y' : 'N'
                    });
                });
            }
            return {
                nodeKey: card.querySelector('[data-field="nodeKey"]').value.trim(),
                nodeType: card.querySelector('[data-field="nodeType"]').value,
                title: card.querySelector('[data-field="title"]').value.trim(),
                content: card.querySelector('[data-field="content"]').value,
                sortOrder: parseInt(card.querySelector('[data-field="sortOrder"]').value, 10) || 1,
                metadata: collectMetadata(card),
                links: links,
                options: options
            };
        });
        return { startNodeKey: startNodeKey, nodes: nodes };
    }

    function validatePayload(payload) {
        if (payload.nodes.length === 0) return '저장하려면 노드를 1개 이상 추가하세요.';
        if (!payload.startNodeKey) return '시작 노드를 지정하세요.';
        var nodeKeys = {};
        for (var i = 0; i < payload.nodes.length; i++) {
            var node = payload.nodes[i];
            if (!node.nodeKey) return (i + 1) + '번째 노드의 관리키를 입력하세요.';
            if (!/^[A-Za-z0-9_-]{1,80}$/.test(node.nodeKey)) return '관리키는 영문, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다.';
            if (nodeKeys[node.nodeKey]) return '중복된 노드 관리키가 있습니다.';
            nodeKeys[node.nodeKey] = true;
            if (!node.title) return (i + 1) + '번째 노드의 제목을 입력하세요.';
            if (node.nodeType === 'END' && node.options.length > 0) return '종료 노드에는 선택지를 등록할 수 없습니다.';
            for (var l = 0; l < (node.links || []).length; l++) {
                var link = node.links[l];
                if (!link.label) return (i + 1) + '번째 노드의 바로가기 버튼명을 입력하세요.';
                if (!link.url) return (i + 1) + '번째 노드의 바로가기 URL을 입력하세요.';
                if (!/^(https?:\/\/|\/).+/.test(link.url)) return '바로가기 URL은 http://, https:// 또는 / 로 시작해야 합니다.';
            }
        }
        if (!nodeKeys[payload.startNodeKey]) return '시작 노드가 노드 목록에 없습니다.';
        for (var n = 0; n < payload.nodes.length; n++) {
            for (var o = 0; o < payload.nodes[n].options.length; o++) {
                var option = payload.nodes[n].options[o];
                if (!option.label) return (n + 1) + '번째 노드의 선택지 문구를 입력하세요.';
                if (option.nextNodeKey && !nodeKeys[option.nextNodeKey]) return '선택지의 다음 노드가 노드 목록에 없습니다: ' + option.nextNodeKey;
            }
        }
        return null;
    }

    function validateMetadataJson(payload) {
        for (var i = 0; i < payload.nodes.length; i++) {
            try {
                JSON.parse(payload.nodes[i].metadata || '{}');
            } catch (e) {
                return (i + 1) + '번째 노드의 메타데이터를 확인하세요.';
            }
        }
        return null;
    }

    function responseMessage(xhr, fallback) {
        try {
            var env = JSON.parse(xhr.responseText);
            return env && env.error && env.error.message ? env.error.message : fallback;
        } catch (e) {
            return fallback;
        }
    }

    function load() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', loadUrl, true);
        xhr.setRequestHeader('Accept', 'application/json');
        xhr.withCredentials = true;
        xhr.onload = function () {
            if (xhr.status !== 200) {
                showMsg(responseMessage(xhr, '흐름을 불러오지 못했습니다. (' + xhr.status + ')'), true);
                return;
            }
            try {
                var env = JSON.parse(xhr.responseText);
                render(env.data || { startNodeKey: null, nodes: [] });
                clearMsg();
            } catch (e) {
                showMsg('흐름 응답을 해석하지 못했습니다.', true);
            }
        };
        xhr.onerror = function () {
            showMsg('흐름을 불러오는 중 네트워크 오류가 발생했습니다.', true);
        };
        xhr.send();
    }

    function save() {
        var payload = collect();
        refreshAll();
        var bad = validatePayload(payload) || validateMetadataJson(payload);
        if (bad) {
            showMsg(bad, true);
            return;
        }

        var xhr = new XMLHttpRequest();
        xhr.open('PUT', saveUrl, true);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        ADM.setCsrfHeaders(xhr);
        xhr.withCredentials = true;
        xhr.onload = function () {
            ADM.updateCsrfMeta(xhr);
            if (xhr.status === 200) {
                dirty = false;
                showMsg('저장했습니다.', false);
                return;
            }
            if (xhr.status === 409) {
                showMsg('이 버전은 저장할 수 없습니다. 초안 버전만 편집할 수 있습니다.', true);
                return;
            }
            if (xhr.status === 401) {
                alert('세션이 만료되었습니다.');
                location.href = ADM.url('login');
                return;
            }
            if (xhr.status === 403) {
                showMsg('보안 토큰이 유효하지 않습니다. 새로고침 후 다시 시도하세요.', true);
                return;
            }
            showMsg(responseMessage(xhr, '저장에 실패했습니다. (' + xhr.status + ')'), true);
        };
        xhr.onerror = function () {
            showMsg('저장 중 네트워크 오류가 발생했습니다.', true);
        };
        xhr.send(JSON.stringify(payload));
    }

    function addNode(seed) {
        var card = nodeCard(seed);
        nodeEditor.appendChild(card);
        selectedUid = card.dataset.uid;
        if (!startNodeKey) startNodeKey = ensureNodeKey(card);
        dirty = true;
        refreshAll();
        return card;
    }

    function createLinkedNode(option) {
        var label = option.querySelector('[data-field="label"]').value.trim() || '새 답변';
        var key = uniqueNodeKey(label);
        var card = addNode({
            nodeKey: key,
            nodeType: 'ANSWER',
            title: label + ' 안내',
            content: label + '에 대한 안내 문구를 입력하세요.',
            sortOrder: nextSortOrder()
        });
        option.querySelector('[data-field="nextNodeKey"]').value = key;
        selectedUid = card.dataset.uid;
        refreshAll();
        card.querySelector('[data-field="content"]').focus();
    }

    nodeNav.addEventListener('click', function (event) {
        var button = event.target.closest('[data-action="selectNode"]');
        if (!button) return;
        selectedUid = button.getAttribute('data-uid');
        refreshAll();
    });

    nodeNav.addEventListener('dragstart', function (event) {
        var button = event.target.closest('[data-action="selectNode"]');
        if (!button) return;
        dragUid = button.getAttribute('data-uid');
        button.classList.add('is-dragging');
        event.dataTransfer.effectAllowed = 'move';
        event.dataTransfer.setData('text/plain', dragUid);
    });

    nodeNav.addEventListener('dragover', function (event) {
        var button = event.target.closest('[data-action="selectNode"]');
        if (!button || !dragUid) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = 'move';
        Array.prototype.forEach.call(nodeNav.querySelectorAll('.is-drop-before, .is-drop-after'), function (item) {
            item.classList.remove('is-drop-before', 'is-drop-after');
        });
        var rect = button.getBoundingClientRect();
        button.classList.add(event.clientY < rect.top + rect.height / 2 ? 'is-drop-before' : 'is-drop-after');
    });

    nodeNav.addEventListener('dragleave', function (event) {
        var button = event.target.closest('[data-action="selectNode"]');
        if (button) button.classList.remove('is-drop-before', 'is-drop-after');
    });

    nodeNav.addEventListener('drop', function (event) {
        var targetButton = event.target.closest('[data-action="selectNode"]');
        if (!targetButton || !dragUid) return;
        event.preventDefault();
        var source = nodeEditor.querySelector('[data-role="node"][data-uid="' + dragUid + '"]');
        var target = nodeEditor.querySelector('[data-role="node"][data-uid="' + targetButton.getAttribute('data-uid') + '"]');
        if (!source || !target || source === target) return;
        var rect = targetButton.getBoundingClientRect();
        if (event.clientY < rect.top + rect.height / 2) {
            nodeEditor.insertBefore(source, target);
        } else {
            nodeEditor.insertBefore(source, target.nextSibling);
        }
        selectedUid = source.dataset.uid;
        startNodeKey = cards()[0] ? ensureNodeKey(cards()[0]) : '';
        dirty = true;
        refreshAll();
    });

    nodeNav.addEventListener('dragend', function () {
        dragUid = '';
        Array.prototype.forEach.call(nodeNav.querySelectorAll('.is-dragging, .is-drop-before, .is-drop-after'), function (item) {
            item.classList.remove('is-dragging', 'is-drop-before', 'is-drop-after');
        });
    });

    nodeEditor.addEventListener('click', function (event) {
        var action = event.target.getAttribute('data-action');
        if (!action) return;
        var card = event.target.closest('[data-role="node"]');
        if (action === 'removeNode') {
            var removedKey = card.querySelector('[data-field="nodeKey"]').value.trim();
            var uid = card.dataset.uid;
            card.remove();
            if (removedKey === startNodeKey) startNodeKey = '';
            if (uid === selectedUid) selectedUid = cards()[0] ? cards()[0].dataset.uid : '';
            dirty = true;
            refreshAll();
        } else if (action === 'setStartNode') {
            startNodeKey = ensureNodeKey(card);
            dirty = true;
            refreshAll();
        } else if (action === 'addOption') {
            var options = card.querySelector('[data-role="options"]');
            options.appendChild(optionRow({ sortOrder: options.children.length + 1, useYn: 'Y' }));
            dirty = true;
            refreshAll();
        } else if (action === 'removeOption') {
            event.target.closest('[data-role="option"]').remove();
            dirty = true;
            refreshAll();
        } else if (action === 'createLinkedNode') {
            createLinkedNode(event.target.closest('[data-role="option"]'));
        } else if (action === 'addLink') {
            var links = card.querySelector('[data-role="links"]');
            links.appendChild(linkRow({ sortOrder: links.children.length, useYn: 'Y' }));
            dirty = true;
        } else if (action === 'removeLink') {
            event.target.closest('[data-role="link"]').remove();
            dirty = true;
        } else if (action === 'addMetadata') {
            card.querySelector('[data-role="metadataRows"]').appendChild(metadataRow({}));
            dirty = true;
        } else if (action === 'removeMetadata') {
            event.target.closest('[data-role="metadata"]').remove();
            dirty = true;
        }
    });

    nodeEditor.addEventListener('input', function (event) {
        var card = event.target.closest('[data-role="node"]');
        if (card && event.target.getAttribute('data-field') === 'title') {
            var keyInput = card.querySelector('[data-field="nodeKey"]');
            if (!keyInput.value.trim()) keyInput.value = uniqueNodeKey(event.target.value);
        }
        dirty = true;
        refreshAll();
    });

    nodeEditor.addEventListener('change', function () {
        dirty = true;
        refreshAll();
    });

    document.getElementById('btnAddNode').addEventListener('click', function () {
        var isFirst = cards().length === 0;
        var card = addNode({
            nodeKey: uniqueNodeKey(isFirst ? 'start' : 'node'),
            nodeType: isFirst ? 'QUESTION' : 'ANSWER',
            title: isFirst ? '첫 질문' : '새 답변',
            content: isFirst ? '무엇을 도와드릴까요?' : '',
            sortOrder: nextSortOrder()
        });
        card.querySelector('[data-field="title"]').focus();
    });

    document.getElementById('btnSave').addEventListener('click', save);
    document.getElementById('btnPreview').addEventListener('click', function (event) {
        if (dirty && !confirm('저장하지 않은 변경이 있습니다. 미리보기로 이동할까요?')) {
            event.preventDefault();
        }
    });

    load();
})();
