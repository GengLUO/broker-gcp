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

// Firebase configuration
const productionFirebaseConfig = {
  apiKey: "AIzaSyBPMvUxzHlxbEHebXinwE4_eA4fTOVPYLs",
  authDomain: "broker-da44b.firebaseapp.com",
  projectId: "broker-da44b",
  storageBucket: "broker-da44b.appspot.com",
  messagingSenderId: "78512882731",
  appId: "1:78512882731:web:7fd2c8c6aa99051296566e",
  measurementId: "G-60LX98H1E5"
};

const localFirebaseConfig = {
  apiKey: "AIzaSyBoLKKR7OFL2ICE15Lc1-8czPtnbej0jWY",
  authDomain: "localhost",
  projectId: "broker-da44b"
};

function setupAuth() {
  const firebaseConfig = (location.hostname === "localhost") ? localFirebaseConfig : productionFirebaseConfig;

  const firebaseApp = initializeApp(firebaseConfig);
  const auth = getAuth(firebaseApp);
  const firestore = getFirestore(firebaseApp);

  setPersistence(auth, browserSessionPersistence)
    .catch((error) => {
      console.error("Error setting persistence:", error);
    });

  if (location.hostname === "localhost") {
    connectAuthEmulator(auth, "http://localhost:8082", { disableWarnings: true });
    connectFirestoreEmulator(firestore, 'localhost', 8084);
  }

  window.firebaseApp = firebaseApp;
  window.auth = auth;
  window.firestore = firestore;

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
        showDashboard();
      }).catch((error) => {
        console.error("Error getting ID token:", error);
      });
    } else {
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
        storeUserInfo(userCredential.user);
        return userCredential.user.getIdToken();
      })
      .then((token) => {
        showDashboard();
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

        storeUserInfo(user);
        return user.getIdToken();
      })
      .then((token) => {
        showDashboard();
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

function showUnAuthenticated() {
  document.getElementById("loginContent").style.display = "block";
  document.getElementById("dashboardContent").style.display = "none";
}

function showDashboard() {
  document.getElementById("loginContent").style.display = "none";
  document.getElementById("dashboardContent").style.display = "block";

  const uid = sessionStorage.getItem('uid');
  const packageDetails = {
    packageId: "",
    userId: uid,
    hotelId: "",
    flightId: "",
    roomsBooked: 0,
    seatsBooked: 0,
    customerName: ""
  };

  fetch('/api/travel/createPackage', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(packageDetails)
    })
      .then(response => response.json())
      .then(data => {
        document.getElementById('packageId').value = data.packageId;
      })
      .catch(error => console.error('Error:', error));

  addEventListeners(document.querySelector('.flight-booking'));
  addEventListeners(document.querySelector('.hotel-booking'));
}

async function sendData(url, data) {
  const response = await fetch(url, {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
  });

  const contentType = response.headers.get('content-type');

  if (contentType && contentType.includes('application/json')) {
      return await response.json();
  } else {
      return await response.text();
  }
}

function addEventListeners(section) {
  section.querySelector('.confirmFlightDetails')?.addEventListener('click', showFlights);
  section.querySelector('.confirmFlightSelection')?.addEventListener('click', showPassengerDetails);
  section.querySelector('.numPassengers')?.addEventListener('input', showPassengerFields);
  section.querySelector('.confirmPassengerNames')?.addEventListener('click', confirmFlightBooking);
  section.querySelector('.confirmHotelDetails')?.addEventListener('click', showHotels);
  section.querySelector('.confirmHotelSelection')?.addEventListener('click', confirmHotelBooking);
}

function showFlights(event) {
  const flightBookingSection = event.target.closest('.flight-booking');
  const date = flightBookingSection.querySelector('.date').value;
  const destination = flightBookingSection.querySelector('.destination').value;
  const flightOptions = flightBookingSection.querySelector('.flightOptions');
  const flightsList = flightBookingSection.querySelector('.flightsList');

  if (date && destination) {
    flightOptions.classList.remove('hidden');
    flightsList.innerHTML = `
      <div class="bg-gray-100 p-2 rounded-lg">
        <input type="radio" id="flight1" name="flight${Date.now()}" value="0">
        <label for="flight1">Flight 1 - $500</label>
      </div>
      <div class="bg-gray-100 p-2 rounded-lg">
        <input type="radio" id="flight2" name="flight${Date.now()}" value="1">
        <label for="flight2">Flight 2 - $450</label>
      </div>
      <div class="bg-gray-100 p-2 rounded-lg">
        <input type="radio" id="flight3" name="flight${Date.now()}" value="2">
        <label for="flight3">Flight 3 - $600</label>
      </div>
    `;
  }
}

function showPassengerDetails(event) {
  const flightBookingSection = event.target.closest('.flight-booking');
  const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked');
  const passengerDetails = flightBookingSection.querySelector('.passengerDetails');

  if (selectedFlight) {
    passengerDetails.classList.remove('hidden');
  }
}

function showPassengerFields(event) {
  const flightBookingSection = event.target.closest('.flight-booking');
  const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
  const passengerNames = flightBookingSection.querySelector('.passengerNames');
  passengerNames.innerHTML = '';

  for (let i = 1; i <= numPassengers; i++) {
    passengerNames.innerHTML += `
      <div class="mb-4">
        <label for="passenger${i}" class="block text-sm font-semibold mb-1">Passenger ${i} Name</label>
        <input type="text" id="passenger${i}" name="passenger${i}" class="border border-gray-300 rounded px-3 py-2 w-full">
      </div>
    `;
  }
}

async function confirmFlightBooking(event) {
  const packageId = document.getElementById('packageId').value;
  const userId = sessionStorage.getItem('uid');
  const flightBookingSection = event.target.closest('.flight-booking');
  const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
  const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
  const customerName = flightBookingSection.querySelector('.passengerNames input').value;

  const flightDetails = {
      packageId: packageId,
      userId: userId,
      flightId: selectedFlight,
      seatsBooked: numPassengers,
      customerName: customerName
  };

  document.getElementById('flightId').value = selectedFlight;
  document.getElementById('seatsBooked').value = numPassengers;
  document.getElementById('customerName').value = customerName;

  document.getElementById('confirmBooking').classList.remove('hidden');

  const response = await fetch('/api/travel/addFlight', {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(flightDetails)
  });

  if (!response.ok) {
      console.error('confirm Flight Booking Error:', response);
  } else {
      console.log('confirm Flight Booking Success:', response);
  }
}

function showHotels(event) {
  const hotelBookingSection = event.target.closest('.hotel-booking');
  const date = hotelBookingSection.querySelector('.hotelDate').value;
  const destination = hotelBookingSection.querySelector('.hotelDestination').value;
  const hotelOptions = hotelBookingSection.querySelector('.hotelOptions');
  const hotelsList = hotelBookingSection.querySelector('.hotelsList');

  if (date && destination) {
    hotelOptions.classList.remove('hidden');
    hotelsList.innerHTML = `
        <div class="bg-gray-100 p-2 rounded-lg">
            <input type="radio" id="hotel1" name="hotel${Date.now()}" value="0">
            <label for="hotel1">Hotel 1 - $200/night</label>
        </div>
        <div class="bg-gray-100 p-2 rounded-lg">
            <input type="radio" id="hotel2" name="hotel${Date.now()}" value="1">
            <label for="hotel2">Hotel 2 - $150/night</label>
        </div>
        <div class="bg-gray-100 p-2 rounded-lg">
            <input type="radio" id="hotel3" name="hotel${Date.now()}" value="2">
            <label for="hotel3">Hotel 3 - $250/night</label>
        </div>
    `;
  }
}

async function confirmHotelBooking(event) {
  const packageId = document.getElementById('packageId').value;
  const userId = sessionStorage.getItem('uid');
  const hotelBookingSection = event.target.closest('.hotel-booking');
  const selectedHotel = hotelBookingSection.querySelector('input[name^="hotel"]:checked').value;
  const numPeople = hotelBookingSection.querySelector('.numPeople').value;

  const hotelDetails = {
      packageId: packageId,
      userId: userId,
      hotelId: selectedHotel,
      roomsBooked: numPeople
  };

  document.getElementById('hotelId').value = selectedHotel;
  document.getElementById('roomsBooked').value = numPeople;

  document.getElementById('confirmBooking').classList.remove('hidden');

  const response = await fetch('/api/travel/addHotel', {
      method: 'POST',
      headers: {
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(hotelDetails)
  });

  if (!response.ok) {
      console.error('confirm Hotel Booking Error:', response.statusText);
  }
  else {
      console.log('confirm Hotel Booking Success:', response);
  }
}

document.getElementById('confirmBooking').addEventListener('click', showBookingSummary);

function showBookingSummary() {
  const summaryContent = document.getElementById('summaryContent');
  summaryContent.innerHTML = '';

  const flightBookingSection = document.querySelector('.flight-booking');
  const destination = flightBookingSection.querySelector('.destination').value;
  const date = flightBookingSection.querySelector('.date').value;
  const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
  const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
  const passengerNames = [];
  for (let i = 1; i <= numPassengers; i++) {
      passengerNames.push(flightBookingSection.querySelector(`#passenger${i}`).value);
  }

  summaryContent.innerHTML += `
      <h3 class="text-xl font-bold mb-2">Flight</h3>
      <p><strong>Destination:</strong> ${destination}</p>
      <p><strong>Date:</strong> ${date}</p>
      <p><strong>Selected Flight:</strong> ${selectedFlight}</p>
      <p><strong>Number of Passengers:</strong> ${numPassengers}</p>
      <p><strong>Passenger Names:</strong> ${passengerNames.join(', ')}</p>
  `;

  const hotelBookingSection = document.querySelector('.hotel-booking');
  const hotelDestination = hotelBookingSection.querySelector('.hotelDestination').value;
  const hotelDate = hotelBookingSection.querySelector('.hotelDate').value;
  const selectedHotel = hotelBookingSection.querySelector('input[name^="hotel"]:checked').value;
  const numPeople = hotelBookingSection.querySelector('.numPeople').value;
  const numDays = hotelBookingSection.querySelector('.numDays').value;

  summaryContent.innerHTML += `
      <h3 class="text-xl font-bold mb-2">Hotel</h3>
      <p><strong>Destination:</strong> ${hotelDestination}</p>
      <p><strong>Date:</strong> ${hotelDate}</p>
      <p><strong>Selected Hotel:</strong> ${selectedHotel}</p>
      <p><strong>Number of People:</strong> ${numPeople}</p>
      <p><strong>Number of Days:</strong> ${numDays}</p>
  `;

  document.getElementById('bookingSummary').classList.remove('hidden');
  document.getElementById('finalizeBooking').classList.remove('hidden');
}

document.getElementById('finalizeBooking').addEventListener('click', () => {
  const packageId = document.getElementById('packageId').value;
  const hotelId = document.getElementById('hotelId').value;
  const roomsBooked = document.getElementById('roomsBooked').value;
  const flightId = document.getElementById('flightId').value;
  const seatsBooked = document.getElementById('seatsBooked').value;
  const customerName = document.getElementById('customerName').value;

  const userId = sessionStorage.getItem('uid');
  console.log("Dashboard JS finalizeBooking: user id: " + userId);

  const bookingDetails = {
      packageId: packageId,
      userId: userId,
      hotelId: hotelId,
      roomsBooked: roomsBooked,
      flightId: flightId,
      seatsBooked: seatsBooked,
      customerName: customerName
  };

  sendData('/api/travel/bookPackage', bookingDetails).then(response => {
      console.log(response);
      alert('Booking confirmed!');
      document.getElementById('flightBookingForm').reset();
      document.getElementById('hotelBookingForm').reset();
      document.getElementById('bookingSummary').classList.add('hidden');
      document.getElementById('confirmBooking').classList.add('hidden');
      document.getElementById('finalizeBooking').classList.add('hidden');
      document.getElementById('flightBookings').innerHTML = '<div class="flight-booking"></div>';
      document.getElementById('hotelBookings').innerHTML = '<div class="hotel-booking"></div>';
  });
});

/////////////////////////////////////////////
document.addEventListener('DOMContentLoaded', function () {
  const getOrdersButton = document.getElementById('getOrders');
  const getCustomersButton = document.getElementById('getCustomers');
  const dataDisplay = document.getElementById('dataDisplay');

  getOrdersButton.addEventListener('click', function () {
    fetch('/api/getAllOrders')
        .then(response => {
          if (!response.ok) {
            throw new Error('Network response was not ok');
          }
          return response.json();
        })
        .then(data => {
          displayData(data, 'Orders');
        })
        .catch(error => {
          console.error('Error fetching orders:', error);
          displayError('You are not authorized.');
        });
  });

  getCustomersButton.addEventListener('click', function () {
    fetch('/api/getAllCustomers')
        .then(response => {
          if (!response.ok) {
            throw new Error('Network response was not ok');
          }
          return response.json();
        })
        .then(data => {
          displayData(data, 'Customers');
        })
        .catch(error => {
          console.error('Error fetching customers:', error);
          displayError('You are not authorized.');
        });
  });

  function displayData(data, type) {
    // Clear any existing data
    dataDisplay.innerHTML = '';

    // Display the new data
    const heading = document.createElement('h3');
    heading.textContent = `Fetched ${type}:`;
    dataDisplay.appendChild(heading);

    const pre = document.createElement('pre');
    pre.textContent = JSON.stringify(data, null, 2);
    dataDisplay.appendChild(pre);
  }

  function displayError(message) {
    dataDisplay.innerHTML = `<p class="text-red-600">${message}</p>`;
  }
});
