INSERT INTO sys_role (code, name) VALUES ('INVESTMENT_ANALYST', 'Investment Analyst');
INSERT INTO sys_role (code, name) VALUES ('FINANCE_SPECIALIST', 'Finance Specialist');
INSERT INTO sys_role (code, name) VALUES ('TECHNICAL_ENGINEER', 'Technical Engineer');
INSERT INTO sys_role (code, name) VALUES ('PROJECT_MANAGER', 'Project Manager');
INSERT INTO sys_role (code, name) VALUES ('SYSTEM_ADMINISTRATOR', 'System Administrator');

INSERT INTO sys_user (username, password_hash, display_name, enabled)
VALUES ('investment_analyst', '$2a$10$rvR010ML34pmLw1cV0ZN/e/m9vEQbvto34PzeUon35XwHVdsgl5DG', 'Investment Analyst', TRUE);

INSERT INTO sys_user (username, password_hash, display_name, enabled)
VALUES ('finance_specialist', '$2a$10$rvR010ML34pmLw1cV0ZN/e/m9vEQbvto34PzeUon35XwHVdsgl5DG', 'Finance Specialist', TRUE);

INSERT INTO sys_user (username, password_hash, display_name, enabled)
VALUES ('technical_engineer', '$2a$10$rvR010ML34pmLw1cV0ZN/e/m9vEQbvto34PzeUon35XwHVdsgl5DG', 'Technical Engineer', TRUE);

INSERT INTO sys_user (username, password_hash, display_name, enabled)
VALUES ('project_manager', '$2a$10$rvR010ML34pmLw1cV0ZN/e/m9vEQbvto34PzeUon35XwHVdsgl5DG', 'Project Manager', TRUE);

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'investment_analyst' AND r.code = 'INVESTMENT_ANALYST';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'finance_specialist' AND r.code = 'FINANCE_SPECIALIST';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'technical_engineer' AND r.code = 'TECHNICAL_ENGINEER';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'project_manager' AND r.code = 'PROJECT_MANAGER';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'admin' AND r.code = 'SYSTEM_ADMINISTRATOR';

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r
WHERE u.username = 'analyst' AND r.code = 'INVESTMENT_ANALYST';