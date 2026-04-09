const { onSchedule } = require('firebase-functions/v2/scheduler');
const logger = require('firebase-functions/logger');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();

function nowPartsForTimezone(timezone) {
  const dtf = new Intl.DateTimeFormat('en-GB', {
    timeZone: timezone || 'UTC',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  });
  const parts = dtf.formatToParts(new Date());
  const map = {};
  for (const p of parts) {
    if (p.type !== 'literal') map[p.type] = p.value;
  }
  return {
    hhmm: `${map.hour}:${map.minute}`,
    ymd: `${map.year}-${map.month}-${map.day}`
  };
}

exports.dispatchDueAlarms = onSchedule(
  {
    schedule: '* * * * *',
    timeZone: 'UTC',
    region: 'europe-west1'
  },
  async () => {
    // Single-device cost mode: only process the most recently updated device.
    const snap = await db.collection('devices').orderBy('updatedAt', 'desc').limit(1).get();
    if (snap.empty) {
      logger.info('No devices found.');
      return;
    }

    let sentCount = 0;
    for (const docSnap of snap.docs) {
      const d = docSnap.data() || {};
      const token = d.token;
      if (!token) continue;

      const timezone = d.timezone || 'UTC';
      const now = nowPartsForTimezone(timezone);
      const alarms = Array.isArray(d.alarms) ? d.alarms : [];
      const interval = d.interval || { on: false };
      const sentKeys = d.sentKeys || {};
      const updates = {};

      for (const alarm of alarms) {
        if (!alarm || !alarm.on || !alarm.time) continue;
        if (alarm.time !== now.hhmm) continue;
        const key = `${now.ymd}_${alarm.id || alarm.time}`;
        if (sentKeys[key]) continue;

        try {
          await admin.messaging().send({
            token,
            notification: {
              title: '⏰ Elysium Bot',
              body: `${alarm.time} — ${alarm.desc || 'Hatırlatma'}`
            },
            data: {
              mode: String(alarm.mode || 'normal'),
              source: 'scheduled-alarm'
            }
          });
          sentCount += 1;
          sentKeys[key] = true;
        } catch (e) {
          logger.warn('Alarm push failed', { deviceId: docSnap.id, err: e.message });
        }
      }

      // Recurring reminder handling
      if (interval.on && Number(interval.minutes || 0) > 0) {
        const minutes = Math.max(1, Number(interval.minutes));
        const nowMs = Date.now();
        const lastMs = Number(d.lastIntervalAt || 0);
        if (!lastMs || nowMs - lastMs >= minutes * 60000) {
          try {
            await admin.messaging().send({
              token,
              notification: {
                title: '⏰ Elysium Bot',
                body: interval.desc || 'Döngüsel hatırlatma'
              },
              data: {
                mode: String(interval.mode || 'normal'),
                source: 'interval-reminder'
              }
            });
            sentCount += 1;
            updates.lastIntervalAt = nowMs;
          } catch (e) {
            logger.warn('Interval push failed', { deviceId: docSnap.id, err: e.message });
          }
        }
      }

      // Keep sent keys only for current day
      const keepPrefix = `${now.ymd}_`;
      const trimmed = {};
      for (const [k, v] of Object.entries(sentKeys)) {
        if (k.startsWith(keepPrefix)) trimmed[k] = v;
      }
      updates.sentKeys = trimmed;

      await docSnap.ref.set(updates, { merge: true });
    }

    logger.info('Scheduler completed.', { sentCount });
  }
);
