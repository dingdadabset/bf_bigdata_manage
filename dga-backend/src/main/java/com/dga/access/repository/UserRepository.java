package com.dga.access.repository;

import com.dga.access.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByOpenId(String openId);
    boolean existsByUsername(String username);
}
