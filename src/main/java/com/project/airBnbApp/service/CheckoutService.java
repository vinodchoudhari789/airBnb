package com.project.airBnbApp.service;

import com.project.airBnbApp.entity.Booking;

public interface CheckoutService {

    public String getCheckoutSession(Booking booking, String successUrl, String failureUrl);
}
