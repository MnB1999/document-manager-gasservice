package com.azienda.documentmanager.services;

import com.azienda.documentmanager.models.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class ReminderRecurrenceService {

    // Moves expiryDate forward by whole periods (at least one) until it is strictly after the given date
    public void advanceExpiryAfter(Document doc, LocalDate date) {
        if (doc.getFrequencyMonths() == null) {
            throw new IllegalStateException("Il documento " + doc.getId() + " non è ricorrente.");
        }
        LocalDate expiry = doc.getExpiryDate();
        do {
            expiry = expiry.plusMonths(doc.getFrequencyMonths());
        } while (!expiry.isAfter(date));
        doc.setExpiryDate(expiry);
    }
}
