package kr.co.cleverchat.domain.auth.mapper;

import kr.co.cleverchat.domain.auth.model.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    UserAccount findByUsername(@Param("username") String username);

    int countByUsername(@Param("username") String username);

    void insertUser(UserAccount user);

    void insertUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    void resetLoginSuccess(@Param("username") String username);

    void increaseFailedAttempt(@Param("username") String username);

    void lockUser(@Param("username") String username, @Param("lockMinutes") int lockMinutes);
}
