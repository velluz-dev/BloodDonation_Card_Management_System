# Donor card — 3D object

The BDCMS donor card as a physical artefact: **85.6 × 54 × 0.76 mm** (ID-1),
modelled to scale. Drag to orbit and see the back — magnetic stripe, contact
chip, conditions of use, signature panel. Export as **OBJ + MTL** or **GLB**.

## Open it

**Double-click `serve.cmd`.** It serves this folder on
<http://localhost:8000/> and opens your browser.

> **Do not open `index.html` directly.** Browsers refuse to load JavaScript
> modules over `file://`, so the stage stays empty. The page detects this and
> tells you, but the fix is to serve it over HTTP.

No `serve.cmd`? From this folder run either:

```powershell
jwebserver -p 8000 -d .        # ships with JDK 18+
python -m http.server 8000     # if you have Python
```

## What this is — and is not

This is a **presentation asset** for the report appendix and the demonstration.
It is deliberately *not* part of the running application.

- **The type is blocked in.** The donor name, blood group and card number are
  extruded rectangles standing in for text. It is a model of the artefact, not
  of any particular donor's card, and it is not bound to the database.
- **It is not linked from the app.** Adding it would mean shipping ~2 MB of
  Three.js inside the WAR to display generic geometry. The application's real
  card rendering is [`CardPdfRenderer`](../../src/main/java/com/bdcms/feature/card/CardPdfRenderer.java),
  which produces a printable ID-1 PDF from actual donor data.

For the flat card design as the system prints it, see the card print screen
in the running app, or `card/print.jsp`.

## Exporting

The toolbar exports the model in two formats:

| Format | Use it for |
|---|---|
| **OBJ + MTL** | Two files. Widely readable, materials as flat colours |
| **GLB** | One binary file. What every modern 3D tool imports; keeps the material properties |

Both inherit the scene's units — **metres** — so the card imports at true size
rather than needing a scale factor.

Every mesh and material is named (`front_blood_group`, `back_magnetic_stripe`,
`crimson`, `contact_chip`, …), and those names carry through into the exported
`o` and `usemtl` entries, so the parts stay identifiable in Blender or elsewhere.

## Files

| File | What it is |
|---|---|
| `index.html` | The page: import map, header, stage |
| `card-3d.js` | The model — every dimension and part of the card |
| `three-d-stage.js` | Reusable viewer: renderer, studio lighting, orbit controls, export toolbar |
| `vendor/three/` | Three.js **0.184.0**, vendored |
| `fonts/` | Archivo and Roboto Mono, so the page needs no network |
| `serve.cmd` | Starts a local web server and opens the page |

### Why Three.js is vendored

The original design file loaded Three.js from unpkg with pinned integrity
hashes. Those copies are checked in here instead, so the page works on a machine
with no internet — the same rule the application follows for Bootstrap and its
fonts, and the reason a demonstration on a lab machine will not fail.

The integrity hashes are not carried over because subresource integrity applies
to remote fetches, not local files. The pinned version is recorded above; the
originals came from:

```
https://unpkg.com/three@0.184.0/build/three.module.js
https://unpkg.com/three@0.184.0/build/three.core.js
https://unpkg.com/three@0.184.0/examples/jsm/controls/OrbitControls.js
https://unpkg.com/three@0.184.0/examples/jsm/exporters/OBJExporter.js
https://unpkg.com/three@0.184.0/examples/jsm/exporters/GLTFExporter.js
```

### One change from the design file

`three-d-stage.js` originally posted export telemetry to `window.parent` for the
design canvas that hosted it. Outside that host `window.parent` is the same
window, so the message went nowhere. It has been removed rather than left in as
dead code.
