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
                displayError('Error fetching orders. Please try again later.');
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
                displayError('Error fetching customers. Please try again later.');
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
