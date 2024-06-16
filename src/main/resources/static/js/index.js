import {
  initializeApp
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-app.js";
import {
  getAuth,
  connectAuthEmulator,
  onAuthStateChanged,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  setPersistence,
  browserSessionPersistence
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";
import {
  getFirestore,
  collection,
  addDoc,
  connectFirestoreEmulator
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-firestore.js";

// Your web app's Firebase configuration for production
const productionFirebaseConfig = {
  apiKey: "AIzaSyBPMvUxzHlxbEHebXinwE4_eA4fTOVPYLs",
  authDomain: "broker-da44b.firebaseapp.com",
  projectId: "broker-da44b",
  storageBucket: "broker-da44b.appspot.com",
  messagingSenderId: "78512882731",
  appId: "1:78512882731:web:7fd2c8c6aa99051296566e",
  measurementId: "G-60LX98H1E5"
};

// Your web app's Firebase configuration for local development
const localFirebaseConfig = {
  apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
  authDomain: "localhost",
  projectId: "broker-da44b"
};

function setupAuth() {
  // Use local or production configuration based on the hostname
  const firebaseConfig = (location.hostname === "localhost") ? productionFirebaseConfig : productionFirebaseConfig;

  // Initialize Firebase app
  const firebaseApp = initializeApp(firebaseConfig);
  const auth = getAuth(firebaseApp);
  const firestore = getFirestore(firebaseApp);

  // Set persistence to session
  setPersistence(auth, browserSessionPersistence)
    .catch((error) => {
      console.error("Error setting persistence:", error);
    });

  // Connect to local emulator when running on localhost
  if (location.hostname === "localhost") {
    // connectAuthEmulator(auth, "http://localhost:8082", { disableWarnings: true });
    connectFirestoreEmulator(firestore, 'localhost', 8084);
  }

  // Save auth and db to global scope
  window.firebaseApp = firebaseApp;
  window.auth = auth;
  window.firestore = firestore;

  // Ensure any existing user is signed out
  try {
    auth.signOut();
  } catch (err) {
    console.error("Error signing out:", err);
  }

  wireUpAuthChange();
  wireGuiUpEvents();
}

setupAuth();

function wireUpAuthChange() {
  onAuthStateChanged(window.auth, (user) => {
    if (user) {
      user.getIdToken().then((token) => {
        fetchData(token);
      }).catch((error) => {
        console.error("Error getting ID token:", error);
      });
    } else {
      // Handle unauthenticated state
      showUnAuthenticated();
    }
  });
}

function wireGuiUpEvents() {
  const email = document.getElementById("email");
  const password = document.getElementById("password");
  const signInButton = document.getElementById("btnSignIn");
  const signUpButton = document.getElementById("btnSignUp");

  signInButton.addEventListener("click", () => {
    setPersistence(window.auth, browserSessionPersistence)
      .then(() => {
        return signInWithEmailAndPassword(window.auth, email.value, password.value);
      })
      .then((userCredential) => {
        storeUserInfo(userCredential.user);  // Store user info
        return userCredential.user.getIdToken();
      })
      .then((token) => {
        fetchData(token);
        // Optionally redirect to dashboard with authenticated state
        window.location.href = 'html/dashboard.html'
      })
      .catch((error) => {
        console.error("Error during sign in:", error.message);
        alert(error.message);
      });
  });

  signUpButton.addEventListener("click", () => {
    setPersistence(window.auth, browserSessionPersistence)
      .then(() => {
        return createUserWithEmailAndPassword(window.auth, email.value, password.value);
      })
      .then(async (userCredential) => {
        const user = userCredential.user;
        await addDoc(collection(window.firestore, "users"), {
          uid: user.uid,
          email: user.email,
          role: "user",
          createdAt: new Date()
        });
        console.log("User profile added to Firestore");

        storeUserInfo(user);  // Store user info

        // Fetch ID token and handle authenticated state
        return user.getIdToken();
      })
      .then((token) => {
        fetchData(token);
        // Optionally redirect to dashboard
        window.location.href = 'html/dashboard.html'
      })
      .catch((error) => {
        console.error("Error during sign up:", error.message);
        alert(error.message);
      });
  });
}

function storeUserInfo(user) {
  user.getIdToken().then(token => {
    sessionStorage.setItem('uid', user.uid);
    sessionStorage.setItem('token', token);
  }).catch(error => {
    console.error("Error getting ID token:", error);
  });
}

function fetchData(token) {
  getHello(token);
  whoami(token);
}

function showUnAuthenticated() {
  document.getElementById("email").value = "";
  document.getElementById("password").value = "";
  document.getElementById("logindiv").style.display = "block";
  document.getElementById("contentdiv").style.display = "none";
}

function getHello(token) {
  fetch('/api/hello', {
    headers: { Authorization: 'Bearer ' + token }
  })
  .then(response => response.text())
  .then(data => {
    console.log(data);
    addContent(data);
  })
  .catch(error => {
    console.error("Error fetching hello:", error);
  });
}

function whoami(token) {
  fetch('/api/whoami', {
    headers: { Authorization: 'Bearer ' + token }
  })
  .then(response => response.json())
  .then(data => {
    console.log(data.email + data.role);
    addContent("Whoami at rest service: " + data.email + " - " + data.role);
  })
  .catch(error => {
    console.error("Error fetching whoami:", error);
  });
}

function addContent(text) {
  document.getElementById("contentdiv").innerHTML += (text + "<br/>");
}

document.getElementById("getAllOrdersBtn").addEventListener("click", function () {
  console.log("button clicked");
  const auth = getAuth();
  console.log("getauth");
  auth.currentUser.getIdToken(true).then(function(token) {
    console.log("inside");
    console.log(token);
    fetch('/api/getAllOrders', {
      method: 'GET', // You might need to adjust this depending on your API requirements
      headers: {
        'Authorization': 'Bearer ' + token
      }
    })
    .then(response => response.text())
    .then(data => {
      console.log(data);
    })
    .catch(error => {
      console.error('Error fetching orders:', error);
      alert("Failed to fetch orders.");
    });
  }).catch(function(error) {
    console.log('Error fetching token:', error);
    alert("Authentication error. Please log in again.");
  });
});