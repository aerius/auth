/* [jooq ignore start] */
/*
 * ae_link_local_user_authority
 * ----------------------------
 * Function to link a local user to a competent authority, probably temporary.
 * Will throw errors if user or authority doesn't exist, or if the link is already there.
 */
CREATE OR REPLACE FUNCTION ae_link_local_user_authority(v_username text, v_authority_code text)
	RETURNS void AS
$BODY$
DECLARE
	v_correct_user boolean;
	v_correct_authority boolean;
BEGIN
	-- Check if user actually exist, to prevent typos
	SELECT EXISTS(
		SELECT 1 FROM auth.users 
			INNER JOIN auth.identity_providers USING (identity_provider_id) 
		WHERE identity_providers.name = 'local'
			AND users.identity_provider_reference = v_username) INTO v_correct_user;
	IF NOT v_correct_user THEN
		RAISE EXCEPTION 'Incorrect user %, be sure to use a local user.', v_username;
	END IF;	

	-- Check if authority actually exist, to prevent typos
	SELECT EXISTS(SELECT 1 FROM auth.competent_authorities WHERE code = v_authority_code) INTO v_correct_authority;
	IF NOT v_correct_authority THEN
		RAISE EXCEPTION 'Incorrect authority %, be sure to use one available in the authorities table.', v_authority_code;
	END IF;
	
	INSERT INTO auth.user_competent_authorities (user_id, competent_authority_id)
	SELECT
		user_id,
		competent_authority_id
	
		FROM auth.users
			INNER JOIN auth.identity_providers USING (identity_provider_id)
		CROSS JOIN auth.competent_authorities
		
		WHERE identity_providers.name = 'local'
			AND users.identity_provider_reference = v_username
			AND competent_authorities.code = v_authority_code;
END;
$BODY$
LANGUAGE plpgsql VOLATILE;
/* [jooq ignore stop] */
