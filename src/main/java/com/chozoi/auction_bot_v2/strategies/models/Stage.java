package com.chozoi.auction_bot_v2.strategies.models;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("chozoi.auction_bot_config")
@Data
@ToString
public class Stage {
    @Id
    private String ID;

    private Integer id;

    private String state;

    @Field("proportion_time")
    private Float proportionTime;

    private Integer sort;

    @Field("stage_name")
    private String stageName;

    @Field("price_target")
    private Float priceTarget;

    public Stage() {}

    public Stage(
            Integer id,
            String state,
            Float proportionTime,
            Integer sort,
            String stageName,
            Float priceTarget) {

        this.id = id;
        this.state = state;
        this.proportionTime = proportionTime;
        this.sort = sort;
        this.stageName = stageName;
        this.priceTarget = priceTarget;
    }

    public Stage(Stage stage) {
        ID = stage.ID;
        id = stage.id;
        state = stage.state;
        proportionTime = stage.proportionTime;
        sort = stage.sort;
        stageName = stage.stageName;
        priceTarget = stage.priceTarget;
    }
}
