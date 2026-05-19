package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
import com.azienda.documentmanager.model.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendDeadlineAlert(String to, List<Document> expiringDocuments) {
        List<Document> expiringFiles = expiringDocuments.stream()
                .filter(d -> d.getType() == DocumentType.FILE)
                .toList();

        List<Document> expiringReminders = expiringDocuments.stream()
                .filter(d -> d.getType() == DocumentType.TEXT_REMINDER)
                .toList();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("AVVISO DI SCADENZA - Documenti Gas Service");

        StringBuilder sb = new StringBuilder();
        sb.append("Gentile ufficio,\n\n");

        if (!expiringFiles.isEmpty()) {
            sb.append("I seguenti documenti o certificazioni sono in scadenza entro i prossimi 21 giorni o sono già scaduti:\n\n");
            for (Document doc : expiringFiles) {
                sb.append("- ").append(doc.getTitle())
                        .append(" (Scadenza: ").append(doc.getExpiryDate()).append(")\n");
            }
            sb.append("\n");
        }

        if (!expiringReminders.isEmpty()) {
            sb.append("I seguenti promemoria richiedono attenzione:\n\n");
            for (Document doc : expiringReminders) {
                sb.append("- ").append(doc.getTitle())
                        .append(" (Scadenza: ").append(doc.getExpiryDate()).append(")\n");
                if (doc.getContent() != null && !doc.getContent().isBlank()) {
                    sb.append("  Nota: ").append(doc.getContent()).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("Si prega di provvedere al rinnovo dove necessario.\nCordiali saluti,\nSistema Automatico DocumentManager");

        message.setText(sb.toString());
        mailSender.send(message);
    }
}