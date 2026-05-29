var ADM = ADM || {};

(function(){
    var ICON_REMOVE = '<svg viewBox="0 0 24 24" fill="none" stroke="#e11d48" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6"></path><path d="M14 11v6"></path><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"></path></svg>';
    var ICON_SELECT = '<svg viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"></path></svg>';

    function ready(fn){
        if(document.readyState === 'loading'){
            document.addEventListener('DOMContentLoaded', fn);
        }else{
            fn();
        }
    }

    function parseJson(xhr, fallback){
        try { return JSON.parse(xhr.responseText); } catch(e) { return fallback; }
    }

    function yes(value){
        return value === 'Y';
    }

    function encode(key, value){
        return ADM.encodeParam(key, value == null ? '' : value);
    }

    function initTree(root){
        var type = root.dataset.legacyTree;
        var isCode = type === 'code';
        if(!window.ADM || !ADM.TreeList){ return; }
        new ADM.TreeList({
            colCount: 3,
            prefix: 'tree',
            tblPrefix: type,
            entityNm: isCode ? '코드' : '메뉴',
            layerWidth: 960,
            layerHeight: isCode ? 440 : 600,
            fields: isCode
                ? { no:'codeNo', pNo:'pCodeNo', nm:'codeNm', depth:'codeDepth' }
                : { no:'menuNo', pNo:'pMenuNo', nm:'menuNm', depth:'menuDepth' },
            urls: {
                select: root.dataset.selectUrl,
                regist: root.dataset.registUrl,
                remove: root.dataset.removeUrl,
                updateNm: root.dataset.updateNmUrl,
                updateSort: root.dataset.updateSortUrl,
                layer: root.dataset.layerUrl,
                refresh: root.dataset.refreshUrl
            },
            deleteParams: isCode ? undefined : function(pNo, no){
                return { menuNo: no };
            }
        });
    }

    function initAuth(root){
        var urls = {
            select: root.dataset.authSelectUrl,
            regist: root.dataset.authRegistUrl,
            remove: root.dataset.authRemoveUrl,
            menuSelect: root.dataset.menuSelectUrl,
            menuRegist: root.dataset.menuRegistUrl
        };
        var selectedAuthNo = '';
        var selectedAuthNm = '';

        var authList = document.getElementById('authList');
        var authCount = document.getElementById('authCount');
        var newAuthNm = document.getElementById('newAuthNm');
        var selectedName = document.getElementById('selectedAuthNm');
        var menuTree = document.getElementById('menuTree');
        var btnAdd = document.getElementById('btnAddAuth');
        var btnSave = document.getElementById('btnSaveMenuAuth');

        if(btnAdd){ btnAdd.addEventListener('click', addAuth); }
        if(btnSave){ btnSave.addEventListener('click', saveMenuAuth); }
        if(newAuthNm){
            newAuthNm.addEventListener('keydown', function(event){
                if(event.key === 'Enter'){ addAuth(); }
            });
        }

        loadAuthList();

        function loadAuthList(){
            ADM.ajaxPost(urls.select, '', function(xhr){
                var list = parseJson(xhr, []);
                authList.innerHTML = '';
                authCount.textContent = list.length;
                if(list.length === 0){
                    authList.innerHTML = '<div class="tree-empty">등록된 권한이 없습니다.</div>';
                    return;
                }
                list.forEach(function(item, index){
                    var no = String(item.authNo || item.id || '');
                    var nm = String(item.authNm || item.code || '');
                    var row = document.createElement('div');
                    row.className = 'auth-item' + (no === selectedAuthNo ? ' active' : '');
                    row.dataset.no = no;
                    row.addEventListener('click', function(){ selectAuth(no, nm); });

                    var name = document.createElement('span');
                    name.className = 'auth-nm';
                    name.textContent = nm;
                    row.appendChild(name);

                    var acts = document.createElement('span');
                    acts.className = 'auth-acts';
                    var remove = document.createElement('button');
                    remove.type = 'button';
                    remove.className = 'btn-icon danger';
                    remove.innerHTML = ICON_REMOVE;
                    remove.title = '삭제';
                    remove.setAttribute('aria-label', nm + ' 권한 삭제');
                    remove.addEventListener('click', function(event){
                        event.stopPropagation();
                        removeAuth(no);
                    });
                    acts.appendChild(remove);

                    var select = document.createElement('button');
                    select.type = 'button';
                    select.className = 'btn-icon select';
                    select.innerHTML = ICON_SELECT;
                    select.title = '선택';
                    select.setAttribute('aria-label', nm + ' 권한 선택');
                    select.addEventListener('click', function(event){
                        event.stopPropagation();
                        selectAuth(no, nm);
                    });
                    acts.appendChild(select);
                    row.appendChild(acts);

                    authList.appendChild(row);
                    if(index === 0 && !selectedAuthNo){
                        selectAuth(no, nm);
                    }
                });
            });
        }

        function addAuth(){
            var value = (newAuthNm.value || '').trim();
            if(!value){
                alert('권한명을 입력해 주세요.');
                newAuthNm.focus();
                return;
            }
            ADM.ajaxPost(urls.regist, encode('authNm', value), function(xhr){
                if(xhr.status === 200 && xhr.responseText.indexOf('true') > -1){
                    newAuthNm.value = '';
                    loadAuthList();
                }else{
                    alert('등록 중 오류가 발생했습니다.');
                }
            });
        }

        function selectAuth(authNo, authNm){
            selectedAuthNo = authNo;
            selectedAuthNm = authNm;
            selectedName.textContent = authNm;
            selectedName.classList.add('is-selected');
            document.querySelectorAll('.auth-item').forEach(function(item){
                item.classList.toggle('active', item.dataset.no === authNo);
            });
            loadMenuTree(authNo);
        }

        function removeAuth(authNo){
            if(!confirm('해당 권한을 삭제하시겠습니까?\n해당 권한의 메뉴 권한도 함께 삭제됩니다.')){ return; }
            ADM.ajaxPost(urls.remove, encode('authNo', authNo), function(xhr){
                if(xhr.status === 200 && xhr.responseText.indexOf('true') > -1){
                    if(selectedAuthNo === authNo){
                        selectedAuthNo = '';
                        selectedAuthNm = '';
                        selectedName.textContent = '권한을 선택해 주세요';
                        selectedName.classList.remove('is-selected');
                        menuTree.innerHTML = '<div class="tree-empty">좌측에서 권한을 선택하면 메뉴 권한을 설정할 수 있습니다.</div>';
                    }
                    loadAuthList();
                }else{
                    alert('삭제 중 오류가 발생했습니다.');
                }
            });
        }

        function loadMenuTree(authNo){
            menuTree.innerHTML = '<div class="tree-empty">로딩 중...</div>';
            ADM.ajaxPost(urls.menuSelect, encode('authNo', authNo), function(xhr){
                var list = parseJson(xhr, []);
                if(list.length === 0){
                    menuTree.innerHTML = '<div class="tree-empty">등록된 메뉴가 없습니다.</div>';
                    return;
                }
                renderTree(list);
            });
        }

        function renderTree(list){
            var orderedList = buildTreeOrder(list);
            var childMap = buildChildMap(list);
            var grid = document.createElement('div');
            grid.className = 'modern-tree-grid';

            var header = document.createElement('div');
            header.className = 'mtg-header';
            header.innerHTML = '<div class="mtg-col-name"></div><div class="mtg-col-perms"><span>조회</span><span>등록</span><span>수정</span><span>삭제</span><span>처리</span></div>';

            var allWrap = header.querySelector('.mtg-col-name');
            var toggleSpacer = document.createElement('span');
            toggleSpacer.className = 'mtg-toggle-spacer';
            allWrap.appendChild(toggleSpacer);

            var checkAll = document.createElement('input');
            checkAll.type = 'checkbox';
            checkAll.id = 'checkAll';
            checkAll.className = 'tree-check';
            checkAll.setAttribute('aria-label', '전체 선택');
            checkAll.addEventListener('change', function(){ toggleAll(checkAll.checked); });
            var allLabel = document.createElement('label');
            allLabel.htmlFor = 'checkAll';
            allLabel.textContent = '전체 선택 목록';
            allWrap.appendChild(checkAll);
            allWrap.appendChild(allLabel);
            grid.appendChild(header);

            orderedList.forEach(function(item){
                var menuNo = String(item.menuNo || '');
                var hasChildren = (childMap[menuNo] || []).length > 0;
                grid.appendChild(renderMenuRow(item, hasChildren));
            });

            menuTree.innerHTML = '';
            menuTree.appendChild(grid);
            updateTreeVisibility();
        }

        function buildChildMap(list){
            return list.reduce(function(map, item){
                var parentNo = String(item.pMenuNo || '');
                if(!map[parentNo]){ map[parentNo] = []; }
                map[parentNo].push(item);
                return map;
            }, {});
        }

        function buildTreeOrder(list){
            var childMap = buildChildMap(list);
            var visited = {};
            var ordered = [];

            function appendBranch(item){
                var menuNo = String(item.menuNo || '');
                if(!menuNo || visited[menuNo]){ return; }
                visited[menuNo] = true;
                ordered.push(item);
                (childMap[menuNo] || []).forEach(appendBranch);
            }

            (childMap[''] || []).forEach(appendBranch);
            list.forEach(function(item){
                appendBranch(item);
            });
            return ordered;
        }

        function renderMenuRow(item, hasChildren){
            var menuNo = String(item.menuNo || '');
            var pMenuNo = String(item.pMenuNo || '');
            var menuNm = String(item.menuNm || '');
            var depth = parseInt(item.depth || item.menuDepth || '1', 10);

            var row = document.createElement('div');
            row.className = 'mtg-row tree-node';
            row.dataset.menuno = menuNo;
            row.dataset.pmenuno = pMenuNo;
            row.dataset.depth = String(depth);
            row.dataset.expanded = 'false';
            row.dataset.hasChildren = hasChildren ? 'true' : 'false';

            var nameCol = document.createElement('div');
            nameCol.className = 'mtg-col-name';
            for(var i = 1; i < depth; i++){
                var indent = document.createElement('span');
                indent.className = 'mtg-indent';
                nameCol.appendChild(indent);
            }
            var toggle = document.createElement('button');
            toggle.type = 'button';
            toggle.className = 'mtg-toggle';
            toggle.disabled = !hasChildren;
            toggle.setAttribute('aria-label', menuNm + ' 하위 메뉴 펼치기');
            toggle.setAttribute('aria-expanded', 'false');
            toggle.innerHTML = '<svg viewBox="0 0 24 24" aria-hidden="true"><polyline points="9 18 15 12 9 6"></polyline></svg>';
            toggle.addEventListener('click', function(event){
                event.stopPropagation();
                toggleMenuRow(row);
            });
            nameCol.appendChild(toggle);

            if(depth > 1){
                var icon = document.createElement('span');
                icon.className = 'mtg-depth-icon';
                icon.textContent = 'ㄴ';
                nameCol.appendChild(icon);
            }
            var nodeCheck = document.createElement('input');
            nodeCheck.type = 'checkbox';
            nodeCheck.className = 'tree-check';
            nodeCheck.checked = yes(item.selectYn);
            nodeCheck.setAttribute('aria-label', menuNm + ' 메뉴 선택');
            nodeCheck.addEventListener('change', function(){ checkNode(nodeCheck); });
            nameCol.appendChild(nodeCheck);

            var label = document.createElement('span');
            label.className = 'mtg-menu-nm';
            label.textContent = menuNm;
            if(hasChildren){
                label.setAttribute('role', 'button');
                label.setAttribute('tabindex', '0');
                label.setAttribute('aria-expanded', 'false');
                label.addEventListener('click', function(){ toggleMenuRow(row); });
                label.addEventListener('keydown', function(event){
                    if(event.key === 'Enter' || event.key === ' '){
                        event.preventDefault();
                        toggleMenuRow(row);
                    }
                });
            }
            nameCol.appendChild(label);
            row.appendChild(nameCol);

            var perms = document.createElement('div');
            perms.className = 'perm-group';
            [
                ['select', item.selectYn, '조회'],
                ['insert', item.insertYn, '등록'],
                ['update', item.updateYn, '수정'],
                ['delete', item.deleteYn, '삭제'],
                ['proc', item.procYn, '처리']
            ].forEach(function(perm){
                var wrap = document.createElement('label');
                var input = document.createElement('input');
                input.type = 'checkbox';
                input.className = 'perm-check';
                input.dataset.perm = perm[0];
                input.checked = yes(perm[1]);
                input.setAttribute('aria-label', menuNm + ' ' + perm[2] + ' 권한');
                wrap.appendChild(input);
                perms.appendChild(wrap);
            });
            row.appendChild(perms);
            return row;
        }

        function toggleMenuRow(row){
            if(row.dataset.hasChildren !== 'true'){ return; }
            row.dataset.expanded = row.dataset.expanded === 'true' ? 'false' : 'true';
            var expanded = row.dataset.expanded === 'true';
            var toggle = row.querySelector('.mtg-toggle');
            var label = row.querySelector('.mtg-menu-nm');
            if(toggle){
                toggle.setAttribute('aria-expanded', String(expanded));
                toggle.setAttribute('aria-label', label.textContent + (expanded ? ' 하위 메뉴 접기' : ' 하위 메뉴 펼치기'));
            }
            if(label){ label.setAttribute('aria-expanded', String(expanded)); }
            updateTreeVisibility();
        }

        function updateTreeVisibility(){
            var expandedByMenuNo = {};
            menuTree.querySelectorAll('.tree-node').forEach(function(row){
                expandedByMenuNo[row.dataset.menuno] = row.dataset.expanded === 'true';
            });
            menuTree.querySelectorAll('.tree-node').forEach(function(row){
                var parentNo = row.dataset.pmenuno || '';
                var visible = true;
                while(parentNo){
                    var parent = menuTree.querySelector('.tree-node[data-menuno="' + parentNo + '"]');
                    if(!parent || expandedByMenuNo[parentNo] !== true){
                        visible = false;
                        break;
                    }
                    parentNo = parent.dataset.pmenuno || '';
                }
                row.classList.toggle('is-hidden', !visible);
            });
        }

        function checkNode(cb){
            var checked = cb.checked;
            var node = cb.closest('.tree-node');
            var depth = parseInt(node.dataset.depth, 10);
            node.querySelectorAll('.perm-group input[type=checkbox]').forEach(function(input){
                input.checked = checked;
            });
            var sibling = node.nextElementSibling;
            while(sibling && sibling.classList.contains('tree-node')){
                var siblingDepth = parseInt(sibling.dataset.depth, 10);
                if(siblingDepth <= depth){ break; }
                sibling.querySelector('.tree-check').checked = checked;
                sibling.querySelectorAll('.perm-group input[type=checkbox]').forEach(function(input){
                    input.checked = checked;
                });
                sibling = sibling.nextElementSibling;
            }
            if(checked){ checkParents(node); }
        }

        function checkParents(node){
            var parentNo = node.dataset.pmenuno;
            if(!parentNo){ return; }
            var parent = document.querySelector('.tree-node[data-menuno="' + parentNo + '"]');
            if(parent){
                parent.querySelector('.tree-check').checked = true;
                var selectPerm = parent.querySelector('[data-perm="select"]');
                if(selectPerm){ selectPerm.checked = true; }
                checkParents(parent);
            }
        }

        function toggleAll(checked){
            menuTree.querySelectorAll('.tree-node').forEach(function(node){
                node.querySelector('.tree-check').checked = checked;
                node.querySelectorAll('.perm-group input[type=checkbox]').forEach(function(input){
                    input.checked = checked;
                });
            });
        }

        function saveMenuAuth(){
            if(!selectedAuthNo){
                alert('권한을 먼저 선택해 주세요.');
                return;
            }
            if(!confirm('메뉴 권한을 저장하시겠습니까?')){ return; }
            var params = [encode('authNo', selectedAuthNo)];
            menuTree.querySelectorAll('.tree-node').forEach(function(node){
                var checked = node.querySelector('.tree-check').checked;
                if(!checked){ return; }
                params.push(encode('arrMenuNo', node.dataset.menuno));
                params.push(encode('arrPMenuNo', node.dataset.pmenuno || ''));
                ['select', 'insert', 'update', 'delete', 'proc'].forEach(function(key){
                    var input = node.querySelector('[data-perm="' + key + '"]');
                    var value = input && input.checked ? 'Y' : 'N';
                    if(key === 'select'){ params.push(encode('arrSelectYn', value)); }
                    if(key === 'insert'){ params.push(encode('arrInsertYn', value)); }
                    if(key === 'update'){ params.push(encode('arrUpdateYn', value)); }
                    if(key === 'delete'){ params.push(encode('arrDeleteYn', value)); }
                    if(key === 'proc'){ params.push(encode('arrProcYn', value)); }
                });
            });
            ADM.ajaxPost(urls.menuRegist, params.join('&'), function(xhr){
                if(xhr.status === 200 && xhr.responseText.indexOf('true') > -1){
                    alert('메뉴 권한이 저장되었습니다.');
                    loadMenuTree(selectedAuthNo);
                }else{
                    alert('저장 중 오류가 발생했습니다.');
                }
            });
        }
    }

    ready(function(){
        document.querySelectorAll('[data-legacy-tree]').forEach(initTree);
        document.querySelectorAll('[data-legacy-auth]').forEach(initAuth);
    });
})();
