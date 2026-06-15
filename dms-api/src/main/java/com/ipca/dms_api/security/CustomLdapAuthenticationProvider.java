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
        // We now check DB password for all users per Oracle migration instructions.
        boolean isPasswordValid = false;
        
        // 1. DB Password check
        if (password != null && password.equals(user.getPassword())) {
            isPasswordValid = true;
        }

        // Check account status
        if (!"Active".equalsIgnoreCase(user.getAccountStatus())) {
            throw new BadCredentialsException("INACTIVE_USER");
        }

        if ("Locked".equalsIgnoreCase(user.getLoginStatus())) {
            throw new BadCredentialsException("ACCOUNT_LOCKED");
        }

        // 2. Fallback to LDAP if DB password didn't match (for legacy support if needed)
        if (!isPasswordValid) {
            if (LdapUtils.ldapCheck(userid, password) != null) {
                isPasswordValid = true;
            }
        }

        if (isPasswordValid) {
            // Successful login
            user.setBadLoginCount(0);
            user.setLoginStatus("Unlocked");
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Grant ROLE_ADMIN for admin, else ROLE_USER
            String role = "admin".equalsIgnoreCase(userid) ? "ROLE_ADMIN" : "ROLE_USER";

            return new UsernamePasswordAuthenticationToken(
                    userid,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(role)));
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
