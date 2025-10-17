package com.swp391.gr3.ev_management.events;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingDomainPublisher {
    private final ApplicationEventPublisher publisher;
    public void publishBookingConfirmed(Long bookingId) {
        publisher.publishEvent(new BookingConfirmedEvent(bookingId));
    }
}
