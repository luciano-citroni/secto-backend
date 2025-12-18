package com.bridge.secto.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class CompanyCreditRequest {
    private UUID company;
    private BigDecimal creditAmount;
}
