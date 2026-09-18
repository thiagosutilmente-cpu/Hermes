/**
 * Servidor Backend Node.js & Express — Radar Delivery AI
 * Gerenciamento de rotas de ofertas de corridas, avaliação neural e métricas de despacho.
 */

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 4000;

// Middlewares
app.use(cors());
app.use(express.json());

// Log simples de requisições
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  console.log(`[RADAR-AI-EXPRESS] ${timestamp} ${req.method} ${req.originalUrl}`);
  next();
});

// Banco de dados em memória para Ofertas do Radar Delivery AI
let offers = [
  {
    id: 'off_01',
    appName: 'iFood',
    restaurant: 'Burger King — Av. Paulista',
    pickupAddress: 'Av. Paulista, 1000 - Bela Vista',
    destinationAddress: 'Rua Augusta, 1500 - Consolação',
    value: 28.50,
    distanceKm: 3.8,
    timeMinutes: 16,
    isMultiStack: false,
    status: 'pending', // pending | accepted | declined | completed
    neuralDecision: {
      decision: 'ACCEPT',
      confidence: 0.94,
      reason: 'Ganho/km excelente de R$ 7,50/km com trajeto rápido'
    },
    gainPerKm: 7.50,
    fuelCost: 0.65,
    netProfit: 27.85,
    highDemandZoneTag: 'Av. Paulista / Jardins',
    createdAt: new Date().toISOString()
  },
  {
    id: 'off_02',
    appName: 'iFood + Rappi',
    restaurant: 'Madero Container & Bacio di Latte',
    pickupAddress: 'Rua Amauri, 250 - Itaim Bibi',
    destinationAddress: 'Rua Funchal, 418 - Vila Olímpia',
    value: 46.00,
    distanceKm: 5.2,
    timeMinutes: 24,
    isMultiStack: true,
    status: 'pending',
    neuralDecision: {
      decision: 'ACCEPT',
      confidence: 0.98,
      reason: 'Multi-Stack de alta rentabilidade: R$ 8,85/km com pontos vizinhos'
    },
    gainPerKm: 8.85,
    fuelCost: 0.88,
    netProfit: 45.12,
    synergySavingsKm: 2.1,
    highDemandZoneTag: 'Itaim Bibi / Faria Lima',
    createdAt: new Date().toISOString()
  },
  {
    id: 'off_03',
    appName: 'Uber Direct',
    restaurant: 'Farmácia Drogasil',
    pickupAddress: 'Rua dos Pinheiros, 700 - Pinheiros',
    destinationAddress: 'Av. Rebouças, 2200 - Pinheiros',
    value: 19.50,
    distanceKm: 2.4,
    timeMinutes: 11,
    isMultiStack: false,
    status: 'pending',
    neuralDecision: {
      decision: 'ACCEPT',
      confidence: 0.91,
      reason: 'Distância curta e ganho compensatório de R$ 8,13/km'
    },
    gainPerKm: 8.13,
    fuelCost: 0.41,
    netProfit: 19.09,
    highDemandZoneTag: 'Pinheiros',
    createdAt: new Date().toISOString()
  },
  {
    id: 'off_04',
    appName: '99Food',
    restaurant: 'Pastelaria e Lanches Mooca',
    pickupAddress: 'Rua da Mooca, 1800 - Mooca',
    destinationAddress: 'Av. Salim Farah Maluf, 4200 - Tatuapé',
    value: 14.00,
    distanceKm: 7.5,
    timeMinutes: 28,
    isMultiStack: false,
    status: 'pending',
    neuralDecision: {
      decision: 'DECLINE',
      confidence: 0.89,
      reason: 'Distância excessiva e retorno financeiro abaixo do piso (R$ 1,87/km)'
    },
    gainPerKm: 1.87,
    fuelCost: 1.28,
    netProfit: 12.72,
    highDemandZoneTag: null,
    createdAt: new Date().toISOString()
  }
];

// Histórico de auditoria de decisões
const auditLogs = [];

/**
 * Função utilitária de avaliação neural Jarvis
 */
function evaluateOfferWithJarvis(value, distanceKm, appName = 'iFood', isMultiStack = false) {
  const gainPerKm = distanceKm > 0 ? Number((value / distanceKm).toFixed(2)) : value;
  const fuelCost = Number((distanceKm * 0.17).toFixed(2));
  const netProfit = Number(Math.max(0, value - fuelCost).toFixed(2));

  let decision = 'DECLINE';
  let confidence = 0.70;
  let reason = 'Ganho/km abaixo do piso ideal';

  if (isMultiStack || gainPerKm >= 5.0) {
    decision = 'ACCEPT';
    confidence = 0.95;
    reason = isMultiStack
      ? 'Multi-Stack de alto valor compensatório agrupado'
      : 'Ganho/km acima da média com margem líquida positiva';
  } else if (gainPerKm >= 3.5 && distanceKm <= 4.0) {
    decision = 'ACCEPT';
    confidence = 0.78;
    reason = 'Distância curta compensa trajeto ágil';
  } else if (distanceKm > 6.0) {
    decision = 'DECLINE';
    confidence = 0.88;
    reason = 'Distância excessiva com alto desgaste e risco de retorno vazio';
  } else {
    decision = 'DECLINE';
    confidence = 0.65;
    reason = 'Ganho/km abaixo do piso de rentabilidade mínima recomendada';
  }

  return {
    decision,
    confidence,
    reason,
    gainPerKm,
    fuelCost,
    netProfit
  };
}

// =========================================================================
// ROTAS DO SERVIDOR EXPRESS
// =========================================================================

// 1. Health check & Status da API
app.get('/', (req, res) => {
  res.json({
    service: 'Radar Delivery AI — Express Backend',
    version: '1.0.0',
    status: 'online',
    timestamp: new Date().toISOString(),
    endpoints: {
      offers: '/api/offers',
      pending: '/api/offers/pending',
      accept: 'POST /api/offers/:id/accept',
      decline: 'POST /api/offers/:id/decline',
      evaluate: 'POST /api/offers/evaluate',
      summary: '/api/offers/summary'
    }
  });
});

app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    system: 'Radar Delivery AI',
    score: 98,
    gpsAccuracy: 3.8,
    latencyMs: 8,
    uptimeSeconds: Math.floor(process.uptime()),
    timestamp: new Date().toISOString()
  });
});

// 2. Listar todas as ofertas (com filtro opcional por status e app)
app.get('/api/offers', (req, res) => {
  const { status, app, minGainPerKm } = req.query;
  let filtered = [...offers];

  if (status) {
    filtered = filtered.filter(o => o.status.toLowerCase() === status.toLowerCase());
  }
  if (app) {
    filtered = filtered.filter(o => o.appName.toLowerCase().includes(app.toLowerCase()));
  }
  if (minGainPerKm) {
    const min = parseFloat(minGainPerKm);
    if (!isNaN(min)) {
      filtered = filtered.filter(o => o.gainPerKm >= min);
    }
  }

  res.json({
    total: filtered.length,
    offers: filtered
  });
});

// 3. Listar apenas ofertas pendentes de despacho
app.get('/api/offers/pending', (req, res) => {
  const pending = offers.filter(o => o.status === 'pending');
  res.json({
    count: pending.length,
    offers: pending
  });
});

// 4. Buscar oferta por ID
app.get('/api/offers/:id', (req, res) => {
  const offer = offers.find(o => o.id === req.params.id);
  if (!offer) {
    return res.status(404).json({ error: 'Oferta não encontrada', id: req.params.id });
  }
  res.json(offer);
});

// 5. Criar nova oferta recebida de aplicativos parceiros (iFood, Rappi, Uber, 99)
app.post('/api/offers', (req, res) => {
  const { appName, restaurant, value, distanceKm, timeMinutes, pickupAddress, destinationAddress, isMultiStack } = req.body;

  if (!appName || value === undefined || distanceKm === undefined) {
    return res.status(400).json({
      error: 'Campos obrigatórios ausentes: appName, value e distanceKm são necessários.'
    });
  }

  const numValue = parseFloat(value);
  const numDistance = parseFloat(distanceKm);
  const neural = evaluateOfferWithJarvis(numValue, numDistance, appName, Boolean(isMultiStack));

  const newOffer = {
    id: `off_${Date.now().toString().slice(-6)}`,
    appName: String(appName),
    restaurant: restaurant || 'Restaurante Parceiro',
    pickupAddress: pickupAddress || 'Ponto de Coleta Central',
    destinationAddress: destinationAddress || 'Endereço de Entrega Cliente',
    value: numValue,
    distanceKm: numDistance,
    timeMinutes: timeMinutes ? parseInt(timeMinutes) : Math.round(numDistance * 3.5 + 5),
    isMultiStack: Boolean(isMultiStack),
    status: 'pending',
    neuralDecision: {
      decision: neural.decision,
      confidence: neural.confidence,
      reason: neural.reason
    },
    gainPerKm: neural.gainPerKm,
    fuelCost: neural.fuelCost,
    netProfit: neural.netProfit,
    highDemandZoneTag: numValue >= 30 ? 'Zona Premium' : null,
    createdAt: new Date().toISOString()
  };

  offers.unshift(newOffer);
  res.status(201).json({
    message: 'Oferta registrada e analisada com sucesso pelo Radar AI',
    offer: newOffer
  });
});

// 6. Aceitar oferta (Accept)
app.post('/api/offers/:id/accept', (req, res) => {
  const offer = offers.find(o => o.id === req.params.id);
  if (!offer) {
    return res.status(404).json({ error: 'Oferta não encontrada' });
  }

  offer.status = 'accepted';
  offer.acceptedAt = new Date().toISOString();

  const auditEntry = {
    action: 'ACCEPT',
    offerId: offer.id,
    appName: offer.appName,
    value: offer.value,
    gainPerKm: offer.gainPerKm,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(auditEntry);

  res.json({
    success: true,
    message: `Oferta ${offer.id} aceita! Rota iniciada no Radar AI.`,
    offer
  });
});

// 7. Recusar oferta (Decline)
app.post('/api/offers/:id/decline', (req, res) => {
  const offer = offers.find(o => o.id === req.params.id);
  if (!offer) {
    return res.status(404).json({ error: 'Oferta não encontrada' });
  }

  offer.status = 'declined';
  offer.declinedAt = new Date().toISOString();

  const auditEntry = {
    action: 'DECLINE',
    offerId: offer.id,
    appName: offer.appName,
    value: offer.value,
    gainPerKm: offer.gainPerKm,
    timestamp: new Date().toISOString()
  };
  auditLogs.unshift(auditEntry);

  res.json({
    success: true,
    message: `Oferta ${offer.id} recusada com sucesso.`,
    offer
  });
});

// 8. Avaliação Neural Instantânea sem persistência (Decision Endpoint)
app.post('/api/offers/evaluate', (req, res) => {
  const { value, distanceKm, distance, appName, app, isMultiStack } = req.body;
  const val = parseFloat(value);
  const dist = parseFloat(distanceKm !== undefined ? distanceKm : distance);
  const appLabel = appName || app || 'iFood';

  if (isNaN(val) || isNaN(dist)) {
    return res.status(400).json({
      error: 'Parâmetros inválidos: informe "value" e "distanceKm" como números.'
    });
  }

  const result = evaluateOfferWithJarvis(val, dist, appLabel, Boolean(isMultiStack));
  res.json({
    appName: appLabel,
    value: val,
    distanceKm: dist,
    ...result
  });
});

// 9. Resumo das Métricas Financeiras e Operacionais
app.get('/api/offers/summary', (req, res) => {
  const accepted = offers.filter(o => o.status === 'accepted');
  const totalGross = accepted.reduce((acc, o) => acc + o.value, 0);
  const totalDistance = accepted.reduce((acc, o) => acc + o.distanceKm, 0);
  const totalFuel = accepted.reduce((acc, o) => acc + o.fuelCost, 0);
  const totalNet = accepted.reduce((acc, o) => acc + o.netProfit, 0);
  const avgGainPerKm = totalDistance > 0 ? totalGross / totalDistance : 0;

  res.json({
    totalOffersRecorded: offers.length,
    acceptedCount: accepted.length,
    pendingCount: offers.filter(o => o.status === 'pending').length,
    declinedCount: offers.filter(o => o.status === 'declined').length,
    totalGrossRevenue: Number(totalGross.toFixed(2)),
    totalDistanceKm: Number(totalDistance.toFixed(2)),
    totalFuelExpense: Number(totalFuel.toFixed(2)),
    totalNetProfit: Number(totalNet.toFixed(2)),
    averageGainPerKm: Number(avgGainPerKm.toFixed(2))
  });
});

// Inicia o servidor se executado diretamente
if (require.main === module) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 [RADAR DELIVERY AI] Servidor Node.js + Express rodando na porta ${PORT}`);
    console.log(`📡 Endpoints disponíveis em http://localhost:${PORT}/api/offers`);
  });
}

module.exports = app;
