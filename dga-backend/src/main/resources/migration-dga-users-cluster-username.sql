-- Manual migration for changing DGA users from global username uniqueness
-- to per-cluster username uniqueness.
--
-- Review duplicate data before running. If duplicate (cluster_name, username)
-- rows already exist, merge or soft-delete duplicates first.

ALTER TABLE dga_users
  ADD COLUMN IF NOT EXISTS cluster_name VARCHAR(255);

UPDATE dga_users
SET cluster_name = 'CDH-Cluster-01'
WHERE cluster_name IS NULL OR cluster_name = '';

ALTER TABLE dga_users
  DROP INDEX uk_username;

ALTER TABLE dga_users
  ADD UNIQUE KEY uk_cluster_username (cluster_name, username);
