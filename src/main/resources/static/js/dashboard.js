import {
  getAuth,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/9.9.4/firebase-auth.js";

document.addEventListener('DOMContentLoaded', () => {
    // Call createTravelPackage API when the page loads
    fetch('/travel/createPackage', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({})
    })
    .then(response => response.json())
    .then(data => {
        // Store the packageId in the hidden input field
        document.getElementById('packageId').value = data.packageId;
    })
    .catch(error => console.error('Error:', error));

    // Add event listeners to the first booking forms
    addEventListeners(document.querySelector('.flight-booking'));
    addEventListeners(document.querySelector('.hotel-booking'));
});

// Function to send data to the backend
function sendData(url, data) {
    return fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    }).then(response => response.json());
}

// Function to add new flight booking form
document.getElementById('addFlightBooking').addEventListener('click', () => {
    const flightBookingTemplate = document.querySelector('.flight-booking').cloneNode(true);
    clearInputs(flightBookingTemplate);
    const uniqueId = Date.now();
    updateUniqueIds(flightBookingTemplate, 'flight', uniqueId);
    addEventListeners(flightBookingTemplate);
    document.getElementById('flightBookings').appendChild(flightBookingTemplate);
});

// Function to add new hotel booking form
document.getElementById('addHotelBooking').addEventListener('click', () => {
    const hotelBookingTemplate = document.querySelector('.hotel-booking').cloneNode(true);
    clearInputs(hotelBookingTemplate);
    const uniqueId = Date.now();
    updateUniqueIds(hotelBookingTemplate, 'hotel', uniqueId);
    addEventListeners(hotelBookingTemplate);
    document.getElementById('hotelBookings').appendChild(hotelBookingTemplate);
});

function clearInputs(section) {
    section.querySelectorAll('input').forEach(input => input.value = '');
    section.querySelectorAll('select').forEach(select => select.value = '');
    section.querySelectorAll('.flightOptions, .hotelOptions, .passengerDetails').forEach(div => div.classList.add('hidden'));
}

function updateUniqueIds(section, type, uniqueId) {
    section.querySelectorAll(`input[name^="${type}"]`).forEach(input => {
        input.name = `${type}${uniqueId}`;
    });
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

// Show flights when date and destination are selected
document.querySelectorAll('.confirmFlightDetails').forEach(button => {
    button.addEventListener('click', showFlights);
});

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

// Show passenger details after flight selection
document.querySelectorAll('.confirmFlightSelection').forEach(button => {
    button.addEventListener('click', showPassengerDetails);
});

function showPassengerDetails(event) {
    const flightBookingSection = event.target.closest('.flight-booking');
    const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked');
    const passengerDetails = flightBookingSection.querySelector('.passengerDetails');

    if (selectedFlight) {
        passengerDetails.classList.remove('hidden');
    }
}

/*********************************************************************************************************/

// Show passenger name fields based on number of passengers
document.querySelectorAll('.numPassengers').forEach(input => {
    input.addEventListener('input', showPassengerFields);
});

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

// Confirm passengers and enable booking confirmation button
document.querySelectorAll('.confirmPassengerNames').forEach(button => {
    button.addEventListener('click', confirmFlightBooking);
});

function confirmFlightBooking(event) {
    const flightBookingSection = event.target.closest('.flight-booking');
    const packageId = document.getElementById('packageId').value;
    const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
    const numPassengers = flightBookingSection.querySelector('.numPassengers').value;

    const flightDetails = {
        packageId: packageId,
        flightId: selectedFlight,
        seatsBooked: numPassengers
    };

    sendData('/travel/addFlight', flightDetails).then(response => {
        console.log(response);
        document.getElementById('confirmBooking').classList.remove('hidden');
    });
}

/*********************************************************************************************************/

// Show hotels when date and destination are selected
document.querySelectorAll('.confirmHotelDetails').forEach(button => {
    button.addEventListener('click', showHotels);
});

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

// Confirm hotel and enable booking confirmation button
document.querySelectorAll('.confirmHotelSelection').forEach(button => {
    button.addEventListener('click', confirmHotelBooking);
});

function confirmHotelBooking(event) {
    const hotelBookingSection = event.target.closest('.hotel-booking');
    const packageId = document.getElementById('packageId').value;
    const selectedHotel = hotelBookingSection.querySelector('input[name^="hotel"]:checked').value;
    const numPeople = hotelBookingSection.querySelector('.numPeople').value;

    const hotelDetails = {
        packageId: packageId,
        hotelId: selectedHotel,
        roomsBooked: numPeople
    };

    sendData('/travel/addHotel', hotelDetails).then(response => {
        console.log(response);
        document.getElementById('confirmBooking').classList.remove('hidden');
    });
}

/*********************************************************************************************************/

// Show booking summary on final confirmation
document.getElementById('confirmBooking').addEventListener('click', showBookingSummary);

function showBookingSummary() {
    const summaryContent = document.getElementById('summaryContent');
    summaryContent.innerHTML = '';

    document.querySelectorAll('.flight-booking').forEach((flightBookingSection, index) => {
        const destination = flightBookingSection.querySelector('.destination').value;
        const date = flightBookingSection.querySelector('.date').value;
        const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
        const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
        const passengerNames = [];
        for (let i = 1; i <= numPassengers; i++) {
            passengerNames.push(flightBookingSection.querySelector(`#passenger${i}`).value);
        }

        summaryContent.innerHTML += `
            <h3 class="text-xl font-bold mb-2">Flight ${index + 1}</h3>
            <p><strong>Destination:</strong> ${destination}</p>
            <p><strong>Date:</strong> ${date}</p>
            <p><strong>Selected Flight:</strong> ${selectedFlight}</p>
            <p><strong>Number of Passengers:</strong> ${numPassengers}</p>
            <p><strong>Passenger Names:</strong> ${passengerNames.join(', ')}</p>
        `;
    });

    document.querySelectorAll('.hotel-booking').forEach((hotelBookingSection, index) => {
        const destination = hotelBookingSection.querySelector('.hotelDestination').value;
        const date = hotelBookingSection.querySelector('.hotelDate').value;
        const selectedHotel = hotelBookingSection.querySelector('input[name^="hotel"]:checked').value;
        const numPeople = hotelBookingSection.querySelector('.numPeople').value;
        const numDays = hotelBookingSection.querySelector('.numDays').value;

        summaryContent.innerHTML += `
            <h3 class="text-xl font-bold mb-2">Hotel ${index + 1}</h3>
            <p><strong>Destination:</strong> ${destination}</p>
            <p><strong>Date:</strong> ${date}</p>
            <p><strong>Selected Hotel:</strong> ${selectedHotel}</p>
            <p><strong>Number of People:</strong> ${numPeople}</p>
            <p><strong>Number of Days:</strong> ${numDays}</p>
        `;
    });

    document.getElementById('bookingSummary').classList.remove('hidden');
}

/*********************************************************************************************************/

// Finalize booking and reset form
document.getElementById('finalizeBooking').addEventListener('click', () => {
    const packageId = document.getElementById('packageId').value;

    const bookingDetails = {
        packageId: packageId
        // Populate with actual booking details
    };
    sendData('/travel/bookPackage', bookingDetails).then(response => {
        console.log(response);
        alert('Booking confirmed!');
        document.getElementById('flightBookingForm').reset();
        document.getElementById('hotelBookingForm').reset();
        document.getElementById('bookingSummary').classList.add('hidden');
        document.getElementById('confirmBooking').classList.add('hidden');
        document.getElementById('flightBookings').innerHTML = '<div class="flight-booking"></div>';
        document.getElementById('hotelBookings').innerHTML = '<div class="hotel-booking"></div>';
    });
});

// Initial setup to add event listeners to the first booking forms
document.addEventListener('DOMContentLoaded', () => {
    addEventListeners(document.querySelector('.flight-booking'));
    addEventListeners(document.querySelector('.hotel-booking'));
});


/*********************************************************************************************************/

//// Add these functions to handle AJAX requests
//function sendRequest(url, data, method = 'POST') {
//    return fetch(url, {
//        method: method,
//        headers: {
//            'Content-Type': 'application/json'
//        },
//        body: JSON.stringify(data)
//    }).then(response => response.json());
//}
//
//// Modify showFlights and showHotels functions to send AJAX requests
//function showFlights(event) {
//    const flightBookingSection = event.target.closest('.flight-booking');
//    const date = flightBookingSection.querySelector('.date').value;
//    const destination = flightBookingSection.querySelector('.destination').value;
//
//    if (date && destination) {
//        const requestData = {
//            date: date,
//            destination: destination
//        };
//
//        sendRequest('/flights/search', requestData).then(response => {
//            const flightOptions = flightBookingSection.querySelector('.flightOptions');
//            const flightsList = flightBookingSection.querySelector('.flightsList');
//            flightsList.innerHTML = '';
//
//            response.flights.forEach(flight => {
//                flightsList.innerHTML += `
//                    <div class="bg-gray-100 p-2 rounded-lg">
//                        <input type="radio" id="flight${flight.id}" name="flight${Date.now()}" value="${flight.id}">
//                        <label for="flight${flight.id}">${flight.name} - $${flight.price}</label>
//                    </div>
//                `;
//            });
//
//            flightOptions.classList.remove('hidden');
//        });
//    }
//}
//
//function showHotels(event) {
//    const hotelBookingSection = event.target.closest('.hotel-booking');
//    const date = hotelBookingSection.querySelector('.hotelDate').value;
//    const destination = hotelBookingSection.querySelector('.hotelDestination').value;
//
//    if (date && destination) {
//        const requestData = {
//            date: date,
//            destination: destination
//        };
//
//        sendRequest('/hotels/search', requestData).then(response => {
//            const hotelOptions = hotelBookingSection.querySelector('.hotelOptions');
//            const hotelsList = hotelBookingSection.querySelector('.hotelsList');
//            hotelsList.innerHTML = '';
//
//            response.hotels.forEach(hotel => {
//                hotelsList.innerHTML += `
//                    <div class="bg-gray-100 p-2 rounded-lg">
//                        <input type="radio" id="hotel${hotel.id}" name="hotel${Date.now()}" value="${hotel.id}">
//                        <label for="hotel${hotel.id}">${hotel.name} - $${hotel.price}/night</label>
//                    </div>
//                `;
//            });
//
//            hotelOptions.classList.remove('hidden');
//        });
//    }
//}
//
//// Modify confirmFlightBooking and confirmHotelBooking to include AJAX requests
//function confirmFlightBooking(event) {
//    const flightBookingSection = event.target.closest('.flight-booking');
//    const selectedFlight = flightBookingSection.querySelector('input[name^="flight"]:checked').value;
//    const numPassengers = flightBookingSection.querySelector('.numPassengers').value;
//    const passengerNames = [];
//    for (let i = 1; i <= numPassengers; i++) {
//        passengerNames.push(flightBookingSection.querySelector(`#passenger${i}`).value);
//    }
//
//    const requestData = {
//        flightId: selectedFlight,
//        passengers: passengerNames
//    };
//
//    sendRequest('/bookings/flights', requestData).then(response => {
//        alert('Flight booked successfully!');
//        document.getElementById('confirmBooking').classList.remove('hidden');
//    });
//}
//
//function confirmHotelBooking(event) {
//    const hotelBookingSection = event.target.closest('.hotel-booking');
//    const selectedHotel = hotelBookingSection.querySelector('input[name^="hotel"]:checked').value;
//    const numPeople = hotelBookingSection.querySelector('.numPeople').value;
//    const numDays = hotelBookingSection.querySelector('.numDays').value;
//
//    const requestData = {
//        hotelId: selectedHotel,
//        numPeople: numPeople,
//        numDays: numDays
//    };
//
//    sendRequest('/bookings/hotels', requestData).then(response => {
//        alert('Hotel booked successfully!');
//        document.getElementById('confirmBooking').classList.remove('hidden');
//    });
//}
