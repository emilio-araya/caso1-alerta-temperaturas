// Simulated sensor data for the monitoring API.
// Mirrors app/src/main/java/com/example/caso1/data/api/MockMonitorApi.kt and the thresholds in
// data/model/Modelos.kt (Umbrales). If a threshold changes there, change it here too.

const INTERVALO_MS = 15 * 60_000;           // a new sensor reading every 15 minutes
const HISTORIAL = 24;                       // readings kept per shed
const INTERVALOS_POR_DIA = (24 * 60 * 60_000) / INTERVALO_MS;

const GALPONES = [
  { id: 1, granja: 'Granja Norte', nombre: 'Galpón 1' },
  { id: 2, granja: 'Granja Norte', nombre: 'Galpón 2' },
  { id: 3, granja: 'Granja Sur', nombre: 'Galpón 3' },
  { id: 4, granja: 'Granja Sur', nombre: 'Galpón 4' },
];

const UMBRALES = {
  TEMP_NORMAL_MIN: 18.0,
  TEMP_NORMAL_MAX: 28.0,
  TEMP_ADVERTENCIA_MAX: 32.0,
  HUMEDAD_NORMAL_MIN: 50.0,
  HUMEDAD_NORMAL_MAX: 70.0,
  HUMEDAD_ADVERTENCIA_MAX: 80.0,
};

function evaluar(t, h) {
  const u = UMBRALES;
  if (t > u.TEMP_ADVERTENCIA_MAX || h > u.HUMEDAD_ADVERTENCIA_MAX) return 'CRITICO';
  if (t > u.TEMP_NORMAL_MAX || h > u.HUMEDAD_NORMAL_MAX ||
      t < u.TEMP_NORMAL_MIN || h < u.HUMEDAD_NORMAL_MIN) return 'ADVERTENCIA';
  return 'NORMAL';
}

function causa(t, h) {
  const u = UMBRALES;
  if (t > u.TEMP_ADVERTENCIA_MAX) return 'TEMPERATURA_ALTA';
  if (h > u.HUMEDAD_ADVERTENCIA_MAX) return 'HUMEDAD_ALTA';
  if (t > u.TEMP_NORMAL_MAX) return 'TEMPERATURA_ALTA';
  if (h > u.HUMEDAD_NORMAL_MAX) return 'HUMEDAD_ALTA';
  if (t < u.TEMP_NORMAL_MIN) return 'TEMPERATURA_BAJA';
  if (h < u.HUMEDAD_NORMAL_MIN) return 'HUMEDAD_BAJA';
  return null;
}

// Small seeded PRNG (mulberry32): the same shed and interval always give the same reading,
// so every endpoint sees consistent values, like a real server would.
function aleatorio(semilla) {
  let a = semilla >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let x = Math.imul(a ^ (a >>> 15), 1 | a);
    x = (x + Math.imul(x ^ (x >>> 7), 61 | x)) ^ x;
    return ((x ^ (x >>> 14)) >>> 0) / 4294967296;
  };
}

const redondear = (v) => Math.round(v * 10) / 10;

/** Readings forced by the demo endpoint, keyed by "galponId:intervalo". */
const lecturasForzadas = new Map();

function medicionEn(galponId, intervalo) {
  const forzada = lecturasForzadas.get(`${galponId}:${intervalo}`);
  if (forzada) return forzada;
  const r = aleatorio(galponId * 1_000_003 + intervalo);
  const ruido = (amplitud) => (r() * 2 - 1) * amplitud;
  const fase = (2 * Math.PI * (intervalo % INTERVALOS_POR_DIA)) / INTERVALOS_POR_DIA + galponId * 1.3;
  return {
    galponId,
    temperatura: redondear(26.0 + 7.0 * Math.sin(fase) + ruido(1.5)),
    humedad: redondear(66.0 + 14.0 * Math.sin(fase + Math.PI / 3) + ruido(3.0)),
    fechaHora: intervalo * INTERVALO_MS,
  };
}

const intervaloActual = (ahora) => Math.floor(ahora / INTERVALO_MS);

/** Builds the whole json-server database for the given instant. */
function generarDb(ahora = Date.now()) {
  const actual = intervaloActual(ahora);
  const galpones = [];
  const mediciones = [];
  const alertas = [];

  for (const g of GALPONES) {
    for (let i = 0; i < HISTORIAL; i++) {
      mediciones.push({ id: mediciones.length + 1, ...medicionEn(g.id, actual - i) });
    }
    const ultima = medicionEn(g.id, actual);
    const estado = evaluar(ultima.temperatura, ultima.humedad);
    galpones.push({ ...g, estado });

    const tipo = causa(ultima.temperatura, ultima.humedad);
    if (estado !== 'NORMAL' && tipo) {
      alertas.push({
        id: alertas.length + 1,
        galponId: g.id,
        tipo,
        nivel: estado,
        activa: true,
        fechaHora: ultima.fechaHora,
      });
    }
  }
  return { galpones, mediciones, alertas };
}

/**
 * Demo: forces a critical heat peak in the first shed that is not critical yet
 * (or shed 1 if all are). Returns the affected shed id.
 */
function simularPicoDeCalor(ahora = Date.now()) {
  const actual = intervaloActual(ahora);
  const galponId = generarDb(ahora).galpones.find((g) => g.estado !== 'CRITICO')?.id ?? GALPONES[0].id;
  const normal = medicionEn(galponId, actual);
  lecturasForzadas.set(`${galponId}:${actual}`, { ...normal, temperatura: redondear(35.0 + Math.random() * 1.5) });
  return galponId;
}

module.exports = { generarDb, simularPicoDeCalor, evaluar, causa, UMBRALES, HISTORIAL };
