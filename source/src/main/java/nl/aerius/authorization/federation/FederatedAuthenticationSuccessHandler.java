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
package nl.aerius.authorization.federation;

import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import nl.aerius.authorization.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Component to automatically add new federated users to our database if they were not yet known yet.
 *
 * This should help linking new users to roles and such.
 * It requires the user to login once through their identity provider, after which an admin can configure the proper authorization.
 */
@Component
public class FederatedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final AuthenticationSuccessHandler delegate;

  private final UserRepository userRepository;

  @Autowired
  public FederatedAuthenticationSuccessHandler(final UserRepository userRepository, final SavedRequestAwareAuthenticationSuccessHandler delegate) {
    this.userRepository = userRepository;
    this.delegate = delegate;
  }

  @Override
  public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication)
      throws IOException, ServletException {
    if (authentication instanceof final OAuth2AuthenticationToken token && authentication.getPrincipal() instanceof final OidcUser user) {
      handleOidcAuthenticatedUser(token, user);
    }

    this.delegate.onAuthenticationSuccess(request, response, authentication);
  }

  private void handleOidcAuthenticatedUser(final OAuth2AuthenticationToken token, final OidcUser user) {
    final String clientRegistrationId = token.getAuthorizedClientRegistrationId();
    if ("local".equalsIgnoreCase(clientRegistrationId)) {
      throw new IllegalArgumentException("Local identity provider shouldn't be used like this: " + clientRegistrationId);
    }
    final Optional<Integer> identityProviderId = userRepository.retrieveIdentityProviderId(clientRegistrationId);
    if (identityProviderId.isEmpty()) {
      throw new IllegalArgumentException("Identity provider not known in database: " + clientRegistrationId);
    }
    if (userRepository.retrieveIdentityProviderUserId(clientRegistrationId, user.getSubject()).isEmpty()) {
      // Add it as a new user
      userRepository.persistNewFederatedUser(identityProviderId.get(), user.getSubject(), toRecognizableName(user));
    }
  }

  private String toRecognizableName(final OidcUser user) {
    final Optional<OidcUser> optUser = Optional.of(user);
    // By documentation the getName method should never return null.
    // However, during testing it returned same as subject, which is some uuid.
    // Hence prefer the fullname, and if that's not available try the preferred username.
    return optUser.map(OidcUser::getFullName)
        .or(() -> optUser.map(OidcUser::getPreferredUsername))
        .or(() -> optUser.map(OidcUser::getName))
        .orElseThrow(() -> new IllegalArgumentException("user has no recognizable name, which is unexpected"));
  }

}
