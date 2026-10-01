(function () {
    'use strict';

    document.querySelectorAll('[data-close-document]').forEach(function (button) {
        button.addEventListener('click', function () {
            var fallbackUrl = button.getAttribute('data-fallback-url') || '/chat';
            window.close();
            window.setTimeout(function () {
                if (!window.closed) {
                    window.location.replace(fallbackUrl);
                }
            }, 150);
        });
    });
}());
