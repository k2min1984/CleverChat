var ADM = ADM || {};

(function(){
    function ready(fn){
        if(document.readyState === 'loading'){
            document.addEventListener('DOMContentLoaded', fn);
        }else{
            fn();
        }
    }

    function csrfHeaders(headers){
        headers = headers || {};
        var token = document.querySelector('meta[name="csrfToken"]');
        var formId = document.querySelector('meta[name="csrfFormId"]');
        if(token && token.content) headers['X-CSRF-Token'] = token.content;
        if(formId && formId.content) headers['X-CSRF-FormId'] = formId.content;
        return headers;
    }

    function updateRanks(list){
        list.querySelectorAll('.scenario-order-item').forEach(function(item, index){
            var rank = item.querySelector('.scenario-order-rank');
            if(rank) rank.textContent = String(index + 1);
        });
    }

    function itemAfterPointer(list, y){
        var candidates = Array.prototype.slice.call(
            list.querySelectorAll('.scenario-order-item:not(.is-dragging)')
        );
        return candidates.reduce(function(closest, item){
            var box = item.getBoundingClientRect();
            var offset = y - box.top - box.height / 2;
            if(offset < 0 && offset > closest.offset){
                return { offset: offset, element: item };
            }
            return closest;
        }, { offset: Number.NEGATIVE_INFINITY, element: null }).element;
    }

    function bindDrag(list){
        list.querySelectorAll('.scenario-order-item').forEach(function(item){
            item.addEventListener('dragstart', function(event){
                item.classList.add('is-dragging');
                event.dataTransfer.effectAllowed = 'move';
                event.dataTransfer.setData('text/plain', item.dataset.scenarioId || '');
            });
            item.addEventListener('dragend', function(){
                item.classList.remove('is-dragging');
                updateRanks(list);
            });
        });

        list.addEventListener('dragover', function(event){
            event.preventDefault();
            var dragging = list.querySelector('.scenario-order-item.is-dragging');
            if(!dragging) return;
            var next = itemAfterPointer(list, event.clientY);
            if(next == null){
                list.appendChild(dragging);
            }else{
                list.insertBefore(dragging, next);
            }
        });
    }

    function saveOrder(root, list, button){
        var ids = Array.prototype.slice.call(list.querySelectorAll('.scenario-order-item'))
            .map(function(item){ return Number(item.dataset.scenarioId); })
            .filter(function(id){ return !Number.isNaN(id); });
        if(ids.length === 0){
            alert('정렬할 활성 시나리오가 없습니다.');
            return;
        }
        button.disabled = true;
        CleverChat.fetch(root.dataset.saveUrl, {
            method: 'POST',
            credentials: 'same-origin',
            headers: csrfHeaders({
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }),
            body: JSON.stringify({ scenarioIds: ids })
        }).then(function(response){
            if(ADM.updateCsrfMetaValue){
                ADM.updateCsrfMetaValue(response.headers.get('X-CSRF-Token'), response.headers.get('X-CSRF-FormId'));
            }
            if(!response.ok) throw new Error('save-failed');
            return response.json();
        }).then(function(){
            updateRanks(list);
            alert('시나리오 정렬 순서를 저장했습니다.');
        }).catch(function(){
            alert('시나리오 정렬 순서를 저장하지 못했습니다.');
        }).finally(function(){
            button.disabled = false;
        });
    }

    ready(function(){
        var root = document.querySelector('[data-scenario-order-root]');
        if(!root) return;
        var list = root.querySelector('[data-scenario-order-list]');
        var button = document.querySelector('[data-scenario-order-save]');
        if(!list || !button) return;
        bindDrag(list);
        updateRanks(list);
        button.addEventListener('click', function(){
            saveOrder(root, list, button);
        });
    });
})();
