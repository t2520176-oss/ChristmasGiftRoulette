/* FigureCraft viewer: Z-up scene, runs fully offline (Three.js bundled locally). */
(function () {
  'use strict';
  const canvas = document.getElementById('c');
  const msgEl = document.getElementById('msg');
  let bridge = null;
  let renderer;
  try {
    renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false, powerPreference: 'default' });
  } catch (e) {
    msgEl.textContent = 'WebGL is not available.\nWebGL을 사용할 수 없습니다.';
    if (window.qt) new QWebChannel(qt.webChannelTransport, ch => ch.objects.bridge.viewerError('webgl'));
    return;
  }
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
  renderer.setClearColor(0x14161b, 1);

  const scene = new THREE.Scene();
  const camera = new THREE.PerspectiveCamera(35, 1, 0.5, 5000);
  camera.up.set(0, 0, 1);
  const controls = new OrbitControls(camera, canvas);
  controls.enableDamping = false;
  controls.screenSpacePanning = true;
  controls.addEventListener('change', render);

  scene.add(new THREE.HemisphereLight(0xffffff, 0x555a66, 1.7));
  const key = new THREE.DirectionalLight(0xffffff, 2.4); key.position.set(-60, -120, 160); scene.add(key);
  const fill = new THREE.DirectionalLight(0xffffff, 0.9); fill.position.set(120, 80, 40); scene.add(fill);

  const grid = new THREE.GridHelper(300, 30, 0x4a5163, 0x2a2f3a);
  grid.rotation.x = Math.PI / 2;           // lie in the XY plane (build plate)
  scene.add(grid);

  const root = new THREE.Group(); scene.add(root);
  let mesh = null, wire = null, bboxHelper = null, cutPlane = null, glbGroup = null;
  let data = null;                           // decoded payload
  let mode = 'original', wireOn = false;
  let hiddenLabels = new Set(), selRegion = -1;
  let faceMap = null;                        // visible-face index -> source face index
  let bounds = null, bboxOn = false;

  function setMsg(t) { msgEl.textContent = t || ''; }
  function render() { renderer.render(scene, camera); }
  function resize() {
    const w = canvas.clientWidth || window.innerWidth, h = canvas.clientHeight || window.innerHeight;
    renderer.setSize(w, h, false); camera.aspect = w / h; camera.updateProjectionMatrix(); render();
  }
  window.addEventListener('resize', resize);

  function b64ToBytes(b64) {
    const bin = atob(b64), n = bin.length, u = new Uint8Array(n);
    for (let i = 0; i < n; i++) u[i] = bin.charCodeAt(i);
    return u;
  }
  function hsvToRgb(h, s, v) {
    const i = Math.floor(h * 6), f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
    const m = [[v, t, p], [q, v, p], [p, v, t], [p, q, v], [t, p, v], [v, p, q]][i % 6];
    return [m[0] * 255, m[1] * 255, m[2] * 255];
  }

  function clearModel() {
    [mesh, wire, bboxHelper, glbGroup].forEach(o => { if (o) { root.remove(o); if (o.geometry) o.geometry.dispose(); } });
    mesh = wire = bboxHelper = glbGroup = null;
  }

  // ----- binary payload ------------------------------------------------
  function decode(payload) {
    const meta = payload.meta, bytes = b64ToBytes(payload.b64), s = meta.sizes;
    let off = 0; const take = (n) => { const b = bytes.buffer.slice(bytes.byteOffset + off, bytes.byteOffset + off + n); off += n; return b; };
    const d = { faces: meta.faces, info: meta.info || {}, bounds: meta.bounds };
    d.pos = new Float32Array(take(s.pos)); d.nrm = new Int8Array(take(s.nrm));
    d.orig = new Uint8Array(take(s.orig)); d.print = new Uint8Array(take(s.print));
    d.label = new Uint8Array(take(s.label)); d.region = new Uint32Array(take(s.region));
    return d;
  }

  function rebuild(keepCamera) {
    if (!data) return;
    [mesh, wire].forEach(o => { if (o) { root.remove(o); o.geometry.dispose(); } });
    const F = data.faces;
    const keep = [];
    for (let f = 0; f < F; f++) if (!hiddenLabels.has(data.label[f])) keep.push(f);
    faceMap = Uint32Array.from(keep);
    const n = keep.length;
    const pos = new Float32Array(n * 9), nrm = new Int8Array(n * 9), col = new Float32Array(n * 9);
    for (let i = 0; i < n; i++) {
      const f = keep[i];
      pos.set(data.pos.subarray(f * 9, f * 9 + 9), i * 9);
      nrm.set(data.nrm.subarray(f * 9, f * 9 + 9), i * 9);
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    g.setAttribute('normal', new THREE.BufferAttribute(nrm, 3, true));
    g.setAttribute('color', new THREE.BufferAttribute(col, 3, false));
    mesh = new THREE.Mesh(g, new THREE.MeshStandardMaterial({ vertexColors: true, roughness: 0.62, metalness: 0.0,
                                                              side: THREE.DoubleSide }));
    root.add(mesh);
    wire = new THREE.LineSegments(new THREE.WireframeGeometry(g), new THREE.LineBasicMaterial({ color: 0x0a0c10, transparent: true, opacity: 0.35 }));
    wire.visible = wireOn; root.add(wire);
    applyColors();
    if (!keepCamera) resetView();
    render();
  }

  const LIN = new Float32Array(256).map((_, i) => { const c = i / 255; return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); });
  function applyColors() {
    if (!mesh || !data) return;
    const col = mesh.geometry.getAttribute('color'), n = faceMap.length;
    const src = mode === 'print' ? data.print : data.orig;
    for (let i = 0; i < n; i++) {
      const f = faceMap[i]; let r, g, b;
      if (mode === 'parts') {
        const id = data.region[f]; const c = hsvToRgb(((id * 0.618034) % 1), 0.55, 0.92); r = c[0]; g = c[1]; b = c[2];
      } else { r = src[f * 9]; g = src[f * 9 + 1]; b = src[f * 9 + 2]; }
      if (selRegion >= 0 && data.region[f] === selRegion) { r = r * 0.45 + 255 * 0.55; g = g * 0.45 + 90; b = b * 0.45 + 40; }
      const lr = LIN[Math.max(0, Math.min(255, r | 0))], lg = LIN[Math.max(0, Math.min(255, g | 0))], lb = LIN[Math.max(0, Math.min(255, b | 0))];
      for (let k = 0; k < 3; k++) { col.array[i * 9 + k * 3] = lr; col.array[i * 9 + k * 3 + 1] = lg; col.array[i * 9 + k * 3 + 2] = lb; }
    }
    col.needsUpdate = true;
  }

  // ----- public API ------------------------------------------------------
  const api = {
    loadPayload(json) {
      try {
        clearModel(); setMsg('');
        data = decode(JSON.parse(json)); bounds = data.bounds; hiddenLabels = new Set(); selRegion = -1;
        rebuild(false); updateBBox();
      } catch (e) { setMsg('Viewer error: ' + e); }
    },
    loadGLB(b64) {
      try {
        clearModel(); data = null; setMsg('');
        new GLTFLoader().parse(b64ToBytes(b64).buffer, '', gltf => {
          glbGroup = new THREE.Group(); glbGroup.add(gltf.scene);
          glbGroup.rotation.x = Math.PI / 2;           // glTF is Y-up, the scene is Z-up
          root.add(glbGroup);
          const box = new THREE.Box3().setFromObject(glbGroup);
          glbGroup.position.z -= box.min.z; glbGroup.updateMatrixWorld(true);
          const b2 = new THREE.Box3().setFromObject(glbGroup);
          bounds = [[b2.min.x, b2.min.y, b2.min.z], [b2.max.x, b2.max.y, b2.max.z]];
          resetView(); updateBBox(); render();
        }, err => setMsg('GLB error: ' + err));
      } catch (e) { setMsg('Viewer error: ' + e); }
    },
    clear() { clearModel(); data = null; bounds = null; setMsg(''); render(); },
    setMode(m) { mode = m; applyColors(); render(); },
    setWireframe(on) { wireOn = !!on; if (wire) wire.visible = wireOn; render(); },
    setBBox(on) { bboxOn = !!on; updateBBox(); render(); },
    setLabelVisible(label, vis) { if (vis) hiddenLabels.delete(label); else hiddenLabels.add(label); rebuild(true); },
    setSelectedRegion(id) { selRegion = id; applyColors(); render(); },
    setCutPlane(z) {
      if (cutPlane) { root.remove(cutPlane); cutPlane.geometry.dispose(); cutPlane = null; }
      if (z === null || z === undefined || !bounds) { render(); return; }
      const w = (bounds[1][0] - bounds[0][0]) * 1.5 + 20, h = (bounds[1][1] - bounds[0][1]) * 1.5 + 20;
      cutPlane = new THREE.Mesh(new THREE.PlaneGeometry(w, h), new THREE.MeshBasicMaterial({ color: 0xff3b3b, transparent: true, opacity: 0.35, side: THREE.DoubleSide, depthWrite: false }));
      cutPlane.position.set((bounds[0][0] + bounds[1][0]) / 2, (bounds[0][1] + bounds[1][1]) / 2, z);
      root.add(cutPlane); render();
    },
    resetView() { resetView(); },
    setView(name) { setView(name); },
  };
  window.fc = api;

  function updateBBox() {
    if (bboxHelper) { root.remove(bboxHelper); bboxHelper.geometry.dispose(); bboxHelper = null; }
    if (bboxOn && bounds) {
      const box = new THREE.Box3(new THREE.Vector3(...bounds[0]), new THREE.Vector3(...bounds[1]));
      bboxHelper = new THREE.Box3Helper(box, 0x4da3ff); root.add(bboxHelper);
    }
  }

  function center() { return bounds ? new THREE.Vector3((bounds[0][0] + bounds[1][0]) / 2, (bounds[0][1] + bounds[1][1]) / 2, (bounds[0][2] + bounds[1][2]) / 2) : new THREE.Vector3(0, 0, 40); }
  function radius() { return bounds ? Math.max(20, new THREE.Vector3(bounds[1][0] - bounds[0][0], bounds[1][1] - bounds[0][1], bounds[1][2] - bounds[0][2]).length() / 2) : 80; }
  function setView(name) {
    const c = center(), r = radius() * 2.6;
    const dir = { front: [0, -1, 0.0001], back: [0, 1, 0.0001], left: [-1, 0, 0.0001], right: [1, 0, 0.0001], top: [0, -0.0001, 1], bottom: [0, -0.0001, -1], iso: [0.55, -1, 0.6] }[name] || [0.55, -1, 0.6];
    const v = new THREE.Vector3(...dir).normalize().multiplyScalar(r);
    camera.position.copy(c).add(v); controls.target.copy(c); camera.near = r / 100; camera.far = r * 20; camera.updateProjectionMatrix();
    controls.update(); render();
  }
  function resetView() { setView('iso'); }

  // click-to-pick a colour region (ignores drags)
  let down = null;
  canvas.addEventListener('pointerdown', e => { down = [e.clientX, e.clientY]; });
  canvas.addEventListener('pointerup', e => {
    if (!down || !mesh || !data) return;
    if (Math.hypot(e.clientX - down[0], e.clientY - down[1]) > 4) return;
    const rect = canvas.getBoundingClientRect();
    const ndc = new THREE.Vector2(((e.clientX - rect.left) / rect.width) * 2 - 1, -((e.clientY - rect.top) / rect.height) * 2 + 1);
    const rc = new THREE.Raycaster(); rc.setFromCamera(ndc, camera);
    const hit = rc.intersectObject(mesh, false)[0];
    if (hit && hit.faceIndex !== undefined && bridge) { bridge.regionPicked(data.region[faceMap[hit.faceIndex]]); }
  });

  if (window.qt && qt.webChannelTransport) {
    new QWebChannel(qt.webChannelTransport, ch => { bridge = ch.objects.bridge; bridge.viewerReady(); });
  }
  resize(); setView('iso');
  new ResizeObserver(resize).observe(canvas);
})();
