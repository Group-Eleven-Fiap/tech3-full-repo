ALTER TABLE usuarios ADD COLUMN nome VARCHAR(150);
ALTER TABLE usuarios ADD COLUMN email VARCHAR(254);

UPDATE usuarios
SET nome = CASE username
               WHEN 'medico' THEN 'Dr. João da Silva'
               WHEN 'enfermeiro' THEN 'Enfermeiro de Plantão'
               WHEN 'paciente' THEN 'Paciente de Teste'
               ELSE CONCAT('Usuário ', id)
           END,
    email = CASE username
                WHEN 'medico' THEN 'medico@exemplo.com'
                WHEN 'enfermeiro' THEN 'enfermeiro@exemplo.com'
                WHEN 'paciente' THEN 'paciente@exemplo.com'
                ELSE CONCAT('usuario-', id, '@exemplo.com')
            END;

ALTER TABLE usuarios ALTER COLUMN nome SET NOT NULL;
ALTER TABLE usuarios ALTER COLUMN email SET NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_email UNIQUE (email);