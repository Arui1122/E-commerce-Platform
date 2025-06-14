#!/bin/bash

# Test script for Notification Service
echo "Testing Notification Service..."

# Wait for service to be ready
echo "Waiting for notification service to be ready..."
sleep 5

# Test health endpoint
echo "Testing health endpoint..."
curl -s http://localhost:8086/api/v1/notifications/health | jq .

echo ""

# Test sending a test email notification
echo "Testing email notification..."
curl -X POST http://localhost:8086/api/v1/notifications/send-email \
  -H "Content-Type: application/json" \
  -d '{
    "to": "test@example.com",
    "subject": "Test Email",
    "template": "welcome",
    "data": {
      "firstName": "Test User",
      "username": "testuser"
    }
  }' | jq .

echo ""

# Test sending a notification message
echo "Testing notification message..."
curl -X POST http://localhost:8086/api/v1/notifications/send \
  -H "Content-Type: application/json" \
  -d '{
    "type": "EMAIL",
    "recipient": "test@example.com",
    "subject": "Test Notification",
    "template": "welcome",
    "data": {
      "firstName": "Test User",
      "username": "testuser"
    }
  }' | jq .

echo ""
echo "Notification service test completed!"
echo "You can view sent emails at: http://localhost:1080"
