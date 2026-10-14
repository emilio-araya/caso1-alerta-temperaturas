process.env.NODE_ENV = 'test';

const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { crearServidor } = require('./server');
const { evaluar, HISTORIAL } = require('./datos');

let http;
let base;

before(async () => {
  http = crearServidor().listen(0);
  await new Promise((resolve) => http.once('listening', resolve));
  base = `http://127.0.0.1:${http.address().port}/api/v1`;
});

after(() => http.close());

const get = async (ruta) => {
  const res = await fetch(`${base}/${ruta}`);
  return { status: res.status, body: await res.json() };
};

test('thresholds match the app (Umbrales in Modelos.kt) at the boundaries', () => {
  assert.equal(evaluar(28.0, 60), 'NORMAL');
  assert.equal(evaluar(28.1, 60), 'ADVERTENCIA');
  assert.equal(evaluar(32.0, 60), 'ADVERTENCIA');
  assert.equal(evaluar(32.1, 60), 'CRITICO');
  assert.equal(evaluar(24, 70.0), 'NORMAL');
  assert.equal(evaluar(24, 70.1), 'ADVERTENCIA');
  assert.equal(evaluar(24, 80.0), 'ADVERTENCIA');
  assert.equal(evaluar(24, 80.1), 'CRITICO');
  assert.equal(evaluar(17.9, 60), 'ADVERTENCIA');
  assert.equal(evaluar(24, 49.9), 'ADVERTENCIA');
});

test('GET galpones returns the 4 sheds with a state', async () => {
  const { status, body } = await get('galpones');
  assert.equal(status, 200);
  assert.deepEqual(body.map((g) => g.id), [1, 2, 3, 4]);
  assert.ok(body.every((g) => ['NORMAL', 'ADVERTENCIA', 'CRITICO'].includes(g.estado)));
});

test('GET galpones/:id/mediciones returns that shed only, newest first', async () => {
  const { status, body } = await get('galpones/2/mediciones');
  assert.equal(status, 200);
  assert.equal(body.length, HISTORIAL);
  assert.ok(body.every((m) => m.galponId === 2));
  for (let i = 1; i < body.length; i++) assert.ok(body[i - 1].fechaHora > body[i].fechaHora);
  assert.ok(Date.now() - body[0].fechaHora < 15 * 60_000, 'latest reading is from the current interval');
});

test('GET galpones/99/mediciones answers 404', async () => {
  const { status } = await get('galpones/99/mediciones');
  assert.equal(status, 404);
});

test('active alerts exist exactly for sheds that are not NORMAL', async () => {
  const galpones = (await get('galpones')).body;
  const alertas = (await get('alertas/activas')).body;
  const fueraDeRango = galpones.filter((g) => g.estado !== 'NORMAL').map((g) => g.id).sort();
  assert.deepEqual(alertas.map((a) => a.galponId).sort(), fueraDeRango);
});

test('POST simulaciones/pico-calor makes that shed critical with a heat alert', async () => {
  const res = await fetch(`${base}/simulaciones/pico-calor`, { method: 'POST' });
  assert.equal(res.status, 200);
  const { galponId } = await res.json();

  const galpon = (await get('galpones')).body.find((g) => g.id === galponId);
  assert.equal(galpon.estado, 'CRITICO');
  const alerta = (await get('alertas/activas')).body.find((a) => a.galponId === galponId);
  assert.equal(alerta.nivel, 'CRITICO');
  assert.equal(alerta.tipo, 'TEMPERATURA_ALTA');
});
