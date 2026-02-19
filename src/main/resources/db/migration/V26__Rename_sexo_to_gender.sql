ALTER TABLE secto.client RENAME COLUMN sexo TO gender;

UPDATE secto.client SET gender = 'MALE' WHERE gender = 'MASCULINO';
UPDATE secto.client SET gender = 'FEMALE' WHERE gender = 'FEMININO';
UPDATE secto.client SET gender = 'OTHER' WHERE gender = 'OUTRO';
