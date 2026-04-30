-- Manual migration for recording the operator who revoked a resource grant.

ALTER TABLE user_resource_access
    ADD COLUMN revoked_by VARCHAR(100) NULL AFTER granted_by;
