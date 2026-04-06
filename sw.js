importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.0.0/firebase-messaging-compat.js');

  const firebaseConfig = {
    apiKey: "AIzaSyCU08n2MbhgejPb8exKyvcPKn-xLkBu0sY",
    authDomain: "botelysium-ddf50.firebaseapp.com",
    projectId: "botelysium-ddf50",
    storageBucket: "botelysium-ddf50.firebasestorage.app",
    messagingSenderId: "763753788539",
    appId: "1:763753788539:web:62715bac995c0dc5b45ede",
    measurementId: "G-7GMTH40N5E"
  };
firebase.initializeApp(firebaseConfig);
const messaging = firebase.messaging();

// Arka plan mesajlarını yakala
messaging.onBackgroundMessage((payload) => {
  console.log('Arka planda mesaj geldi:', payload);
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: './icon.svg'
  };
  self.registration.showNotification(notificationTitle, notificationOptions);
});
const CACHE = 'elysium-v2';
const ASSETS = ['./', './index.html', './manifest.webmanifest', './icon.svg'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))));
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  e.respondWith(caches.match(e.request).then(r => r || fetch(e.request).catch(() => caches.match('./index.html'))));
});

self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(clients.matchAll({ type: 'window' }).then(list => {
    if (list.length) return list[0].focus();
    return clients.openWindow('./');
  }));
});
