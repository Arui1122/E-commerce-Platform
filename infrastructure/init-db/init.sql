-- 創建各個微服務的數據庫
CREATE DATABASE ecommerce_user;
CREATE DATABASE ecommerce_product;
CREATE DATABASE ecommerce_order;
CREATE DATABASE ecommerce_inventory;

-- 創建用戶並分配權限
CREATE USER user_service_user WITH PASSWORD 'user_service_pass';
CREATE USER product_service_user WITH PASSWORD 'product_service_pass';
CREATE USER order_service_user WITH PASSWORD 'order_service_pass';
CREATE USER inventory_service_user WITH PASSWORD 'inventory_service_pass';

GRANT ALL PRIVILEGES ON DATABASE ecommerce_user TO user_service_user;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_product TO product_service_user;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_order TO order_service_user;
GRANT ALL PRIVILEGES ON DATABASE ecommerce_inventory TO inventory_service_user;
