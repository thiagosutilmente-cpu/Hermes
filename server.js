/**
 * Servidor Backend Express.js — Radar Delivery AI
 * Estrutura de API REST para listar e filtrar ofertas de entrega para os entregadores.
 * Também atua como supervisor resiliente para garantir disponibilidade do ecossistema.
 */

const express = require('express');
const cors = require('cors');
const http = require('http');
const path = require('path');
const { spawn } = require('child_process');

const app = express();
const PORT = process.env.APP_PORT || process.env.DEFAULT_APP_PORT || 3000;
const PYTHON_BACKEND_PORT = 5000;

// Middlewares essenciais
app.use(cors());
app.use(express.json());

// Log amigável de requisições
app.use((req, res, next) => {
  console.log(`[EXPRESS-RADAR] ${new Date().toLocaleTimeString()} ${req.method} ${req.originalUrl}`);
  next();
});

// =========================================================================
// BANCO DE DADOS DE OFERTAS DE ENTREGA (Mock em memória para entregadores)
// =========================================================================
let deliveryOffers = [
  {
    id: 'off_ifood_101',
    app: 'iFood',
    restaurant: 'Burger King — Av. Paulista',
    pickupAddress: 'Av. Paulista, 1000 - Bela Vista, SP',
    deliveryAddress: 'Rua Augusta, 1420 - Consolação, SP',
    pickupLat: -23.5645,
    pickupLng: -46.6525,
    deliveryLat: -23.5535,
    deliveryLng: -46.6590,
    value: 28.50,
    distanceKm: 3.8,
    estimatedTimeMin: 16,
    isMultiStack: false,
    status: 'pending', // pending | accepted | declined | completed
    urgency: 'high',
    highDemandZone: 'Av. Paulista / Jardins',
    driverScoreBonus: 10,
    createdAt: new Date(Date.now() - 3 * 60000).toISOString()
  },
  {
    id: 'off_stack_102',
    app: 'iFood + Rappi',
    restaurant: 'Madero Container & Bacio di Latte',
    pickupAddress: 'Rua Amauri, 250 - Itaim Bibi, SP',
    deliveryAddress: 'Rua Funchal, 418 - Vila Olímpia, SP',
    pickupLat: -23.5855,
    pickupLng: -46.6820,
    deliveryLat: -23.5935,
    deliveryLng: -46.6890,
    value: 46.00,
    distanceKm: 5.2,
    estimatedTimeMin: 24,
    isMultiStack: true,
    status: 'pending',
    urgency: 'critical',
    highDemandZone: 'Faria Lima / Itaim',
    driverScoreBonus: 25,
    createdAt: new Date(Date.now() - 5 * 60000).toISOString()
  },
  {
    id: 'off_uber_103',
    app: 'Uber Direct',
    restaurant: 'Drogasil — Pinheiros',
    pickupAddress: 'Rua dos Pinheiros, 700 - Pinheiros, SP',
    deliveryAddress: 'Av. Rebouças, 2200 - Pinheiros, SP',
    pickupLat: -23.5670,
    pickupLng: -46.6835,
    deliveryLat: -23.5630,
    deliveryLng: -46.6780,
    value: 19.50,
    distanceKm: 2.4,
    estimatedTimeMin: 11,
    isMultiStack: false,
    status: 'pending',
    urgency: 'medium',
    highDemandZone: 'Pinheiros',
    driverScoreBonus: 5,
    createdAt: new Date(Date.now() - 8 * 60000).toISOString()
  },
  {
    id: 'off_99_104',
    app: '99Food',
    restaurant: 'Pastelaria & Lanches Mooca',
    pickupAddress: 'Rua da Mooca, 1800 - Mooca, SP',
    deliveryAddress: 'Av. Salim Farah Maluf, 4200 - Tatuapé, SP',
    pickupLat: -23.5550,
    pickupLng: -46.5990,
    deliveryLat: -23.5410,
    deliveryLng: -46.5780,
    value: 14.00,
    distanceKm: 7.5,
    estimatedTimeMin: 28,
    isMultiStack: false,
    status: 'pending',
    urgency: 'low',
    highDemandZone: null,
    driverScoreBonus: 0,
    createdAt: new Date(Date.now() - 12 * 60000).toISOString()
  },
  {
    id: 'off_rappi_105',
    app: 'Rappi',
    restaurant: 'Starbucks — Frei Caneca',
    pickupAddress: 'Rua Frei Caneca, 569 - Consolação, SP',
    deliveryAddress: 'Rua da Consolação, 2300 - Cerqueira César, SP',
    pickupLat: -23.5528,
    pickupLng: -46.6540,
    deliveryLat: -23.5590,
    deliveryLng: -46.6620,
    value: 22.00,
    distanceKm: 2.9,
    estimatedTimeMin: 14,
    isMultiStack: false,
    status: 'pending',
    urgency: 'medium',
    highDemandZone: 'Consolação',
    driverScoreBonus: 10,
    createdAt: new Date(Date.now() - 2 * 60000).toISOString()
  },
  {
    id: 'off_stack_106',
    app: 'iFood + 99',
    restaurant: 'Sukiya & Ragazzo Aclimação',
    pickupAddress: 'Rua Vergueiro, 1200 - Paraíso, SP',
    deliveryAddress: 'Av. Lins de Vasconcelos, 1600 - Cambuci, SP',
    pickupLat: -23.5710,
    pickupLng: -46.6415,
    deliveryLat: -23.5680,
    deliveryLng: -46.6270,
    value: 31.00,
    distanceKm: 4.5,
    estimatedTimeMin: 20,
    isMultiStack: true,
    status: 'pending',
    urgency: 'high',
    highDemandZone: 'Aclimação / Paraíso',
    driverScoreBonus: 18,
    createdAt: new Date(Date.now() - 1 * 60000).toISOString()
  }
];

// Estado global em memória da localização em tempo real do entregador
let currentDriverLocation = {
  latitude: -23.561684,
  longitude: -46.655981,
  accuracy: 4.2,
  speed: 0.0,
  heading: 0.0,
  altitude: 780.0,
  isMoving: false,
  safetyLock: false,
  lastUpdated: new Date().toISOString(),
  source: 'default_initial'
};

// Histórico de telemetria de localização em tempo real (últimos 50 pontos)
let driverLocationHistory = [];

/**
 * Cálculo de distância geodésica pela fórmula de Haversine (em quilômetros)
 */
function calculateHaversineKm(lat1, lon1, lat2, lon2) {
  const R = 6371.0; // Raio da Terra em km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Number((R * c).toFixed(2));
}

// Funções utilitárias de cálculo de rentabilidade
function calculateMetrics(offer, driverLat = null, driverLng = null) {
  const gainPerKm = offer.distanceKm > 0 ? Number((offer.value / offer.distanceKm).toFixed(2)) : offer.value;
  const estimatedFuelCost = Number((offer.distanceKm * 0.17).toFixed(2));
  const estimatedNetProfit = Number(Math.max(0, offer.value - estimatedFuelCost).toFixed(2));
  
  // Cálculo de proximidade até o restaurante de coleta
  let proximityKm = null;
  let estimatedTimeToPickupMin = null;
  const effectiveLat = driverLat !== null ? driverLat : currentDriverLocation.latitude;
  const effectiveLng = driverLng !== null ? driverLng : currentDriverLocation.longitude;

  if (offer.pickupLat !== undefined && offer.pickupLng !== undefined && effectiveLat !== null && effectiveLng !== null) {
    proximityKm = calculateHaversineKm(effectiveLat, effectiveLng, offer.pickupLat, offer.pickupLng);
    // Estimativa de deslocamento até o restaurante (~25 km/h média moto trânsito SP)
    estimatedTimeToPickupMin = Math.max(1, Math.round(proximityKm * 2.5 + 2));
  }

  // Recomendação neural da IA para o entregador
  let recommendation = 'DECLINE';
  let recommendationReason = 'Rentabilidade abaixo da média recomendada';
  
  if (offer.isMultiStack || gainPerKm >= 5.0) {
    recommendation = 'ACCEPT';
    recommendationReason = offer.isMultiStack
      ? 'Multi-Stack de alto valor por km acumulado'
      : 'Excelente valor por km (acima de R$ 5,00/km)';
  } else if (gainPerKm >= 3.5 && offer.distanceKm <= 4.0) {
    recommendation = 'ACCEPT';
    recommendationReason = 'Distância curta compensa trajeto rápido';
  } else if (offer.distanceKm > 6.0) {
    recommendation = 'DECLINE';
    recommendationReason = 'Distância excessiva com retorno vazio';
  }

  // Se o restaurante estiver excessivamente longe da posição atual do entregador (> 6 km até a coleta)
  if (proximityKm !== null && proximityKm > 6.0 && !offer.isMultiStack) {
    recommendation = 'DECLINE';
    recommendationReason = `Ponto de coleta muito distante (${proximityKm} km de deslocamento vazio)`;
  }

  return {
    ...offer,
    gainPerKm,
    estimatedFuelCost,
    estimatedNetProfit,
    proximityKm,
    estimatedTimeToPickupMin,
    aiRecommendation: {
      action: recommendation,
      confidence: recommendation === 'ACCEPT' ? 0.92 : 0.84,
      reason: recommendationReason
    }
  };
}

// =========================================================================
// ROTAS DA API DE OFERTAS DE ENTREGA (Express.js)
// =========================================================================

/**
 * GET /api/offers
 * Endpoint principal com múltiplos filtros:
 * - app: filtrar por aplicativo (iFood, Rappi, Uber, 99)
 * - status: pending, accepted, declined
 * - minGainPerKm: piso mínimo de R$/km desejado pelo entregador
 * - maxDistanceKm: distância máxima permitida da corrida
 * - minValue: valor mínimo em reais
 * - isMultiStack: true/false para ofertas agrupadas
 * - search: busca por restaurante ou endereço
 * - sort: 'gain' (maior R$/km), 'value' (maior valor), 'distance' (menor distância corrida), 'proximity' (mais perto de você)
 * - maxProximityKm: raio máximo em km da posição atual do entregador até o restaurante de coleta
 * - lat / lng: coordenadas em tempo real opcionais do entregador (se omitidas, usa currentDriverLocation)
 */
app.get('/api/offers', (req, res) => {
  const {
    app: appFilter,
    status = 'pending',
    minGainPerKm,
    maxDistanceKm,
    minValue,
    isMultiStack,
    search,
    sort,
    maxProximityKm,
    lat,
    lng
  } = req.query;

  // Atualiza coordenadas em tempo real se passadas na query
  let customLat = null;
  let customLng = null;
  if (lat !== undefined && lng !== undefined) {
    const parsedLat = parseFloat(lat);
    const parsedLng = parseFloat(lng);
    if (!isNaN(parsedLat) && !isNaN(parsedLng)) {
      customLat = parsedLat;
      customLng = parsedLng;
      // Sincroniza estado de localização em tempo real
      currentDriverLocation.latitude = parsedLat;
      currentDriverLocation.longitude = parsedLng;
      currentDriverLocation.lastUpdated = new Date().toISOString();
      currentDriverLocation.source = 'api_query';
    }
  }

  const driverLat = customLat !== null ? customLat : currentDriverLocation.latitude;
  const driverLng = customLng !== null ? customLng : currentDriverLocation.longitude;

  let results = deliveryOffers.map(o => calculateMetrics(o, driverLat, driverLng));

  // Filtro por status
  if (status && status !== 'all') {
    results = results.filter(o => o.status.toLowerCase() === status.toLowerCase());
  }

  // Filtro por aplicativo parceiro
  if (appFilter) {
    results = results.filter(o => o.app.toLowerCase().includes(appFilter.toLowerCase()));
  }

  // Filtro por piso de R$/km
  if (minGainPerKm) {
    const minGain = parseFloat(minGainPerKm);
    if (!isNaN(minGain)) {
      results = results.filter(o => o.gainPerKm >= minGain);
    }
  }

  // Filtro por distância máxima
  if (maxDistanceKm) {
    const maxDist = parseFloat(maxDistanceKm);
    if (!isNaN(maxDist)) {
      results = results.filter(o => o.distanceKm <= maxDist);
    }
  }

  // Filtro por proximidade máxima até o restaurante (Raio de Coleta)
  if (maxProximityKm) {
    const maxProx = parseFloat(maxProximityKm);
    if (!isNaN(maxProx)) {
      results = results.filter(o => o.proximityKm !== null && o.proximityKm <= maxProx);
    }
  }

  // Filtro por valor mínimo da corrida
  if (minValue) {
    const minVal = parseFloat(minValue);
    if (!isNaN(minVal)) {
      results = results.filter(o => o.value >= minVal);
    }
  }

  // Filtro por multi-stack agrupado
  if (isMultiStack !== undefined) {
    const isStackBool = isMultiStack === 'true';
    results = results.filter(o => o.isMultiStack === isStackBool);
  }

  // Busca textual
  if (search) {
    const term = search.toLowerCase();
    results = results.filter(o =>
      o.restaurant.toLowerCase().includes(term) ||
      o.pickupAddress.toLowerCase().includes(term) ||
      o.deliveryAddress.toLowerCase().includes(term) ||
      o.app.toLowerCase().includes(term)
    );
  }

  // Ordenação inteligente
  if (sort === 'gain') {
    results.sort((a, b) => b.gainPerKm - a.gainPerKm);
  } else if (sort === 'value') {
    results.sort((a, b) => b.value - a.value);
  } else if (sort === 'distance') {
    results.sort((a, b) => a.distanceKm - b.distanceKm);
  } else if (sort === 'proximity') {
    results.sort((a, b) => (a.proximityKm || 999) - (b.proximityKm || 999));
  }

  res.json({
    success: true,
    totalCount: results.length,
    driverLocation: {
      latitude: driverLat,
      longitude: driverLng,
      accuracy: currentDriverLocation.accuracy,
      speed: currentDriverLocation.speed,
      lastUpdated: currentDriverLocation.lastUpdated,
      source: currentDriverLocation.source
    },
    filtersApplied: {
      app: appFilter || null,
      status,
      minGainPerKm: minGainPerKm || null,
      maxDistanceKm: maxDistanceKm || null,
      maxProximityKm: maxProximityKm || null,
      minValue: minValue || null,
      isMultiStack: isMultiStack !== undefined ? isMultiStack : null,
      sort: sort || 'default'
    },
    offers: results
  });
});

/**
 * GET /api/driver/location
 * Obtém a localização GPS em tempo real do entregador e status de telemetria
 */
app.get('/api/driver/location', (req, res) => {
  res.json({
    success: true,
    location: currentDriverLocation,
    recentHistory: driverLocationHistory.slice(-10)
  });
});

/**
 * POST /api/driver/location
 * Atualiza em tempo real a localização do entregador (GPS via celular, app nativo ou cockpit)
 */
app.post('/api/driver/location', (req, res) => {
  const {
    latitude,
    longitude,
    accuracy = 4.2,
    speed = 0.0,
    heading = 0.0,
    altitude = 780.0,
    source = 'mobile_gps'
  } = req.body;

  if (latitude === undefined || longitude === undefined) {
    return res.status(400).json({
      success: false,
      error: 'Parâmetros "latitude" e "longitude" são obrigatórios.'
    });
  }

  const parsedLat = parseFloat(latitude);
  const parsedLng = parseFloat(longitude);
  const parsedSpeed = parseFloat(speed) || 0.0;

  if (isNaN(parsedLat) || isNaN(parsedLng)) {
    return res.status(400).json({
      success: false,
      error: 'Coordenadas latitude/longitude inválidas.'
    });
  }

  const isMoving = parsedSpeed > 2.0;
  const safetyLock = parsedSpeed > 10.0; // Bloqueio preventivo acima de 10 km/h

  currentDriverLocation = {
    latitude: parsedLat,
    longitude: parsedLng,
    accuracy: parseFloat(accuracy) || 4.2,
    speed: parsedSpeed,
    heading: parseFloat(heading) || 0.0,
    altitude: parseFloat(altitude) || 780.0,
    isMoving,
    safetyLock,
    lastUpdated: new Date().toISOString(),
    source
  };

  // Armazena no histórico de telemetria
  driverLocationHistory.push({
    ...currentDriverLocation,
    timestamp: Date.now()
  });
  if (driverLocationHistory.length > 50) {
    driverLocationHistory.shift();
  }

  // Avalia quantas ofertas pendentes estão no raio imediato de proximidade
  const nearbyOffers = deliveryOffers
    .filter(o => o.status === 'pending')
    .map(o => calculateMetrics(o, parsedLat, parsedLng))
    .filter(o => o.proximityKm !== null && o.proximityKm <= 3.0);

  res.json({
    success: true,
    message: 'Localização em tempo real sincronizada com sucesso no backend.',
    location: currentDriverLocation,
    nearbyPendingOffersCount: nearbyOffers.length,
    closestOffer: nearbyOffers.length > 0 ? nearbyOffers.sort((a,b) => a.proximityKm - b.proximityKm)[0] : null
  });
});

/**
 * GET /api/offers/:id
 * Detalhes de uma oferta individual
 */
app.get('/api/offers/:id', (req, res) => {
  const offer = deliveryOffers.find(o => o.id === req.params.id);
  if (!offer) {
    return res.status(404).json({ success: false, error: 'Oferta de entrega não encontrada.' });
  }
  res.json({
    success: true,
    offer: calculateMetrics(offer)
  });
});

/**
 * POST /api/offers/:id/accept
 * O entregador aceita a oferta
 */
app.post('/api/offers/:id/accept', (req, res) => {
  const offer = deliveryOffers.find(o => o.id === req.params.id);
  if (!offer) {
    return res.status(404).json({ success: false, error: 'Oferta não encontrada.' });
  }

  offer.status = 'accepted';
  offer.acceptedAt = new Date().toISOString();

  res.json({
    success: true,
    message: `Corrida ${offer.id} aceita com sucesso! Trajeto traçado no radar.`,
    offer: calculateMetrics(offer)
  });
});

/**
 * POST /api/offers/:id/decline
 * O entregador recusa a oferta
 */
app.post('/api/offers/:id/decline', (req, res) => {
  const offer = deliveryOffers.find(o => o.id === req.params.id);
  if (!offer) {
    return res.status(404).json({ success: false, error: 'Oferta não encontrada.' });
  }

  offer.status = 'declined';
  offer.declinedAt = new Date().toISOString();

  res.json({
    success: true,
    message: `Oferta ${offer.id} recusada.`,
    offer: calculateMetrics(offer)
  });
});

/**
 * POST /api/offers
 * Registra nova oferta despachada pelos apps parceiros
 */
app.post('/api/offers', (req, res) => {
  const {
    app: appName = 'iFood',
    restaurant = 'Restaurante Parceiro',
    pickupAddress = 'Ponto de Coleta Central',
    deliveryAddress = 'Destino do Pedido',
    value,
    distanceKm,
    estimatedTimeMin,
    isMultiStack = false
  } = req.body;

  if (value === undefined || distanceKm === undefined) {
    return res.status(400).json({
      success: false,
      error: 'Parâmetros "value" e "distanceKm" são obrigatórios.'
    });
  }

  const newOffer = {
    id: `off_${Date.now().toString().slice(-6)}`,
    app: appName,
    restaurant,
    pickupAddress,
    deliveryAddress,
    value: parseFloat(value),
    distanceKm: parseFloat(distanceKm),
    estimatedTimeMin: estimatedTimeMin ? parseInt(estimatedTimeMin) : Math.round(parseFloat(distanceKm) * 3.5 + 5),
    isMultiStack: Boolean(isMultiStack),
    status: 'pending',
    urgency: 'high',
    highDemandZone: parseFloat(value) >= 30 ? 'Zona Premium de Alta Demanda' : null,
    driverScoreBonus: 10,
    createdAt: new Date().toISOString()
  };

  deliveryOffers.unshift(newOffer);

  res.status(201).json({
    success: true,
    message: 'Nova oferta de entrega cadastrada no radar.',
    offer: calculateMetrics(newOffer)
  });
});

/**
 * GET /api/offers/stats/summary
 * Métricas e estatísticas consolidadas para os entregadores
 */
app.get('/api/offers/stats/summary', (req, res) => {
  const calculated = deliveryOffers.map(calculateMetrics);
  const accepted = calculated.filter(o => o.status === 'accepted');
  const pending = calculated.filter(o => o.status === 'pending');

  const totalEarnings = accepted.reduce((sum, o) => sum + o.value, 0);
  const totalKm = accepted.reduce((sum, o) => sum + o.distanceKm, 0);
  const totalNetProfit = accepted.reduce((sum, o) => sum + o.estimatedNetProfit, 0);

  res.json({
    success: true,
    stats: {
      totalOffers: deliveryOffers.length,
      pendingOffers: pending.length,
      acceptedOffers: accepted.length,
      totalGrossEarnings: Number(totalEarnings.toFixed(2)),
      totalNetProfit: Number(totalNetProfit.toFixed(2)),
      totalKmDriven: Number(totalKm.toFixed(2)),
      avgGainPerKm: totalKm > 0 ? Number((totalEarnings / totalKm).toFixed(2)) : 0
    }
  });
});

// =========================================================================
// INTEGRAÇÃO COM A INTERFACE E PROXY DO COCKPIT (Garante SPA e fallback)
// =========================================================================

// Encaminha requisições que não forem /api/offers para o backend Python SPA
app.use((req, res) => {
  const headers = { ...req.headers };
  delete headers.host;
  delete headers.connection;
  delete headers['content-length'];
  headers['connection'] = 'close';

  let bodyData = null;
  if (['POST', 'PUT', 'PATCH'].includes(req.method) && req.body && Object.keys(req.body).length > 0) {
    bodyData = JSON.stringify(req.body);
    headers['content-type'] = 'application/json';
    headers['content-length'] = Buffer.byteLength(bodyData);
  }

  const options = {
    hostname: '127.0.0.1',
    port: PYTHON_BACKEND_PORT,
    path: req.originalUrl,
    method: req.method,
    headers: headers
  };

  const proxyReq = http.request(options, (proxyRes) => {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (err) => {
    if (req.path.startsWith('/api/')) {
      res.status(404).json({ error: 'Endpoint não encontrado', path: req.path });
    } else {
      res.type('html').send(`
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8">
          <title>Radar Delivery AI — Express Server</title>
          <style>
            body { background: #0a0a0f; color: #00ff88; font-family: system-ui; display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100vh; margin: 0; }
            .card { background: #111118; border: 1px solid rgba(0,255,136,0.3); border-radius: 12px; padding: 24px; max-width: 500px; text-align: center; }
            a { color: #00d2ff; text-decoration: none; font-weight: bold; }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>🎯 Radar Delivery AI — Express.js</h1>
            <p>Servidor Express ativo na porta ${PORT}!</p>
            <p><a href="/api/offers">👉 Clique aqui para ver /api/offers</a></p>
            <p><a href="/api/offers/stats/summary">📊 Resumo de Estatísticas</a></p>
          </div>
        </body>
        </html>
      `);
    }
  });

  if (bodyData) {
    proxyReq.write(bodyData);
  }
  proxyReq.end();
});

// =========================================================================
// SUPERVISOR DE SUBPROCESSO PYTHON (Mantém app.py ativo na porta 5000)
// =========================================================================
function startPythonBackend() {
  console.log('[NODE SUPERVISOR] Garantindo inicialização do python3 app.py...');
  const py = spawn('python3', [path.join(__dirname, 'app.py')], {
    stdio: 'inherit',
    cwd: __dirname
  });

  py.on('error', (err) => {
    console.error('[NODE SUPERVISOR] Falha ao iniciar python3:', err);
  });

  py.on('close', (code) => {
    console.log(`[NODE SUPERVISOR] Python encerrou com código ${code}, reiniciando em 3s...`);
    setTimeout(startPythonBackend, 3000);
  });
}

// Inicia servidor Express
const server = app.listen(PORT, '0.0.0.0', () => {
  console.log(`🚀 [RADAR DELIVERY AI] Servidor Express.js ativo na porta ${PORT}`);
  console.log(`📡 Endpoints disponíveis:`);
  console.log(`   - GET  /api/offers (com filtros: ?app=, ?minGainPerKm=, ?maxDistanceKm=, ?sort=)`);
  console.log(`   - GET  /api/offers/:id`);
  console.log(`   - POST /api/offers/:id/accept`);
  console.log(`   - POST /api/offers/:id/decline`);
  console.log(`   - POST /api/offers`);
  console.log(`   - GET  /api/offers/stats/summary`);

  // Verifica e inicia o backend auxiliar Python na porta 5000 se não estiver rodando
  const testReq = http.request({ host: '127.0.0.1', port: PYTHON_BACKEND_PORT, path: '/', method: 'GET', timeout: 1000 }, () => {});
  testReq.on('error', () => {
    startPythonBackend();
  });
  testReq.end();
});

module.exports = app;
