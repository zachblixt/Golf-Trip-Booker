package org.golftripbooker.data;

import org.golftripbooker.data.mappers.UserMapper;
import org.golftripbooker.models.Role;
import org.golftripbooker.models.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcClientRepository implements UserRepository {

    private static final String BASE_SELECT =
            "select user_id, email, username, password, role from app_user";

    private final JdbcClient jdbcClient;

    public UserJdbcClientRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public User findById(int userId) throws DataAccessException {
        final String sql = BASE_SELECT + " where user_id = ?;";
        return jdbcClient.sql(sql)
                .param(userId)
                .query(new UserMapper())
                .optional().orElse(null);
    }

    @Override
    public User findByEmail(String email) throws DataAccessException {
        final String sql = BASE_SELECT + " where email = ?;";
        return jdbcClient.sql(sql)
                .param(email)
                .query(new UserMapper())
                .optional().orElse(null);
    }

    @Override
    public User findByUsername(String username) throws DataAccessException {
        final String sql = BASE_SELECT + " where username = ?;";
        return jdbcClient.sql(sql)
                .param(username)
                .query(new UserMapper())
                .optional().orElse(null);
    }

    /**
     * The role is hard-coded to CLIENT rather than read off the argument. Even if a
     * service handed this method a User claiming to be a HOST, the row would be a
     * CLIENT. Promotion happens in the database by hand, on purpose.
     */
    @Override
    public User create(User user) throws DataAccessException {
        final String sql = """
                insert into app_user (email, username, password, role)
                values (:email, :username, :password, 'CLIENT');
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rowsAffected = jdbcClient.sql(sql)
                .param("email", user.getEmail())
                .param("username", user.getUsername())
                .param("password", user.getPassword())
                .update(keyHolder, "user_id");

        if (rowsAffected == 0) {
            return null;
        }

        user.setUserId(keyHolder.getKey().intValue());
        user.setRole(Role.CLIENT);
        return user;
    }
}
