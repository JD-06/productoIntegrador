-- V3: Datos semilla — roles y usuario Admin por defecto
-- PIN por defecto: 1234 (SHA-256)

INSERT INTO roles (nombre) VALUES ('ADMIN'), ('CAJERO'), ('SUPERVISOR')
ON CONFLICT (nombre) DO NOTHING;

-- Agrega columna UNIQUE en nombre de usuarios si no existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'usuarios_nombre_unique'
    ) THEN
        ALTER TABLE usuarios ADD CONSTRAINT usuarios_nombre_unique UNIQUE (nombre);
    END IF;
END
$$;

-- Usuario Admin con PIN 1234 (SHA-256: 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4)
INSERT INTO usuarios (nombre, pin_hash, rol_id)
SELECT 'Admin',
       '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4',
       id
FROM roles WHERE nombre = 'ADMIN'
ON CONFLICT (nombre) DO NOTHING;
