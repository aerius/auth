-- Script run after each migration, so each time the application is started.
-- Ensure queries are runnable multiple times.

INSERT INTO auth.local_users (username, password)
	VALUES ('testviewer', '{bcrypt}$2a$12$sG0hLLmx3/3Mjts69Y1Lp.B.EXCH51dnUxkRPXQ2RJ8tsrbU4OsqS')
	ON CONFLICT DO NOTHING;

INSERT INTO auth.users (identity_provider_id, identity_provider_reference)
SELECT
	identity_provider_id,
	username

	FROM auth.identity_providers
	CROSS JOIN auth.local_users

	WHERE identity_providers.name = 'local'
		AND local_users.username = 'testviewer'
	ON CONFLICT DO NOTHING;

INSERT INTO auth.user_roles (user_id, role_id)
SELECT
	user_id,
	role_id

	FROM auth.users
		INNER JOIN auth.identity_providers USING (identity_provider_id)
	CROSS JOIN auth.roles

	WHERE identity_providers.name = 'local'
		AND users.identity_provider_reference = 'testviewer'
		AND roles.code = 'VIEWER'
	ON CONFLICT DO NOTHING;

INSERT INTO auth.user_competent_authorities (user_id, competent_authority_id)
SELECT
	user_id,
	competent_authority_id

	FROM auth.users
		INNER JOIN auth.identity_providers USING (identity_provider_id)
	CROSS JOIN auth.competent_authorities
	
	WHERE identity_providers.name = 'local'
		AND users.identity_provider_reference = 'testviewer'
		AND competent_authorities.code = 'PROVINCIE_OVERIJSSEL'
	ON CONFLICT DO NOTHING;
