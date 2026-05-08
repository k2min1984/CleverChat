package kr.co.cleverchat.domain.auth.service;

import java.time.OffsetDateTime;
import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import kr.co.cleverchat.domain.auth.security.AuthenticatedUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public DatabaseUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UserAccount account = userMapper.findByUsername(username);
        if (account == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }
        if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new LockedException("계정이 잠겨 있습니다.");
        }

        return new AuthenticatedUser(
            account,
            account.getRoles().stream()
                .filter(role -> role != null && !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList()
        );
    }
}
