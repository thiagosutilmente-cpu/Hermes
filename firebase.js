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
  updateProfile
} from "firebase/auth";
import { 
  getFirestore,
  doc,
  setDoc,
  getDoc,
  getDocs,
  collection,
  query,
  orderBy,
  limit,
  onSnapshot
} from "firebase/firestore";

// Firebase configuration using environment variables from .env with fallback values from .env.example
const firebaseConfig = {
  apiKey: "AIzaSyDB2k0gU2leh9kHjHwxEuLfCVSoCfhmWH0",
  authDomain: "radar-delivery-dd7f2.firebaseapp.com",
  projectId: "radar-delivery-dd7f2",
  storageBucket: "radar-delivery-dd7f2.firebasestorage.app",
  messagingSenderId: "755379343634",
  appId: "1:755379343634:android:5898fb5536bd219f69e3cb"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firebase Services
export const auth = getAuth(app);
export const db = getFirestore(app);

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

export default app;
