package org.golftripbooker.data;

import org.golftripbooker.models.User;

public interface UserRepository {

    User findById(int userId) throws DataAccessException;

    User findByEmail(String email) throws DataAccessException;

    User findByUsername(String username) throws DataAccessException;

    /** Always inserts a CLIENT. There is no repository method that promotes anyone to HOST. */
    User create(User user) throws DataAccessException;
}
