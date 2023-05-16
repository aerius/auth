/* Table structure for competent authorities and their link to users */


/*
 * competent_authorities
 * ---------------------
 * Table to contain competent authorities, that can be linked to a user.
 */
CREATE TABLE competent_authorities (
	competent_authority_id serial NOT NULL,
	code text NOT NULL,
	description text NOT NULL,

	CONSTRAINT competent_authorities_pkey PRIMARY KEY (competent_authority_id)
	
);

/* [jooq ignore start] */
ALTER TABLE competent_authorities ADD CONSTRAINT competent_authorities_unique_code UNIQUE (code);
/* [jooq ignore stop] */

/*
 * user_competent_authorities
 * --------------------------
 * Table to contain link between a user and competent authorities.
 */
CREATE TABLE user_competent_authorities (
	user_id integer NOT NULL,
	competent_authority_id integer NOT NULL,

	CONSTRAINT user_competent_authorities_pkey PRIMARY KEY (user_id, competent_authority_id),
	CONSTRAINT user_competent_authorities_fkey_users FOREIGN KEY (user_id) REFERENCES users,
	CONSTRAINT user_competent_authorities_fkey_competent_authorities FOREIGN KEY (competent_authority_id) REFERENCES competent_authorities
);
