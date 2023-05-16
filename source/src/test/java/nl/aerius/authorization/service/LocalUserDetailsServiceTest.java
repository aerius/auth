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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import nl.aerius.authorization.generated.db.tables.records.LocalUsersRecord;
import nl.aerius.authorization.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class LocalUserDetailsServiceTest {

  private static final String USERNAME = "SomeUserName";
  private static final String PASSWORD = "someSecret";

  @Mock
  UserRepository userRepository;

  @InjectMocks
  LocalUserDetailsService localUserDetailsService;

  @Test
  void testValid() {
    final LocalUsersRecord record = mock(LocalUsersRecord.class);
    when(record.getUsername()).thenReturn(USERNAME);
    when(record.getPassword()).thenReturn(PASSWORD);
    when(record.getEnabled()).thenReturn(true);
    when(userRepository.retrieveLocalUser(USERNAME))
        .thenReturn(Optional.of(record));

    final UserDetails details = localUserDetailsService.loadUserByUsername(USERNAME);

    assertNotNull(details, "Object should be returned");
    assertEquals(USERNAME, details.getUsername(), "Username should match");
    assertEquals(PASSWORD, details.getPassword(), "Password should match");
    assertTrue(details.isEnabled(), "user should be enabled");
  }

  @Test
  void testNotFound() {
    when(userRepository.retrieveLocalUser(USERNAME))
        .thenReturn(Optional.empty());

    final UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
        () -> localUserDetailsService.loadUserByUsername(USERNAME));

    assertEquals(USERNAME, exception.getMessage(), "Username should be used as exception message");
  }

  @Test
  void testDisabled() {
    final LocalUsersRecord record = mock(LocalUsersRecord.class);
    when(record.getUsername()).thenReturn(USERNAME);
    when(record.getPassword()).thenReturn(PASSWORD);
    when(record.getEnabled()).thenReturn(false);
    when(userRepository.retrieveLocalUser(USERNAME))
        .thenReturn(Optional.of(record));

    final UserDetails details = localUserDetailsService.loadUserByUsername(USERNAME);

    assertNotNull(details, "Object should be returned");
    assertEquals(USERNAME, details.getUsername(), "Username should match");
    assertEquals(PASSWORD, details.getPassword(), "Password should match");
    assertFalse(details.isEnabled(), "user should be disabled");
  }

}
