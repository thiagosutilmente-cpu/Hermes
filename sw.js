const CACHE_NAME = 'radar-motoboy-v1';
const ASSETS_TO_CACHE = [
  '/',
  '/index.html',
  '/manifest.json'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS_TO_CACHE);
    })
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keyList) => {
      return Promise.all(keyList.map((key) => {
        if (key !== CACHE_NAME) {
          return caches.delete(key);
        }
      }));
    })
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  if (event.request.method !== 'GET') return;
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      return cachedResponse || fetch(event.request).catch(() => caches.match('/index.html'));
    })
  );
});

// Push Notification Event Handler (Firebase Cloud Messaging & Web Push)
self.addEventListener('push', (event) => {
  let data = {
    title: '🚀 NOVO STACK ALTA RENTABILIDADE!',
    body: 'Multi-app iFood + Rappi: R$ 33,00 (R$ 7,86/km). Toque para aceitar imediatamente.',
    icon: '/assets/icon-192.png',
    badge: '/assets/icon-192.png',
    tag: 'high-value-stack',
    data: { url: '/' }
  };

  if (event.data) {
    try {
      const payload = event.data.json();
      data = { ...data, ...payload };
    } catch (e) {
      data.body = event.data.text();
    }
  }

  const options = {
    body: data.body,
    icon: data.icon || '/assets/icon-192.png',
    badge: data.badge || '/assets/icon-192.png',
    vibrate: [300, 100, 300, 100, 400],
    tag: data.tag || 'high-value-stack',
    renotify: true,
    data: data.data || { url: '/' },
    actions: [
      { action: 'accept', title: '⚡ ACEITAR STACK' },
      { action: 'decline', title: '❌ RECUSAR' }
    ]
  };

  event.waitUntil(
    self.registration.showNotification(data.title, options)
  );
});

// Background Sync Event Handler (Service Worker)
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-firestore-queue' || event.tag === 'sync-earnings-performance') {
    event.waitUntil(
      self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
        for (const client of clientList) {
          client.postMessage({
            type: 'TRIGGER_FIRESTORE_QUEUE_FLUSH'
          });
        }
      })
    );
  }
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const action = event.action;

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.focus();
          client.postMessage({
            type: 'NOTIFICATION_ACTION',
            action: action,
            notificationData: event.notification.data
          });
          return;
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow('/#dashboard');
      }
    })
  );
});
