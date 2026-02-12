package com.bridge.secto.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StripeProductDto {
    private String productId;
    private String priceId;
    private String name;
    private String description;
    private Long unitAmount;       // price in cents
    private String currency;
    private String type;           // "recurring" or "one_time"
    private String interval;       // "month", "year", etc. (null for one_time)
    private Integer credits;       // from product metadata "credits"
}
