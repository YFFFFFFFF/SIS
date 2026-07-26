INSERT INTO sys_role (code, name) VALUES ('ANALYST', 'Investment Analyst');
INSERT INTO sys_role (code, name) VALUES ('ADMIN', 'System Administrator');

INSERT INTO sys_user (username, password_hash, display_name, enabled)
VALUES ('analyst', '$2a$10$rvR010ML34pmLw1cV0ZN/e/m9vEQbvto34PzeUon35XwHVdsgl5DG', 'Investment Analyst', TRUE);

INSERT INTO sys_user (username, password_hash, display_name, enabled)
VALUES ('admin', '$2a$10$rvR010ML34pmLw1cV0ZN/e/m9vEQbvto34PzeUon35XwHVdsgl5DG', 'System Administrator', TRUE);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'analyst' AND r.code = 'ANALYST';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'ADMIN';
