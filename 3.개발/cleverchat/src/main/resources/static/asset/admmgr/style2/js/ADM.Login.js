(function(){
    function getCookie(name){
        return document.cookie.split(';').map(function(value){ return value.trim(); })
            .find(function(value){ return value.indexOf(name + '=') === 0; });
    }

    function setSavedUsername(username){
        var expires = new Date();
        expires.setTime(expires.getTime() + 7 * 24 * 60 * 60 * 1000);
        document.cookie = 'savedUserId=' + encodeURIComponent(username) + ';expires=' + expires.toUTCString() + ';path=/;SameSite=Lax';
    }

    function clearSavedUsername(){
        document.cookie = 'savedUserId=;expires=Thu, 01 Jan 1970 00:00:01 GMT;path=/;SameSite=Lax';
    }

    document.addEventListener('DOMContentLoaded', function(){
        var form = document.getElementById('loginForm');
        var username = document.getElementById('username');
        var password = document.getElementById('password');
        var saveId = document.getElementById('saveId');
        var capsMsg = document.getElementById('capsLockMsg');
        var loading = document.getElementById('loginLoading');
        var saved = getCookie('savedUserId');

        if(saved && username && saveId){
            username.value = decodeURIComponent(saved.split('=').slice(1).join('='));
            saveId.checked = true;
        }

        function checkCaps(event){
            if(!capsMsg || !event.getModifierState){ return; }
            capsMsg.classList.toggle('show', event.getModifierState('CapsLock'));
        }

        if(password){
            password.addEventListener('keydown', checkCaps);
            password.addEventListener('keyup', checkCaps);
            password.addEventListener('blur', function(){ if(capsMsg){ capsMsg.classList.remove('show'); } });
            password.addEventListener('focus', checkCaps);
        }

        if(form){
            form.addEventListener('submit', function(){
                if(saveId && username){
                    if(saveId.checked){ setSavedUsername(username.value || ''); }
                    else { clearSavedUsername(); }
                }
                if(loading){
                    loading.classList.add('show');
                    loading.setAttribute('aria-hidden', 'false');
                }
                var submit = form.querySelector('button[type="submit"]');
                if(submit){ submit.disabled = true; }
            });
        }
    });
})();
