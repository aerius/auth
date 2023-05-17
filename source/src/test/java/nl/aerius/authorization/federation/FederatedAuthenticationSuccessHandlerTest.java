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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import nl.aerius.authorization.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class FederatedAuthenticationSuccessHandlerTest {

  private static final String CLIENT_REGISTRATION_ID = "OurLocalClientId";
  private static final int MATCHING_IDENTITY_PROVIDER_ID = 99;
  private static final String REFERENCE = "Some-Reference-probably-UUID-ey";
  private static final String FULL_NAME = "First de Last";
  private static final String USERNAME = "SomeUserName";
  private static final String NAME = "Somehow Some Other Name";

  @Mock
  SavedRequestAwareAuthenticationSuccessHandler delegate;
  @Mock
  UserRepository userRepository;
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  OidcUser user;

  @InjectMocks
  FederatedAuthenticationSuccessHandler handler;

  @BeforeEach
  public void beforeEach() {
    lenient().when(user.getSubject()).thenReturn(REFERENCE);
    lenient().when(user.getName()).thenReturn(NAME);
    lenient().when(user.getFullName()).thenReturn(FULL_NAME);
    lenient().when(user.getPreferredUsername()).thenReturn(USERNAME);
  }

  @Test
  void testSuccesfulNewUser() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn(CLIENT_REGISTRATION_ID);

    when(userRepository.retrieveIdentityProviderId(CLIENT_REGISTRATION_ID)).thenReturn(Optional.of(MATCHING_IDENTITY_PROVIDER_ID));
    when(userRepository.retrieveIdentityProviderUserId(CLIENT_REGISTRATION_ID, REFERENCE)).thenReturn(Optional.empty());

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(userRepository).persistNewFederatedUser(MATCHING_IDENTITY_PROVIDER_ID, REFERENCE, FULL_NAME);
    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

  @Test
  void testSuccesfulNewUserWithoutFullName() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn(CLIENT_REGISTRATION_ID);

    when(userRepository.retrieveIdentityProviderId(CLIENT_REGISTRATION_ID)).thenReturn(Optional.of(MATCHING_IDENTITY_PROVIDER_ID));
    when(userRepository.retrieveIdentityProviderUserId(CLIENT_REGISTRATION_ID, REFERENCE)).thenReturn(Optional.empty());

    when(user.getFullName()).thenReturn(null);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(userRepository).persistNewFederatedUser(MATCHING_IDENTITY_PROVIDER_ID, REFERENCE, USERNAME);
    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

  @Test
  void testSuccesfulNewUserWithoutFullNameAndUsername() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn(CLIENT_REGISTRATION_ID);

    when(userRepository.retrieveIdentityProviderId(CLIENT_REGISTRATION_ID)).thenReturn(Optional.of(MATCHING_IDENTITY_PROVIDER_ID));
    when(userRepository.retrieveIdentityProviderUserId(CLIENT_REGISTRATION_ID, REFERENCE)).thenReturn(Optional.empty());

    when(user.getFullName()).thenReturn(null);
    when(user.getPreferredUsername()).thenReturn(null);

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(userRepository).persistNewFederatedUser(MATCHING_IDENTITY_PROVIDER_ID, REFERENCE, NAME);
    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

  @Test
  void testSuccesfulNewUserWithoutAnyNames() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn(CLIENT_REGISTRATION_ID);

    when(userRepository.retrieveIdentityProviderId(CLIENT_REGISTRATION_ID)).thenReturn(Optional.of(MATCHING_IDENTITY_PROVIDER_ID));
    when(userRepository.retrieveIdentityProviderUserId(CLIENT_REGISTRATION_ID, REFERENCE)).thenReturn(Optional.empty());

    when(user.getFullName()).thenReturn(null);
    when(user.getPreferredUsername()).thenReturn(null);
    when(user.getName()).thenReturn(null);

    final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> handler.onAuthenticationSuccess(request, response, authentication));

    assertEquals("user has no recognizable name, which is unexpected", e.getMessage());
    verify(userRepository, never()).persistNewFederatedUser(anyInt(), any(), any());
    verifyNoInteractions(delegate);
  }

  @Test
  void testSuccesfulExistingUser() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn(CLIENT_REGISTRATION_ID);

    when(userRepository.retrieveIdentityProviderId(CLIENT_REGISTRATION_ID)).thenReturn(Optional.of(MATCHING_IDENTITY_PROVIDER_ID));
    when(userRepository.retrieveIdentityProviderUserId(CLIENT_REGISTRATION_ID, REFERENCE)).thenReturn(Optional.of(88));

    handler.onAuthenticationSuccess(request, response, authentication);

    verify(userRepository, never()).persistNewFederatedUser(anyInt(), any(), any());
    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

  @Test
  void testLocalId() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn("local");

    final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> handler.onAuthenticationSuccess(request, response, authentication));

    assertEquals("Local identity provider shouldn't be used like this: local", e.getMessage());

    verifyNoInteractions(userRepository);
    verifyNoInteractions(delegate);
  }

  @Test
  void testUnknownClientId() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authentication.getAuthorizedClientRegistrationId()).thenReturn(CLIENT_REGISTRATION_ID);

    when(userRepository.retrieveIdentityProviderId(CLIENT_REGISTRATION_ID)).thenReturn(Optional.empty());

    final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
        () -> handler.onAuthenticationSuccess(request, response, authentication));

    assertEquals("Identity provider not known in database: " + CLIENT_REGISTRATION_ID, e.getMessage());

    verify(userRepository, never()).persistNewFederatedUser(anyInt(), any(), any());
    verifyNoInteractions(delegate);
  }

  @Test
  void testUnknownAuthentication() throws IOException, ServletException {
    final Authentication authentication = mock(Authentication.class);

    // Shouldn't trigger any exceptions, work as usual
    handler.onAuthenticationSuccess(request, response, authentication);

    verifyNoInteractions(userRepository);
    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

  @Test
  void testUnknownPrincipal() throws IOException, ServletException {
    final OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
    // Currently only supporting Oidc users
    when(authentication.getPrincipal()).thenReturn(mock(OAuth2User.class));

    // Shouldn't trigger any exceptions, work as usual
    handler.onAuthenticationSuccess(request, response, authentication);

    verifyNoInteractions(userRepository);
    verify(delegate).onAuthenticationSuccess(request, response, authentication);
  }

}
