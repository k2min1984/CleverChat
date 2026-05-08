package kr.co.cleverchat.domain.auth.security;

import java.util.Collection;
import kr.co.cleverchat.domain.auth.model.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class AuthenticatedUser extends User {

    private final Long id;
    private final String displayName;

    public AuthenticatedUser(UserAccount account, Collection<? extends GrantedAuthority> authorities) {
        super(account.getUsername(), account.getPasswordHash(), account.isEnabled(), true, true, true, authorities);
        this.id = account.getId();
        this.displayName = account.getDisplayName();
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
