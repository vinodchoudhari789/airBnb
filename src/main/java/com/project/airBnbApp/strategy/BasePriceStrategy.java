package com.project.airBnbApp.strategy;

import com.project.airBnbApp.entity.Inventory;

import java.math.BigDecimal;

public class BasePriceStrategy implements PricingStrategy{
    @Override
    public BigDecimal calculatePrice(Inventory inventory) {
       return inventory.getRoom().getBasePrice();
    }
}
