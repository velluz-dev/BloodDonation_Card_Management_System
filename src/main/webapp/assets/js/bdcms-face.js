/* ---------------------------------------------------------------------------
   Assisted face check (FR-4, advisory).

   Compares the live camera image against the donor's stored photograph and
   shows the operator a similarity reading. That is ALL it does.

   ── What this is not ──────────────────────────────────────────────────────
   It is not a control, and nothing in the system trusts it:

     * The staff confirmation tick is still required. This never ticks it, and
       a strong match does not enable the submit button.
     * The score is computed in the browser, so a determined user could send
       any number they like. The server therefore treats it as an observation
       to record - "this is what the operator was shown" - never as evidence.
       Accountability stays exactly where it was: on the named person who
       ticked the box.
     * A refusal is still a human decision. The system will happily record a
       donation with a poor score, and will refuse one with a perfect score if
       the operator does not confirm.

   The value it adds is a prompt to look twice, and a number in the audit trail
   next to the confirmation.

   ── Loading ───────────────────────────────────────────────────────────────
   The library is 1.3 MB and the models 6.8 MB. Both load on first use only,
   never on page load, so an operator who never presses the button pays
   nothing. Everything is bundled locally - no CDN, works offline.
   --------------------------------------------------------------------------- */
(function () {
    'use strict';

    var ctx = document.body.getAttribute('data-ctx') || '';
    var MODELS = ctx + '/assets/vendor/face-api/models';
    var LIB = ctx + '/assets/vendor/face-api/face-api.js';

    /* face-api returns a Euclidean distance between 128-d descriptors: smaller
       is more alike. 0.6 is the library's own conventional threshold. The bands
       below are deliberately cautious and, just as deliberately, never say
       "same person" - only how alike two images are. */
    var STRONG = 0.45;
    var POSSIBLE = 0.60;

    var loading = null;

    function loadEngine(say) {
        if (loading) {
            return loading;
        }
        say('Loading the face model (about 8 MB, first use only)…');
        loading = new Promise(function (resolve, reject) {
            var s = document.createElement('script');
            s.src = LIB;
            s.onload = resolve;
            s.onerror = function () { reject(new Error('Could not load the face library.')); };
            document.head.appendChild(s);
        }).then(function () {
            return Promise.all([
                faceapi.nets.tinyFaceDetector.loadFromUri(MODELS),
                faceapi.nets.faceLandmark68Net.loadFromUri(MODELS),
                faceapi.nets.faceRecognitionNet.loadFromUri(MODELS)
            ]);
        });
        return loading;
    }

    function describe(input) {
        return faceapi
            .detectSingleFace(input, new faceapi.TinyFaceDetectorOptions({ inputSize: 416 }))
            .withFaceLandmarks()
            .withFaceDescriptor();
    }

    function band(distance) {
        if (distance < STRONG) {
            return { key: 'strong', label: 'Strong likeness',
                     note: 'The two images are very alike. Confirm only if the person also matches the document.' };
        }
        if (distance < POSSIBLE) {
            return { key: 'possible', label: 'Possible likeness',
                     note: 'Inconclusive. Rely on the photograph and the identity document.' };
        }
        return { key: 'weak', label: 'Little likeness',
                 note: 'The images do not look alike. Check carefully before confirming, and do not confirm if in doubt.' };
    }

    document.addEventListener('DOMContentLoaded', function () {
        var panel = document.getElementById('faceCheck');
        if (!panel) {
            return;
        }

        var startBtn = document.getElementById('faceStart');
        var stopBtn = document.getElementById('faceStop');
        var video = document.getElementById('faceVideo');
        var status = document.getElementById('faceStatus');
        var result = document.getElementById('faceResult');
        var scoreField = document.getElementById('faceScore');
        var stored = document.getElementById('faceStored');
        var stage = document.getElementById('faceStage');

        var stream = null;

        function say(text, tone) {
            status.textContent = text;
            status.className = 'bdcms-scan-status' + (tone ? ' bdcms-scan-' + tone : '');
        }

        function show(distance) {
            var b = band(distance);
            // Similarity shown as a percentage purely so the reading is legible
            // at a glance; the distance is what the model actually produced and
            // is what gets recorded.
            var pct = Math.max(0, Math.round((1 - distance / 1.2) * 100));
            result.hidden = false;
            result.className = 'bdcms-face-result bdcms-face-' + b.key;
            result.innerHTML =
                '<div class="bdcms-face-verdict">' + b.label + '</div>' +
                '<div class="bdcms-face-figures">similarity ' + pct +
                '% &middot; distance ' + distance.toFixed(3) + '</div>' +
                '<div class="bdcms-face-note">' + b.note + '</div>';

            // Recorded with the attempt so the audit trail shows what the
            // operator saw. Advisory: the server logs it, never acts on it.
            if (scoreField) {
                scoreField.value = distance.toFixed(3);
            }
        }

        function stop() {
            if (stream) {
                stream.getTracks().forEach(function (t) { t.stop(); });
                stream = null;
            }
            video.srcObject = null;
            stage.hidden = true;
            startBtn.hidden = false;
            stopBtn.hidden = true;
        }

        startBtn.addEventListener('click', function () {
            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
                stage.hidden = false;
                say('This browser cannot open a camera. Compare the photograph by eye.', 'bad');
                return;
            }
            if (!(window.isSecureContext || location.hostname === 'localhost'
                    || location.hostname === '127.0.0.1')) {
                stage.hidden = false;
                say('The camera needs HTTPS, or the site opened on localhost. '
                    + 'Compare the photograph by eye.', 'bad');
                return;
            }

            stage.hidden = false;
            startBtn.hidden = true;
            stopBtn.hidden = false;
            result.hidden = true;

            loadEngine(say)
                .then(function () {
                    say('Starting the camera…');
                    return navigator.mediaDevices.getUserMedia({
                        video: { width: { ideal: 640 }, height: { ideal: 480 } },
                        audio: false
                    });
                })
                .then(function (s) {
                    stream = s;
                    video.srcObject = s;
                    return video.play();
                })
                .then(function () {
                    say('Look at the camera. Reading…');
                    // A beat for exposure to settle; a first frame taken in the
                    // dark produces a poor descriptor and a misleading score.
                    return new Promise(function (r) { setTimeout(r, 900); });
                })
                .then(function () {
                    return Promise.all([describe(video), describe(stored)]);
                })
                .then(function (pair) {
                    var live = pair[0];
                    var onFile = pair[1];

                    if (!onFile) {
                        say('No face could be found in the stored photograph, so no comparison '
                            + 'is possible. Compare by eye.', 'bad');
                        stop();
                        return;
                    }
                    if (!live) {
                        say('No face found in the camera image. Move into frame and try again.', 'bad');
                        stop();
                        return;
                    }

                    var distance = faceapi.euclideanDistance(live.descriptor, onFile.descriptor);
                    say('Comparison complete.');
                    show(distance);
                    stop();
                })
                .catch(function (err) {
                    var message = 'The face check could not run. Compare the photograph by eye.';
                    if (err && (err.name === 'NotAllowedError' || err.name === 'SecurityError')) {
                        message = 'Camera permission was refused. Compare the photograph by eye.';
                    } else if (err && err.name === 'NotFoundError') {
                        message = 'No camera was found on this machine.';
                    }
                    say(message, 'bad');
                    stop();
                });
        });

        stopBtn.addEventListener('click', function () {
            stop();
            say('Camera stopped.');
        });

        // Release the camera when leaving the page, so the indicator light does
        // not stay on after the operator has moved elsewhere.
        window.addEventListener('pagehide', stop);
    });
}());
