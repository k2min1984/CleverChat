/**************************************************
 * @desc    : Scenario graph edit view (DRAFT only)
 **************************************************/
(function () {
    var root = document.querySelector('.content-area[data-graph-load-url], .admin-content[data-graph-load-url]');
    if (!root) return;

    var loadUrl = root.getAttribute('data-graph-load-url');
    var saveUrl = root.getAttribute('data-graph-save-url');

    var startInput = document.getElementById('startNodeKey');
    var nodeList = document.getElementById('nodeTbody');
    var emptyHelp = document.getElementById('nodeEmptyHelp');
    var msgBox = document.getElementById('graphMsg');
    var dirty = false;

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

    function nodeTypeLabel(value) {
        var labels = {
            QUESTION: '질문',
            ANSWER: '답변',
            BRANCH: '분기',
            END: '종료'
        };
        return labels[value] || value || '노드';
    }

    function parseMetadataRows(metadata) {
        if (!metadata) return [];
        try {
            var parsed = JSON.parse(metadata);
            if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
                return [];
            }
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

    function nodeCard(node) {
        var card = document.createElement('article');
        card.className = 'graph-node-card';
        card.dataset.role = 'node';
        card.innerHTML =
            '<header class="graph-node-card-header">' +
                '<div>' +
                    '<strong data-role="nodeTitle">노드</strong>' +
                    '<span class="badge badge-default" data-role="nodeTypeBadge">질문</span>' +
                '</div>' +
                '<button type="button" class="btn btn-xs btn-secondary red" data-action="removeNode" aria-label="노드 삭제">삭제</button>' +
            '</header>' +
            '<div class="graph-field-grid">' +
                '<label class="graph-field">노드 키<input type="text" data-field="nodeKey" maxlength="80" aria-label="노드 키" placeholder="예: start"></label>' +
                '<label class="graph-field">노드 유형<select data-field="nodeType" aria-label="노드 유형">' +
                    '<option value="QUESTION">질문</option>' +
                    '<option value="ANSWER">답변</option>' +
                    '<option value="BRANCH">분기</option>' +
                    '<option value="END">종료</option>' +
                '</select></label>' +
                '<label class="graph-field">정렬 순서<input type="number" data-field="sortOrder" min="1" step="1" aria-label="노드 정렬 순서"></label>' +
                '<label class="graph-field graph-field-wide">제목<input type="text" data-field="title" maxlength="150" aria-label="노드 제목" placeholder="사용자에게 보이는 제목"></label>' +
                '<label class="graph-field graph-field-wide">내용<textarea data-field="content" maxlength="10000" rows="4" aria-label="노드 내용" placeholder="챗봇이 안내할 문구를 입력하세요."></textarea></label>' +
            '</div>' +
            '<section class="graph-metadata-box" aria-label="메타데이터">' +
                '<div class="graph-subheader">' +
                    '<div><strong>메타데이터</strong><p class="help-text">선택 항목입니다. JSON을 직접 쓰지 않고 항목명과 값으로 입력하면 저장 시 자동 변환됩니다.</p></div>' +
                    '<button type="button" class="btn btn-xs btn-secondary" data-action="addMetadata" aria-label="메타데이터 추가">항목 추가</button>' +
                '</div>' +
                '<div data-role="metadataRows" class="graph-metadata-list"></div>' +
            '</section>' +
            '<section class="graph-options-box" aria-label="선택지 목록">' +
                '<div class="graph-subheader">' +
                    '<div><strong>선택지</strong><p class="help-text">버튼 문구와 이동할 다음 노드 키를 입력합니다. 종료 노드는 선택지를 둘 수 없습니다.</p></div>' +
                    '<button type="button" class="btn btn-xs btn-secondary" data-action="addOption" aria-label="선택지 추가">선택지 추가</button>' +
                '</div>' +
                '<div data-role="options" class="graph-option-list"></div>' +
            '</section>';

        card.querySelector('[data-field="nodeKey"]').value = node.nodeKey || '';
        card.querySelector('[data-field="nodeType"]').value = node.nodeType || 'QUESTION';
        card.querySelector('[data-field="title"]').value = node.title || '';
        card.querySelector('[data-field="content"]').value = node.content || '';
        card.querySelector('[data-field="sortOrder"]').value = node.sortOrder || 1;
        parseMetadataRows(node.metadata).forEach(function (item) {
            card.querySelector('[data-role="metadataRows"]').appendChild(metadataRow(item));
        });
        (node.options || []).forEach(function (op) {
            card.querySelector('[data-role="options"]').appendChild(optionRow(op));
        });
        refreshNodeSummary(card);
        return card;
    }

    function optionRow(op) {
        var row = document.createElement('div');
        row.className = 'graph-option-row';
        row.dataset.role = 'option';
        row.innerHTML =
            '<label>버튼 문구<input type="text" data-field="label" maxlength="150" aria-label="선택지 버튼 문구"></label>' +
            '<label>다음 노드 키<input type="text" data-field="nextNodeKey" maxlength="80" aria-label="다음 노드 키"></label>' +
            '<label>조건식<input type="text" data-field="conditionExpr" maxlength="500" aria-label="선택지 조건식" placeholder="선택"></label>' +
            '<label>순서<input type="number" data-field="sortOrder" min="1" step="1" aria-label="선택지 정렬 순서"></label>' +
            '<label class="graph-check"><input type="checkbox" data-field="enabled" aria-label="선택지 사용 여부"> 사용</label>' +
            '<button type="button" class="btn btn-xs btn-secondary red" data-action="removeOption" aria-label="선택지 삭제">삭제</button>';
        row.querySelector('[data-field="label"]').value = op.label || '';
        row.querySelector('[data-field="nextNodeKey"]').value = op.nextNodeKey || '';
        row.querySelector('[data-field="conditionExpr"]').value = op.conditionExpr || '';
        row.querySelector('[data-field="sortOrder"]').value = op.sortOrder || 1;
        row.querySelector('[data-field="enabled"]').checked = op.enabled !== false;
        return row;
    }

    function refreshNodeSummary(card) {
        var key = card.querySelector('[data-field="nodeKey"]').value.trim();
        var type = card.querySelector('[data-field="nodeType"]').value;
        card.querySelector('[data-role="nodeTitle"]').textContent = key ? '노드: ' + key : '새 노드';
        card.querySelector('[data-role="nodeTypeBadge"]').textContent = nodeTypeLabel(type);
    }

    function collectMetadata(card) {
        var metadata = {};
        Array.prototype.forEach.call(card.querySelectorAll('[data-role="metadata"]'), function (row) {
            var key = row.querySelector('[data-field="metadataKey"]').value.trim();
            var value = row.querySelector('[data-field="metadataValue"]').value.trim();
            if (key) {
                metadata[key] = value;
            }
        });
        return Object.keys(metadata).length ? JSON.stringify(metadata) : '{}';
    }

    function render(graph) {
        var nodes = graph.nodes || [];
        startInput.value = graph.startNodeKey || '';
        nodeList.innerHTML = '';
        nodes.forEach(function (n) {
            nodeList.appendChild(nodeCard(n));
        });
        emptyHelp.hidden = nodes.length > 0;
        dirty = false;
    }

    function collect() {
        var nodes = [];
        Array.prototype.forEach.call(nodeList.querySelectorAll('[data-role="node"]'), function (card) {
            var opts = [];
            Array.prototype.forEach.call(card.querySelectorAll('[data-role="option"]'), function (orow) {
                opts.push({
                    label: orow.querySelector('[data-field="label"]').value.trim(),
                    nextNodeKey: orow.querySelector('[data-field="nextNodeKey"]').value.trim() || null,
                    conditionExpr: orow.querySelector('[data-field="conditionExpr"]').value.trim() || null,
                    sortOrder: parseInt(orow.querySelector('[data-field="sortOrder"]').value, 10) || 1,
                    enabled: orow.querySelector('[data-field="enabled"]').checked
                });
            });
            nodes.push({
                nodeKey: card.querySelector('[data-field="nodeKey"]').value.trim(),
                nodeType: card.querySelector('[data-field="nodeType"]').value,
                title: card.querySelector('[data-field="title"]').value.trim(),
                content: card.querySelector('[data-field="content"]').value,
                sortOrder: parseInt(card.querySelector('[data-field="sortOrder"]').value, 10) || 1,
                metadata: collectMetadata(card),
                options: opts
            });
        });
        return { startNodeKey: startInput.value.trim(), nodes: nodes };
    }

    function validatePayload(payload) {
        if (!payload.startNodeKey) {
            return '시작 노드 키를 입력하세요.';
        }
        if (payload.nodes.length === 0) {
            return '저장하려면 노드를 1개 이상 추가하세요.';
        }
        var nodeKeys = {};
        for (var i = 0; i < payload.nodes.length; i++) {
            var node = payload.nodes[i];
            if (!node.nodeKey) {
                return (i + 1) + '번째 노드의 노드 키를 입력하세요.';
            }
            if (!/^[A-Za-z0-9_-]{1,80}$/.test(node.nodeKey)) {
                return '노드 키는 영문, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다.';
            }
            if (nodeKeys[node.nodeKey]) {
                return '중복된 노드 키가 있습니다.';
            }
            nodeKeys[node.nodeKey] = true;
            if (!node.title) {
                return (i + 1) + '번째 노드의 제목을 입력하세요.';
            }
            if (node.nodeType === 'END' && node.options.length > 0) {
                return '종료 노드에는 선택지를 등록할 수 없습니다.';
            }
            for (var j = 0; j < node.options.length; j++) {
                var option = node.options[j];
                if (!option.label) {
                    return (i + 1) + '번째 노드의 ' + (j + 1) + '번째 선택지 문구를 입력하세요.';
                }
                if (option.nextNodeKey && !nodeKeys[option.nextNodeKey]) {
                    // The target may be declared later, so validate after collecting all keys below.
                }
            }
        }
        if (!nodeKeys[payload.startNodeKey]) {
            return '시작 노드 키가 노드 목록에 없습니다.';
        }
        for (var n = 0; n < payload.nodes.length; n++) {
            for (var o = 0; o < payload.nodes[n].options.length; o++) {
                var nextNodeKey = payload.nodes[n].options[o].nextNodeKey;
                if (nextNodeKey && !nodeKeys[nextNodeKey]) {
                    return '선택지의 다음 노드 키가 노드 목록에 없습니다: ' + nextNodeKey;
                }
            }
        }
        return null;
    }

    function validateMetadataJson(payload) {
        for (var i = 0; i < payload.nodes.length; i++) {
            var metadata = payload.nodes[i].metadata;
            if (!metadata) continue;
            try {
                JSON.parse(metadata);
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
                showMsg(responseMessage(xhr, '그래프를 불러오지 못했습니다. (' + xhr.status + ')'), true);
                return;
            }
            try {
                var env = JSON.parse(xhr.responseText);
                render(env.data || { startNodeKey: null, nodes: [] });
                clearMsg();
            } catch (e) {
                showMsg('그래프 응답을 해석하지 못했습니다.', true);
            }
        };
        xhr.onerror = function () {
            showMsg('그래프를 불러오는 중 네트워크 오류가 발생했습니다.', true);
        };
        xhr.send();
    }

    function save() {
        var payload = collect();
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

    nodeList.addEventListener('click', function (e) {
        var act = e.target.getAttribute('data-action');
        if (act === 'removeNode') {
            e.target.closest('[data-role="node"]').remove();
            emptyHelp.hidden = nodeList.children.length > 0;
            dirty = true;
        } else if (act === 'addOption') {
            var ob = e.target.closest('[data-role="node"]').querySelector('[data-role="options"]');
            ob.appendChild(optionRow({ sortOrder: ob.children.length + 1, enabled: true }));
            dirty = true;
        } else if (act === 'removeOption') {
            e.target.closest('[data-role="option"]').remove();
            dirty = true;
        } else if (act === 'addMetadata') {
            var mb = e.target.closest('[data-role="node"]').querySelector('[data-role="metadataRows"]');
            mb.appendChild(metadataRow({}));
            dirty = true;
        } else if (act === 'removeMetadata') {
            e.target.closest('[data-role="metadata"]').remove();
            dirty = true;
        }
    });
    nodeList.addEventListener('input', function (e) {
        var node = e.target.closest('[data-role="node"]');
        if (node) refreshNodeSummary(node);
        dirty = true;
        emptyHelp.hidden = nodeList.children.length > 0;
    });
    nodeList.addEventListener('change', function (e) {
        var node = e.target.closest('[data-role="node"]');
        if (node) refreshNodeSummary(node);
        dirty = true;
    });
    startInput.addEventListener('input', function () {
        dirty = true;
    });

    document.getElementById('btnAddNode').addEventListener('click', function () {
        nodeList.appendChild(nodeCard({ nodeType: 'QUESTION', sortOrder: nodeList.children.length + 1 }));
        emptyHelp.hidden = true;
        dirty = true;
    });
    document.getElementById('btnSave').addEventListener('click', save);
    document.getElementById('btnPreview').addEventListener('click', function (e) {
        if (dirty && !confirm('저장하지 않은 변경이 있습니다. 미리보기로 이동할까요?')) {
            e.preventDefault();
        }
    });

    load();
})();
