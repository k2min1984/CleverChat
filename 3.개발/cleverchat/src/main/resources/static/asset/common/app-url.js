(function () {
    'use strict';
    var app = window.CleverChat = window.CleverChat || {};
    app.url = function (path) {
        if (typeof path !== 'string' || /^(?:[a-z][a-z0-9+.-]*:|\/\/|#)/i.test(path)) return path;
        var meta = document.querySelector('meta[name="ctx"]');
        var context = (meta && meta.content ? meta.content : '/').replace(/\/$/, '');
        if (context && (path === context || path.indexOf(context + '/') === 0)) return path;
        return context + '/' + path.replace(/^\//, '');
    };
    app.fetch = function (path, options) {
        return window.fetch(typeof path === 'string' && /^\/(?:admin|chat|login|logout)(?:\/|\?|$)/.test(path) ? app.url(path) : path, options);
    };
})();
