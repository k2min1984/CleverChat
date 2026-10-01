(function () {
    var root = document.querySelector('.content-area[data-graph-load-url]');
    if (!root) return;

    var loadUrl = root.getAttribute('data-graph-load-url');
    var nodeNav = document.getElementById('nodeNav');
    var nodeDetail = document.getElementById('nodeDetail');
    var emptyHelp = document.getElementById('nodeEmptyHelp');
    var selectHelp = document.getElementById('nodeSelectHelp');
    var msgBox = document.getElementById('graphMsg');

    var graph = { startNodeKey: '', nodes: [] };
    var selectedKey = '';

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function showMsg(text, isError) {
        msgBox.hidden = false;
        msgBox.textContent = text;
        msgBox.setAttribute('role', isError ? 'alert' : 'status');
        msgBox.classList.toggle('is-error', !!isError);
        msgBox.style.color = isError ? '#dc2626' : '#16a34a';
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

    function nodeByKey(key) {
        return (graph.nodes || []).find(function (node) {
            return node.nodeKey === key;
        });
    }

    function titleForKey(key) {
        var node = nodeByKey(key);
        if (!node) return key || '상담 종료';
        return (node.title || node.nodeKey || '노드') + (node.nodeKey ? ' (' + node.nodeKey + ')' : '');
    }

    function renderNav() {
        var nodes = graph.nodes || [];
        nodeNav.innerHTML = nodes.map(function (node, index) {
            var isSelected = node.nodeKey === selectedKey;
            var isStart = node.nodeKey && node.nodeKey === graph.startNodeKey;
            var linkCount = (node.links || []).length;
            var optionCount = (node.options || []).length;
            return '<button type="button" class="graph-node-nav-item is-readonly'
                + (isSelected ? ' is-selected' : '')
                + (isStart ? ' is-start' : '')
                + '" data-node-key="' + escapeHtml(node.nodeKey) + '" role="listitem" aria-pressed="' + (isSelected ? 'true' : 'false') + '">'
                + '<span class="graph-node-nav-index">' + (index + 1) + '</span>'
                + '<span class="graph-node-nav-main">'
                + '<strong>' + escapeHtml(node.title || node.nodeKey || '노드') + '</strong>'
                + '<span>' + escapeHtml(node.nodeKey || '관리키 없음') + '</span>'
                + '</span>'
                + '<span class="graph-node-nav-meta">'
                + '<b class="graph-node-type" data-type="' + escapeHtml(node.nodeType) + '">' + escapeHtml(nodeTypeLabel(node.nodeType)) + '</b>'
                + '<small class="graph-node-link-count">링크 ' + linkCount + '</small>'
                + '<small class="graph-node-opt-count">선택지 ' + optionCount + '</small>'
                + '</span>'
                + '</button>';
        }).join('');
        emptyHelp.hidden = nodes.length > 0;
        selectHelp.hidden = nodes.length > 0;
    }

    function optionList(node) {
        var options = node.options || [];
        if (node.nodeType === 'END') {
            return '<span class="graph-link-empty">종료 노드입니다.</span>';
        }
        if (options.length === 0) {
            return '<span class="graph-link-empty">연결된 선택지가 없습니다.</span>';
        }
        return options.map(function (option) {
            var next = option.nextNodeKey ? titleForKey(option.nextNodeKey) : '상담 종료';
            return '<div class="graph-readonly-option">'
                + '<strong>' + escapeHtml(option.label || '선택지') + '</strong>'
                + '<span aria-hidden="true">→</span>'
                + '<span>' + escapeHtml(next) + '</span>'
                + '</div>';
        }).join('');
    }

    function linkList(node) {
        var links = node.links || [];
        if (links.length === 0) {
            return '<span class="graph-link-empty">등록된 바로가기가 없습니다.</span>';
        }
        return links.map(function (link) {
            var type = link.linkType || 'EXTERNAL';
            var useYn = link.useYn === 'N' ? '미사용' : '사용';
            return '<div class="graph-readonly-link">'
                + '<strong>' + escapeHtml(link.label || '바로가기') + '</strong>'
                + '<a href="' + escapeHtml(link.url || '#') + '" target="_blank" rel="noopener noreferrer">' + escapeHtml(link.url || '-') + '</a>'
                + '<span>' + escapeHtml(type) + ' · ' + escapeHtml(useYn) + '</span>'
                + '</div>';
        }).join('');
    }

    function metadataPreview(metadata) {
        if (!metadata || metadata === '{}') {
            return '<span class="help-text">등록된 메타데이터가 없습니다.</span>';
        }
        return '<pre class="code-preview">' + escapeHtml(metadata) + '</pre>';
    }

    function renderDetail() {
        var node = nodeByKey(selectedKey);
        if (!node) {
            nodeDetail.innerHTML = '';
            selectHelp.hidden = false;
            return;
        }
        selectHelp.hidden = true;
        nodeDetail.innerHTML =
            '<article class="graph-node-card is-readonly' + (node.nodeKey === graph.startNodeKey ? ' is-start' : '') + '">'
            + '<header class="graph-node-card-header">'
            + '<div class="graph-node-title-wrap">'
            + '<span class="graph-node-index">' + ((graph.nodes || []).indexOf(node) + 1) + '</span>'
            + '<div><strong>' + escapeHtml(node.title || node.nodeKey || '노드') + '</strong>'
            + '<div class="graph-node-badges">'
            + '<span class="badge graph-node-type" data-type="' + escapeHtml(node.nodeType) + '">' + escapeHtml(nodeTypeLabel(node.nodeType)) + '</span>'
            + (node.nodeKey === graph.startNodeKey ? '<span class="badge badge-success">시작</span>' : '')
            + '</div></div></div>'
            + '</header>'
            + '<div class="graph-readonly-body">'
            + '<dl class="graph-readonly-fields">'
            + '<dt>관리키</dt><dd>' + escapeHtml(node.nodeKey || '-') + '</dd>'
            + '<dt>정렬 순서</dt><dd>' + escapeHtml(node.sortOrder || '-') + '</dd>'
            + '<dt>챗봇 문구</dt><dd><pre class="code-preview">' + escapeHtml(node.content || '-') + '</pre></dd>'
            + '<dt>바로가기 링크</dt><dd>' + linkList(node) + '</dd>'
            + '<dt>선택지와 연결</dt><dd>' + optionList(node) + '</dd>'
            + '<dt>고급 설정</dt><dd>' + metadataPreview(node.metadata) + '</dd>'
            + '</dl>'
            + '</div>'
            + '</article>';
    }

    function render() {
        var nodes = graph.nodes || [];
        selectedKey = selectedKey || graph.startNodeKey || (nodes[0] && nodes[0].nodeKey) || '';
        renderNav();
        renderDetail();
    }

    nodeNav.addEventListener('click', function (event) {
        var button = event.target.closest('[data-node-key]');
        if (!button) return;
        selectedKey = button.getAttribute('data-node-key');
        render();
    });

    var xhr = new XMLHttpRequest();
    xhr.open('GET', loadUrl, true);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.withCredentials = true;
    xhr.onload = function () {
        if (xhr.status !== 200) {
            showMsg('시나리오 버전 정보를 불러오지 못했습니다. (' + xhr.status + ')', true);
            return;
        }
        try {
            var env = JSON.parse(xhr.responseText);
            graph = env.data || { startNodeKey: '', nodes: [] };
            render();
        } catch (e) {
            showMsg('시나리오 버전 응답을 해석하지 못했습니다.', true);
        }
    };
    xhr.onerror = function () {
        showMsg('시나리오 버전 정보를 불러오는 중 네트워크 오류가 발생했습니다.', true);
    };
    xhr.send();
})();
