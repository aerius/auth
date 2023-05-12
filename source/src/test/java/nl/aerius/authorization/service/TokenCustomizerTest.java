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
package nl.aerius.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import nl.aerius.authorization.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TokenCustomizerTest {

  private static final String USERNAME = "SomeUserName";
  private static final String ROLE = "SomeActor";
  private static final String COMPETENT_AUTHORITY = "SomethingCompetent";

  @Mock
  UserRepository userRepository;

  @InjectMocks
  TokenCustomizer tokenCustomizer;

  @Test
  void testCustomize() {
    final User user = mock(User.class);
    when(user.getUsername()).thenReturn(USERNAME);
    final Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(user);

    final JwtClaimsSet.Builder builder = JwtClaimsSet.builder();
    builder.claim("Nonsense", "Builder needs at least 1 claim");
    final JwtEncodingContext context = JwtEncodingContext
        .with(
            JwsHeader.with(SignatureAlgorithm.RS256),
            builder)
        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
        .principal(authentication)
        .build();

    when(userRepository.retrieveUserRoles("local", USERNAME)).thenReturn(Set.of(ROLE));
    when(userRepository.retrieveCompetentAuthorities("local", USERNAME)).thenReturn(Set.of(COMPETENT_AUTHORITY));

    tokenCustomizer.customize(context);

    final JwtClaimsSet claimSet = builder.build();
    assertTrue(claimSet.getClaims().containsKey("roles"), "Roles should be present");
    assertTrue(claimSet.getClaims().containsKey("aerius_authorities"), "Authorities should be present");
    assertEquals(Set.of(ROLE), claimSet.getClaim("roles"), "roles should be obtained from repository");
    assertEquals(Set.of(COMPETENT_AUTHORITY), claimSet.getClaim("aerius_authorities"), "authorities should be obtained from repository");
  }

  @Test
  void testCustomizeDifferentUserObject() {
    final Object user = mock(Object.class);
    final Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(user);

    final JwtClaimsSet.Builder builder = JwtClaimsSet.builder();
    builder.claim("Nonsense", "Builder needs at least 1 claim");
    final JwtEncodingContext context = JwtEncodingContext
        .with(
            JwsHeader.with(SignatureAlgorithm.RS256),
            builder)
        .tokenType(OAuth2TokenType.ACCESS_TOKEN)
        .principal(authentication)
        .build();

    tokenCustomizer.customize(context);

    final JwtClaimsSet claimSet = builder.build();
    assertTrue(claimSet.getClaims().containsKey("roles"), "Roles should be present");
    assertTrue(claimSet.getClaims().containsKey("aerius_authorities"), "Authorities should be present");
    assertEquals(Set.of(), claimSet.getClaim("roles"), "Roles should be empty");
    assertEquals(Set.of(), claimSet.getClaim("aerius_authorities"), "Authorities should be empty");

    verifyNoInteractions(userRepository);
  }

  @Test
  void testCustomizeOtherTokenType() {
    final Authentication authentication = mock(Authentication.class);

    final JwtClaimsSet.Builder builder = JwtClaimsSet.builder();
    builder.claim("Nonsense", "Builder needs at least 1 claim");
    final JwtEncodingContext context = JwtEncodingContext
        .with(
            JwsHeader.with(SignatureAlgorithm.RS256),
            builder)
        .tokenType(new OAuth2TokenType("SomeOtherToken"))
        .principal(authentication)
        .build();

    tokenCustomizer.customize(context);

    final JwtClaimsSet claimSet = builder.build();
    assertFalse(claimSet.getClaims().containsKey("roles"), "Roles shouldn't be present");
    assertFalse(claimSet.getClaims().containsKey("aerius_authorities"), "Authoritiesn't should be present");

    verifyNoInteractions(userRepository);
  }

}
