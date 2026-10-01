/**
 * Firebase Cloud Function for Need You App
 * Listens for new pings in Firestore and dispatches immediate FCM push notifications.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onPingCreated = onDocumentCreated("pings/{pingId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    console.log("No data associated with the event");
    return;
  }

  const pingData = snapshot.data();
  const toUserId = pingData.toUserId;
  const fromName = pingData.fromName || "Your friend";
  const pingType = pingData.type || "NEED_YOU";

  console.log(`Processing ping [${event.params.pingId}] from ${fromName} to user: ${toUserId}, type: ${pingType}`);

  try {
    // 1. Fetch recipient's profile to obtain FCM registration token
    const userDoc = await admin.firestore().collection("users").doc(toUserId).get();
    if (!userDoc.exists) {
      console.warn(`User ${toUserId} does not exist in Firestore.`);
      return;
    }

    const recipientData = userDoc.data();
    const fcmToken = recipientData.fcmToken;

    if (!fcmToken) {
      console.warn(`User ${toUserId} does not have an active FCM token registered.`);
      return;
    }

    // 2. Prepare notification copy and sound
    const isNeedYou = pingType === "NEED_YOU";
    const notificationTitle = isNeedYou ? "🚨 I NEED YOU" : "👋 HOW'S YOUR DAY?";
    const notificationBody = isNeedYou
      ? `${fromName} needs you right now.`
      : `${fromName}: Hi, how is your day going? 😊`;

    const channelId = isNeedYou ? "need_you_urgent_alerts_v2" : "need_you_bored_alerts_v2";
    const soundName = isNeedYou ? "urgent_ping" : "bored_ping";

    // 3. Construct high-priority FCM payload with Android-specific notification settings
    const message = {
      token: fcmToken,
      notification: {
        title: notificationTitle,
        body: notificationBody,
      },
      data: {
        type: pingType,
        from_name: fromName,
        ping_id: event.params.pingId,
        click_action: "OPEN_APP",
      },
      android: {
        priority: "high",
        notification: {
          channelId: channelId,
          sound: soundName,
          priority: "max",
          visibility: "public",
          defaultVibrateTimings: false,
          vibrateTimingsMillis: isNeedYou ? [0, 400, 200, 400, 200, 600] : [0, 200, 100, 200],
        },
      },
    };

    // 4. Dispatch FCM push notification
    const response = await admin.messaging().send(message);
    console.log(`Successfully sent push notification to ${toUserId}:`, response);

    // Update ping status in Firestore
    await snapshot.ref.update({
      status: "delivered",
      fcmMessageId: response,
      deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
    });

  } catch (error) {
    console.error(`Error sending push notification for ping ${event.params.pingId}:`, error);
    await snapshot.ref.update({
      status: "failed",
      error: error.message,
    });
  }
});
