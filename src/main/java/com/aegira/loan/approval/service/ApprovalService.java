package com.aegira.loan.approval.service;

import com.aegira.loan.approval.dto.ApprovalHistoryResponse;
import com.aegira.loan.approval.dto.ApprovalRequest;
import com.aegira.loan.approval.entity.ApprovalDecision;
import com.aegira.loan.approval.entity.ApprovalHistory;
import com.aegira.loan.approval.repository.ApprovalHistoryRepository;
import com.aegira.loan.audit.service.AuditService;
import com.aegira.loan.common.exception.BadRequestException;
import com.aegira.loan.common.security.SecurityUtil;
import com.aegira.loan.loanapplication.dto.LoanApplicationResponse;
import com.aegira.loan.loanapplication.entity.ApplicationStatus;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanapplication.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalService {
    private static final BigDecimal HO_THRESHOLD = new BigDecimal("50000000.00");
    private final LoanApplicationService loanApplicationService;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final AuditService auditService;
    private final SecurityUtil securityUtil;

    @Transactional
    public LoanApplicationResponse riskApprove(UUID id, ApprovalRequest request) {
        ApprovalRequest safeRequest = request == null ? new ApprovalRequest() : request;
        LoanApplication application = loanApplicationService.get(id);
        withCorrelation(application);
        try {
            requireStatus(application, ApplicationStatus.WAITING_RISK_REVIEW);
            BigDecimal approvedAmount = safeRequest.getApprovedAmount() == null ? application.getRequestedAmount() : safeRequest.getApprovedAmount();
            ApplicationStatus old = application.getStatus();
            ApplicationStatus next = approvedAmount.compareTo(HO_THRESHOLD) > 0
                    ? ApplicationStatus.WAITING_HO_APPROVAL
                    : ApplicationStatus.HO_APPROVED;
            application.setStatus(next);
            createHistory(application, ApprovalDecision.RISK_APPROVE, approvedAmount, emptyToDash(safeRequest.getNotes()));
            
            audit(application, "RISK_APPROVE", old, next, safeRequest.getNotes());
            
        
            log.info("event_name=loan_application_approved resource_id={} approved_amount={} next_status={}", id, approvedAmount, next);
            
            return loanApplicationService.toResponse(application);
        } finally {
            MDC.remove("business_correlation_id");
        }
    }

    @Transactional
    public LoanApplicationResponse riskReject(UUID id, ApprovalRequest request) {
        requireNotes(request);
        LoanApplication application = loanApplicationService.get(id);
        withCorrelation(application);
        try {
            requireStatus(application, ApplicationStatus.WAITING_RISK_REVIEW);
            ApplicationStatus old = application.getStatus();
            application.setStatus(ApplicationStatus.RISK_REJECTED);
            createHistory(application, ApprovalDecision.RISK_REJECT, null, request.getNotes());
            audit(application, "RISK_REJECT", old, application.getStatus(), request.getNotes());
            log.warn("event_name=loan_application_rejected resource_id={} role=RISK", id);
            return loanApplicationService.toResponse(application);
        } finally {
            MDC.remove("business_correlation_id");
        }
    }

    @Transactional
    public LoanApplicationResponse requestRevision(UUID id, ApprovalRequest request) {
        requireNotes(request);
        LoanApplication application = loanApplicationService.get(id);
        withCorrelation(application);
        try {
            requireStatus(application, ApplicationStatus.WAITING_RISK_REVIEW);
            ApplicationStatus old = application.getStatus();
            application.setStatus(ApplicationStatus.REVISION_REQUESTED);
            createHistory(application, ApprovalDecision.RISK_REQUEST_REVISION, null, request.getNotes());
            audit(application, "RISK_REQUEST_REVISION", old, application.getStatus(), request.getNotes());
            return loanApplicationService.toResponse(application);
        } finally {
            MDC.remove("business_correlation_id");
        }
    }

    @Transactional
    public LoanApplicationResponse hoApprove(UUID id, ApprovalRequest request) {
        ApprovalRequest safeRequest = request == null ? new ApprovalRequest() : request;
        LoanApplication application = loanApplicationService.get(id);
        withCorrelation(application);
        try {
            requireStatus(application, ApplicationStatus.WAITING_HO_APPROVAL);
            ApplicationStatus old = application.getStatus();
            application.setStatus(ApplicationStatus.HO_APPROVED);
            createHistory(application, ApprovalDecision.HO_APPROVE, safeRequest.getApprovedAmount(), emptyToDash(safeRequest.getNotes()));
            audit(application, "HO_APPROVE", old, application.getStatus(), safeRequest.getNotes());
            return loanApplicationService.toResponse(application);
        } finally {
            MDC.remove("business_correlation_id");
        }
    }

    @Transactional
    public LoanApplicationResponse hoReject(UUID id, ApprovalRequest request) {
        requireNotes(request);
        LoanApplication application = loanApplicationService.get(id);
        withCorrelation(application);
        try {
            requireStatus(application, ApplicationStatus.WAITING_HO_APPROVAL);
            ApplicationStatus old = application.getStatus();
            application.setStatus(ApplicationStatus.HO_REJECTED);
            createHistory(application, ApprovalDecision.HO_REJECT, null, request.getNotes());
            audit(application, "HO_REJECT", old, application.getStatus(), request.getNotes());
            log.warn("event_name=loan_application_rejected resource_id={} role=HO", id);
            return loanApplicationService.toResponse(application);
        } finally {
            MDC.remove("business_correlation_id");
        }
    }

    public List<ApprovalHistoryResponse> histories(UUID id) {
        return approvalHistoryRepository.findByLoanApplicationIdOrderByCreatedAtAsc(id).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void createHistory(LoanApplication application, ApprovalDecision decision, BigDecimal approvedAmount, String notes) {
        ApprovalHistory history = new ApprovalHistory();
        history.setLoanApplication(application);
        history.setPerformedBy(securityUtil.currentUser());
        history.setDecision(decision);
        history.setApprovedAmount(approvedAmount);
        history.setNotes(notes);
        history.setCorrelationId(application.getCustomer().getId().toString());
        approvalHistoryRepository.save(history);
    }

    private void audit(LoanApplication application, String action, ApplicationStatus oldStatus, ApplicationStatus newStatus, String notes) {
        auditService.log("LOAN_APPLICATION", application.getId(), action, securityUtil.currentUser(),
                oldStatus.name(), newStatus.name(), notes, application.getCustomer().getId().toString());
    }

    private void requireStatus(LoanApplication application, ApplicationStatus expected) {
        if (application.getStatus() != expected) {
            throw new BadRequestException("Application status must be " + expected);
        }
    }

    private void requireNotes(ApprovalRequest request) {
        if (request == null || request.getNotes() == null || request.getNotes().trim().isEmpty()) {
            throw new BadRequestException("Notes are required");
        }
    }

    private String emptyToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private void withCorrelation(LoanApplication application) {
        MDC.put("business_correlation_id", application.getCustomer().getId().toString());
    }

    private ApprovalHistoryResponse toResponse(ApprovalHistory history) {
        return ApprovalHistoryResponse.builder()
                .id(history.getId())
                .loanApplicationId(history.getLoanApplication().getId())
                .performedBy(history.getPerformedBy().getId())
                .decision(history.getDecision())
                .approvedAmount(history.getApprovedAmount())
                .notes(history.getNotes())
                .correlationId(history.getCorrelationId())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
