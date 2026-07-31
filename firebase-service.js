/**
 * Firebase & Firestore Initialization Service
 * Initializes Firebase SDK using environment configuration from .env / process.env / fallback
 * and handles real-time interaction with database collections ('pedidos', 'audit_logs', 'logs') and Firebase Auth.
 */

const getEnvVar = (key, fallback) => {
  if (typeof process !== 'undefined' && process.env && process.env[key]) {
    return process.env[key];
  }
  if (typeof window !== 'undefined' && window.process && window.process.env && window.process.env[key]) {
    return window.process.env[key];
  }
  if (typeof window !== 'undefined' && window[key]) {
    return window[key];
  }
  return fallback;
};

const apiKey = getEnvVar('FIREBASE_API_KEY', 'AIzaSyFallbackKeyForRadarDelivery2026');
const projectId = getEnvVar('FIREBASE_PROJECT_ID', 'radar-delivery-2026');
const appId = getEnvVar('FIREBASE_APPLICATION_ID', '1:1234567890:android:abc123xyz');

const firebaseConfig = {
  apiKey: apiKey,
  authDomain: `${projectId}.firebaseapp.com`,
  projectId: projectId,
  storageBucket: `${projectId}.appspot.com`,
  messagingSenderId: "1234567890",
  appId: appId
};

// Initialize Firebase if compat SDK is loaded and not initialized
if (typeof firebase !== 'undefined' && !firebase.apps.length) {
  try {
    firebase.initializeApp(firebaseConfig);
    console.log("⚡ Firebase initialized successfully with project:", firebaseConfig.projectId);
  } catch (err) {
    console.error("Erro ao inicializar o Firebase:", err);
  }
}

// Service object for Firestore collection operations
const firestoreService = {
  get db() {
    return (typeof firebase !== 'undefined' && firebase.firestore) ? firebase.firestore() : null;
  },

  /**
   * Listen to active orders in 'pedidos' collection in real-time
   */
  listenToPedidos(callback) {
    if (!this.db) {
      console.warn("Firestore não disponível para escutar 'pedidos'");
      return null;
    }
    return this.db.collection('pedidos')
      .where('status', 'in', ['PENDING', 'ACTIVE'])
      .onSnapshot((snapshot) => {
        const pedidos = [];
        snapshot.forEach(doc => {
          pedidos.push({ id: doc.id, ...doc.data() });
        });
        callback(pedidos);
      }, (error) => {
        console.error("Erro no listener da coleção 'pedidos':", error);
      });
  },

  /**
   * Update status of an order in 'pedidos'
   */
  async updatePedidoStatus(pedidoId, status, previousStatus = 'PENDING') {
    if (!this.db) return false;
    try {
      await this.db.collection('pedidos').doc(pedidoId).update({
        status: status,
        updatedAt: new Date().toISOString()
      });
      
      // Automatic audit log
      await this.recordAuditLog({
        orderId: pedidoId,
        action: 'ORDER_STATUS_CHANGED',
        previousStatus: previousStatus,
        newStatus: status,
        actorId: 'driver_web_ui',
        details: `Pedido ${pedidoId} alterado de ${previousStatus} para ${status}`
      });
      return true;
    } catch (err) {
      console.error(`Erro ao atualizar pedido ${pedidoId}:`, err);
      return false;
    }
  },

  /**
   * Record a critical status change in 'audit_logs' collection
   */
  async recordAuditLog(logData) {
    if (!this.db) return false;
    try {
      const nowMs = Date.now();
      const entry = {
        orderId: logData.orderId || 'unknown_order',
        action: logData.action || 'STATUS_UPDATE',
        previousStatus: logData.previousStatus || 'UNKNOWN',
        newStatus: logData.newStatus || 'UPDATED',
        actorId: logData.actorId || 'system_driver',
        details: logData.details || '',
        timestamp: nowMs,
        formattedTime: new Date(nowMs).toISOString(),
        securityLevel: logData.securityLevel || 'CRITICAL_STATUS_CHANGE'
      };

      const ref = await this.db.collection('audit_logs').add(entry);
      console.log(`🔒 Audit log gravado com sucesso em 'audit_logs' (Doc ID: ${ref.id})`);
      return ref.id;
    } catch (err) {
      console.error("Erro ao gravar audit log em 'audit_logs':", err);
      return null;
    }
  },

  /**
   * Listen to real-time audit logs for monitoring in dashboard
   */
  listenToAuditLogs(callback, limit = 50) {
    if (!this.db) return null;
    return this.db.collection('audit_logs')
      .orderBy('timestamp', 'desc')
      .limit(limit)
      .onSnapshot((snapshot) => {
        const logs = [];
        snapshot.forEach(doc => {
          logs.push({ id: doc.id, ...doc.data() });
        });
        callback(logs);
      }, (error) => {
        console.error("Erro ao escutar coleção 'audit_logs':", error);
      });
  }
};

// Authentication Service using Firebase Auth
const authService = {
  get auth() {
    return (typeof firebase !== 'undefined' && firebase.auth) ? firebase.auth() : null;
  },

  onAuthStateChanged(callback) {
    if (!this.auth) return null;
    return this.auth.onAuthStateChanged(callback);
  },

  async signInWithEmail(email, password) {
    if (!this.auth) throw new Error("Firebase Auth não está disponível no ambiente.");
    return await this.auth.signInWithEmailAndPassword(email, password);
  },

  async signUpWithEmail(email, password) {
    if (!this.auth) throw new Error("Firebase Auth não está disponível no ambiente.");
    return await this.auth.createUserWithEmailAndPassword(email, password);
  },

  async signOut() {
    if (!this.auth) return;
    return await this.auth.signOut();
  },

  getCurrentUser() {
    return this.auth ? this.auth.currentUser : null;
  }
};

// Export to window object for global browser availability
if (typeof window !== 'undefined') {
  window.firestoreService = firestoreService;
  window.authService = authService;
  window.firebaseConfig = firebaseConfig;
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { firestoreService, authService, firebaseConfig };
}

