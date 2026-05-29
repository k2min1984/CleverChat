var ADM = ADM || {};

ADM.TreeList = function(config){
    this.cfg = config || {};
    this.colCount = this.cfg.colCount || 3;
    this.prefix = this.cfg.prefix || 'tree';
    this.tblPrefix = this.cfg.tblPrefix || this.prefix;
    this.fields = this.cfg.fields || { no:'id', pNo:'parentId', nm:'name', depth:'depth' };
    this.urls = this.cfg.urls || {};
    this.editClass = this.cfg.editClass || (this.prefix + '-edit-input');
    this.layerWidth = this.cfg.layerWidth || 960;
    this.layerHeight = this.cfg.layerHeight || 500;
    this.entityNm = this.cfg.entityNm || '항목';
    this.deleteParams = this.cfg.deleteParams || function(pNo, no){
        var params = {};
        params[this.fields.pNo] = pNo;
        params[this.fields.no] = no;
        return params;
    }.bind(this);
    this.registParams = this.cfg.registParams || function(pNo, nm, depth){
        var params = {};
        params[this.fields.pNo] = pNo;
        params[this.fields.nm] = nm;
        params[this.fields.depth] = depth;
        return params;
    }.bind(this);
    this.icons = Object.assign({
        setting: '<svg viewBox="0 0 24 24" fill="none" stroke="#4f46e5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06A1.65 1.65 0 0 0 15 19.4a1.65 1.65 0 0 0-1 .6 1.65 1.65 0 0 0-.33 1.82V22a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 20.6a1.65 1.65 0 0 0-1.82-.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.6 15a1.65 1.65 0 0 0-.6-1 1.65 1.65 0 0 0-1.82-.33H2a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 3.4 9a1.65 1.65 0 0 0 .33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-.6A1.65 1.65 0 0 0 10.33 2.18V2a2 2 0 1 1 4 0v.09A1.65 1.65 0 0 0 15 3.4a1.65 1.65 0 0 0 1.82.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 .6 1 1.65 1.65 0 0 0 1.82.33H22a2 2 0 1 1 0 4h-.09A1.65 1.65 0 0 0 19.4 15z"></path></svg>',
        remove: '<svg viewBox="0 0 24 24" fill="none" stroke="#e11d48" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6"></path><path d="M14 11v6"></path><path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"></path></svg>',
        select: '<svg viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M9 18l6-6-6-6"></path></svg>',
        drag: '<svg viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M8 6h13"></path><path d="M8 12h13"></path><path d="M8 18h13"></path><path d="M3 6h.01"></path><path d="M3 12h.01"></path><path d="M3 18h.01"></path></svg>'
    }, this.cfg.icons || {});
    this.messages = Object.assign({
        parentRequired: '상위 ' + this.entityNm + '를 먼저 선택해 주세요.',
        nameRequired: this.entityNm + '명을 입력해 주세요.',
        deleteConfirm: '삭제하시겠습니까?\n하위 ' + this.entityNm + '도 함께 삭제됩니다.',
        registError: '등록 중 오류가 발생했습니다.',
        deleteError: '삭제 중 오류가 발생했습니다.',
        editError: '수정 중 오류가 발생했습니다.',
        sortError: '순서 저장 중 오류가 발생했습니다.',
        refreshConfirm: '변경내역을 서버에 반영하시겠습니까?',
        refreshSuccess: '서버 반영이 완료되었습니다.',
        refreshError: '서버 반영 중 오류가 발생했습니다.'
    }, this.cfg.messages || {});

    this._init();
};

ADM.TreeList.prototype = {
    _init: function(){
        var self = this;
        document.querySelectorAll('.' + this.prefix + '-add-btn').forEach(function(btn){
            btn.addEventListener('click', function(){ self._onAdd(this); });
        });
        document.querySelectorAll('.add-' + this.fields.nm).forEach(function(input){
            input.addEventListener('keydown', function(event){
                if(event.key === 'Enter'){
                    this.closest('.' + self.prefix + '-col').querySelector('.' + self.prefix + '-add-btn').click();
                }
            });
        });
        var btnRefresh = document.getElementById('btnRefresh');
        if(btnRefresh){
            btnRefresh.addEventListener('click', function(){ self._onRefresh(); });
        }
        this.search('0', '', 1);
    },

    search: function(pNo, no, depth){
        var self = this;
        depth = Number(depth);
        for(var i = depth; i <= this.colCount; i++){
            var tbody = document.querySelector('#' + this.tblPrefix + 'Tbl_' + i + ' tbody');
            if(tbody){ tbody.innerHTML = ''; }
            var col = document.querySelectorAll('.' + this.prefix + '-col')[i - 1];
            if(col){
                var pInput = col.querySelector('.add-p' + this._capitalize(this.fields.no));
                if(pInput){ pInput.value = (i === depth) ? pNo : ''; }
                var nmInput = col.querySelector('.add-' + this.fields.nm);
                if(nmInput){ nmInput.value = ''; }
            }
        }
        if(depth === 1){ pNo = '0'; }

        ADM.ajaxPost(this.urls.select, ADM.encodeParam(this.fields.pNo, pNo), function(xhr){
            if(xhr.status !== 200){ return; }
            var list = [];
            try { list = JSON.parse(xhr.responseText); } catch(e) { list = []; }
            var tbody = document.querySelector('#' + self.tblPrefix + 'Tbl_' + depth + ' tbody');
            if(!tbody){ return; }
            if(list.length === 0){
                self._renderEmpty(tbody, depth);
                return;
            }
            list.forEach(function(item){
                self._renderRow(tbody, item, pNo, depth);
            });
        });
    },

    _renderEmpty: function(tbody, depth){
        var tr = document.createElement('tr');
        var td = document.createElement('td');
        td.className = 'tree-empty';
        td.colSpan = depth < this.colCount ? 6 : 5;
        td.textContent = '등록된 ' + this.entityNm + '가 없습니다.';
        tr.appendChild(td);
        tbody.appendChild(tr);
    },

    _renderRow: function(tbody, item, pNo, depth){
        var self = this;
        var fields = this.fields;
        var no = item[fields.no];
        var tr = document.createElement('tr');
        tr.setAttribute('data-no', no);
        tr.setAttribute('data-p-no', item[fields.pNo] || '');
        tr.setAttribute('draggable', 'true');
        this._bindDrag(tr, tbody, pNo, depth);

        var tdNo = document.createElement('td');
        tdNo.className = 'col-no';
        tdNo.textContent = no;
        tr.appendChild(tdNo);

        var tdNm = document.createElement('td');
        tdNm.className = 'col-name';
        tdNm.textContent = item[fields.nm] || '';
        tdNm.addEventListener('dblclick', function(){
            self._onEdit(this, item[fields.pNo], no, depth);
        });
        tr.appendChild(tdNm);

        var tdDrag = document.createElement('td');
        tdDrag.className = 'col-drag';
        tdDrag.innerHTML = '<div class="drag-handle" title="드래그하여 이동" aria-label="순서 이동">' + this.icons.drag + '</div>';
        tr.appendChild(tdDrag);

        var tdSet = document.createElement('td');
        tdSet.className = 'col-act';
        tdSet.innerHTML = '<button type="button" class="btn-icon" title="설정" aria-label="설정">' + this.icons.setting + '</button>';
        tdSet.querySelector('button').addEventListener('click', function(){ self._onLayer(no, depth); });
        tr.appendChild(tdSet);

        var tdDel = document.createElement('td');
        tdDel.className = 'col-act';
        tdDel.innerHTML = '<button type="button" class="btn-icon danger" title="삭제" aria-label="삭제">' + this.icons.remove + '</button>';
        tdDel.querySelector('button').addEventListener('click', function(){
            self._onDelete(item[fields.pNo], no, depth);
        });
        tr.appendChild(tdDel);

        if(depth < this.colCount){
            var tdSel = document.createElement('td');
            tdSel.className = 'col-act';
            tdSel.innerHTML = '<button type="button" class="btn-icon select" title="선택" aria-label="하위 목록 선택">' + this.icons.select + '</button>';
            tdSel.querySelector('button').addEventListener('click', function(){
                self._onSelect(tr, no, depth);
            });
            tr.appendChild(tdSel);
        }
        tbody.appendChild(tr);
    },

    _bindDrag: function(tr, tbody, pNo, depth){
        var self = this;
        tr.addEventListener('dragstart', function(event){
            tr.classList.add('dragging');
            event.dataTransfer.setData('text/plain', tr.getAttribute('data-no'));
            window._admDraggedTreeRow = tr;
        });
        tr.addEventListener('dragend', function(){
            tr.classList.remove('dragging');
            var params = [];
            tbody.querySelectorAll('tr[data-no]').forEach(function(row){
                params.push(ADM.encodeParam(self.fields.no, row.getAttribute('data-no')));
            });
            ADM.ajaxPost(self.urls.updateSort, params.join('&'), function(xhr){
                if(xhr.status !== 200 || xhr.responseText.indexOf('true') === -1){
                    alert(self.messages.sortError);
                    self.search(pNo, '', depth);
                }
            });
        });
        if(!tbody._treeDragBound){
            tbody._treeDragBound = true;
            tbody.addEventListener('dragover', function(event){
                event.preventDefault();
                var dragging = window._admDraggedTreeRow;
                if(!dragging || dragging.parentElement !== tbody){ return; }
                var after = self._getDragAfterElement(tbody, event.clientY);
                if(after == null){ tbody.appendChild(dragging); }
                else { tbody.insertBefore(dragging, after); }
            });
        }
    },

    _getDragAfterElement: function(container, y){
        var closest = { offset: Number.NEGATIVE_INFINITY, element: null };
        container.querySelectorAll('tr[data-no]:not(.dragging)').forEach(function(child){
            var box = child.getBoundingClientRect();
            var offset = y - box.top - box.height / 2;
            if(offset < 0 && offset > closest.offset){
                closest = { offset: offset, element: child };
            }
        });
        return closest.element;
    },

    _onSelect: function(tr, no, depth){
        tr.parentElement.querySelectorAll('tr').forEach(function(row){ row.classList.remove('selected'); });
        tr.classList.add('selected');
        var nextDepth = Number(depth) + 1;
        var nextCol = document.querySelectorAll('.' + this.prefix + '-col')[nextDepth - 1];
        if(nextCol){
            var pInput = nextCol.querySelector('.add-p' + this._capitalize(this.fields.no));
            if(pInput){ pInput.value = no; }
        }
        this.search(no, '', nextDepth);
    },

    _onAdd: function(btn){
        var depth = btn.getAttribute('data-depth');
        var col = btn.closest('.' + this.prefix + '-col');
        var pInput = col.querySelector('.add-p' + this._capitalize(this.fields.no));
        var pNo = pInput ? pInput.value : '';
        var nmInput = col.querySelector('.add-' + this.fields.nm);
        var nm = nmInput ? nmInput.value.trim() : '';
        if(depth === '1'){ pNo = '0'; }
        if(!pNo && depth !== '1'){
            alert(this.messages.parentRequired);
            return;
        }
        if(!nm){
            alert(this.messages.nameRequired);
            if(nmInput){ nmInput.focus(); }
            return;
        }
        var self = this;
        ADM.ajaxPost(this.urls.regist, ADM.encodeParams(this.registParams(pNo, nm, depth)), function(xhr){
            if(xhr.status === 200 && xhr.responseText.indexOf('true') > -1){
                nmInput.value = '';
                self.search(pNo, '', depth);
            }else{
                alert(self.messages.registError);
            }
        });
    },

    _onDelete: function(pNo, no, depth){
        if(!confirm(this.messages.deleteConfirm)){ return; }
        var self = this;
        ADM.ajaxPost(this.urls.remove, ADM.encodeParams(this.deleteParams(pNo, no)), function(xhr){
            if(xhr.status === 200 && xhr.responseText.indexOf('true') > -1){
                self.search(pNo || '0', '', depth);
            }else{
                alert(self.messages.deleteError);
            }
        });
    },

    _onEdit: function(td, pNo, no, depth){
        if(td.querySelector('input')){ return; }
        var self = this;
        var originText = td.textContent;
        var input = document.createElement('input');
        input.type = 'text';
        input.className = this.editClass;
        input.value = originText;
        input.maxLength = 100;
        td.textContent = '';
        td.appendChild(input);
        input.focus();
        input.select();

        var done = false;
        function save(){
            if(done){ return; }
            done = true;
            var newVal = input.value.trim() || originText;
            td.textContent = newVal;
            if(newVal === originText){ return; }
            var params = {};
            params[self.fields.no] = no;
            params[self.fields.nm] = newVal;
            ADM.ajaxPost(self.urls.updateNm, ADM.encodeParams(params), function(xhr){
                if(xhr.status !== 200 || xhr.responseText.indexOf('true') === -1){
                    td.textContent = originText;
                    alert(self.messages.editError);
                }
            });
        }
        input.addEventListener('keydown', function(event){
            if(event.key === 'Enter'){ input.blur(); }
            if(event.key === 'Escape'){
                done = true;
                td.textContent = originText;
            }
        });
        input.addEventListener('blur', save);
    },

    _onLayer: function(no, depth){
        if(!this.urls.layer || !ADM.Modal || !ADM.Modal.open){ return; }
        var self = this;
        var url = this.urls.layer + '?' + ADM.encodeParam(this.fields.no, no);
        ADM.Modal.open(url, this.layerWidth, this.layerHeight, function(){
            self.search('0', '', 1);
        });
    },

    _onRefresh: function(){
        var self = this;
        if(!confirm(this.messages.refreshConfirm)){ return; }
        ADM.ajaxPost(this.urls.refresh, '', function(xhr){
            if(xhr.status === 200 && xhr.responseText.indexOf('true') > -1){
                alert(self.messages.refreshSuccess);
            }else{
                alert(self.messages.refreshError);
            }
        });
    },

    _capitalize: function(str){
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
};
