var ADM = ADM || {};

(function(){
    function ready(fn){
        if(document.readyState === 'loading'){
            document.addEventListener('DOMContentLoaded', fn);
        }else{
            fn();
        }
    }

    function openPreview(url){
        if(!ADM.Modal || typeof ADM.Modal.open !== 'function'){
            return false;
        }
        ADM.Modal.open(url, 720, 640);
        return true;
    }

    function bindPreviewLinks(root){
        root.querySelectorAll('[data-preview-url]:not([data-preview-bound])').forEach(function(link){
            link.setAttribute('data-preview-bound', 'true');
            link.addEventListener('click', function(event){
                if(!openPreview(link.dataset.previewUrl || link.href)){
                    return;
                }
                event.preventDefault();
            });
        });
    }

    function bindPreviewLayer(root){
        root.querySelectorAll('[data-preview-close]:not([data-preview-close-bound])').forEach(function(button){
            button.setAttribute('data-preview-close-bound', 'true');
            button.addEventListener('click', function(){
                if(ADM.Modal && typeof ADM.Modal.close === 'function'){
                    ADM.Modal.close();
                }
            });
        });

        root.querySelectorAll('[data-preview-detail]:not([data-preview-detail-bound])').forEach(function(link){
            link.setAttribute('data-preview-detail-bound', 'true');
            link.addEventListener('click', function(event){
                event.preventDefault();
                window.location.href = link.href;
            });
        });

        root.querySelectorAll('.scenario-preview-layer .preview-options a:not([data-preview-option-bound])').forEach(function(link){
            link.setAttribute('data-preview-option-bound', 'true');
            link.addEventListener('click', function(event){
                if(!openPreview(link.href)){
                    return;
                }
                event.preventDefault();
            });
        });
    }

    ready(function(){
        bindPreviewLinks(document);
        bindPreviewLayer(document);
    });

    document.addEventListener('adm:modal-loaded', function(event){
        bindPreviewLayer(event.detail && event.detail.container ? event.detail.container : document);
    });
})();
