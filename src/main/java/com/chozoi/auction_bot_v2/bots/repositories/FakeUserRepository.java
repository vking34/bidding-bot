package com.chozoi.auction_bot_v2.bots.repositories;

import com.chozoi.auction_bot_v2.bots.models.FakeUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FakeUserRepository extends JpaRepository<FakeUser, Integer> {
}
