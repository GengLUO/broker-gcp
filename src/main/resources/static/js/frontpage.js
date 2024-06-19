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
  setDoc,
  doc,
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
// Use local or production configuration based on the hostname
const firebaseConfig = productionFirebaseConfig;

// Initialize Firebase app
const firebaseApp = initializeApp(firebaseConfig);
const auth = getAuth(firebaseApp);
const firestore = getFirestore(firebaseApp);

document.addEventListener('DOMContentLoaded', () => {
  setupAuth();
  wireGuiUpEvents();
  setupManaerFunc();
});

function setupAuth() {
  setPersistence(auth, browserSessionPersistence)
    .catch((error) => {
      console.error("Error setting persistence:", error);
    });

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

  onAuthStateChanged(window.auth, (user) => {
    if (user) {
      user.getIdToken().then((token) => {
        fetchData(token);
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
  const dashboardButton = document.getElementById("btnDashboard");
  const myBookingsButton = document.getElementById("btnMyBookings");
  const signOutButton = document.getElementById("btnLogout");

  signInButton.addEventListener("click", () => {
    setPersistence(window.auth, browserSessionPersistence)
      .then(() => {
        return signInWithEmailAndPassword(window.auth, email.value, password.value);
      })
      .then((userCredential) => {
        storeUserInfo(userCredential.user); // Store user info
        return userCredential.user.getIdToken();
      })
    .then(async (token) => {
        // createPackageWhenLoggedIn(token);
      // Check the user's role and show the appropriate dashboard
          const response = await fetch('/api/whoami', {
        headers: { Authorization: 'Bearer ' + token }
      });
      // TODO: check 
      const user = await response.json();
      if (user.role === 'manager') {
        showManagerDashboard();
      }
      else {
        showDashboard();
      }

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
        const userRef = doc(window.firestore, "users", user.uid); // Correctly create document reference
        await setDoc(userRef, {
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
      })
      .catch((error) => {
        console.error("Error during sign up:", error.message);
        alert(error.message);
      });
  });

  dashboardButton.addEventListener("click", () => {
    // if authenticated, show the dashboard
    if (auth.currentUser) {
      showDashboard();
    }
  });

  myBookingsButton.addEventListener("click", () => {
    // if authenticated, fetch the user's bookings
    if (auth.currentUser) {
      auth.currentUser.getIdToken().then(token => {
        fetchMyBookings(token);
      }
      ).catch(error => {
        console.error("Please log in:", error);
      });
    }
  });

  signOutButton.addEventListener("click", () => {
    auth.signOut()
      .then(() => {
        showUnAuthenticated();
      })
      .catch((error) => {
        console.error("Error signing out:", error);
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
  // clear the user info in the login form
  document.getElementById("email").value = "";
  document.getElementById("password").value = "";

  document.getElementById("loginContent").style.display = "block";
  document.getElementById("dashboardContent").style.display = "none";
  document.getElementById("dashboardManagerContent").style.display = "none";
  document.getElementById("myBookingsContent").style.display = "none";

  // hide the navigation links
  document.getElementById("mainNav").style.display = "none";
}

function showDashboard() {
  document.getElementById("loginContent").style.display = "none";
  document.getElementById("dashboardContent").style.display = "block";
  document.getElementById("dashboardManagerContent").style.display = "none";
  document.getElementById("myBookingsContent").style.display = "none";

  //unhide the navigation links
  document.getElementById("mainNav").style.display = "block";

  addEventListeners(document.querySelector('.flight-booking'));
  addEventListeners(document.querySelector('.hotel-booking'));
}

function createPackage(token) {
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
          Authorization: 'Bearer ' + token,
          'Content-Type': 'application/json'
      },
      body: JSON.stringify(packageDetails)
  })
      .then(response => response.json())
      .then(data => {
          document.getElementById('packageId').value = data.packageId;
      })
      .catch(error => console.error('Error:', error));
}

function showManagerDashboard() {
  document.getElementById("loginContent").style.display = "none";
  document.getElementById("dashboardContent").style.display = "none";
  document.getElementById("dashboardManagerContent").style.display = "block";
  document.getElementById("myBookingsContent").style.display = "none";
}

function getHello(token) {
  fetch('/api/hello', {
    headers: { Authorization: 'Bearer ' + token }
  })
  .then(response => response.text())
  .then(data => {
    console.log("Hello: " + data);
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
    console.log("Whoami: " + data.email + data.role);
  })
  .catch(error => {
    console.error("Error fetching whoami:", error);
  });
}

async function sendData(url, data) {
  const response = await fetch(url, {
      method: 'POST',
      headers: {
          Authorization: 'Bearer ' + sessionStorage.getItem('token'), 
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

  // create travel package
  createPackage(sessionStorage.getItem('token'));
}

async function confirmFlightBooking(event) {
  const packageId = document.getElementById('packageId').value;
  const userId = sessionStorage.getItem('uid');
  const flightBookingSection = event.target.closest('.flight-booking');
  const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
  const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
  const customerName = flightBookingSection.querySelector('.passengerNames input').value;
  const flightDestination = flightBookingSection.querySelector('.destination').value;
  const flightDate = flightBookingSection.querySelector('.date').value;

  const token = sessionStorage.getItem('token');

  const flightDetails = {
      packageId: packageId,
      flightId: selectedFlight,
      seatsBooked: numPassengers,
      customerName: customerName,
      flightDestination: flightDestination,
      flightDate: flightDate
  };

  document.getElementById('flightId').value = selectedFlight;
  document.getElementById('seatsBooked').value = numPassengers;
  document.getElementById('customerName').value = customerName;

  document.getElementById('confirmBooking').classList.remove('hidden');
  
try {
      const response = await fetch('/api/travel/addFlight', {
          method: 'POST',
          headers: {
              Authorization: 'Bearer ' + token,
              'Content-Type': 'application/json'
          },
          body: JSON.stringify(flightDetails)
      });

      if (!response.ok) {
          throw new Error('Network response was not ok');
      }

      // Show the success message
      alert('Passengers Successfully Confirmed!');
      console.log('confirm Flight Booking Success:', response);
  } catch (error) {
      console.error('confirm Flight Booking Error:', error);
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
  const hotelDestination = hotelBookingSection.querySelector('.hotelDestination').value;
  const hotelDate = hotelBookingSection.querySelector('.hotelDate').value;
  const hotelDays = hotelBookingSection.querySelector('.numDays').value;

  const token = sessionStorage.getItem('token');

  const hotelDetails = {
      packageId: packageId,
      hotelId: selectedHotel,
      roomsBooked: numPeople,
      hotelDestination: hotelDestination,
      hotelDate: hotelDate,
      hotelDays: hotelDays

  };

  document.getElementById('hotelId').value = selectedHotel;
  document.getElementById('roomsBooked').value = numPeople;

  document.getElementById('confirmBooking').classList.remove('hidden');

try {
      const response = await fetch('/api/travel/addHotel', {
          method: 'POST',
          headers: {
              Authorization: 'Bearer ' + token,
              'Content-Type': 'application/json'
          },
          body: JSON.stringify(hotelDetails)
      });

      if (!response.ok) {
          throw new Error('Network response was not ok');
      }

      // Show the success message
      alert('Hotel Successfully Confirmed!');
      console.log('confirm Hotel Booking Success:', response);
  } catch (error) {
      console.error('confirm Hotel Booking Error:', error);
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
  // get flight booking section by class name
  const flightBookingSection = document.querySelector('.flight-booking');
  const flightId = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
  const seatsBooked = flightBookingSection.querySelector('.numPassengers').value;
  const customerName = flightBookingSection.querySelector('.passengerNames input').value;

  // get hotel booking section by class name
  const hotelBookingSection = document.querySelector('.hotel-booking');
  const hotelId = hotelBookingSection.querySelector('input[name^="hotel"]:checked').value;
  const roomsBooked = hotelBookingSection.querySelector('.numPeople').value;

  const packageId = document.getElementById('packageId').value;

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
      // clear the packageId
      document.getElementById('packageId').value = '';
  });
});

function fetchMyBookings(token) {
  const userId = sessionStorage.getItem('uid');
  fetch(`/api/getUserBookings?userId=${userId}`, {
    headers: { Authorization: 'Bearer ' + token }
  })
  .then(response => response.json())
  .then(data => {
    displayMyBookings(data);
    showMyBookings();
  })
  .catch(error => {
    console.error("Error fetching my bookings:", error);
  });
}

function displayMyBookings(bookings) {
  if (!Array.isArray(bookings)) {
    console.error("Expected an array but received:", bookings);
    return;
  }

  const tableBody = document.getElementById('bookingsTableBody');
  tableBody.innerHTML = ''; // Clear any existing rows

  // Define headers
  const headers = ['Package ID', 'Customer Name', 'Flight Destination', 'Flight Date', 'Flight ID', 'Seats Booked', 'Flight Confirmation','Hotel Destination', 'Hotel Date', 'Hotel Days', 'Hotel ID', 'Rooms Booked', 'Hotel Confirmation'];

  // Create table header row
  const headerRow = document.createElement('tr');
  headers.forEach(header => {
    const th = document.createElement('th');
    th.className = 'py-2 px-4 border-b border-gray-200';
    th.textContent = header;
    headerRow.appendChild(th);
  });
  tableBody.appendChild(headerRow);

  // Create table body
  bookings.forEach(booking => {
    const row = document.createElement('tr');
    const cells = ['packageId', 'customerName', 'flightDestination', 'flightDate', 'flightId', 'seatsBooked', 'flightConfirmStatus', 'hotelDestination', 'hotelDate', 'hotelDays', 'hotelId', 'roomsBooked', 'hotelConfirmStatus'];
    cells.forEach(cell => {
      const td = document.createElement('td');
      td.className = 'py-2 px-4 border-b border-gray-200';
      td.textContent = booking[cell] || 'N/A';
      row.appendChild(td);
    });
    tableBody.appendChild(row);
  });
}

function showMyBookings() {
  document.getElementById("loginContent").style.display = "none";
  document.getElementById("dashboardContent").style.display = "none";
  document.getElementById("dashboardManagerContent").style.display = "none";
  document.getElementById("myBookingsContent").style.display = "block";
}


function setupManaerFunc() {
  const getOrdersButton = document.getElementById('getOrders');
  const getCustomersButton = document.getElementById('getCustomers');
  const dataDisplay = document.getElementById('dataDisplay');

  getOrdersButton.addEventListener('click', function () {
    fetch('/api/getAllOrders', {
      headers: {
        'Authorization': 'Bearer ' + sessionStorage.getItem('token')
      }
    }).then(response => {
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
    fetch('/api/getAllCustomers', {
      headers: {
        'Authorization': 'Bearer ' + sessionStorage.getItem('token')
      }
    }).then(response => {
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
  
    // Create a table
    const table = document.createElement('table');
    table.className = 'my-beautiful-table';
  
    // Create table header
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    Object.keys(data[0]).forEach(key => {
      const th = document.createElement('th');
      th.textContent = key;
      headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);
    table.appendChild(thead);
  
    // Create table body
    const tbody = document.createElement('tbody');
    data.forEach(item => {
      const row = document.createElement('tr');
      Object.values(item).forEach(value => {
        const td = document.createElement('td');
        td.textContent = value;
        row.appendChild(td);
      });
      tbody.appendChild(row);
    });
    table.appendChild(tbody);
  
    // Append the table to the data display
    dataDisplay.appendChild(table);
  }

  function displayError(message) {
    dataDisplay.innerHTML = `<p class="text-red-600">${message}</p>`;
  }
}
