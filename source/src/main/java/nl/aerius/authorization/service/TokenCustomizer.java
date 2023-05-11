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

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import nl.aerius.authorization.repository.UserRepository;

/**
 * Token customizer to add information in generated JWT's.
 *
 * As it implements OAuth2TokenCustomizer, it's automatically picked up by Spring authorization server.
 */
@Component
public class TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private final UserRepository repository;

  @Autowired
  public TokenCustomizer(final UserRepository repository) {
    this.repository = repository;
  }

  @Override
  public void customize(final JwtEncodingContext context) {
    if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
      final Authentication authentication = context.getPrincipal();
      context.getClaims().claim("roles", determineRoles(authentication));
      context.getClaims().claim("aerius_authorities", determineAuthorities(authentication));
    }
  }

  private Set<String> determineRoles(final Authentication authentication) {
    final Object principal = authentication.getPrincipal();
    // No clue yet how this'll work for federated users (perhaps using OAuth2User as first check?), so for now only local
    if (principal instanceof User) {
      // Assume local user, and retrieve roles accordingly
      return repository.retrieveUserRoles("local", ((User) principal).getUsername());
    } else {
      return Set.of();
    }
  }

  private Set<String> determineAuthorities(final Authentication authentication) {
    // TODO: actual implementation where authorities are retrieved from database.
    final Set<String> aeriusAuthorities;
    if ("testeditor".equals(authentication.getName())) {
      aeriusAuthorities = Set.of("mocked_authority_code_1", "mocked_authority_code_2");
    } else {
      aeriusAuthorities = Set.of("mocked_authority_code_1");
    }
    return aeriusAuthorities;
  }

}
