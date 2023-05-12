/* [jooq ignore start] */
/*
 * ae_create_local_user
 * --------------------
 * Function to create a local user, probably temporary.
 * Will create the user with 1 role, as is currently required.
 * 
 * Ensure the password matches a bcrypted password, for instance using https://bcrypt-generator.com/.
 * It should look something like '$2a$12$HuEccui1qRGJAjgDbPGYOe/KTQKptFY2hlyJkHZu15aLXI8/31mma'
 */
CREATE OR REPLACE FUNCTION ae_create_local_user(v_username text, v_bcrypt_password text, v_role text)
	RETURNS void AS
$BODY$
DECLARE
	v_password text;
	v_correct_roles integer;
BEGIN
	-- Prefix the bcrypted password with the tag required by the application to recognize the method.
	v_password = '{bcrypt}' || v_bcrypt_password;
	
	-- Check if roles actually exist, to prevent creating a user without a role (though technically that should be OK as well)
	SELECT count(*) INTO v_correct_roles FROM auth.roles WHERE code = v_role;
	-- 
	IF v_correct_roles != 1 THEN
		RAISE EXCEPTION 'Incorrect role %, be sure to use one available in the roles table.', v_role;
	END IF;
	
	INSERT INTO auth.local_users (username, password)
	VALUES (v_username, v_password);

	INSERT INTO auth.users (identity_provider_id, identity_provider_reference)
	SELECT
		identity_provider_id,
		username

		FROM auth.identity_providers
		CROSS JOIN auth.local_users

		WHERE identity_providers.name = 'local'
			AND local_users.username = v_username;

	INSERT INTO auth.user_roles (user_id, role_id)
	SELECT
		user_id,
		role_id

		FROM auth.users
			INNER JOIN auth.identity_providers USING (identity_provider_id)
		CROSS JOIN auth.roles

		WHERE identity_providers.name = 'local'
			AND users.identity_provider_reference = v_username
			AND roles.code = v_role;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;
/* [jooq ignore stop] */
