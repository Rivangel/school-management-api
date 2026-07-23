-- Datos de prueba. Se ejecuta en cada arranque; INSERT IGNORE evita duplicados.
--
-- Credenciales de los usuarios de prueba:
--   admin@escuela.com          / admin123    (ADMIN)
--   juan.perez@escuela.com     / maestro123  (MAESTRO)
--   laura.gomez@escuela.com    / maestro123  (MAESTRO)
--   ana.lopez@escuela.com      / alumno123   (ALUMNO)
--   carlos.ramirez@escuela.com / alumno123   (ALUMNO)
--   maria.torres@escuela.com   / alumno123   (ALUMNO)

-- Usuarios (los emails de maestros/alumnos coinciden con sus registros para
-- aplicar las reglas de propiedad)
INSERT IGNORE INTO usuarios (id, nombre, email, password, rol) VALUES
    (1, 'Administrador', 'admin@escuela.com', '$2a$10$pvCUQ1ohtuxwWotLpuHvduK2oPqKmQusUD0unDNOpYgA90NTnNj1q', 'ADMIN'),
    (2, 'Juan Pérez', 'juan.perez@escuela.com', '$2a$10$Hx8U6VpkDULYyos6J6Fadu0LWhvuJk/1jEGGPcJYJMFdt7O8m4sSe', 'MAESTRO'),
    (3, 'Laura Gómez', 'laura.gomez@escuela.com', '$2a$10$Hx8U6VpkDULYyos6J6Fadu0LWhvuJk/1jEGGPcJYJMFdt7O8m4sSe', 'MAESTRO'),
    (4, 'Ana López', 'ana.lopez@escuela.com', '$2a$10$rNvw6j6Vqz1AjA7jAeYGb.yArw4lAAlQFvQO4zyxoajUWOSt4GjHS', 'ALUMNO'),
    (5, 'Carlos Ramírez', 'carlos.ramirez@escuela.com', '$2a$10$rNvw6j6Vqz1AjA7jAeYGb.yArw4lAAlQFvQO4zyxoajUWOSt4GjHS', 'ALUMNO'),
    (6, 'María Torres', 'maria.torres@escuela.com', '$2a$10$rNvw6j6Vqz1AjA7jAeYGb.yArw4lAAlQFvQO4zyxoajUWOSt4GjHS', 'ALUMNO');

-- Maestros
INSERT IGNORE INTO maestros (id, nombre, apellido, email, especialidad) VALUES
    (1, 'Juan', 'Pérez', 'juan.perez@escuela.com', 'Matemáticas'),
    (2, 'Laura', 'Gómez', 'laura.gomez@escuela.com', 'Ciencias de la Computación');

-- Alumnos
INSERT IGNORE INTO alumnos (id, nombre, apellido, matricula, email, grupo) VALUES
    (1, 'Ana', 'López', 'A2026001', 'ana.lopez@escuela.com', 'A'),
    (2, 'Carlos', 'Ramírez', 'A2026002', 'carlos.ramirez@escuela.com', 'A'),
    (3, 'María', 'Torres', 'A2026003', 'maria.torres@escuela.com', 'B');

-- Materias
INSERT IGNORE INTO materias (id, nombre, creditos, maestro_id) VALUES
    (1, 'Matemáticas I', 8, 1),
    (2, 'Programación Orientada a Objetos', 10, 2),
    (3, 'Bases de Datos', 8, 2);

-- Calificaciones (periodo 2026-1)
INSERT IGNORE INTO calificaciones (id, alumno_id, materia_id, calificacion, periodo) VALUES
    (1, 1, 1, 9.50, '2026-1'),
    (2, 1, 2, 8.70, '2026-1'),
    (3, 1, 3, 10.00, '2026-1'),
    (4, 2, 1, 7.20, '2026-1'),
    (5, 2, 2, 5.80, '2026-1'),
    (6, 3, 2, 8.00, '2026-1'),
    (7, 3, 3, 9.10, '2026-1');

-- Asistencias
INSERT IGNORE INTO asistencias (id, alumno_id, materia_id, fecha, presente) VALUES
    (1, 1, 1, '2026-07-20', 1),
    (2, 1, 1, '2026-07-21', 1),
    (3, 1, 2, '2026-07-20', 0),
    (4, 2, 1, '2026-07-20', 1),
    (5, 2, 2, '2026-07-21', 1),
    (6, 3, 2, '2026-07-20', 1),
    (7, 3, 3, '2026-07-21', 0);
