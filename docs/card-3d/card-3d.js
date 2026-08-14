/**
 * The BDCMS donor card as a physical object, modelled to scale.
 *
 * Taken unchanged from the "Donor Card 3D" design file. Dimensions are ID-1:
 * 85.6 x 54 x 0.76 mm, expressed in metres because the stage exports in scene
 * units and every 3D tool expects metres.
 *
 * Type is blocked in rather than set: the name, blood group and card number are
 * extruded rectangles standing in for text. This is a model of the artefact,
 * not of any particular donor's card - it is not, and cannot be, bound to a
 * record in the database.
 */
import * as THREE from 'three';

const stage = document.querySelector('three-d-stage');
const { THREE: T } = await stage.ready;

const CARD_W = 0.0856, CARD_H = 0.054, CARD_T = 0.00076;
const R = 0.0032;

const mat = {
  body: new T.MeshStandardMaterial({ name: 'card_bone', color: 0xF3EFE7, roughness: 0.42, metalness: 0.04 }),
  back: new T.MeshStandardMaterial({ name: 'card_ink', color: 0x1B1913, roughness: 0.5, metalness: 0.05 }),
  crimson: new T.MeshStandardMaterial({ name: 'crimson', color: 0xB01E2A, roughness: 0.35, metalness: 0.06 }),
  dark: new T.MeshStandardMaterial({ name: 'print_dark', color: 0x2A2620, roughness: 0.62, metalness: 0.03 }),
  grey: new T.MeshStandardMaterial({ name: 'print_grey', color: 0x8C877D, roughness: 0.7, metalness: 0.02 }),
  photo: new T.MeshStandardMaterial({ name: 'photo_panel', color: 0xC9C2B6, roughness: 0.55, metalness: 0.03 }),
  chip: new T.MeshStandardMaterial({ name: 'contact_chip', color: 0xC9A227, roughness: 0.3, metalness: 0.38 }),
  stripe: new T.MeshStandardMaterial({ name: 'magnetic_stripe', color: 0x413B33, roughness: 0.85, metalness: 0.02 })
};

function roundedRectShape(w, h, r) {
  const s = new T.Shape();
  const x = -w / 2, y = -h / 2;
  s.moveTo(x + r, y);
  s.lineTo(x + w - r, y);
  s.quadraticCurveTo(x + w, y, x + w, y + r);
  s.lineTo(x + w, y + h - r);
  s.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  s.lineTo(x + r, y + h);
  s.quadraticCurveTo(x, y + h, x, y + h - r);
  s.lineTo(x, y + r);
  s.quadraticCurveTo(x, y, x + r, y);
  return s;
}

const card = new T.Group();
card.name = 'donor_card';

// Core body: extruded rounded rect, bevelled edges, lying in XY then tipped up.
const bodyGeo = new T.ExtrudeGeometry(roundedRectShape(CARD_W, CARD_H, R), {
  depth: CARD_T, bevelEnabled: true, bevelThickness: 0.00012, bevelSize: 0.00016,
  bevelSegments: 2, curveSegments: 24
});
bodyGeo.translate(0, 0, -CARD_T / 2);
const body = new T.Mesh(bodyGeo, mat.body);
body.name = 'card_body';
body.castShadow = true;
body.receiveShadow = true;
card.add(body);

const BEVEL = 0.00012;
const FRONT = CARD_T / 2 + BEVEL + 0.00006;
const BACK = -CARD_T / 2 - BEVEL - 0.00006;

function plate(name, material, w, h, x, y, side = 'front', lift = 0) {
  const g = new T.Mesh(new T.BoxGeometry(w, h, 0.00012 + lift), material);
  g.name = name;
  g.position.set(x, y, side === 'front' ? FRONT + lift / 2 : BACK - lift / 2);
  g.castShadow = false;
  card.add(g);
  return g;
}

// ——— FRONT ———
// crimson header band, clipped to the card's rounded top corners by insetting width
plate('front_header_band', mat.crimson, CARD_W - 2 * R * 0.55, 0.0035, 0, CARD_H / 2 - 0.0031);

// organisation name + subtitle rules
plate('front_org_name', mat.dark, 0.0245, 0.0018, -CARD_W / 2 + 0.0175, CARD_H / 2 - 0.0092);
plate('front_org_sub', mat.grey, 0.0205, 0.0009, -CARD_W / 2 + 0.0155, CARD_H / 2 - 0.0122);
// logo mark top-right: rounded tile with a raised drop, matching the on-screen mark
const logoTileGeo = new T.ExtrudeGeometry(roundedRectShape(0.0042, 0.0042, 0.001), {
  depth: 0.00016, bevelEnabled: false, curveSegments: 12
});
const logoTile = new T.Mesh(logoTileGeo, mat.crimson);
logoTile.name = 'front_logo_tile';
logoTile.position.set(CARD_W / 2 - 0.0155, CARD_H / 2 - 0.0098, FRONT);
card.add(logoTile);

const dropShape = new T.Shape();
dropShape.moveTo(0, 0.0016);
dropShape.bezierCurveTo(0.0011, 0.0004, 0.0013, -0.0002, 0.0013, -0.0006);
dropShape.absarc(0, -0.0006, 0.0013, 0, -Math.PI, true);
dropShape.bezierCurveTo(-0.0013, -0.0002, -0.0011, 0.0004, 0, 0.0016);
const drop = new T.Mesh(
  new T.ExtrudeGeometry(dropShape, { depth: 0.00012, bevelEnabled: false, curveSegments: 16 }),
  mat.body
);
drop.name = 'front_logo_drop';
drop.position.set(CARD_W / 2 - 0.0155, CARD_H / 2 - 0.0098, FRONT + 0.00016);
card.add(drop);

plate('front_logo_word', mat.grey, 0.0082, 0.0011, CARD_W / 2 - 0.0086, CARD_H / 2 - 0.0098);

// donor photograph panel, slightly recessed frame + raised panel
const photoFrame = new T.Mesh(new T.BoxGeometry(0.0178, 0.0224, 0.00009), mat.grey);
photoFrame.name = 'front_photo_frame';
photoFrame.position.set(-CARD_W / 2 + 0.0118, -0.0025, FRONT);
card.add(photoFrame);
const photoPanel = new T.Mesh(new T.BoxGeometry(0.0166, 0.0212, 0.00016), mat.photo);
photoPanel.name = 'front_photo_panel';
photoPanel.position.set(-CARD_W / 2 + 0.0118, -0.0025, FRONT + 0.00008);
card.add(photoPanel);

// name block
plate('front_label_donor', mat.grey, 0.0062, 0.0008, -CARD_W / 2 + 0.0246, 0.0064);
plate('front_donor_name', mat.dark, 0.0232, 0.0026, -CARD_W / 2 + 0.0331, 0.0026);

// blood group — the largest typographic element, in crimson
plate('front_label_group', mat.grey, 0.0055, 0.0008, -CARD_W / 2 + 0.0242, -0.0032);
const group = new T.Mesh(new T.BoxGeometry(0.0072, 0.0048, 0.00022), mat.crimson);
group.name = 'front_blood_group';
group.position.set(-CARD_W / 2 + 0.0251, -0.0083, FRONT + 0.0001);
card.add(group);

// issue date
plate('front_label_issued', mat.grey, 0.0055, 0.0008, -CARD_W / 2 + 0.0408, -0.0032);
plate('front_issue_date', mat.dark, 0.0112, 0.0016, -CARD_W / 2 + 0.0436, -0.0075);

// QR code: raised quiet-zone tile + a modest module grid so it reads at a glance
const qrSize = 0.0152, qrX = CARD_W / 2 - 0.0122, qrY = -0.0028;
const qrTile = new T.Mesh(new T.BoxGeometry(qrSize, qrSize, 0.00012), mat.body);
qrTile.name = 'front_qr_quietzone';
qrTile.position.set(qrX, qrY, FRONT + 0.00003);
card.add(qrTile);

const N = 13, cell = (qrSize - 0.0022) / N, modGeo = new T.BoxGeometry(cell * 0.94, cell * 0.94, 0.00011);
const qrModules = new T.Group();
qrModules.name = 'front_qr_modules';
// deterministic pseudo-pattern + three finder squares
const finder = (cx, cy) => {
  for (let i = 0; i < N; i++) for (let j = 0; j < N; j++) {
    const dx = Math.abs(i - cx), dy = Math.abs(j - cy);
    if (dx > 3 || dy > 3) continue;
    const ring = Math.max(dx, dy);
    if (ring === 3 || ring === 1 || ring === 0) marks.add(i + ',' + j);
  }
};
const marks = new Set();
finder(3, 3); finder(N - 4, 3); finder(3, N - 4);
let seed = 20260813;
for (let i = 0; i < N; i++) for (let j = 0; j < N; j++) {
  seed = (seed * 1103515245 + 12345) & 0x7fffffff;
  const inFinder = (i < 8 && j < 8) || (i > N - 9 && j < 8) || (i < 8 && j > N - 9);
  if (!inFinder && (seed >> 16) % 100 < 46) marks.add(i + ',' + j);
}
for (const key of marks) {
  const [i, j] = key.split(',').map(Number);
  const m = new T.Mesh(modGeo, mat.dark);
  m.position.set(
    qrX - qrSize / 2 + 0.0011 + (i + 0.5) * cell,
    qrY - qrSize / 2 + 0.0011 + (j + 0.5) * cell,
    FRONT + 0.00012
  );
  qrModules.add(m);
}
card.add(qrModules);

// footer rule + card number
plate('front_footer_rule', mat.grey, CARD_W - 0.024, 0.00035, 0, -CARD_H / 2 + 0.0092);
plate('front_card_number', mat.dark, 0.0198, 0.0021, -CARD_W / 2 + 0.0218, -CARD_H / 2 + 0.0056);
plate('front_validity_note', mat.grey, 0.0148, 0.0009, CARD_W / 2 - 0.0194, -CARD_H / 2 + 0.006);

// ——— BACK ———
const backFace = new T.Mesh(
  new T.ExtrudeGeometry(roundedRectShape(CARD_W - 0.0004, CARD_H - 0.0004, R * 0.9), {
    depth: 0.00014, bevelEnabled: false, curveSegments: 24
  }),
  mat.back
);
backFace.name = 'back_face';
backFace.position.z = BACK - 0.00014;
card.add(backFace);

const BZ = BACK - 0.00016;
function backPlate(name, material, w, h, x, y, lift = 0.00008) {
  const m = new T.Mesh(new T.BoxGeometry(w, h, 0.00008 + lift), material);
  m.name = name;
  m.position.set(x, y, BZ - lift / 2);
  card.add(m);
  return m;
}

// magnetic stripe across the top of the back
backPlate('back_magnetic_stripe', mat.stripe, CARD_W - 0.0022, 0.0092, 0, CARD_H / 2 - 0.0092, 0.00016);

// contact chip
const chip = new T.Mesh(new T.BoxGeometry(0.0106, 0.0082, 0.0003), mat.chip);
chip.name = 'back_contact_chip';
chip.position.set(-CARD_W / 2 + 0.0116, 0.0022, BZ - 0.00015);
card.add(chip);
for (let i = 0; i < 3; i++) {
  const line = new T.Mesh(new T.BoxGeometry(0.0092, 0.00035, 0.00012), mat.back);
  line.name = 'back_chip_contact_' + (i + 1);
  line.position.set(-CARD_W / 2 + 0.0116, 0.0022 + (i - 1) * 0.0022, BZ - 0.00031);
  card.add(line);
}

// conditions-of-use text block
backPlate('back_conditions_label', mat.crimson, 0.0148, 0.0009, -CARD_W / 2 + 0.0324, 0.0058);
for (let i = 0; i < 5; i++) {
  const w = [0.0402, 0.0388, 0.0396, 0.0352, 0.0246][i];
  backPlate('back_conditions_line_' + (i + 1), mat.grey, w, 0.00085, -CARD_W / 2 + 0.025 + w / 2, 0.0026 - i * 0.0028);
}

// signature panel
backPlate('back_signature_panel', mat.body, 0.0362, 0.0072, -CARD_W / 2 + 0.0242, -CARD_H / 2 + 0.0072, 0.0001);
backPlate('back_signature_rule', mat.grey, 0.0318, 0.0003, -CARD_W / 2 + 0.0242, -CARD_H / 2 + 0.0056, 0.00016);

// helpline + card number on the back, right side
backPlate('back_helpline', mat.grey, 0.0164, 0.0014, CARD_W / 2 - 0.0126, -CARD_H / 2 + 0.0102);
backPlate('back_card_number', mat.body, 0.0186, 0.0016, CARD_W / 2 - 0.0137, -CARD_H / 2 + 0.0058);

// Present standing, tipped back a little, resting on its lower edge.
card.rotation.x = -Math.PI / 2;
card.rotation.z = 0;
const holder = new T.Group();
holder.name = 'donor_card_display';
holder.add(card);
card.rotation.set(-0.14, 0, 0);
card.position.y = CARD_H / 2 * Math.cos(0.14);

window.donorCard = holder;
stage.setObject(holder);
