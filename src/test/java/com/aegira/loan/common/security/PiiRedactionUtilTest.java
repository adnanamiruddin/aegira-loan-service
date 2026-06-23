package com.aegira.loan.common.security;

import com.aegira.loan.audit.entity.AuditLog;
import com.aegira.loan.audit.repository.AuditLogRepository;
import com.aegira.loan.audit.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PiiRedactionUtilTest {
    @Mock
    private AuditLogRepository repository;
    @InjectMocks
    private AuditService auditService;
    @Test
    void shouldMaskEmail() {
        assertEquals("bu***@mail.com", PiiRedactionUtil.sanitize("budi@mail.com"));
    }

    @Test
    void shouldMaskPhoneNumber() {
        assertEquals("0812****6789", PiiRedactionUtil.sanitize("08123456789"));
    }

    @Test
    void shouldMaskNikLikeNumber() {
        assertEquals("3173****0001", PiiRedactionUtil.sanitize("3173000000000001"));
    }

    @Test
    void shouldRedactTokenAndPassword() {
        assertEquals("password=[REDACTED] token=[REDACTED] Authorization: Bearer [REDACTED]",
                PiiRedactionUtil.sanitize("password=secret token=abc Authorization: Bearer jwt-value"));
    }

    @Test
    void shouldSanitizeAuditLogNotes() {
        when(repository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.log("LOAN_APPLICATION", UUID.randomUUID(), "UPDATE", null, "3173000000000001", "budi@mail.com",
                "phone=08123456789 password=secret", "business-correlation");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        assertEquals("3173****0001", captor.getValue().getOldValue());
        assertEquals("bu***@mail.com", captor.getValue().getNewValue());
        assertEquals("phone=0812****6789 password=[REDACTED]", captor.getValue().getNotes());
    }
}
