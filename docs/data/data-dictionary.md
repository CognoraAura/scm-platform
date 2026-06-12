# Data Dictionary

## Order Service (db_order)

### ord_order
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| order_no | VARCHAR(32) | Order number (unique) |
| customer_id | BIGINT | Customer ID |
| status | VARCHAR(20) | Order status |
| total_amount | DECIMAL(12,2) | Total amount |
| create_time | TIMESTAMP | Creation time |
| update_time | TIMESTAMP | Last update time |

### ord_order_item
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| order_id | BIGINT | Order ID (FK) |
| sku_id | VARCHAR(32) | SKU ID |
| quantity | INT | Quantity |
| unit_price | DECIMAL(12,2) | Unit price |

## Inventory Service (db_inventory)

### inv_stock
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| sku_id | VARCHAR(32) | SKU ID |
| warehouse_id | BIGINT | Warehouse ID |
| available_qty | INT | Available quantity |
| reserved_qty | INT | Reserved quantity |
| safety_stock | INT | Safety stock level |

### inv_reservation
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| reservation_no | VARCHAR(32) | Reservation number |
| order_id | BIGINT | Order ID |
| sku_id | VARCHAR(32) | SKU ID |
| quantity | INT | Reserved quantity |
| status | VARCHAR(20) | Reservation status |
| reserved_at | TIMESTAMP | Reservation time |

## Product Service (db_product)

### pro_product
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| product_name | VARCHAR(100) | Product name |
| category_id | BIGINT | Category ID |
| brand | VARCHAR(50) | Brand |
| status | VARCHAR(20) | Product status |

### pro_sku
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| product_id | BIGINT | Product ID (FK) |
| sku_code | VARCHAR(32) | SKU code |
| sku_name | VARCHAR(100) | SKU name |
| price | DECIMAL(12,2) | Price |

## Last Updated
- Date: 2026-06-12
- Version: 1.0