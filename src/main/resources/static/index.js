import {
  initializeApp
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-app.js";
import {
  getAuth,
  connectAuthEmulator,
  onAuthStateChanged,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";

// Firestore Functions
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

// we setup the authentication, and then wire up some key events to event handlers
setupAuth();
wireGuiUpEvents();
wireUpAuthChange();

//setup authentication with local or cloud configuration. 
function setupAuth() {
  let firebaseConfig;
  if (location.hostname === "localhost") {
    firebaseConfig = localFirebaseConfig;
  } else {
    firebaseConfig = productionFirebaseConfig;
  }

  // signout any existing user. Removes any token still in the auth context
  const firebaseApp = initializeApp(firebaseConfig);
  const auth = getAuth(firebaseApp);
  // initialize Firebase app
  const firestore = getFirestore(firebaseApp);

  try {
    auth.signOut();
  } catch (err) { }

  // connect to local emulator when running on localhost
  if (location.hostname === "localhost") {
    connectAuthEmulator(auth, "http://localhost:8082", { disableWarnings: true });
    // connect to Firestore emulator
    connectFirestoreEmulator(firestore, 'localhost', 8084);
  }

  // Save auth and db to global scope
  window.firebaseApp = firebaseApp;
  window.auth = auth;
  window.firestore = firestore;
}

function wireGuiUpEvents() {
  // Get references to the email and password inputs, and the sign in, sign out and sign up buttons
  var email = document.getElementById("email");
  var password = document.getElementById("password");
  var signInButton = document.getElementById("btnSignIn");
  var signUpButton = document.getElementById("btnSignUp");
  var logoutButton = document.getElementById("btnLogout");

  // Add event listeners to the sign in and sign up buttons
  signInButton.addEventListener("click", function () {
    // Sign in the user using Firebase's signInWithEmailAndPassword method

      console.log("Sign in button clicked");
      console.log("Email:", email.value);
      console.log("Password:", password.value);
    signInWithEmailAndPassword(getAuth(), email.value, password.value)
      .then(function (userCredential) {
        console.log("signed in");
        // Get the ID token
        userCredential.user.getIdToken().then((token) => {
          console.log("ID Token:", token);
          // You can use this token to make authenticated requests

          // Redirect to the dashboard page
          window.location.href = 'dashboard.html';
        });
      })
      .catch(function (error) {
        // Show an error message
        console.log("error signInWithEmailAndPassword:")
        console.log(error.message);
        alert(error.message);
      });
  });

  signUpButton.addEventListener("click", function () {
    // Sign up the user using Firebase's createUserWithEmailAndPassword method

    createUserWithEmailAndPassword(getAuth(), email.value, password.value)
      .then(async function (userCredential) {
        console.log("created");
        // Add user profile to Firestore
        const user = userCredential.user;
        await addDoc(collection(getFirestore(), "users"), {
          email: user.email,
          uid: user.uid,
          createdAt: new Date()
        });
        console.log("User profile added to Firestore");
      })
      .catch(function (error) {
        // Show an error message
        console.log("error createUserWithEmailAndPassword:");
        console.log(error.message);
        alert(error.message);
      });
  });

  // logoutButton.addEventListener("click", function () {
  //   try {
  //     var auth = getAuth();
  //     auth.signOut();
  //   } catch (err) { }
  // });

}

function wireUpAuthChange() {

  var auth = getAuth();
  onAuthStateChanged(auth, (user) => {
    console.log("onAuthStateChanged");
    if (user == null) {
      console.log("user is null");
      showUnAuthenticated();
      return;
    }
    if (auth == null) {
      console.log("auth is null");
      showUnAuthenticated();
      return;
    }
    if (auth.currentUser === undefined || auth.currentUser == null) {
      console.log("currentUser is undefined or null");
      showUnAuthenticated();
      return;
    }

    auth.currentUser.getIdTokenResult().then((idTokenResult) => {
      console.log("Hello " + auth.currentUser.email)

      //update GUI when user is authenticated
      showAuthenticated(auth.currentUser.email);

      console.log("Token: " + idTokenResult.token);

      //fetch data from server when authentication was successful. 
      var token = idTokenResult.token;
      fetchData(token);

      // Redirect to the dashboard page
      window.location.href = 'dashboard.html';
    });

  });
}

function fetchData(token) {
  getHello(token);
  whoami(token);
}
function showAuthenticated(username) {
  // document.getElementById("namediv").innerHTML = "Hello " + username;
  // document.getElementById("logindiv").style.display = "none";
  // document.getElementById("contentdiv").style.display = "block";
}

function showUnAuthenticated() {
  // document.getElementById("namediv").innerHTML = "";
  document.getElementById("email").value = "";
  document.getElementById("password").value = "";
  document.getElementById("logindiv").style.display = "block";
  document.getElementById("contentdiv").style.display = "none";
}

function addContent(text) {
  document.getElementById("contentdiv").innerHTML += (text + "<br/>");
}

// calling /api/hello on the rest service to illustrate text based data retrieval
function getHello(token) {

  fetch('/api/hello', {
    headers: { Authorization: 'Bearer ' + token }
  })
    .then((response) => {
      return response.text();
    })
    .then((data) => {

      console.log(data);
      addContent(data);
    })
    .catch(function (error) {
      console.log(error);
    });


}
// calling /api/whoami on the rest service to illustrate JSON based data retrieval
function whoami(token) {

  fetch('/api/whoami', {
    headers: { Authorization: 'Bearer ' + token }
  })
    .then((response) => {
      return response.json();
    })
    .then((data) => {
      console.log(data.email + data.role);
      addContent("Whoami at rest service: " + data.email + " - " + data.role);

    })
    .catch(function (error) {
      console.log(error);
    });

}

