-- V4: Usuario cajero por defecto
-- PIN por defecto: 1234

INSERT INTO usuarios (nombre, pin_hash, rol_id, activo)
SELECT 'Cajero',
       '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4',
       r.id,
       TRUE
FROM roles r
WHERE r.nombre = 'CAJERO'
ON CONFLICT (nombre) DO NOTHING;
