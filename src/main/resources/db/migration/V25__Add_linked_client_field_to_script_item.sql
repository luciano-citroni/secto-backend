-- Adicionar campo linked_client_field ao script_item para vincular perguntas a dados do cliente
ALTER TABLE secto.script_item ADD COLUMN linked_client_field VARCHAR(50);
