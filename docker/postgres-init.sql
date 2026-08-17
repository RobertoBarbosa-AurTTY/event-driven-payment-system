SELECT 'CREATE DATABASE order_db OWNER payments'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order_db')\gexec
SELECT 'CREATE DATABASE payment_db OWNER payments'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec
SELECT 'CREATE DATABASE fraud_db OWNER payments'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'fraud_db')\gexec
SELECT 'CREATE DATABASE notification_db OWNER payments'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec