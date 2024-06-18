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

// Use local or production configuration based on the hostname
const firebaseConfig = (location.hostname === "localhost") ? productionFirebaseConfig : productionFirebaseConfig;

// Initialize Firebase app
const firebaseApp = initializeApp(firebaseConfig);
const auth = getAuth(firebaseApp);
const firestore = getFirestore(firebaseApp);

// Connect to Firestore emulator when running on localhost
if (location.hostname === "localhost") {
  connectFirestoreEmulator(firestore, 'localhost', 8084);
  // Uncomment if using auth emulator
  // connectAuthEmulator(auth, "http://localhost:8082", { disableWarnings: true });
}

// Setup authentication persistence and event listeners
function setupAuth() {
  setPersistence(auth, browserSessionPersistence)
    .then(() => {
      // Sign out any existing user
      if (auth.currentUser) {
        auth.signOut().catch((error) => {
          console.error("Error signing out existing user:", error);
        });
      }

      console.log("Auth persistence set");
      onAuthStateChanged(auth, (user) => {
        if (user) {
          user.getIdToken().then((token) => {
            fetchData(token);
          }).catch((error) => {
            console.error("Error getting ID token:", error);
          });
        } else {
          showUnAuthenticated();
        }
      });
    })
    .catch((error) => {
      console.error("Error setting persistence:", error);
    });
  wireGuiUpEvents();
}

setupAuth();

// allowing other javascript files to use the auth and firestore objects via import{auth, firestore, user} from './index.js'
export { firestore, auth }; 

function wireGuiUpEvents() {
  const email = document.getElementById("email");
  const password = document.getElementById("password");
  const signInButton = document.getElementById("btnSignIn");
  const signUpButton = document.getElementById("btnSignUp");

  signInButton.addEventListener("click", () => {
    signInWithEmailAndPassword(auth, email.value, password.value)
      .then((userCredential) => {
        console.log("signin button: auth.currentUser id", auth.currentUser.uid);
        storeUserInfo(userCredential.user);
        return userCredential.user.getIdToken();
      })
      .then((token) => {
        fetchData(token);
        window.location.href = 'html/dashboard.html';
      })
      .catch((error) => {
        console.error("Error during sign in:", error.message);
        alert(error.message);
      });
  });

  signUpButton.addEventListener("click", () => {
    createUserWithEmailAndPassword(auth, email.value, password.value)
      .then(async (userCredential) => {
        console.log("signup button: auth.currentUser id", auth.currentUser.uid);
        const user = userCredential.user;
        await addDoc(collection(firestore, "users"), {
          uid: user.uid,
          email: user.email,
          role: "user",
          createdAt: new Date()
        });
        console.log("User profile added to Firestore");

        storeUserInfo(user);
        return user.getIdToken();
      })
      .then((token) => {
        fetchData(token);
        window.location.href = 'html/dashboard.html';
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