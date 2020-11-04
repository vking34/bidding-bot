package com.chozoi.auction_bot_v2.users.repositories;

import com.chozoi.auction_bot_v2.users.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
//    public Optional<User> findById(Long id);
}
