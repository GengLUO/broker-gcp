import {
    onAuthStateChanged,
    setPersistence,
    browserSessionPersistence,
    // for autenticated user token
    signInWithCustomToken
  } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";
  import { connectFirestoreEmulator } from "https://www.gstatic.com/firebasejs/9.9.4/firebase-firestore.js";

  function setupDashboard() {
    const auth = window.auth;
    const firestore = window.firestore;

    if (location.hostname === "localhost") {
      connectFirestoreEmulator(firestore, 'localhost', 8084);
    }

    // setPersistence(auth, browserSessionPersistence)
    //   .then(() => {
    //     onAuthStateChanged(auth, (user) => {
    //       if (user) {
    //         user.getIdToken().then((token) => {
    //           fetchData(token, user.uid); // Pass the user ID to fetchData
    //         }).catch((error) => {
    //           console.error("Error getting ID token:", error);
    //         });
    //       } else {
    //         // Return to the login page if the user is not logged in
    //         window.location.href = 'html/index.html';
    //       }
    //     });
    //   })
    //   .catch((error) => {
    //     console.error("Error setting persistence:", error);
    //   });
  }

  // Call the setup function when the page loads
  window.onload = setupDashboard;

  document.addEventListener('DOMContentLoaded', () => {
    const uid = sessionStorage.getItem('uid');
    // print user id
    console.log("Dashboard JS user id: " + uid);

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
  });

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

  /*********************************************************************************************************/

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

  /*********************************************************************************************************/

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

  /*********************************************************************************************************/
  async function confirmFlightBooking(event) {
    const packageId = document.getElementById('packageId').value;
    const userId = sessionStorage.getItem('uid');
    const flightBookingSection = event.target.closest('.flight-booking');
    const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
    const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
    const customerName = flightBookingSection.querySelector('.passengerNames input').value; // Assuming the first passenger's name is the customer's name

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

    // Post the flightDetails to the server
    const response = await fetch('/api/travel/addFlight', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(flightDetails)
    });

    if (!response.ok) {
//        console.error('confirm Flight Booking Error:', response.statusText);
        console.error('confirm Flight Booking Error:', response);
    } else {
        console.log('confirm Flight Booking Success:', response);
    }
}

/*********************************************************************************************************/

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

/*********************************************************************************************************/

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

    // Post the hotelDetails to the server
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

/*********************************************************************************************************/

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

/*********************************************************************************************************/

document.getElementById('finalizeBooking').addEventListener('click', () => {
    const packageId = document.getElementById('packageId').value;
    const hotelId = document.getElementById('hotelId').value;
    const roomsBooked = document.getElementById('roomsBooked').value;
    const flightId = document.getElementById('flightId').value;
    const seatsBooked = document.getElementById('seatsBooked').value;
    const customerName = document.getElementById('customerName').value;

    const userId = sessionStorage.getItem('uid');
    console.log("Dashboard JS finalizeBooking: user id: " + userId); // print user id

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

