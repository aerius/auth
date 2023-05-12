/* Table structure for users and roles */


/*
 * identity_providers
 * ------------------
 * Table to contain identity providers, that can handle authentication of an end user.
 */
CREATE TABLE identity_providers (
	identity_provider_id serial NOT NULL,
	name text NOT NULL,

	CONSTRAINT identity_providers_pkey PRIMARY KEY (identity_provider_id)
	
);

/* [jooq ignore start] */
ALTER TABLE identity_providers ADD CONSTRAINT identity_providers_unique_name UNIQUE (name);
/* [jooq ignore stop] */

/*
 * local_users
 * -----------
 * Table to contain local users. These are users that can login (authenticate) to authorization server itself.
 */
CREATE TABLE local_users (
	local_user_id serial NOT NULL,
	username text NOT NULL,
	password text NOT NULL,
	enabled boolean NOT NULL DEFAULT true,

	CONSTRAINT local_users_pkey PRIMARY KEY (local_user_id)
);

-- Ensure case insensitive uniqueness is used.
/* [jooq ignore start] */
CREATE UNIQUE INDEX local_users_idx_username_unique on local_users (LOWER(username));
/* [jooq ignore stop] */

/*
 * users
 * -----
 * Table to contain users, representing authenticated users.
 */
CREATE TABLE users (
	user_id serial NOT NULL,
	identity_provider_id integer NOT NULL,
	identity_provider_reference text NOT NULL,

	CONSTRAINT users_pkey PRIMARY KEY (user_id),
	CONSTRAINT users_fkey_identity_providers FOREIGN KEY (identity_provider_id) REFERENCES identity_providers
);

/* [jooq ignore start] */
ALTER TABLE users ADD CONSTRAINT users_unique_identity_provider_reference UNIQUE (identity_provider_id, identity_provider_reference);
/* [jooq ignore stop] */

/*
 * roles
 * -----
 * Table to contain roles.
 */
CREATE TABLE roles (
	role_id serial NOT NULL,
	code text NOT NULL,
	description text NOT NULL,

	CONSTRAINT roles_pkey PRIMARY KEY (role_id)
);

/* [jooq ignore start] */
ALTER TABLE roles ADD CONSTRAINT roles_unique_code UNIQUE (code);
/* [jooq ignore stop] */

/*
 * user_roles
 * ----------
 * Table to contain link between a user and roles.
 */
CREATE TABLE user_roles (
	user_id integer NOT NULL,
	role_id integer NOT NULL,

	CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
	CONSTRAINT user_roles_fkey_users FOREIGN KEY (user_id) REFERENCES users,
	CONSTRAINT user_roles_fkey_roles FOREIGN KEY (role_id) REFERENCES roles
);
