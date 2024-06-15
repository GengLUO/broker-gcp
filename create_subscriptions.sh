#!/bin/bash

# Define variables
PROJECT_ID="hotel-426314"
HOTEL_TOPIC="hotel-responses"
FLIGHT_TOPIC="flight-responses"
HOTEL_PUSH_ENDPOINT="https://your-server-url/hotel/pubsub/push"
FLIGHT_PUSH_ENDPOINT="https://your-server-url/flight/pubsub/push"

# Create hotel responses subscription
gcloud pubsub subscriptions create hotel-responses-subscription \
    --topic=$HOTEL_TOPIC \
    --push-endpoint=$HOTEL_PUSH_ENDPOINT \
    --ack-deadline=10 \
    --project=$PROJECT_ID

# Create flight responses subscription
gcloud pubsub subscriptions create flight-responses-subscription \
    --topic=$FLIGHT_TOPIC \
    --push-endpoint=$FLIGHT_PUSH_ENDPOINT \
    --ack-deadline=10 \
    --project=$PROJECT_ID

echo "Subscriptions created successfully."
