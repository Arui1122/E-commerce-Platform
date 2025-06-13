-- 創建各個微服務的數據庫
CREATE DATABASE user_service;
CREATE DATABASE product_service;
CREATE DATABASE order_service;
CREATE DATABASE inventory_service;

-- 創建用戶並分配權限
CREATE USER user_service_user WITH PASSWORD 'user_service_pass';
CREATE USER product_service_user WITH PASSWORD 'product_service_pass';
CREATE USER order_service_user WITH PASSWORD 'order_service_pass';
CREATE USER inventory_service_user WITH PASSWORD 'inventory_service_pass';

GRANT ALL PRIVILEGES ON DATABASE user_service TO user_service_user;
GRANT ALL PRIVILEGES ON DATABASE product_service TO product_service_user;
GRANT ALL PRIVILEGES ON DATABASE order_service TO order_service_user;
GRANT ALL PRIVILEGES ON DATABASE inventory_service TO inventory_service_user;
