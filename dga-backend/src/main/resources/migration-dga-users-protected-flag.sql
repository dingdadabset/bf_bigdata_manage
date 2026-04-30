-- Manual migration for configurable protected users in permission management.
-- The application treats NULL as "use built-in defaults" so existing protected
-- big-data accounts remain protected until admin explicitly cancels protection.

ALTER TABLE dga_users
    ADD COLUMN is_protected BOOLEAN DEFAULT NULL COMMENT 'NULL means use built-in protection defaults; true/false means admin override';
