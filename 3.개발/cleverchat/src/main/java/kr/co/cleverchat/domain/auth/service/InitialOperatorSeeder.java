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
public class InitialOperatorSeeder implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;
    private final boolean enabled;

    public InitialOperatorSeeder(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            @Value("${cleverchat.operator.seed.username:}") String username,
            @Value("${cleverchat.operator.seed.password:}") String password,
            @Value("${cleverchat.operator.seed.display-name:Initial Operator}") String displayName,
            @Value("${cleverchat.operator.seed.enabled:false}") boolean enabled) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() || password.isBlank() || userMapper.countByUsername(username) > 0) {
            return;
        }

        UserAccount operator = new UserAccount();
        operator.setUsername(username);
        operator.setPasswordHash(passwordEncoder.encode(password));
        operator.setDisplayName(displayName);
        operator.setEnabled(enabled);
        operator.setMustChangePassword(true);
        userMapper.insertUser(operator);
        userMapper.insertUserRole(operator.getId(), "OPERATOR");
    }
}
