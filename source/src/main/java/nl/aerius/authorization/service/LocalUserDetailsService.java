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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import nl.aerius.authorization.generated.db.tables.records.LocalUsersRecord;
import nl.aerius.authorization.repository.UserRepository;

/**
 * Component to retrieve local users (users that are authenticated against our own database).
 *
 * As it implements UserDetailsService, it's automatically picked up by Spring Security.
 */
@Component
public class LocalUserDetailsService implements UserDetailsService {

  private final UserRepository repository;

  @Autowired
  public LocalUserDetailsService(final UserRepository repository) {
    this.repository = repository;
  }

  @Override
  public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
    final LocalUsersRecord dbUser = repository.retrieveLocalUser(username).orElseThrow(() -> new UsernameNotFoundException(username));
    return User
        .withUsername(dbUser.getUsername())
        .password(dbUser.getPassword())
        .authorities(List.of())
        .build();
  }

}
