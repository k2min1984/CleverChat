package kr.co.cleverchat.domain.auth.service;

import kr.co.cleverchat.domain.auth.mapper.UserMapper;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InitialAdminSeeder implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public InitialAdminSeeder(
        UserMapper userMapper,
        PasswordEncoder passwordEncoder,
        @Value("${cleverchat.admin.seed.username:}") String username,
        @Value("${cleverchat.admin.seed.password:}") String password,
        @Value("${cleverchat.admin.seed.display-name:Initial Admin}") String displayName
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || userMapper.countByUsername(username) > 0) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(displayName);
        admin.setEnabled(true);
        userMapper.insertUser(admin);
        userMapper.insertUserRole(admin.getId(), "ADMIN");
    }
}
