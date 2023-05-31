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
package nl.aerius.authorization.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfigurationSource;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import nl.aerius.authorization.federation.FederatedAuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

  @Value("${aerius.authorization.clients.register-api.id:register-client}")
  private String registerApiClientId;

  @Value("${aerius.authorization.clients.register-api.secret:{noop}registerssecret}")
  private String registerApiClientSecret;

  @Value("${aerius.authorization.clients.register-api.redirecturi:http://127.0.0.1:8080/authorized}")
  private final List<String> registerApiRedirectUris = new ArrayList<>();

  @Value("${aerius.authorization.clients.register-ui.id:register-client}")
  private String registerUIClientId;

  @Value("${aerius.authorization.clients.register-ui.redirecturi:http://127.0.0.1:8080/authorized}")
  private final List<String> registerUIRedirectUris = new ArrayList<>();

  @Value("${aerius.authorization.server.issuer:}")
  private String issuer;

  @Value("${aerius.authorization.formlogin:true}")
  private boolean useFormLogin;

  @Value("${aerius.authorization.federation:false}")
  private boolean useFederation;

  @Bean
  @Order(1)
  public SecurityFilterChain authorizationServerSecurityFilterChain(final HttpSecurity http, final CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
    http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
        // Enable OpenID Connect 1.0
        .oidc(Customizer.withDefaults());
    http
        // Ensure cors gets handled properly
        .cors(c -> c.configurationSource(corsConfigurationSource))
        // Redirect to the login page when not authenticated from the authorization endpoint
        .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
        // Accept access tokens for User Info
        .oauth2ResourceServer(c -> c.jwt(Customizer.withDefaults()));

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain defaultSecurityFilterChain(final HttpSecurity http,
      final FederatedAuthenticationSuccessHandler authenticationSuccessHandler,
      final CorsConfigurationSource corsConfigurationSource)
      throws Exception {
    http
        // Ensure cors gets handled properly
        .cors(c -> c.configurationSource(corsConfigurationSource))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
    if (useFormLogin) {
      // Form login handles the redirect to the login page from the authorization server filter chain
      http.formLogin(Customizer.withDefaults());
    }
    if (useFederation) {
      http.oauth2Login()
          .successHandler(authenticationSuccessHandler);
    }

    return http.build();
  }

  @Bean
  public RegisteredClientRepository registeredClientRepository() {
    final RegisteredClient apiClient = getRegisterApiClient();
    final RegisteredClient uiClient = getRegisterUIClient();

    return new InMemoryRegisteredClientRepository(apiClient, uiClient);
  }

  private RegisteredClient getRegisterUIClient() {
    final RegisteredClient.Builder registerUIClientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(registerUIClientId)
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .clientSettings(ClientSettings.builder()
            .requireAuthorizationConsent(false)
            .requireProofKey(true)
            .build());

    for (final String redirectUri : registerUIRedirectUris) {
      registerUIClientBuilder.redirectUri(redirectUri);
    }

    return registerUIClientBuilder.build();
  }

  private RegisteredClient getRegisterApiClient() {
    // TODO: configure to what register actually needs
    final RegisteredClient.Builder registerApiClientBuilder = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId(registerApiClientId)
        .clientSecret(registerApiClientSecret)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .clientSettings(ClientSettings.builder().build());

    for (final String redirectUri : registerApiRedirectUris) {
      registerApiClientBuilder.redirectUri(redirectUri);
    }

    return registerApiClientBuilder.build();
  }

  @Bean
  public JWKSource<SecurityContext> jwkSource() {
    final KeyPair keyPair = generateRsaKey();
    final RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    final RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
    final RSAKey rsaKey = new RSAKey.Builder(publicKey)
        .privateKey(privateKey)
        .keyID(UUID.randomUUID().toString())
        .build();
    final JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  private static KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (final Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }

  @Bean
  public JwtDecoder jwtDecoder(final JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }

  @Bean
  public AuthorizationServerSettings authorizationServerSettings() {
    final AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
    if (issuer != null && !issuer.isEmpty()) {
      builder.issuer(issuer);
    }
    return builder.build();
  }

  @Bean
  public SavedRequestAwareAuthenticationSuccessHandler savedRequestAwareAuthenticationSuccessHandler() {
    return new SavedRequestAwareAuthenticationSuccessHandler();
  }

}
