/* ---------------------------------------------------------------------------
   Webcam QR scanning for donor cards (FR-4).

   The proposal lists a webcam or handheld scanner as optional peripherals, with
   card numbers typable by hand. This adds the webcam path without removing the
   other two: the text field still works, and a handheld scanner still types
   into it and presses Enter.

   Two decoders, in order of preference:

     1. BarcodeDetector — built into Chrome and Edge. Native, hardware
        accelerated, nothing to download.
     2. jsQR — bundled locally as a fallback for Firefox and Safari, which have
        no BarcodeDetector. Loaded only if it is actually needed, so browsers
        with the native API never pay for the 251 KB.

   Nothing here is a security control. The scanner only fills in the card number
   field; the server verifies the card, the photograph, the PIN and eligibility
   exactly as it does for a typed number.

   getUserMedia requires a secure context: HTTPS, or localhost. On localhost —
   how this system is run and demonstrated — it works over plain HTTP. Deployed
   to another machine over HTTP it will not, and the button says so rather than
   failing silently.
   --------------------------------------------------------------------------- */
(function () {
    'use strict';

    var ctx = document.body.getAttribute('data-ctx') || '';

    function supportsCamera() {
        return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
    }

    /** Secure context: HTTPS everywhere, or any protocol on localhost. */
    function secureEnough() {
        return window.isSecureContext ||
            location.hostname === 'localhost' ||
            location.hostname === '127.0.0.1';
    }

    function loadJsQr() {
        if (window.jsQR) {
            return Promise.resolve(window.jsQR);
        }
        return new Promise(function (resolve, reject) {
            var s = document.createElement('script');
            s.src = ctx + '/assets/vendor/jsqr/jsQR.js';
            s.onload = function () { resolve(window.jsQR); };
            s.onerror = function () { reject(new Error('Could not load the QR decoder.')); };
            document.head.appendChild(s);
        });
    }

    function CardScanner(field, form) {
        this.field = field;
        this.form = form;
        this.stream = null;
        this.running = false;
        this.detector = null;
        this._build();
    }

    CardScanner.prototype._build = function () {
        var self = this;

        this.button = document.createElement('button');
        this.button.type = 'button';
        this.button.className = 'btn btn-quiet bdcms-scan-btn';
        this.button.textContent = 'Scan with camera';
        this.button.addEventListener('click', function () {
            self.running ? self.stop() : self.start();
        });

        this.panel = document.createElement('div');
        this.panel.className = 'bdcms-scan-panel';
        this.panel.hidden = true;

        this.video = document.createElement('video');
        this.video.setAttribute('playsinline', '');
        this.video.muted = true;

        this.status = document.createElement('div');
        this.status.className = 'bdcms-scan-status';

        this.panel.appendChild(this.video);
        this.panel.appendChild(this.status);

        this.canvas = document.createElement('canvas');
        this.ctx2d = this.canvas.getContext('2d', { willReadFrequently: true });
    };

    /** Puts the button next to the field and the preview under the form. */
    CardScanner.prototype.attach = function (buttonHost, panelHost) {
        buttonHost.appendChild(this.button);
        panelHost.appendChild(this.panel);
    };

    CardScanner.prototype._say = function (message, tone) {
        this.status.textContent = message;
        this.status.className = 'bdcms-scan-status' + (tone ? ' bdcms-scan-' + tone : '');
    };

    CardScanner.prototype.start = function () {
        var self = this;

        if (!supportsCamera()) {
            this.panel.hidden = false;
            this._say('This browser cannot open a camera. Type the card number instead.', 'bad');
            return;
        }
        if (!secureEnough()) {
            this.panel.hidden = false;
            this._say('The camera needs HTTPS, or the site opened on localhost. ' +
                      'Type the card number instead.', 'bad');
            return;
        }

        this.panel.hidden = false;
        this._say('Starting the camera…');
        this.button.textContent = 'Stop camera';

        // The rear camera is the useful one on a tablet at a counter; on a
        // laptop there is only one and the hint is ignored.
        navigator.mediaDevices.getUserMedia({
            video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
            audio: false
        }).then(function (stream) {
            self.stream = stream;
            self.video.srcObject = stream;
            return self.video.play();
        }).then(function () {
            self.running = true;
            self._say('Point the card’s QR code at the camera.');
            return self._pickDecoder();
        }).then(function () {
            self._tick();
        }).catch(function (err) {
            var message = 'Could not open the camera.';
            if (err && (err.name === 'NotAllowedError' || err.name === 'SecurityError')) {
                message = 'Camera permission was refused. Allow it in the browser, ' +
                          'or type the card number instead.';
            } else if (err && err.name === 'NotFoundError') {
                message = 'No camera was found on this machine.';
            }
            self._say(message, 'bad');
            self.stop(true);
        });
    };

    CardScanner.prototype._pickDecoder = function () {
        var self = this;
        if ('BarcodeDetector' in window) {
            return window.BarcodeDetector.getSupportedFormats().then(function (formats) {
                if (formats.indexOf('qr_code') !== -1) {
                    self.detector = new window.BarcodeDetector({ formats: ['qr_code'] });
                    return null;
                }
                return loadJsQr();
            }).catch(function () {
                return loadJsQr();
            });
        }
        return loadJsQr();
    };

    CardScanner.prototype._tick = function () {
        var self = this;
        if (!this.running) {
            return;
        }
        if (this.video.readyState !== this.video.HAVE_ENOUGH_DATA) {
            requestAnimationFrame(function () { self._tick(); });
            return;
        }

        if (this.detector) {
            this.detector.detect(this.video).then(function (codes) {
                if (codes && codes.length) {
                    self._found(codes[0].rawValue);
                } else {
                    requestAnimationFrame(function () { self._tick(); });
                }
            }).catch(function () {
                requestAnimationFrame(function () { self._tick(); });
            });
            return;
        }

        // jsQR path: copy the frame into a canvas and decode the pixels.
        this.canvas.width = this.video.videoWidth;
        this.canvas.height = this.video.videoHeight;
        this.ctx2d.drawImage(this.video, 0, 0, this.canvas.width, this.canvas.height);
        var image = this.ctx2d.getImageData(0, 0, this.canvas.width, this.canvas.height);
        var code = window.jsQR ? window.jsQR(image.data, image.width, image.height, {
            inversionAttempts: 'dontInvert'
        }) : null;

        if (code && code.data) {
            this._found(code.data);
        } else {
            // A quarter-second between attempts: decoding every frame pins a CPU
            // for no benefit, since a hand holding a card is not moving that fast.
            setTimeout(function () { self._tick(); }, 250);
        }
    };

    CardScanner.prototype._found = function (raw) {
        var value = (raw || '').trim().toUpperCase();
        if (!value) {
            return;
        }
        this._say('Read ' + value, 'good');
        this.field.value = value;
        this.stop();

        // Submit for the operator. Scanning a card is the whole intent - making
        // them then click Look up would be a step for nothing.
        if (this.form) {
            this.form.submit();
        }
    };

    CardScanner.prototype.stop = function (quiet) {
        this.running = false;
        this.button.textContent = 'Scan with camera';
        if (this.stream) {
            this.stream.getTracks().forEach(function (t) { t.stop(); });
            this.stream = null;
        }
        this.video.srcObject = null;
        if (!quiet) {
            this.panel.hidden = this.status.textContent.indexOf('Read ') !== 0;
        }
    };

    // ---- wire up whichever card-number field is on this page ----------------

    document.addEventListener('DOMContentLoaded', function () {
        var field = document.getElementById('cardNo');
        if (!field) {
            return;
        }
        var form = field.closest('form');
        var host = field.closest('.bdcms-scanbar') || form;
        if (!host) {
            return;
        }

        var scanner = new CardScanner(field, form);

        var buttonHost = document.createElement('div');
        buttonHost.style.flex = 'none';
        host.appendChild(buttonHost);

        // The preview belongs INSIDE the scan panel, not underneath it: the
        // camera is part of starting a donation, and a video that escapes the
        // panel reads as a separate thing that happened to appear. The scan bar
        // is a wrapping flex row, so the preview claims a full-width row of its
        // own within it.
        scanner.attach(buttonHost, host);

        // Release the camera when leaving the page, so the indicator light does
        // not stay on after the operator has moved elsewhere.
        window.addEventListener('pagehide', function () { scanner.stop(true); });
    });
}());
