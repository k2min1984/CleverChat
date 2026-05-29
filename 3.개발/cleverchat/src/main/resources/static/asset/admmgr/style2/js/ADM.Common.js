/**************************************************
 * @desc    : 관리자 공통 유틸 (ADM.Form, ADM.Modal, ADM.ajaxPost 등)
 *            모든 관리자 페이지에서 로드되는 공통 JS
 * @author  : MOOK
 * @since   : 2026.04.09
 * @version : 1.0
 * @see
 * <pre>
 * << 개정이력(Modification Information) >>
 * Date        Author   Description
 * ------------------------------------------
 * 2026.04.09  MOOK     ADM.TreeList.js에서 분리
 * </pre>
 **************************************************/
var ADM = ADM || {};

/**[공통]컨텍스트 패스 기반 URL 조립 (head.html meta[name=ctx] 기준)**/
ADM.url = function(path){
    var meta = document.querySelector('meta[name="ctx"]');
    var ctx  = (meta && meta.content) ? meta.content : '/';
    if( ctx.charAt(ctx.length - 1) !== '/' ) ctx += '/';
    if( path && path.charAt(0) === '/' ) path = path.substring(1);
    return ctx + (path || '');
};

/**[공통]페이지 이동 (Thymeleaf 페이징 공통 함수)**/
function fn_GoPage(page){
    var frm = document.getElementById('frm');
    if( !frm ) return;
    var pageInput = frm.querySelector('input[name=page]');
    if( !pageInput ){
        pageInput = document.createElement('input');
        pageInput.type = 'hidden';
        pageInput.name = 'page';
        frm.appendChild(pageInput);
    }
    pageInput.value = page;
    frm.submit();
}

/**[공통]폼 필수값 검증 유틸 (em.required 기반 자동 탐색)**/
ADM.Form = {
    /**폼 초기화 - required 필드에 blur 이벤트 바인딩 + textarea 글자수 카운터**/
    init: function(formEl){
        if( !formEl ) return;
        var fields = ADM.Form._findRequired(formEl);
        fields.forEach(function(f){
            f.input.addEventListener('blur', function(){ ADM.Form._check(f); });
            f.input.addEventListener('input', function(){ ADM.Form._check(f); });
            if( f.input.tagName === 'SELECT' ){
                f.input.addEventListener('change', function(){ ADM.Form._check(f); });
            }
        });
        ADM.Form.initCharCounter(formEl);
    },
    /**전체 검증 - 실패 시 false, 첫 번째 에러 필드에 포커스**/
    validate: function(formEl){
        var fields = ADM.Form._findRequired(formEl);
        var firstError = null;
        fields.forEach(function(f){
            var ok = ADM.Form._check(f);
            if( !ok && !firstError ) firstError = f.input;
        });
        if( firstError ){ firstError.focus(); return false; }
        return true;
    },
    /**단일 필드 검증**/
    _check: function(f){
        //disabled 또는 숨겨진 필드는 검증 건너뜀
        //(보이는지 판단은 offsetParent 가 가장 안전 — display:none / 부모 hidden / hidden 속성 모두 처리)
        if( f.input.disabled || f.input.offsetParent === null ) return true;
        var val = (f.input.value || '').trim();
        //에러 표시 대상: textarea-wrap/flex-row-gap 안이면 그 부모(td)에, 아니면 부모에
        var errTarget = f.input.closest('.textarea-wrap') ? f.input.closest('.textarea-wrap').parentElement
                      : f.input.closest('.flex-row-gap') ? f.input.closest('.flex-row-gap').parentElement
                      : f.input.parentElement;
        var errEl = errTarget.querySelector('.field-error');
        if( !val ){
            f.input.classList.add('is-error');
            if( !errEl ){
                errEl = document.createElement('div');
                errEl.className = 'field-error';
                var josa = ADM.Form._getJosa(f.label, '은', '는');
                errEl.textContent = '⚠ ' + f.label + josa + ' 필수로 입력되어야 합니다.';
                errTarget.appendChild(errEl);
            }
            return false;
        }else{
            f.input.classList.remove('is-error');
            if( errEl ) errEl.parentElement.removeChild(errEl);
            return true;
        }
    },
    /**textarea 글자수 카운터 자동 초기화**/
    initCharCounter: function(formEl){
        if( !formEl ) return;
        formEl.querySelectorAll('.textarea-wrap').forEach(function(wrap){
            var ta = wrap.querySelector('textarea');
            var counter = wrap.querySelector('.byte-counter span');
            if( !ta || !counter ) return;
            var attrMax = parseInt(ta.getAttribute('data-max-char'));
            var labelNums = (counter.parentElement.textContent || '').match(/[\d,]+자/g);
            var labelMax = 0;
            if( labelNums ){
                labelNums.forEach(function(s){ var n = parseInt(s.replace(/[^0-9]/g,'')); if(!isNaN(n) && n > labelMax) labelMax = n; });
            }
            var maxLen = attrMax || labelMax || 1000;
            function update(){
                var len = ta.value.length;
                if( len > maxLen ){
                    ta.value = ta.value.substring(0, maxLen);
                    len = maxLen;
                }
                counter.textContent = len;
                counter.style.color = len >= maxLen ? '#dc2626' : '';
            }
            ta.addEventListener('input', update);
            ta.addEventListener('keyup', update);
            update();
        });
    },
    /**em.required가 있는 th → 같은 tr의 td에서 input/select/textarea 탐색**/
    _findRequired: function(formEl){
        var results = [];
        formEl.querySelectorAll('em.required').forEach(function(em){
            var th = em.closest('th');
            if( !th ) return;
            var tr = th.closest('tr');
            if( !tr ) return;
            var td = th.nextElementSibling;
            if( !td || td.tagName !== 'TD' ) td = tr.querySelector('td');
            if( !td ) return;
            var input = td.querySelector('input[type="text"]:not([data-no-validate]), input[type="password"]:not([data-no-validate]), input[type="date"]:not([data-no-validate]), input[type="number"]:not([data-no-validate]), input[type="email"]:not([data-no-validate]), select:not([data-no-validate]), textarea:not([data-no-validate])');
            if( !input ) return;
            //[라벨 추출] 1순위 input[data-label], 2순위 help-tip(도움말) 제거한 th textContent
            var label = input.getAttribute('data-label');
            if( !label ){
                var thClone = th.cloneNode(true);
                thClone.querySelectorAll('.help-tip, .help-pop, em.required').forEach(function(n){ n.remove(); });
                label = thClone.textContent.replace(/\*/g, '').trim();
            }
            results.push({ label: label, input: input });
        });
        return results;
    },
    /**한글 조사 자동 판별 (받침 유무)**/
    _getJosa: function(str, withBatchim, withoutBatchim){
        if( !str ) return withBatchim;
        var code = str.charCodeAt(str.length - 1) - 44032;
        if( code < 0 || code > 11171 ) return withBatchim;
        return (code % 28 > 0) ? withBatchim : withoutBatchim;
    },

    /**[공통]FormData fetch 전송 + RedirectScript 응답 처리**/
    submit: function(frm, formData){
        if( !formData ) formData = new FormData(frm);
        var headers = { 'X-Requested-With': 'XMLHttpRequest' };
        var formIdMeta = document.querySelector('meta[name="csrfFormId"]');
        var tokenMeta  = document.querySelector('meta[name="csrfToken"]');
        if( tokenMeta && tokenMeta.content ){
            headers['X-CSRF-Token'] = tokenMeta.content;
            if( !formData.has('csrfToken') ) formData.append('csrfToken', tokenMeta.content);
        }
        if( formIdMeta && formIdMeta.content ){
            headers['X-CSRF-FormId'] = formIdMeta.content;
            if( !formData.has('csrfFormId') ) formData.append('csrfFormId', formIdMeta.content);
        }
        fetch(frm.action, { method:'POST', body:formData, headers:headers, credentials:'same-origin' })
        .then(function(res){
            ADM.updateCsrfMetaValue(res.headers.get('X-CSRF-Token'), res.headers.get('X-CSRF-FormId'));
            if( res.status === 401 ){
                alert('세션이 만료되었습니다.\n다시 로그인해 주세요.');
                location.href = ADM.url('login');
                return '';
            }
            if( res.status === 403 ){
                alert('잘못된 접근입니다.(CSRF값이 유효하지 않습니다.)');
                return '';
            }
            return res.text();
        })
        .then(function(html){
            if( !html ) return;
            //alert 메시지 추출 (유니코드 이스케이프 → JSON.parse로 디코딩)
            var alertMatch = html.match(/alert\('([^']*)'\)/);
            if( alertMatch ){
                var raw = alertMatch[1];
                try{ raw = JSON.parse('"' + raw + '"'); }catch(e){}
                alert(raw);
            }
            //이동 URL 추출 (form action 또는 location.href)
            var urlMatch = html.match(/action="([^"]+)"/);
            if( urlMatch ){
                location.href = urlMatch[1];
            }else{
                var locMatch = html.match(/location\.href\s*=\s*["']([^"']+)["']/);
                if( locMatch ) location.href = locMatch[1];
                else history.back();
            }
        })
        .catch(function(){ alert('저장 중 오류가 발생했습니다.'); });
    }
};

/**[보안]XSS 방지 - HTML 특수문자 이스케이프**/
ADM.escapeHtml = function(str){
    if( !str ) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
};

/**[보안]URL 파라미터 인코딩**/
ADM.encodeParam = function(key, val){
    return encodeURIComponent(key) + '=' + encodeURIComponent(val);
};

/**[보안]객체 → URL 인코딩 문자열 변환**/
ADM.encodeParams = function(obj){
    var parts = [];
    for( var key in obj ){
        if( obj.hasOwnProperty(key) ){
            parts.push(ADM.encodeParam(key, obj[key]));
        }
    }
    return parts.join('&');
};

/**[CSRF]meta 태그에서 CSRF 토큰 추출하여 URL 인코딩 문자열 반환**/
ADM.getCsrfParam = function(){
    var formIdMeta = document.querySelector('meta[name="csrfFormId"]');
    var tokenMeta  = document.querySelector('meta[name="csrfToken"]');
    if( formIdMeta && tokenMeta && formIdMeta.content && tokenMeta.content ){
        return ADM.encodeParam('csrfFormId', formIdMeta.content) + '&' + ADM.encodeParam('csrfToken', tokenMeta.content);
    }
    return '';
};

/**[CSRF]meta 태그 값을 AJAX 요청 헤더에 첨부**/
ADM.setCsrfHeaders = function(xhr){
    var formIdMeta = document.querySelector('meta[name="csrfFormId"]');
    var tokenMeta  = document.querySelector('meta[name="csrfToken"]');
    if( tokenMeta && tokenMeta.content ){
        xhr.setRequestHeader('X-CSRF-Token', tokenMeta.content);
    }
    if( formIdMeta && formIdMeta.content ){
        xhr.setRequestHeader('X-CSRF-FormId', formIdMeta.content);
    }
};

/**[CSRF]AJAX 응답 헤더에서 갱신된 토큰을 meta 태그에 반영 (연속 AJAX 호출 지원)**/
ADM.updateCsrfMeta = function(xhr){
    var newFormId = xhr.getResponseHeader('X-CSRF-FormId');
    var newToken  = xhr.getResponseHeader('X-CSRF-Token');
    ADM.updateCsrfMetaValue(newToken, newFormId);
};

/**[CSRF]응답에서 갱신된 토큰 값을 meta 태그에 반영**/
ADM.updateCsrfMetaValue = function(newToken, newFormId){
    if( newFormId && newToken ){
        var formIdMeta = document.querySelector('meta[name="csrfFormId"]');
        var tokenMeta  = document.querySelector('meta[name="csrfToken"]');
        if( formIdMeta ) formIdMeta.content = newFormId;
        if( tokenMeta )  tokenMeta.content  = newToken;
    }
};

/**[공통]AJAX POST 래퍼 (401 세션만료 + CSRF + 네트워크 에러 공통 처리)**/
ADM.ajaxPost = function(url, data, onSuccess, onError){
    var xhr = new XMLHttpRequest();
    xhr.open('POST', url, true);
    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
    ADM.setCsrfHeaders(xhr);
    //[CSRF] meta 태그에서 토큰 추출하여 파라미터에 자동 추가
    var csrfData = ADM.getCsrfParam();
    if( csrfData ){
        data = data ? (data + '&' + csrfData) : csrfData;
    }
    xhr.onload = function(){
        //[CSRF] 응답 헤더에서 갱신된 토큰 추출하여 meta 태그 업데이트 (연속 AJAX 호출 지원)
        ADM.updateCsrfMeta(xhr);
        if( xhr.status === 401 ){
            alert('세션이 만료되었습니다.\n다시 로그인해 주세요.');
            location.href = ADM.url('login');
            return;
        }
        if( xhr.status === 403 ){
            alert('잘못된 접근입니다.(CSRF값이 유효하지 않습니다.)');
            return;
        }
        if( typeof onSuccess === 'function' ) onSuccess(xhr);
    };
    xhr.onerror = function(){
        alert('네트워크 오류가 발생했습니다.');
        if( typeof onError === 'function' ) onError();
    };
    xhr.send(data);
};

/**[공통]모달 레이어 팝업 (fetch + div 방식, iframe 미사용)**/
ADM.Modal = ADM.Modal || {
    _onClose: null,

    /**모달 닫기**/
    close: function(){
        var modal = document.getElementById('dimLayerPopup');
        if( modal ) document.body.removeChild(modal);
        if( typeof ADM.Modal._onClose === 'function' ){
            var cb = ADM.Modal._onClose;
            ADM.Modal._onClose = null;
            cb();
        }
    },

    /**모달 열기 (url, width, height, onClose)**/
    open: function(url, width, height, onClose){
        ADM.Modal.close();
        ADM.Modal._onClose = onClose || null;
        width  = width  || 960;
        height = height || 500;

        var overlay = document.createElement('div');
        overlay.id = 'dimLayerPopup';
        overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.6);z-index:9999;display:flex;align-items:center;justify-content:center;backdrop-filter:blur(3px);';
        overlay.addEventListener('click', function(e){ if( e.target === overlay ) ADM.Modal.close(); });

        var box = document.createElement('div');
        box.id = 'dimLayerContent';
        box.style.cssText = 'width:'+width+'px;max-height:'+height+'px;background:#ffffff;border:none;border-radius:12px;overflow:hidden;display:flex;flex-direction:column;box-shadow:0 25px 50px rgba(0, 0, 0, 0.4);animation:layerFadeIn 0.2s ease;';
        var loadingDiv = document.createElement('div');
        loadingDiv.style.cssText = 'text-align:center;padding:60px;color:#64748b;font-size:13px;';
        loadingDiv.textContent = '로딩 중...';
        box.appendChild(loadingDiv);
        overlay.appendChild(box);
        document.body.appendChild(overlay);

        if( !document.getElementById('layerStyle') ){
            var s = document.createElement('style');
            s.id = 'layerStyle';
            s.textContent = '@keyframes layerFadeIn{from{opacity:0;transform:translateY(-20px);}to{opacity:1;transform:translateY(0);}}';
            document.head.appendChild(s);
        }

        var xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.withCredentials = true;
        xhr.onload = function(){
            if( xhr.status !== 200 ){
                while(box.firstChild) box.removeChild(box.firstChild);
                var errDiv = document.createElement('div');
                errDiv.style.cssText = 'text-align:center;padding:60px;color:#dc2626;';
                errDiv.textContent = '로딩 실패 (' + xhr.status + ')';
                box.appendChild(errDiv);
                return;
            }
            var parser = new DOMParser();
            var doc = parser.parseFromString(xhr.responseText, 'text/html');
            while(box.firstChild) box.removeChild(box.firstChild);

            Array.from(doc.body.children).forEach(function(el){
                if( el.tagName !== 'SCRIPT' ) box.appendChild(document.adoptNode(el));
            });
            if( ADM.ManageLayer && typeof ADM.ManageLayer.init === 'function' ){
                ADM.ManageLayer.init(box);
            }
            document.dispatchEvent(new CustomEvent('adm:modal-loaded', { detail:{ container: box } }));

            var scripts = Array.from(doc.querySelectorAll('body script'));
            var execNext = function(i){
                if( i >= scripts.length ) return;
                var old = scripts[i];
                var ns  = document.createElement('script');
                if( old.src ){
                    var absSrc = new URL(old.src, location.href).href;
                    if( document.querySelector('script[src="'+old.src+'"]') || document.querySelector('script[src="'+absSrc+'"]') ){
                        execNext(i+1);
                        return;
                    }
                    ns.src = old.src;
                    ns.onload  = function(){ execNext(i+1); };
                    ns.onerror = function(){ execNext(i+1); };
                }else{
                    ns.textContent = old.textContent;
                }
                box.appendChild(ns);
                if( !old.src ) execNext(i+1);
            };
            execNext(0);
        };
        xhr.onerror = function(){
            while(box.firstChild) box.removeChild(box.firstChild);
            var netErr = document.createElement('div');
            netErr.style.cssText = 'text-align:center;padding:60px;color:#dc2626;';
            netErr.textContent = '네트워크 오류';
            box.appendChild(netErr);
        };
        xhr.send();
    },

    /**레이어 내 form을 AJAX로 전송 (procScript 응답 파싱)**/
    submitForm: function(form, onSuccess){
        var xhr = new XMLHttpRequest();
        xhr.open('POST', form.action, true);
        xhr.withCredentials = true;
        xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
        ADM.setCsrfHeaders(xhr);
        xhr.onload = function(){
            //[CSRF] 응답 헤더에서 갱신된 토큰 반영
            ADM.updateCsrfMeta(xhr);
            if( xhr.status === 401 ){
                alert('세션이 만료되었습니다.\n다시 로그인해 주세요.');
                location.href = ADM.url('login');
                return;
            }
            if( xhr.status === 403 ){
                alert('잘못된 접근입니다.(CSRF값이 유효하지 않습니다.)');
                return;
            }
            var html = xhr.responseText;
            var alertMatch = html.match(/alert\('([^']+)'\)/);
            if( alertMatch ){
                var msg = alertMatch[1].replace(/\\u([0-9a-fA-F]{4})/g, function(m, hex){
                    return String.fromCharCode(parseInt(hex, 16));
                });
                alert(msg);
            }
            if( typeof onSuccess === 'function' ) onSuccess(html);
        };
        xhr.onerror = function(){
            alert('처리 중 오류가 발생했습니다.');
        };
        xhr.send(new FormData(form));
    }
};

//parent.GF 호환
window.GF = ADM.Modal;

ADM.initCmsLayout = function(){
    var sidebar = document.querySelector('.sidebar');
    if( !sidebar ) return;

    var currentPath = window.location.pathname.replace(/\/$/, '');
    var pageLabel = document.querySelector('[data-current-admin-page]');
    var activeLabel = '';

    sidebar.querySelectorAll('a[href]').forEach(function(link){
        var href = new URL(link.getAttribute('href'), window.location.origin).pathname.replace(/\/$/, '');
        if( href && (currentPath === href || (href !== '/admin' && currentPath.indexOf(href + '/') === 0)) ){
            link.classList.add('active');
            var group = link.closest('.nav-group');
            if( group ) group.classList.add('open');
            activeLabel = (link.textContent || '').trim();
        }
    });

    if( pageLabel && activeLabel ){
        pageLabel.textContent = activeLabel;
    }

    sidebar.querySelectorAll('.nav-group-header').forEach(function(button){
        button.addEventListener('click', function(){
            var group = button.closest('.nav-group');
            if( group ) group.classList.toggle('open');
        });
    });

    var toggle = sidebar.querySelector('[data-sidebar-toggle]');
    if( toggle ){
        toggle.addEventListener('click', function(){
            sidebar.classList.toggle('collapsed');
        });
    }
};

if( document.readyState === 'loading' ){
    document.addEventListener('DOMContentLoaded', ADM.initCmsLayout);
}else{
    ADM.initCmsLayout();
}
