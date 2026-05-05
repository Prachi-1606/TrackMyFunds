(function () {
    'use strict';

    const form = document.getElementById('filterForm');
    if (!form) return; // only active on the list page

    const sel = {
        category:  form.querySelector('[name="category"]'),
        payment:   form.querySelector('[name="paymentMethod"]'),
        keyword:   form.querySelector('[name="keyword"]'),
        dateFrom:  form.querySelector('[name="dateFrom"]'),
        dateTo:    form.querySelector('[name="dateTo"]'),
        amountMin: form.querySelector('[name="amountMin"]'),
        amountMax: form.querySelector('[name="amountMax"]'),
    };

    // ── 1. Auto-submit on dropdown change ────────────────────────────────────
    [sel.category, sel.payment].forEach(el => {
        el?.addEventListener('change', () => { if (validate()) form.submit(); });
    });

    // ── 2. Debounced keyword auto-submit (300 ms) ────────────────────────────
    let kwTimer;
    sel.keyword?.addEventListener('input', () => {
        clearTimeout(kwTimer);
        kwTimer = setTimeout(() => { if (validate()) form.submit(); }, 300);
    });

    // ── 3. Range validation with inline error messages ───────────────────────
    function anchor(input) {
        // Amount inputs are inside .input-group; date inputs are bare
        return input.closest('.input-group') ?? input;
    }

    function showErr(input, msg) {
        clearErr(input);
        input.classList.add('is-invalid');
        const div = document.createElement('div');
        div.className = 'invalid-feedback d-block tmf-err';
        div.textContent = msg;
        anchor(input).insertAdjacentElement('afterend', div);
    }

    function clearErr(input) {
        input.classList.remove('is-invalid');
        const next = anchor(input).nextElementSibling;
        if (next && next.classList.contains('tmf-err')) next.remove();
    }

    function validate() {
        let ok = true;

        // Amount range: max must be >= min
        if (sel.amountMin) clearErr(sel.amountMin);
        if (sel.amountMax) clearErr(sel.amountMax);
        const lo = parseFloat(sel.amountMin?.value);
        const hi = parseFloat(sel.amountMax?.value);
        if (!isNaN(lo) && !isNaN(hi) && hi < lo) {
            showErr(sel.amountMax, 'Max amount must be ≥ min amount');
            ok = false;
        }

        // Date range: dateTo must be >= dateFrom
        if (sel.dateFrom) clearErr(sel.dateFrom);
        if (sel.dateTo)   clearErr(sel.dateTo);
        if (sel.dateFrom?.value && sel.dateTo?.value && sel.dateTo.value < sel.dateFrom.value) {
            showErr(sel.dateTo, 'End date must be on or after start date');
            ok = false;
        }

        return ok;
    }

    // Live feedback whenever a range field changes
    [sel.amountMin, sel.amountMax, sel.dateFrom, sel.dateTo].forEach(el => {
        el?.addEventListener('change', validate);
    });

    // Prevent submission when ranges are invalid
    form.addEventListener('submit', e => { if (!validate()) e.preventDefault(); });

    // ── 4. Clear-all button ──────────────────────────────────────────────────
    document.getElementById('clearAllFilters')?.addEventListener('click', () => {
        // Reset every filter input
        Object.values(sel).forEach(el => { if (el) el.value = ''; });
        // Remove any validation highlights
        [sel.amountMin, sel.amountMax, sel.dateFrom, sel.dateTo].forEach(el => {
            if (el) clearErr(el);
        });
        form.submit();
    });

    // ── 5. Active-filter indicator on dropdowns ──────────────────────────────
    function refreshIndicators() {
        [sel.category, sel.payment].forEach(el => {
            if (!el) return;
            el.classList.toggle('tmf-filter-active', el.value !== '');
        });
    }

    refreshIndicators(); // apply on page load (server-side pre-selected value)
    [sel.category, sel.payment].forEach(el => {
        el?.addEventListener('change', refreshIndicators);
    });

})();
