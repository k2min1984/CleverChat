(function () {
    var modeSelect = document.querySelector('[data-schedule-mode]');


    var rows = document.querySelectorAll('[data-schedule-row]');

    function syncScheduleRows() {
        var mode = modeSelect ? modeSelect.value || 'INTERVAL' : 'INTERVAL';
        rows.forEach(function (row) {
            row.hidden = row.getAttribute('data-schedule-row') !== mode;
        });
    }

    if (modeSelect) modeSelect.addEventListener('change', syncScheduleRows);
    syncScheduleRows();
    var exportToggle = document.getElementById('jsonExportEnabled');
    var exportDirectory = document.getElementById('jsonExportDirectory');
    function syncExport() {
        if (!exportToggle || !exportDirectory) return;
        exportDirectory.required = false;
        exportDirectory.disabled = !exportToggle.checked;
    }
    if (exportToggle) exportToggle.addEventListener('change', syncExport);
    syncExport();
})();
