package com.ipca.dms_api.security;

import java.time.LocalDateTime;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.ipca.dms_api.entity.User;
import com.ipca.dms_api.repository.UserRepository;

@Component
public class CustomLdapAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserRepository userRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userid = authentication.getName();
        String password = authentication.getCredentials().toString();

        User user = userRepository.findByUserId(userid)
                .orElseThrow(() -> new BadCredentialsException("USER_NOT_REGISTERED"));
        // --- Special case: Admin user (DB password only, no lockout) ---
        if ("admin".equalsIgnoreCase(userid)) { // replace with your actual admin ID
            if (password.equals(user.getPassword())) { // recommend BCrypt check in production
                return new UsernamePasswordAuthenticationToken(
                        userid,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
            } else {
                throw new BadCredentialsException("INVALID_ADMIN_CREDENTIALS");
            }
        }

        // Check account status
        if (!"Active".equalsIgnoreCase(user.getAccountStatus())) {
            throw new BadCredentialsException("INACTIVE_USER");
        }

        if ("Locked".equalsIgnoreCase(user.getLoginStatus())) {
            throw new BadCredentialsException("ACCOUNT_LOCKED");
        }

        // --- LDAP authentication check
        if (LdapUtils.ldapCheck(userid, password) != null) {
            // Successful login
            user.setBadLoginCount(0);
            user.setLoginStatus("Unlocked");
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            return new UsernamePasswordAuthenticationToken(
                    userid,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        } else {
            // Failed login
            int badLoginCount = (user.getBadLoginCount() == null ? 0 : user.getBadLoginCount()) + 1;
            user.setBadLoginCount(badLoginCount);

            if (badLoginCount >= 5) {
                user.setLoginStatus("Locked");
                userRepository.save(user);
                throw new BadCredentialsException("LOGIN_FAILED"); // code
            } else {
                userRepository.save(user);
                throw new BadCredentialsException("BAD_CREDENTIALS:" + badLoginCount); // code with attempt
            }
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
