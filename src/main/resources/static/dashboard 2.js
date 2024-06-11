import {
  getAuth,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";

function reserve(product) {
  alert("Reserving " + product);
}

document.getElementById("btnLogout").addEventListener("click", function () {
  try {
    var auth = getAuth();
    auth.signOut().then(() => {
      window.location.href = 'index.html';
    });
  } catch (err) {
    console.error(err);
  }
});

var auth = getAuth();
onAuthStateChanged(auth, (user) => {
  if (user == null) {
    window.location.href = 'index.html';
  } else {
    document.getElementById("namediv").innerHTML = "Hello " + user.email;
  }
});
