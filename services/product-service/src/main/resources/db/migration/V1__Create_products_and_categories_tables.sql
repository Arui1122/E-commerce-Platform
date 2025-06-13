-- Create categories table
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create products table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category_id BIGINT,
    brand VARCHAR(100),
    sku VARCHAR(100) UNIQUE,
    image_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Create indexes
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_status ON products(status);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_categories_active ON categories(active);
CREATE INDEX idx_categories_sort_order ON categories(sort_order);

-- Insert sample categories
INSERT INTO categories (name, description, sort_order) VALUES
('Electronics', 'Electronic devices and gadgets', 1),
('Clothing', 'Apparel and fashion items', 2),
('Books', 'Books and educational materials', 3),
('Home & Garden', 'Home improvement and garden supplies', 4),
('Sports', 'Sports equipment and accessories', 5);

-- Insert sample products
INSERT INTO products (name, description, price, category_id, brand, sku, status) VALUES
('Smartphone X1', 'Latest generation smartphone with advanced features', 999.99, 1, 'TechBrand', 'SP-X1-001', 'ACTIVE'),
('Laptop Pro 15', 'High-performance laptop for professionals', 1299.99, 1, 'CompuTech', 'LP-PRO-15', 'ACTIVE'),
('Cotton T-Shirt', 'Comfortable cotton t-shirt in various colors', 29.99, 2, 'FashionCo', 'TS-COT-001', 'ACTIVE'),
('Java Programming Guide', 'Comprehensive guide to Java programming', 49.99, 3, 'TechBooks', 'BK-JAVA-001', 'ACTIVE'),
('Garden Tools Set', 'Complete set of essential garden tools', 89.99, 4, 'GreenThumb', 'GT-SET-001', 'ACTIVE'),
('Running Shoes', 'Professional running shoes for athletes', 129.99, 5, 'SportMax', 'RS-PRO-001', 'ACTIVE');
