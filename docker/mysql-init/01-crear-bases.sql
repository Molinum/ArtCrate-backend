-- Se ejecuta UNA sola vez, la primera vez que se crea el volumen de MySQL.
-- Una base de datos por servicio; el usuario "artcrate" tiene permisos solo sobre ellas.
CREATE DATABASE IF NOT EXISTS usuarios_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS catalogo_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventario_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS carrito_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pedidos_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pagos_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON usuarios_db.*   TO 'artcrate'@'%';
GRANT ALL PRIVILEGES ON catalogo_db.*   TO 'artcrate'@'%';
GRANT ALL PRIVILEGES ON inventario_db.* TO 'artcrate'@'%';
GRANT ALL PRIVILEGES ON carrito_db.*    TO 'artcrate'@'%';
GRANT ALL PRIVILEGES ON pedidos_db.*    TO 'artcrate'@'%';
GRANT ALL PRIVILEGES ON pagos_db.*      TO 'artcrate'@'%';
