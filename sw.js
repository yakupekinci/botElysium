/* Elysium Bot SW v3 - Firebase optional */
const CACHE = 'elysium-v6';
const ASSETS = ['./', './index.html', './manifest.webmanifest', './icon.svg'];

/* Try Firebase — if CDN fails, SW still works */
try {
  importScripts('https://www.gstatic.com/firebasejs/9.22.2/firebase-app-compat.js');
  importScripts('https://www.gstatic.com/firebasejs/9.22.2/firebase-messaging-compat.js');
  const firebaseConfig = {
    apiKey: "AIzaSyCU08n2MbhgejPb8exKyvcPKn-xLkBu0sY",
    authDomain: "botelysium-ddf50.firebaseapp.com",
    projectId: "botelysium-ddf50",
    storageBucket: "botelysium-ddf50.firebasestorage.app",
    messagingSenderId: "763753788539",
    appId: "1:763753788539:web:62715bac995c0dc5b45ede"
  };
  firebase.initializeApp(firebaseConfig);
  const messaging = firebase.messaging();
  messaging.onBackgroundMessage((payload) => {
    self.registration.showNotification(
      payload.notification?.title || '⏰ Elysium Bot',
      { body: payload.notification?.body || '', icon: './icon.svg', badge: './icon.svg', vibrate: [300,100,300,100,300] }
    );
  });
} catch(e) { /* Firebase not available — native alarms still work */ }

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS).catch(()=>{})));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys =>
    Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
  ));
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  e.respondWith(
    caches.match(e.request).then(r => r || fetch(e.request).catch(() =>
      e.request.mode === 'navigate' ? caches.match('./index.html') : new Response('', {status: 404})
    ))
  );
});

/* Native alarm notifications from SW */
self.addEventListener('message', e => {
  if (e.data?.type === 'SCHEDULE_ALARM') {
    const { delay, title, body, alarmId } = e.data;
    setTimeout(() => {
      self.registration.showNotification(title, {
        body, icon: './icon.svg', badge: './icon.svg',
        tag: 'alarm-' + alarmId, renotify: true,
        vibrate: [400, 150, 400, 150, 400, 150, 400],
        requireInteraction: true,
        actions: [{ action: 'dismiss', title: 'Tamam' }]
      });
    }, delay);
  }
});

self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
    if (list.length) return list[0].focus();
    return clients.openWindow('./');
  }));
});
