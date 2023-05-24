-- Script run after each migration, so each time the application is started.
-- Ensure queries are runnable multiple times.

INSERT INTO auth.local_users (username, password)
VALUES 
	('testviewer', '{bcrypt}$2a$12$sG0hLLmx3/3Mjts69Y1Lp.B.EXCH51dnUxkRPXQ2RJ8tsrbU4OsqS'),
	('viewer', '{bcrypt}$2a$12$YcGqW.gqhPYfWpILQGoyv.neAZZsvQBbUPTl0yvDiezOGr779jWnC'),
	('editor', '{bcrypt}$2a$12$PLYDm40MTcnyza65qQn3xeiWnj.mopUf2eKcjjAHiqBWc31E13jke'),
	('superuser', '{bcrypt}$2a$12$/Zrv8cXcvruG.eGgCl1yl.QVNsjrmMjOFgj20aKTLvUqypZtJYisC'),
	('admin', '{bcrypt}$2a$12$.dzrMeJ6s2GzFtB.5uUIsenqYhBxAXLBbRE4pYdKEJAMrPwqGhFEi'),
	('special', '{bcrypt}$2a$12$1Zqh2OSxja5tYnD3PysgNeunLI7cYcoNfiLUcIZfoxKqg3ZoXccLy')
	ON CONFLICT DO NOTHING;

WITH test_users AS (
SELECT 
	unnest(ARRAY['testviewer', 'viewer', 'editor', 'superuser', 'admin', 'special']) AS username
)
INSERT INTO auth.users (identity_provider_id, identity_provider_reference)
SELECT
	identity_provider_id,
	username

	FROM auth.identity_providers
	CROSS JOIN auth.local_users
		INNER JOIN test_users USING (username)

	WHERE identity_providers.name = 'local'
	ON CONFLICT DO NOTHING;

WITH test_users AS (
SELECT 
	unnest(ARRAY['testviewer', 'viewer', 'editor', 'superuser', 'admin', 'special']) AS identity_provider_reference,
	unnest(ARRAY['VIEWER', 'VIEWER', 'EDITOR', 'SUPER_USER', 'ADMIN', 'SPECIAL']) AS code
)
INSERT INTO auth.user_roles (user_id, role_id)
SELECT
	user_id,
	role_id

	FROM test_users
		INNER JOIN auth.users USING (identity_provider_reference)
		INNER JOIN auth.identity_providers USING (identity_provider_id)
		INNER JOIN auth.roles USING (code)

	WHERE identity_providers.name = 'local'
	ON CONFLICT DO NOTHING;

WITH test_users AS (
SELECT 
	unnest(ARRAY['testviewer', 'viewer', 'editor', 'superuser', 'admin', 'special']) AS identity_provider_reference,
	unnest(ARRAY['PROVINCIE_OVERIJSSEL', 'PROVINCIE_UTRECHT', 'PROVINCIE_UTRECHT', 'MINISTERIE_LNV', 'MINISTERIE_LNV', 'MINISTERIE_LNV']) AS code
)
INSERT INTO auth.user_competent_authorities (user_id, competent_authority_id)
SELECT
	user_id,
	competent_authority_id

	FROM test_users
		INNER JOIN auth.users USING (identity_provider_reference)
		INNER JOIN auth.identity_providers USING (identity_provider_id)
		INNER JOIN auth.competent_authorities USING (code)
	
	WHERE identity_providers.name = 'local'
	ON CONFLICT DO NOTHING;
