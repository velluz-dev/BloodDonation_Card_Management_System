/* ---------------------------------------------------------------------------
   Blood Donation Card Management System - client-side helpers.

   Everything here is a convenience for the person at the counter. None of it is
   a control: card numbers, PINs, dates and confirmations are all validated
   again on the server, because anything sent from a browser can be altered.
   --------------------------------------------------------------------------- */
(function () {
    'use strict';

    /* -- Card number entry ---------------------------------------------------
       A handheld QR/barcode scanner behaves like a very fast keyboard and ends
       with Enter, so it submits the lookup form without any extra step. What it
       does not do is normalise: it may deliver lower case, or the operator may
       type O for 0. Uppercasing as they type keeps what is on screen matching
       what the server will store. The equivalence mapping itself (I/L to 1,
       O to 0) is applied server-side in CardNumberGenerator.normalise. */
    var cardNo = document.getElementById('cardNo');
    if (cardNo) {
        cardNo.addEventListener('input', function () {
            var start = cardNo.selectionStart;
            cardNo.value = cardNo.value.toUpperCase();
            cardNo.setSelectionRange(start, start);
        });
    }

    /* -- PIN fields: digits only -------------------------------------------- */
    document.querySelectorAll('.bdcms-pin').forEach(function (field) {
        field.addEventListener('input', function () {
            field.value = field.value.replace(/\D/g, '').slice(0, 4);
        });
    });

    /* -- Photograph preview --------------------------------------------------
       Registration staff should see the photo they just chose before saving it;
       a wrong or unusable photo found later means the donor has to come back. */
    var photoInput = document.getElementById('photo');
    var photoPreview = document.getElementById('photoPreview');
    if (photoInput && photoPreview) {
        photoInput.addEventListener('change', function () {
            var file = photoInput.files && photoInput.files[0];
            if (!file) {
                photoPreview.classList.add('d-none');
                return;
            }
            photoPreview.src = URL.createObjectURL(file);
            photoPreview.classList.remove('d-none');
            photoPreview.onload = function () {
                URL.revokeObjectURL(photoPreview.src);
            };
        });
    }

    /* -- Guard against a double-submitted donation ---------------------------
       The server already redirects after a successful POST so a refresh cannot
       repeat it; this stops the impatient second click that would otherwise
       reach the server as a second request. */
    document.querySelectorAll('form[action$="/donations/record"]').forEach(function (form) {
        form.addEventListener('submit', function () {
            var button = form.querySelector('button[type="submit"]');
            if (button) {
                button.disabled = true;
                button.textContent = 'Recording...';
            }
        });
    });
}());
