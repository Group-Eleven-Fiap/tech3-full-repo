CREATE SEQUENCE usuarios_id_seq START WITH 4;

ALTER TABLE usuarios ALTER COLUMN id SET DEFAULT nextval('usuarios_id_seq');