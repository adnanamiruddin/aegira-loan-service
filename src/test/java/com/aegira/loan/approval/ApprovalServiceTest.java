package com.aegira.loan.approval;

import com.aegira.loan.approval.dto.ApprovalRequest;
import com.aegira.loan.approval.entity.ApprovalDecision;
import com.aegira.loan.approval.entity.ApprovalHistory;
import com.aegira.loan.approval.repository.ApprovalHistoryRepository;
import com.aegira.loan.approval.service.ApprovalService;
import com.aegira.loan.audit.service.AuditService;
import com.aegira.loan.common.exception.BadRequestException;
import com.aegira.loan.common.security.SecurityUtil;
import com.aegira.loan.customer.entity.Customer;
import com.aegira.loan.loanapplication.entity.ApplicationStatus;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanapplication.service.LoanApplicationService;
import com.aegira.loan.user.entity.Role;
import com.aegira.loan.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {
    @Mock
    private LoanApplicationService loanApplicationService;
    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private SecurityUtil securityUtil;
    @InjectMocks
    private ApprovalService approvalService;
    private User riskUser;

    @BeforeEach
    void setUp() {
        riskUser = new User();
        riskUser.setId(UUID.randomUUID());
        riskUser.setRole(Role.RISK);
    }

    @Test
    void riskApprove_shouldApproveWhenAmountIsAtThreshold() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_RISK_REVIEW);
        ApprovalRequest request = approvalRequest(new BigDecimal("50000000.00"), null);
        givenRiskUser();
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act
        approvalService.riskApprove(application.getId(), request);

        // Assert
        assertEquals(ApplicationStatus.HO_APPROVED, application.getStatus());

        verify(approvalHistoryRepository).save(any(ApprovalHistory.class));

        verify(auditService).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    

    @Test
    void riskApprove_shouldWaitForHoWhenAmountIsAboveThreshold() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_RISK_REVIEW);
        ApprovalRequest request = approvalRequest(new BigDecimal("50000000.01"), null);
        givenRiskUser();
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act
        approvalService.riskApprove(application.getId(), request);

        // Assert
        assertEquals(ApplicationStatus.WAITING_HO_APPROVAL, application.getStatus());
    }

    @Test
    void riskApprove_shouldUseRequestedAmountWhenApprovedAmountIsEmpty() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_RISK_REVIEW);
        givenRiskUser();
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act
        approvalService.riskApprove(application.getId(), approvalRequest(null, null));

        // Assert
        ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(approvalHistoryRepository).save(captor.capture());
        assertEquals(application.getRequestedAmount(), captor.getValue().getApprovedAmount());
    }

    @Test
    void riskApprove_shouldThrowErrorWhenApplicationIsNotWaitingForRisk() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.DRAFT);
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act and Assert
        assertThrows(BadRequestException.class,
                () -> approvalService.riskApprove(application.getId(), approvalRequest(null, null)));
        verify(approvalHistoryRepository, never()).save(any(ApprovalHistory.class));
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void riskReject_shouldRejectAndSaveNotes() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_RISK_REVIEW);
        ApprovalRequest request = approvalRequest(null, "Document is incomplete");
        givenRiskUser();
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act
        approvalService.riskReject(application.getId(), request);

        // Assert
        assertEquals(ApplicationStatus.RISK_REJECTED, application.getStatus());
        ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(approvalHistoryRepository).save(captor.capture());
        assertEquals(ApprovalDecision.RISK_REJECT, captor.getValue().getDecision());
        assertEquals("Document is incomplete", captor.getValue().getNotes());
    }

    @Test
    void hoApprove_shouldApproveWhenApplicationIsWaitingForHo() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_HO_APPROVAL);
        givenRiskUser();
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act
        approvalService.hoApprove(application.getId(), approvalRequest(null, null));

        // Assert
        assertEquals(ApplicationStatus.HO_APPROVED, application.getStatus());
    }

    @Test
    void hoApprove_shouldThrowErrorWhenApplicationIsNotWaitingForHo() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_RISK_REVIEW);
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act and Assert
        assertThrows(BadRequestException.class,
                () -> approvalService.hoApprove(application.getId(), approvalRequest(null, null)));
        verify(approvalHistoryRepository, never()).save(any(ApprovalHistory.class));
    }

    @Test
    void approvalHistory_shouldSaveBusinessCorrelationId() {
        // Arrange
        LoanApplication application = loanApplication(ApplicationStatus.WAITING_RISK_REVIEW);
        givenRiskUser();
        when(loanApplicationService.get(application.getId())).thenReturn(application);

        // Act
        approvalService.riskApprove(application.getId(), approvalRequest(null, null));

        // Assert
        ArgumentCaptor<ApprovalHistory> captor = ArgumentCaptor.forClass(ApprovalHistory.class);
        verify(approvalHistoryRepository).save(captor.capture());
        assertEquals(application.getCustomer().getId().toString(), captor.getValue().getCorrelationId());
    }

    private LoanApplication loanApplication(ApplicationStatus status) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());

        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setCustomer(customer);
        application.setStatus(status);
        application.setRequestedAmount(new BigDecimal("25000000.00"));
        return application;
    }

    private ApprovalRequest approvalRequest(BigDecimal approvedAmount, String notes) {
        ApprovalRequest request = new ApprovalRequest();
        request.setApprovedAmount(approvedAmount);
        request.setNotes(notes);
        return request;
    }

    private void givenRiskUser() {
        when(securityUtil.currentUser()).thenReturn(riskUser);
    }

}
