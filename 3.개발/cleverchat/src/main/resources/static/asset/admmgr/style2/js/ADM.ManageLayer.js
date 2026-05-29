var ADM = ADM || {};

(function(){
    function bindLayerForms(root){
        root.querySelectorAll('[data-manage-layer]:not([data-layer-bound])').forEach(function(form){
            form.setAttribute('data-layer-bound', 'true');
            form.querySelectorAll('[data-close-layer]').forEach(function(button){
                button.addEventListener('click', function(){
                    if(ADM.Modal && ADM.Modal.close){ ADM.Modal.close(); }
                });
            });
            form.querySelectorAll('[data-save-layer]').forEach(function(button){
                button.addEventListener('click', function(){
                    submitLayer(form);
                });
            });
            form.querySelectorAll('input[name="menuType"]').forEach(function(radio){
                radio.addEventListener('change', function(){ syncMenuType(form); });
            });
            syncMenuType(form);
        });
    }

    function submitLayer(form){
        var required = form.querySelectorAll('[data-layer-required]');
        for(var i = 0; i < required.length; i++){
            var input = required[i];
            if(!String(input.value || '').trim()){
                alert((input.getAttribute('data-label') || '필수값') + '을 입력해 주세요.');
                input.focus();
                return;
            }
        }
        if(!confirm('저장하시겠습니까?')){ return; }
        ADM.Modal.submitForm(form, function(){
            ADM.Modal.close();
        });
    }

    function syncMenuType(form){
        var selected = form.querySelector('input[name="menuType"]:checked');
        var type = selected ? selected.value : 'ADM';
        var url = form.querySelector('[name="menuUrl"]');
        var related = form.querySelector('[data-related-url-row]');
        if(url){
            url.readOnly = type === 'N/A';
            if(type === 'N/A'){ url.value = ''; }
        }
        if(related){
            related.style.display = type === 'BOARD' ? '' : 'none';
        }
    }

    function ready(){
        bindLayerForms(document);
        document.addEventListener('adm:modal-loaded', function(event){
            bindLayerForms(event.detail && event.detail.container ? event.detail.container : document);
        });
        var observer = new MutationObserver(function(records){
            records.forEach(function(record){
                record.addedNodes.forEach(function(node){
                    if(node.nodeType === 1){ bindLayerForms(node); }
                });
            });
        });
        observer.observe(document.body, { childList:true, subtree:true });
    }

    ADM.ManageLayer = {
        init: bindLayerForms
    };

    if(document.readyState === 'loading'){
        document.addEventListener('DOMContentLoaded', ready);
    }else{
        ready();
    }
})();
