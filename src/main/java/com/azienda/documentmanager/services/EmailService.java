package com.azienda.documentmanager.services;

import com.azienda.documentmanager.models.Document;
import com.azienda.documentmanager.models.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String SUBJECT = "AVVISO DI SCADENZA - Documenti Gas Service";
    private static final String GREETING = "Gentile ufficio,\n\n";
    private static final String FILES_HEADER = "I seguenti documenti o certificazioni sono in scadenza entro i prossimi 28 giorni o sono già scaduti:\n\n";
    private static final String REMINDERS_HEADER = "I seguenti promemoria richiedono attenzione:\n\n";
    private static final String CLOSING = "Si prega di provvedere al rinnovo dove necessario.\nCordiali saluti,\nSistema Automatico DocumentManager";

    private final JavaMailSender mailSender;

    public void sendDeadlineAlert(List<String> to, List<Document> expiringDocuments) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to.toArray(new String[0]));
        message.setSubject(SUBJECT);
        message.setText(buildMessageBody(expiringDocuments));

        mailSender.send(message);
    }


    private String buildMessageBody(List<Document> expiringDocuments) {
        List<Document> files = filterByType(expiringDocuments, DocumentType.FILE);
        List<Document> reminders = filterByType(expiringDocuments, DocumentType.TEXT_REMINDER);

        StringBuilder body = new StringBuilder(GREETING);
        appendFilesSection(body, files);
        appendRemindersSection(body, reminders);
        body.append(CLOSING);

        return body.toString();
    }

    private List<Document> filterByType(List<Document> documents, DocumentType type) {
        return documents.stream()
                .filter(d -> d.getType() == type)
                .toList();
    }

    private void appendFilesSection(StringBuilder body, List<Document> files) {
        if (files.isEmpty()) return;

        body.append(FILES_HEADER);
        files.forEach(doc -> body.append("- ")
                .append(doc.getTitle())
                .append(" (Scadenza: ").append(doc.getExpiryDate()).append(")\n"));
        body.append("\n");
    }

    private void appendRemindersSection(StringBuilder body, List<Document> reminders) {
        if (reminders.isEmpty()) return;

        body.append(REMINDERS_HEADER);
        reminders.forEach(doc -> {
            body.append("- ")
                    .append(doc.getTitle())
                    .append(" (Scadenza: ").append(doc.getExpiryDate()).append(")\n");
            if (doc.getContent() != null && !doc.getContent().isBlank()) {
                body.append("  Nota: ").append(doc.getContent()).append("\n");
            }
            if (doc.getFrequencyMonths() != null) {
                body.append("  Ricorrente: la scadenza viene rinnovata automaticamente di ")
                        .append(doc.getFrequencyMonths()).append(" mesi.\n");
            }
        });
        body.append("\n");
    }
}