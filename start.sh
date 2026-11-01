#!/usr/bin/env bash
# =============================================================================
# Swiggy Food Delivery — Automated 1-Click Launch Script (macOS / Linux)
# =============================================================================

set -e

echo ""
echo "======================================================================"
echo "       🍔  SWIGGY FOOD DELIVERY ECOSYSTEM — AUTOMATED STARTUP  🛵      "
echo "======================================================================"
echo ""

# 1. Check Docker
echo "▶ Step 1/5: Checking Docker engine status..."
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and run this script again."
    exit 1
fi
echo "   ✔ Docker engine is active."

# 2. Start MySQL and Redis containers
echo ""
echo "▶ Step 2/5: Starting database & cache containers (MySQL + Redis)..."
docker compose up -d

# 3. Wait for MySQL readiness
echo "   Waiting for MySQL to accept connections..."
until docker exec food-delivery-mysql mysqladmin ping -h localhost -u root -proot_password --silent > /dev/null 2>&1; do
    echo "   ... waiting for database"
    sleep 2
done
echo "   ✔ MySQL and Redis are healthy and ready!"

# 4. Seed database
echo ""
echo "▶ Step 3/5: Seeding 12 Restaurants, 80+ Dishes, Customers, Riders & Coupons..."
if [ -f "seed_swiggy_db.sql" ]; then
    docker exec -i food-delivery-mysql mysql -u root -proot_password food_delivery_db < seed_swiggy_db.sql
    echo "   ✔ Database seeded successfully!"
fi

# 5. Check frontend dependencies
echo ""
echo "▶ Step 4/5: Verifying frontend node dependencies..."
if [ ! -d "frontend/node_modules" ]; then
    echo "   Installing frontend node packages (one-time setup)..."
    npm install --prefix frontend
fi
echo "   ✔ Frontend dependencies are ready."

# 6. Start Backend & Frontend
echo ""
echo "▶ Step 5/5: Launching Spring Boot backend and React frontend..."
./mvnw spring-boot:run &
BACKEND_PID=$!

npm run dev --prefix frontend &
FRONTEND_PID=$!

echo "   ✔ Backend PID: $BACKEND_PID | Frontend PID: $FRONTEND_PID"

sleep 5

# Open browser if command exists
if command -v xdg-open > /dev/null; then
    xdg-open "http://localhost:5173"
elif command -v open > /dev/null; then
    open "http://localhost:5173"
fi

echo ""
echo "======================================================================"
echo "       🎉  ALL SERVICES STARTED!  🎉                                  "
echo "======================================================================"
echo " 🌐 Customer Portal:  http://localhost:5173"
echo " 🍳 Partner Tab:      http://localhost:5173/?portal=partner"
echo " 🛵 Fleet Rider Tab:  http://localhost:5173/?portal=rider"
echo ""
echo " Press Ctrl+C to stop servers."

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true; docker compose down" EXIT
wait

