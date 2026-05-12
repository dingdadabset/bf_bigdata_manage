ALTER TABLE `dga_users`
  ADD COLUMN `display_name` VARCHAR(255) NULL AFTER `last_name`,
  ADD COLUMN `ldap_dn` VARCHAR(500) NULL AFTER `display_name`,
  ADD COLUMN `uid_number` BIGINT NULL AFTER `ldap_dn`,
  ADD COLUMN `gid_number` BIGINT NULL AFTER `uid_number`,
  ADD COLUMN `home_directory` VARCHAR(255) NULL AFTER `gid_number`,
  ADD COLUMN `login_shell` VARCHAR(100) NULL AFTER `home_directory`,
  ADD COLUMN `primary_group_name` VARCHAR(255) NULL AFTER `login_shell`,
  ADD COLUMN `supplementary_groups` TEXT NULL AFTER `primary_group_name`,
  ADD COLUMN `ldap_locked` BOOLEAN NULL DEFAULT FALSE AFTER `supplementary_groups`,
  ADD COLUMN `ldap_attributes_json` LONGTEXT NULL AFTER `ldap_locked`;
