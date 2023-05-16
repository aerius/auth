/*
 * Copyright the State of the Netherlands
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package nl.aerius.authorization.repository;

import java.util.Optional;
import java.util.Set;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import nl.aerius.authorization.generated.db.Tables;
import nl.aerius.authorization.generated.db.tables.records.LocalUsersRecord;

/**
 * Repository for retrieving user information.
 */
@Repository
public class UserRepository {

  private final DSLContext context;

  @Autowired
  public UserRepository(final DSLContext dslContext) {
    this.context = dslContext;
  }

  public Optional<LocalUsersRecord> retrieveLocalUser(final String username) {
    return this.context.selectFrom(Tables.LOCAL_USERS)
        .where(Tables.LOCAL_USERS.USERNAME.equalIgnoreCase(username))
        .fetchOptional();
  }

  public Set<String> retrieveUserRoles(final String identityProvider, final String userReference) {
    return this.context.select(Tables.ROLES.CODE)
        .from(Tables.ROLES)
        .join(Tables.USER_ROLES).using(Tables.USER_ROLES.ROLE_ID)
        .join(Tables.USERS).using(Tables.USERS.USER_ID)
        .join(Tables.IDENTITY_PROVIDERS).using(Tables.IDENTITY_PROVIDERS.IDENTITY_PROVIDER_ID)
        .where(
            Tables.IDENTITY_PROVIDERS.NAME.eq(identityProvider),
            Tables.USERS.IDENTITY_PROVIDER_REFERENCE.eq(userReference))
        .fetchSet(Tables.ROLES.CODE);
  }

  public Set<String> retrieveCompetentAuthorities(final String identityProvider, final String userReference) {
    return this.context.select(Tables.COMPETENT_AUTHORITIES.CODE)
        .from(Tables.COMPETENT_AUTHORITIES)
        .join(Tables.USER_COMPETENT_AUTHORITIES).using(Tables.USER_COMPETENT_AUTHORITIES.COMPETENT_AUTHORITY_ID)
        .join(Tables.USERS).using(Tables.USERS.USER_ID)
        .join(Tables.IDENTITY_PROVIDERS).using(Tables.IDENTITY_PROVIDERS.IDENTITY_PROVIDER_ID)
        .where(
            Tables.IDENTITY_PROVIDERS.NAME.eq(identityProvider),
            Tables.USERS.IDENTITY_PROVIDER_REFERENCE.eq(userReference))
        .fetchSet(Tables.COMPETENT_AUTHORITIES.CODE);
  }

}
