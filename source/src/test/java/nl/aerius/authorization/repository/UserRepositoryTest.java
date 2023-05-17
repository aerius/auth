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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import nl.aerius.authorization.generated.db.Tables;
import nl.aerius.authorization.generated.db.tables.records.LocalUsersRecord;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

  @Test
  void testRetrieveLocalUser() {
    final MockDataProvider provider = (ctx) -> {
      final String sql = ctx.sql();
      assertEquals("""
          select \
          "auth"."local_users"."local_user_id", \
          "auth"."local_users"."username", \
          "auth"."local_users"."password", \
          "auth"."local_users"."enabled" \
          from "auth"."local_users" \
          where lower("auth"."local_users"."username") = lower(?)""", sql, "Expected SQL");
      // Mock 1 match
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      final LocalUsersRecord record = create.newRecord(Tables.LOCAL_USERS)
          .values(1, "SomeName", "SomePassword", true);
      return new MockResult[] {new MockResult(record)};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Optional<LocalUsersRecord> result = userRepository.retrieveLocalUser("SomeName");

    assertTrue(result.isPresent(), "Should be a result");
  }

  @Test
  void testRetrieveLocalUserNotFound() {
    final MockDataProvider provider = (ctx) -> {
      // Mock 0 rows
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      return new MockResult[] {new MockResult(0, create.newResult(Tables.LOCAL_USERS))};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Optional<LocalUsersRecord> result = userRepository.retrieveLocalUser("SomeName");

    assertFalse(result.isPresent(), "Shouldn't be a result");
  }

  @Test
  void testRetrieveIdentityProviderId() {
    final MockDataProvider provider = (ctx) -> {
      final String sql = ctx.sql();
      assertEquals("""
          select \
          "auth"."identity_providers"."identity_provider_id" \
          from "auth"."identity_providers" \
          where lower("auth"."identity_providers"."name") = lower(?)""", sql, "Expected SQL");
      // Mock 1 match
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      final Record1<Integer> record = create.newRecord(Tables.IDENTITY_PROVIDERS.IDENTITY_PROVIDER_ID)
          .values(1);
      return new MockResult[] {new MockResult(record)};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Optional<Integer> result = userRepository.retrieveIdentityProviderId("SomeName");

    assertTrue(result.isPresent(), "Identity provider should exist");
    assertEquals(1, result.get(), "Identity provider ID should match what is returned");
  }

  @Test
  void testRetrieveIdentityProviderIdNotExisting() {
    final MockDataProvider provider = (ctx) -> {
      // Mock no match
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      return new MockResult[] {new MockResult(0, create.newResult(Tables.IDENTITY_PROVIDERS.IDENTITY_PROVIDER_ID))};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Optional<Integer> result = userRepository.retrieveIdentityProviderId("SomeName");

    assertFalse(result.isPresent(), "Identity provider shouldn't exist");
  }

  @Test
  void testRetrieveIdentityProviderUserId() {
    final MockDataProvider provider = (ctx) -> {
      final String sql = ctx.sql();
      assertEquals("""
          select \
          "auth"."users"."user_id" \
          from "auth"."users" \
          join "auth"."identity_providers" using ("identity_provider_id") \
          where (lower("auth"."identity_providers"."name") = lower(?) \
          and "auth"."users"."identity_provider_reference" = ?)""", sql, "Expected SQL");
      // Mock 1 match
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      final Record1<Integer> record = create.newRecord(Tables.USERS.USER_ID)
          .values(1);
      return new MockResult[] {new MockResult(record)};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Optional<Integer> result = userRepository.retrieveIdentityProviderUserId("SomeIdentityId", "SomeUserReference");

    assertTrue(result.isPresent(), "Identity provider user should exist");
    assertEquals(1, result.get(), "Identity provider user ID should match what is returned");
  }

  @Test
  void testRetrieveIdentityProviderUserIdNotExisting() {
    final MockDataProvider provider = (ctx) -> {
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      return new MockResult[] {new MockResult(0, create.newResult(Tables.USERS.USER_ID))};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Optional<Integer> result = userRepository.retrieveIdentityProviderUserId("SomeIdentityId", "SomeUserReference");

    assertFalse(result.isPresent(), "Identity provider user shouldn't exist");
  }

  @Test
  void testPersistNewFederatedUser() {
    final MockDataProvider provider = (ctx) -> {
      final String sql = ctx.sql();
      assertEquals("""
          insert into "auth"."users" \
          ("identity_provider_id", "identity_provider_reference", "name") \
          values \
          (?, ?, ?)""", sql, "Expected SQL");
      // Mock 1 match
      return new MockResult[] {new MockResult(1)};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final int recordsCreated = userRepository.persistNewFederatedUser(1, "SomeUserReference", "SomeUserName");

    assertEquals(1, recordsCreated, "Records created");
  }

  @Test
  void testRetrieveUserRoles() {
    final MockDataProvider provider = (ctx) -> {
      final String sql = ctx.sql();
      assertEquals("""
          select "auth"."roles"."code" \
          from "auth"."roles" \
          join "auth"."user_roles" using ("role_id") \
          join "auth"."users" using ("user_id") \
          join "auth"."identity_providers" \
          using ("identity_provider_id") \
          where ("auth"."identity_providers"."name" = ? \
          and "auth"."users"."identity_provider_reference" = ?)""", sql, "Expected SQL");
      // Mock 2 matches
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      final Result<Record1<String>> result = create.newResult(Tables.ROLES.CODE);
      result.add(create.newRecord(Tables.ROLES.CODE).values("Role1"));
      result.add(create.newRecord(Tables.ROLES.CODE).values("Role2"));
      return new MockResult[] {new MockResult(2, result)};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Set<String> result = userRepository.retrieveUserRoles("SomeId", "SomeName");

    assertEquals(result, Set.of("Role1", "Role2"), "Roles should be retrieved");
  }

  @Test
  void testRetrieveUserRolesNonFound() {
    final MockDataProvider provider = (ctx) -> {
      // Mock no match
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      return new MockResult[] {new MockResult(0, create.newResult(Tables.ROLES.CODE))};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Set<String> result = userRepository.retrieveUserRoles("SomeId", "SomeName");

    assertEquals(result, Set.of(), "Roles should be retrieved");
  }

  @Test
  void testRetrieveCompetentAuthorities() {
    final MockDataProvider provider = (ctx) -> {
      final String sql = ctx.sql();
      assertEquals("""
          select "auth"."competent_authorities"."code" \
          from "auth"."competent_authorities" \
          join "auth"."user_competent_authorities" using ("competent_authority_id") \
          join "auth"."users" using ("user_id") \
          join "auth"."identity_providers" \
          using ("identity_provider_id") \
          where ("auth"."identity_providers"."name" = ? \
          and "auth"."users"."identity_provider_reference" = ?)""", sql, "Expected SQL");
      // Mock 2 matches
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      final Result<Record1<String>> result = create.newResult(Tables.COMPETENT_AUTHORITIES.CODE);
      result.add(create.newRecord(Tables.COMPETENT_AUTHORITIES.CODE).values("Province"));
      result.add(create.newRecord(Tables.COMPETENT_AUTHORITIES.CODE).values("Ministery"));
      return new MockResult[] {new MockResult(2, result)};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Set<String> result = userRepository.retrieveCompetentAuthorities("SomeId", "SomeName");

    assertEquals(result, Set.of("Province", "Ministery"), "Authorities should be returned");
  }

  @Test
  void testRetrieveCompetentAuthoritiesNonFound() {
    final MockDataProvider provider = (ctx) -> {
      // Mock no match
      final DSLContext create = DSL.using(SQLDialect.POSTGRES);
      return new MockResult[] {new MockResult(0, create.newResult(Tables.COMPETENT_AUTHORITIES.CODE))};
    };
    final MockConnection connection = new MockConnection(provider);
    final DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);
    final UserRepository userRepository = new UserRepository(context);

    final Set<String> result = userRepository.retrieveCompetentAuthorities("SomeId", "SomeName");

    assertEquals(result, Set.of(), "Authorities should be returned");
  }

}
