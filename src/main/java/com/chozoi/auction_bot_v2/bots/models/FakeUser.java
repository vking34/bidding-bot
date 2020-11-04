package com.chozoi.auction_bot_v2.bots.models;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Where;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(schema = "accounts", name = "user")
@Immutable
@Data
@Where(clause = "role = 'BOT' and state = 'ACTIVE'")
public class FakeUser {
    @Id
    private Integer id;

    private String role;
    private String state;
}
