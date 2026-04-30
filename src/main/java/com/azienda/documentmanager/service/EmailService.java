package com.azienda.documentmanager.service;

import com.azienda.documentmanager.model.Document;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("AVVISO DI SCADENZA - Documenti Gas Service");

        StringBuilder sb = new StringBuilder();
        sb.append("Gentile ufficio,\n\nI seguenti documenti o certificazioni sono in scadenza entro i prossimi 21 giorni o sono già scaduti:\n\n");

        for (Document doc : expiringDocuments) {
            sb.append("- ").append(doc.getTitle())
                    .append(" (Scadenza: ").append(doc.getExpiryDate()).append(")\n");
        }

        sb.append("\nSi prega di provvedere al rinnovo.\nCordiali saluti,\nSistema Automatico DocumentManager");

        message.setText(sb.toString());
        mailSender.send(message);
        
    }
}