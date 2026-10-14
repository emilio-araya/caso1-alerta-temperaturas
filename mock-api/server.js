// Simulated monitoring API served with json-server.
// Routes match app/src/main/java/com/example/caso1/data/api/MonitorApi.kt:
//   GET  /api/v1/galpones
//   GET  /api/v1/galpones/:id/mediciones   (newest first)
//   GET  /api/v1/alertas/activas
//   POST /api/v1/simulaciones/pico-calor   (demo)
// Usage: npm install && npm start, then npm run android   (PORT env var, default 3000)

const jsonServer = require('json-server');
const { generarDb, simularPicoDeCalor, HISTORIAL } = require('./datos');

function crearServidor() {
  const server = jsonServer.create();
  const router = jsonServer.router(generarDb());

  server.use(jsonServer.defaults({ logger: process.env.NODE_ENV !== 'test' }));

  // Data depends on the current time: refresh the in-memory database before each request.
  server.use((req, res, next) => {
    router.db.setState(generarDb());
    next();
  });

  server.post('/api/v1/simulaciones/pico-calor', (req, res) => {
    res.json({ galponId: simularPicoDeCalor() });
  });

  // Unknown shed: 404, like the in-app simulated server.
  server.get('/api/v1/galpones/:id/mediciones', (req, res, next) => {
    const existe = router.db.get('galpones').find({ id: Number(req.params.id) }).value();
    if (!existe) return res.status(404).json({ error: `Galpón no encontrado: ${req.params.id}` });
    next();
  });

  server.use(jsonServer.rewriter({
    '/api/v1/galpones/:id/mediciones': `/mediciones?galponId=:id&_sort=fechaHora&_order=desc&_limit=${HISTORIAL}`,
    '/api/v1/alertas/activas': '/alertas?activa=true',
    '/api/v1/*': '/$1',
  }));
  server.use(router);
  return server;
}

if (require.main === module) {
  const puerto = Number(process.env.PORT) || 3000;
  crearServidor().listen(puerto, '0.0.0.0', () => {
    console.log(`Monitoring API on http://localhost:${puerto}/api/v1/`);
    console.log(`Android device/emulator: run "npm run android" (adb reverse tcp:${puerto} tcp:${puerto}).`);
  });
}

module.exports = { crearServidor };
