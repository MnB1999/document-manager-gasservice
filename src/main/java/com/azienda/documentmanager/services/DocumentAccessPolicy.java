package com.azienda.documentmanager.services;

import com.azienda.documentmanager.models.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

//Every endpoint that touches documents gets here first

@Component
@RequiredArgsConstructor
public class DocumentAccessPolicy {

    private final SecurityService securityService;

    public boolean canViewSpecial() {
        return securityService.isAdmin();
    }

    public void assertCanView(Document doc) {
        if (doc.isSpecial() && !canViewSpecial()) {
            throw new AccessDeniedException("Non hai i permessi per visualizzare questo documento.");
        }
    }

    public void assertCanModify(Document doc) {
        if (doc.isSpecial() && !securityService.isAdmin()) {
            throw new AccessDeniedException("Operazione negata: solo gli amministratori possono modificare documenti speciali.");
        }
    }

    public void assertCanCreateSpecial(boolean special) {
        if (special && !securityService.isAdmin()) {
            throw new AccessDeniedException("Operazione negata: solo gli amministratori possono caricare documenti speciali.");
        }
    }
}