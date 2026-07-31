// Firebase JS SDK configuration and initialization
// This file initializes Firebase Auth and Firestore using environment variables.

import { initializeApp } from "firebase/app";
import { 
  getAuth, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword, 
  signOut, 
  onAuthStateChanged,
  sendPasswordResetEmail,
  updateProfile,
  GoogleAuthProvider,
  signInWithPopup
} from "firebase/auth";
import { 
  getFirestore,
  doc,
  setDoc,
  getDoc,
  getDocs,
  collection,
  collectionGroup,
  addDoc,
  deleteDoc,
  serverTimestamp,
  query,
  orderBy,
  limit,
  onSnapshot,
  enableIndexedDbPersistence
} from "firebase/firestore";

// Firebase configuration using environment variables from .env with fallback values from .env.example
const firebaseConfig = {
  apiKey: (typeof process !== 'undefined' && process.env?.FIREBASE_API_KEY) || "AIzaSyFallbackKeyForRadarDelivery2026",
  authDomain: ((typeof process !== 'undefined' && process.env?.FIREBASE_PROJECT_ID) || "radar-delivery-2026") + ".firebaseapp.com",
  projectId: (typeof process !== 'undefined' && process.env?.FIREBASE_PROJECT_ID) || "radar-delivery-2026",
  storageBucket: ((typeof process !== 'undefined' && process.env?.FIREBASE_PROJECT_ID) || "radar-delivery-2026") + ".appspot.com",
  messagingSenderId: "1234567890",
  appId: (typeof process !== 'undefined' && process.env?.FIREBASE_APPLICATION_ID) || "1:1234567890:android:abc123xyz"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firebase Services
export const auth = getAuth(app);
export const db = getFirestore(app);

// Ativa a persistência offline para lidar com perdas de conexão (bateria acabando, túnel, área sem cobertura)
enableIndexedDbPersistence(db).catch((err) => {
  if (err.code == 'failed-precondition') {
    console.warn("Múltiplas abas abertas, persistência ativada apenas na primeira.");
  } else if (err.code == 'unimplemented') {
    console.warn("Navegador atual não suporta persistência offline do Firestore.");
  }
});

/**
 * Sign in a delivery driver with email and password
 * @param {string} email 
 * @param {string} password 
 * @returns {Promise<{user: import("firebase/auth").User|null, error: string|null}>}
 */
export const loginDriver = async (email, password) => {
  try {
    const userCredential = await signInWithEmailAndPassword(auth, email, password);
    return { user: userCredential.user, error: null };
  } catch (error) {
    console.error("Error signing in delivery driver:", error);
    return { user: null, error: error.message };
  }
};

/**
 * Sign in with Google Auth provider
 * @returns {Promise<{user: import("firebase/auth").User|null, error: string|null}>}
 */
export const loginWithGoogle = async () => {
  try {
    const provider = new GoogleAuthProvider();
    // Configure default parameters if needed
    provider.setCustomParameters({ prompt: 'select_account' });
    const result = await signInWithPopup(auth, provider);
    return { user: result.user, error: null };
  } catch (error) {
    console.error("Error signing in with Google:", error);
    return { user: null, error: error.message };
  }
};

/**
 * Register a new delivery driver with email and password
 * @param {string} email 
 * @param {string} password 
 * @param {string} [displayName]
 * @returns {Promise<{user: import("firebase/auth").User|null, error: string|null}>}
 */
export const registerDriver = async (email, password, displayName = "") => {
  try {
    const userCredential = await createUserWithEmailAndPassword(auth, email, password);
    if (displayName) {
      await updateProfile(userCredential.user, { displayName });
    }
    return { user: userCredential.user, error: null };
  } catch (error) {
    console.error("Error registering delivery driver:", error);
    return { user: null, error: error.message };
  }
};

/**
 * Sign out the current driver
 * @returns {Promise<{error: string|null}>}
 */
export const logoutDriver = async () => {
  try {
    await signOut(auth);
    return { error: null };
  } catch (error) {
    console.error("Error signing out delivery driver:", error);
    return { error: error.message };
  }
};

/**
 * Send a password reset email to a driver
 * @param {string} email 
 * @returns {Promise<{success: boolean, error: string|null}>}
 */
export const resetDriverPassword = async (email) => {
  try {
    await sendPasswordResetEmail(auth, email);
    return { success: true, error: null };
  } catch (error) {
    console.error("Error sending password reset email:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Subscribe to driver auth state changes
 * @param {(user: import("firebase/auth").User|null) => void} callback 
 * @returns {import("firebase/auth").Unsubscribe}
 */
export const onDriverAuthStateChanged = (callback) => {
  return onAuthStateChanged(auth, callback);
};

/**
 * Save or update a driver's profile details in Firestore.
 * Path: riders/{driverId}/profile/details
 * @param {string} driverId
 * @param {object} profileData
 * @returns {Promise<{success: boolean, error: string|null}>}
 */
export const saveDriverProfile = async (driverId, profileData) => {
  try {
    const profileDocRef = doc(db, "riders", driverId, "profile", "details");
    await setDoc(profileDocRef, profileData, { merge: true });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving driver profile to Firestore:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Retrieve a driver's profile details from Firestore.
 * Path: riders/{driverId}/profile/details
 * @param {string} driverId
 * @returns {Promise<{profile: object|null, error: string|null}>}
 */
export const getDriverProfile = async (driverId) => {
  try {
    const profileDocRef = doc(db, "riders", driverId, "profile", "details");
    const docSnap = await getDoc(profileDocRef);
    if (docSnap.exists()) {
      return { profile: docSnap.data(), error: null };
    }
    return { profile: null, error: null };
  } catch (error) {
    console.error("Error getting driver profile from Firestore:", error);
    return { profile: null, error: error.message };
  }
};

/**
 * Save or update a driver's filtered offer preferences and configurations.
 * Path: riders/{driverId}/config/settings
 * @param {string} driverId
 * @param {object} settingsData
 * @returns {Promise<{success: boolean, error: string|null}>}
 */
export const saveDriverSettings = async (driverId, settingsData) => {
  try {
    const settingsDocRef = doc(db, "riders", driverId, "config", "settings");
    await setDoc(settingsDocRef, settingsData, { merge: true });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving driver settings to Firestore:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Retrieve a driver's filtered offer preferences.
 * Path: riders/{driverId}/config/settings
 * @param {string} driverId
 * @returns {Promise<{settings: object|null, error: string|null}>}
 */
export const getDriverSettings = async (driverId) => {
  try {
    const settingsDocRef = doc(db, "riders", driverId, "config", "settings");
    const docSnap = await getDoc(settingsDocRef);
    if (docSnap.exists()) {
      return { settings: docSnap.data(), error: null };
    }
    return { settings: null, error: null };
  } catch (error) {
    console.error("Error getting driver settings from Firestore:", error);
    return { settings: null, error: error.message };
  }
};

/**
 * Save or update a delivery order / offer.
 * Path: riders/{driverId}/offers/{orderId}
 * @param {string} driverId
 * @param {string} orderId (commonly timestamp or custom UUID)
 * @param {object} orderData
 * @returns {Promise<{success: boolean, error: string|null}>}
 */
export const saveDeliveryOrder = async (driverId, orderId, orderData) => {
  try {
    const orderDocRef = doc(db, "riders", driverId, "offers", orderId);
    await setDoc(
      orderDocRef, 
      { ...orderData, id: orderId, timestamp: orderData.timestamp || Date.now() }, 
      { merge: true }
    );
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving delivery order to Firestore:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Retrieve delivery orders / offers for a driver sorted by timestamp.
 * Path: riders/{driverId}/offers
 * @param {string} driverId
 * @param {number} limitVal (optional, defaults to 50)
 * @returns {Promise<{orders: Array, error: string|null}>}
 */
export const getDeliveryOrders = async (driverId, limitVal = 50) => {
  try {
    const ordersColRef = collection(db, "riders", driverId, "offers");
    const q = query(ordersColRef, orderBy("timestamp", "desc"), limit(limitVal));
    const querySnapshot = await getDocs(q);
    const orders = [];
    querySnapshot.forEach((doc) => {
      orders.push({ id: doc.id, ...doc.data() });
    });
    return { orders, error: null };
  } catch (error) {
    console.error("Error getting delivery orders from Firestore:", error);
    return { orders: [], error: error.message };
  }
};

/**
 * Subscribe to real-time updates for delivery orders / offers.
 * @param {string} driverId
 * @param {(orders: Array) => void} callback
 * @param {(error: Error) => void} [errorCallback]
 * @param {number} [limitVal]
 * @returns {import("firebase/firestore").Unsubscribe}
 */
export const subscribeToDeliveryOrders = (driverId, callback, errorCallback = null, limitVal = 50) => {
  try {
    const ordersColRef = collection(db, "riders", driverId, "offers");
    const q = query(ordersColRef, orderBy("timestamp", "desc"), limit(limitVal));
    return onSnapshot(q, (querySnapshot) => {
      const orders = [];
      querySnapshot.forEach((doc) => {
        orders.push({ id: doc.id, ...doc.data() });
      });
      callback(orders);
    }, (error) => {
      console.error("Error in real-time orders snapshot:", error);
      if (errorCallback) errorCallback(error);
    });
  } catch (e) {
    console.error("Failed to establish real-time orders subscription:", e);
    if (errorCallback) errorCallback(e);
    return () => {};
  }
};

/**
 * Subscribe to real-time driver settings changes.
 * @param {string} driverId
 * @param {(settings: object|null) => void} callback
 * @param {(error: Error) => void} [errorCallback]
 * @returns {import("firebase/firestore").Unsubscribe}
 */
export const subscribeToDriverSettings = (driverId, callback, errorCallback = null) => {
  try {
    const settingsDocRef = doc(db, "riders", driverId, "config", "settings");
    return onSnapshot(settingsDocRef, (docSnap) => {
      if (docSnap.exists()) {
        callback(docSnap.data());
      } else {
        callback(null);
      }
    }, (error) => {
      console.error("Error in real-time settings snapshot:", error);
      if (errorCallback) errorCallback(error);
    });
  } catch (e) {
    console.error("Failed to establish real-time settings subscription:", e);
    if (errorCallback) errorCallback(e);
    return () => {};
  }
};

/**
 * Subscribe to real-time driver profile changes.
 * @param {string} driverId
 * @param {(profile: object|null) => void} callback
 * @param {(error: Error) => void} [errorCallback]
 * @returns {import("firebase/firestore").Unsubscribe}
 */
export const subscribeToDriverProfile = (driverId, callback, errorCallback = null) => {
  try {
    const profileDocRef = doc(db, "riders", driverId, "profile", "details");
    return onSnapshot(profileDocRef, (docSnap) => {
      if (docSnap.exists()) {
        callback(docSnap.data());
      } else {
        callback(null);
      }
    }, (error) => {
      console.error("Error in real-time profile snapshot:", error);
      if (errorCallback) errorCallback(error);
    });
  } catch (e) {
    console.error("Failed to establish real-time profile subscription:", e);
    if (errorCallback) errorCallback(e);
    return () => {};
  }
};

/**
 * Subscribe to all driver profiles across the platform (Admin usage).
 * Path: collectionGroup("profile")
 * @param {(profiles: Array<object>) => void} callback
 * @param {(error: Error) => void} [errorCallback]
 * @returns {import("firebase/firestore").Unsubscribe}
 */
export const subscribeToAllProfiles = (callback, errorCallback = null) => {
  try {
    const profilesQuery = query(collectionGroup(db, "profile"));
    return onSnapshot(profilesQuery, (querySnapshot) => {
      const allProfiles = [];
      querySnapshot.forEach((docSnap) => {
        const data = docSnap.data();
        // Since details document is inside riders/{driverId}/profile/details,
        // docSnap.ref.parent.parent.id gives us the driverId/UID
        const driverId = docSnap.ref.parent?.parent?.id;
        if (driverId) {
          allProfiles.push({
            driverId,
            ...data
          });
        }
      });
      callback(allProfiles);
    }, (error) => {
      console.error("Error subscribing to all profiles:", error);
      if (errorCallback) errorCallback(error);
    });
  } catch (e) {
    console.error("Failed to subscribe to all profiles:", e);
    if (errorCallback) errorCallback(e);
    return () => {};
  }
};

/**
 * Save a rejected delivery order / offer.
 * Path: riders/{driverId}/rejected_offers/{orderId}
 * @param {string} driverId
 * @param {string} orderId
 * @param {object} orderData
 * @returns {Promise<{success: boolean, error: string|null}>}
 */
export const saveRejectedOrder = async (driverId, orderId, orderData) => {
  try {
    const rejectedDocRef = doc(db, "riders", driverId, "rejected_offers", orderId);
    await setDoc(rejectedDocRef, {
      ...orderData,
      id: orderId,
      rejectedAt: Date.now()
    }, { merge: true });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving rejected order to Firestore:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Retrieve rejected order IDs for a driver.
 * Path: riders/{driverId}/rejected_offers
 * @param {string} driverId
 * @returns {Promise<{rejectedIds: Set<string>, error: string|null}>}
 */
export const getRejectedOrders = async (driverId) => {
  try {
    const colRef = collection(db, "riders", driverId, "rejected_offers");
    const querySnapshot = await getDocs(colRef);
    const rejectedIds = new Set();
    querySnapshot.forEach((doc) => {
      rejectedIds.add(doc.id);
    });
    return { rejectedIds, error: null };
  } catch (error) {
    console.error("Error getting rejected orders from Firestore:", error);
    return { rejectedIds: new Set(), error: error.message };
  }
};

/**
 * Subscribe to real-time module health status.
 * Path: riders/{driverId}/session/module_health
 */
export const subscribeToModuleHealth = (driverId, callback) => {
  try {
    const docRef = doc(db, "riders", driverId, "session", "module_health");
    return onSnapshot(docRef, (docSnap) => {
      if (docSnap.exists()) {
        callback(docSnap.data());
      } else {
        callback(null);
      }
    });
  } catch (error) {
    console.error("Error subscribing to module health:", error);
    return () => {};
  }
};

/**
 * Subscribe to real-time active session statistics.
 * Path: riders/{driverId}/session/active_stats
 */
export const subscribeToActiveSessionStats = (driverId, callback) => {
  try {
    const docRef = doc(db, "riders", driverId, "session", "active_stats");
    return onSnapshot(docRef, (docSnap) => {
      if (docSnap.exists()) {
        callback(docSnap.data());
      } else {
        callback(null);
      }
    });
  } catch (error) {
    console.error("Error subscribing to session stats:", error);
    return () => {};
  }
};

/**
 * Send a remote command to the Android app.
 * Path: riders/{driverId}/commands/latest
 */
export const sendRemoteCommand = async (driverId, action) => {
  try {
    const docRef = doc(db, "riders", driverId, "commands", "latest");
    await setDoc(docRef, {
      action,
      timestamp: Date.now(),
      status: "pending"
    });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error sending remote command:", error);
    return { success: false, error: error.message };
  }
};

export default app;

export const sendEmergencyAlert = async (driverId, location) => {
  try {
    const colRef = collection(db, "emergencies");
    await addDoc(colRef, {
      driverId,
      location,
      timestamp: serverTimestamp(),
      resolved: false
    });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error sending emergency alert:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Save WhatsApp notification details in Firestore.
 * Path: riders/{driverId}/whatsapp/last_received
 */
export const saveWhatsAppNotification = async (driverId, sender, text) => {
  try {
    const docRef = doc(db, "riders", driverId, "whatsapp", "last_received");
    await setDoc(docRef, {
      sender,
      text,
      timestamp: Date.now(),
      isRead: false
    });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving WhatsApp notification:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Subscribe to real-time WhatsApp notifications in Firestore.
 */
export const subscribeToWhatsAppNotification = (driverId, callback) => {
  try {
    const docRef = doc(db, "riders", driverId, "whatsapp", "last_received");
    return onSnapshot(docRef, (docSnap) => {
      if (docSnap.exists()) {
        callback(docSnap.data());
      } else {
        callback(null);
      }
    });
  } catch (error) {
    console.error("Error subscribing to WhatsApp notifications:", error);
    return () => {};
  }
};

/**
 * Send WhatsApp reply command to the Android app.
 * Path: riders/{driverId}/whatsapp/reply_command
 */
export const sendWhatsAppReplyCommand = async (driverId, text) => {
  try {
    const docRef = doc(db, "riders", driverId, "whatsapp", "reply_command");
    await setDoc(docRef, {
      text,
      timestamp: Date.now(),
      status: "pending"
    });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error sending WhatsApp reply command:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Save driver backup to Firestore.
 * Path: riders/{driverId}/backups/latest and riders/{driverId}/backups_history
 */
export const saveDriverBackup = async (driverId, backupData) => {
  try {
    const backupLatestRef = doc(db, "riders", driverId, "backups", "latest");
    const backupHistoryColRef = collection(db, "riders", driverId, "backups_history");
    
    // Set in latest
    await setDoc(backupLatestRef, {
      ...backupData,
      updatedAt: serverTimestamp()
    });
    
    // Add to history
    await addDoc(backupHistoryColRef, {
      ...backupData,
      createdAt: serverTimestamp()
    });
    
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving driver backup:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Get driver backups history from Firestore.
 */
export const getDriverBackups = async (driverId, limitVal = 10) => {
  try {
    const historyColRef = collection(db, "riders", driverId, "backups_history");
    const q = query(historyColRef, orderBy("timestamp", "desc"), limit(limitVal));
    const querySnapshot = await getDocs(q);
    const backups = [];
    querySnapshot.forEach((doc) => {
      backups.push({ id: doc.id, ...doc.data() });
    });
    return { backups, error: null };
  } catch (error) {
    console.error("Error getting driver backups:", error);
    return { backups: [], error: error.message };
  }
};

/**
 * Subscribe to real-time Jarvis proactive messages in Firestore.
 * Path: riders/{driverId}/jarvis/proactive_message
 */
export const subscribeToProactiveMessages = (driverId, callback) => {
  try {
    const docRef = doc(db, "riders", driverId, "jarvis", "proactive_message");
    return onSnapshot(docRef, (docSnap) => {
      if (docSnap.exists()) {
        const data = docSnap.data();
        // Check if message is fresh (less than 60 seconds old)
        if (data.message && (Date.now() - data.timestamp < 60000)) {
           callback(data.message);
        } else {
           callback(null);
        }
      } else {
        callback(null);
      }
    });
  } catch (error) {
    console.error("Error subscribing to proactive messages:", error);
    return () => {};
  }
};

/**
 * Save or update a customized voice profile.
 * Path: riders/{driverId}/voice_profiles/{profileId}
 */
export const saveVoiceProfile = async (driverId, profileId, profileData) => {
  try {
    const profileDocRef = doc(db, "riders", driverId, "voice_profiles", profileId);
    await setDoc(profileDocRef, {
      ...profileData,
      updatedAt: serverTimestamp()
    }, { merge: true });
    return { success: true, error: null };
  } catch (error) {
    console.error("Error saving voice profile:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Delete a custom voice profile.
 */
export const deleteVoiceProfile = async (driverId, profileId) => {
  try {
    const profileDocRef = doc(db, "riders", driverId, "voice_profiles", profileId);
    await deleteDoc(profileDocRef);
    return { success: true, error: null };
  } catch (error) {
    console.error("Error deleting voice profile:", error);
    return { success: false, error: error.message };
  }
};

/**
 * Subscribe in real-time to the collection of custom voice profiles.
 */
export const subscribeToVoiceProfiles = (driverId, callback) => {
  try {
    const colRef = collection(db, "riders", driverId, "voice_profiles");
    // Snapshot query
    const q = query(colRef);
    return onSnapshot(q, (snapshot) => {
      const profiles = [];
      snapshot.forEach((doc) => {
        profiles.push({ id: doc.id, ...doc.data() });
      });
      callback(profiles);
    }, (error) => {
      console.error("Error in voice profiles snapshot listener:", error);
    });
  } catch (error) {
    console.error("Error subscribing to voice profiles:", error);
    return () => {};
  }
};

/**
 * Send a generic Jarvis query and listen for a response.
 * Path: jarvis_requests/{requestId}
 */
export const sendJarvisGeneralQuery = async (text, driverId = "motoboy_thiago_01") => {
  try {
    const colRef = collection(db, "jarvis_requests");
    const docRef = await addDoc(colRef, {
      text,
      status: "pending",
      driverId,
      timestamp: Date.now()
    });
    return { success: true, requestId: docRef.id, error: null };
  } catch (error) {
    console.error("Error sending Jarvis general query:", error);
    return { success: false, requestId: null, error: error.message };
  }
};

/**
 * Subscribe to response for a specific Jarvis request.
 */
export const subscribeToJarvisResponse = (requestId, callback) => {
  try {
    const docRef = doc(db, "jarvis_requests", requestId);
    return onSnapshot(docRef, (docSnap) => {
      if (docSnap.exists()) {
        const data = docSnap.data();
        if (data.status === "completed" || data.status === "error") {
          callback(data.response || data.error);
        }
      }
    });
  } catch (error) {
    console.error("Error subscribing to Jarvis response:", error);
    return () => {};
  }
};


