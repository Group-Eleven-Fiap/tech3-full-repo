INSERT INTO usuarios (id, username, password_hash, user_role, nome, email)
VALUES (nextval('usuarios_id_seq'), 'paciente2',
     '$2a$10$J2vNwkBoMXAcRCiJ72wyKuBpmg/fiV6J5EK2g1KcO5rn86DZQkXni',
     'PACIENTE', 'paciente2', 'paciente2@exemplo.com');
