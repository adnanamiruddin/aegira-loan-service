package com.aegira.loan.approval;

import com.aegira.loan.approval.dto.ApprovalTaskFilter;
import com.aegira.loan.approval.dto.ApprovalTaskItemResponse;
import com.aegira.loan.approval.service.ApprovalTaskService;
import com.aegira.loan.calculation.entity.LoanCalculation;
import com.aegira.loan.calculation.repository.LoanCalculationRepository;
import com.aegira.loan.common.dto.PageResponse;
import com.aegira.loan.common.exception.ForbiddenException;
import com.aegira.loan.common.security.SecurityUtil;
import com.aegira.loan.customer.entity.Customer;
import com.aegira.loan.loanapplication.entity.ApplicationStatus;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanapplication.entity.RiskLevel;
import com.aegira.loan.loanapplication.repository.LoanApplicationRepository;
import com.aegira.loan.loanproduct.entity.LoanProduct;
import com.aegira.loan.user.entity.Role;
import com.aegira.loan.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalTaskServiceTest {
    @Mock
    private LoanApplicationRepository applicationRepository;
    @Mock
    private LoanCalculationRepository calculationRepository;
    @Mock
    private SecurityUtil securityUtil;
    @InjectMocks
    private ApprovalTaskService service;

    @Test
    void riskCanGetApprovalTaskList() {
        User risk = user(Role.RISK);
        LoanApplication application = application(ApplicationStatus.WAITING_RISK_REVIEW);
        when(securityUtil.currentUser()).thenReturn(risk);
        when(applicationRepository.findByStatus(ApplicationStatus.WAITING_RISK_REVIEW)).thenReturn(Collections.singletonList(application));
        when(calculationRepository.findTopByLoanApplicationIdOrderByCreatedAtDesc(application.getId())).thenReturn(Optional.of(calculation(application)));

        PageResponse<ApprovalTaskItemResponse> result = service.tasks(new ApprovalTaskFilter());

        assertEquals(1, result.getTotalElements());
        assertEquals(ApplicationStatus.WAITING_RISK_REVIEW, result.getContent().get(0).getStatus());
        assertEquals(new BigDecimal("36.4000"), result.getContent().get(0).getProjectedDsr());
    }

    @Test
    void hoCanGetApprovalTaskList() {
        User ho = user(Role.HO);
        LoanApplication application = application(ApplicationStatus.WAITING_HO_APPROVAL);
        when(securityUtil.currentUser()).thenReturn(ho);
        when(applicationRepository.findByStatus(ApplicationStatus.WAITING_HO_APPROVAL)).thenReturn(Collections.singletonList(application));
        when(calculationRepository.findTopByLoanApplicationIdOrderByCreatedAtDesc(application.getId())).thenReturn(Optional.of(calculation(application)));

        PageResponse<ApprovalTaskItemResponse> result = service.tasks(new ApprovalTaskFilter());

        assertEquals(1, result.getTotalElements());
        assertEquals(ApplicationStatus.WAITING_HO_APPROVAL, result.getContent().get(0).getStatus());
    }

    @Test
    void shouldRejectRoleWhenNotAllowedToAccessApprovalTask() {
        when(securityUtil.currentUser()).thenReturn(user(Role.AGENT));

        assertThrows(ForbiddenException.class, () -> service.tasks(new ApprovalTaskFilter()));

        verifyNoInteractions(applicationRepository, calculationRepository);
    }

    private LoanApplication application(ApplicationStatus status) {
        Customer customer = new Customer();
        customer.setName("Budi Santoso");
        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setApplicationNumber("APP-1");
        application.setCustomer(customer);
        application.setLoanProduct(new LoanProduct());
        application.setRequestedAmount(new BigDecimal("30000000.00"));
        application.setRiskLevel(RiskLevel.MEDIUM);
        application.setStatus(status);
        application.setSubmittedAt(OffsetDateTime.now());
        return application;
    }

    private LoanCalculation calculation(LoanApplication application) {
        LoanCalculation calculation = new LoanCalculation();
        calculation.setLoanApplication(application);
        calculation.setProjectedDsr(new BigDecimal("36.4000"));
        return calculation;
    }

    private User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }
}
