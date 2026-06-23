package com.aegira.loan.loanapplication;

import com.aegira.loan.approval.entity.ApprovalDecision;
import com.aegira.loan.approval.entity.ApprovalHistory;
import com.aegira.loan.approval.repository.ApprovalHistoryRepository;
import com.aegira.loan.audit.service.AuditService;
import com.aegira.loan.calculation.entity.LoanCalculation;
import com.aegira.loan.calculation.repository.LoanCalculationRepository;
import com.aegira.loan.calculation.service.LoanCalculationService;
import com.aegira.loan.common.exception.ForbiddenException;
import com.aegira.loan.common.security.SecurityUtil;
import com.aegira.loan.customer.entity.Customer;
import com.aegira.loan.eligibility.entity.EligibilityResult;
import com.aegira.loan.eligibility.entity.EligibilityRule;
import com.aegira.loan.eligibility.repository.EligibilityResultRepository;
import com.aegira.loan.eligibility.service.EligibilityService;
import com.aegira.loan.loanapplication.dto.LoanApplicationDetailResponse;
import com.aegira.loan.loanapplication.entity.ApplicationStatus;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanapplication.entity.RiskLevel;
import com.aegira.loan.loanapplication.provider.LoanDataProviderResolver;
import com.aegira.loan.loanapplication.repository.LoanApplicationRepository;
import com.aegira.loan.loanapplication.service.LoanApplicationService;
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
class LoanApplicationUiServiceTest {
    @Mock
    private LoanApplicationRepository applicationRepository;
    @Mock
    private LoanDataProviderResolver loanDataProviderResolver;
    @Mock
    private LoanCalculationService loanCalculationService;
    @Mock
    private LoanCalculationRepository calculationRepository;
    @Mock
    private EligibilityService eligibilityService;
    @Mock
    private EligibilityResultRepository eligibilityRepository;
    @Mock
    private ApprovalHistoryRepository approvalHistoryRepository;
    @Mock
    private SecurityUtil securityUtil;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private LoanApplicationService service;

    @Test
    void applicationDetailReturnsAggregateData() {
        // Arrange: buat data yang diperlukan test secara langsung.
        User agent = new User();
        agent.setId(UUID.randomUUID());
        agent.setRole(Role.AGENT);

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setNik("3171234567890001");
        customer.setName("Budi Santoso");
        customer.setPhoneNumber("08123456789");
        customer.setMonthlyIncome(new BigDecimal("8000000.00"));
        customer.setMonthlyExpense(new BigDecimal("2500000.00"));
        customer.setExistingInstallment(new BigDecimal("1000000.00"));

        LoanProduct product = new LoanProduct();
        product.setId(UUID.randomUUID());
        product.setName("Personal Loan");
        product.setAnnualInterestRate(new BigDecimal("0.1200"));
        product.setMaximumDsr(new BigDecimal("40.0000"));

        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setApplicationNumber("APP-20260503-0001");
        application.setCustomer(customer);
        application.setAgent(agent);
        application.setLoanProduct(product);
        application.setRequestedAmount(new BigDecimal("30000000.00"));
        application.setRequestedTenure(24);
        application.setLoanPurpose("Working Capital");
        application.setStatus(ApplicationStatus.WAITING_RISK_REVIEW);
        application.setRiskLevel(RiskLevel.MEDIUM);
        application.setCreatedAt(OffsetDateTime.now());

        LoanCalculation calculation = new LoanCalculation();
        calculation.setLoanApplication(application);
        calculation.setTotalInterest(new BigDecimal("7200000.00"));
        calculation.setTotalPayment(new BigDecimal("37200000.00"));
        calculation.setMonthlyInstallment(new BigDecimal("1550000.00"));
        calculation.setCurrentDsr(new BigDecimal("12.5000"));
        calculation.setProjectedDsr(new BigDecimal("31.8750"));
        calculation.setEligible(true);

        EligibilityResult eligibility = new EligibilityResult();
        eligibility.setLoanApplication(application);
        eligibility.setRuleName(EligibilityRule.MINIMUM_INCOME);
        eligibility.setPassed(true);
        eligibility.setMessage("Customer income meets minimum requirement");

        User riskUser = new User();
        riskUser.setId(UUID.randomUUID());
        riskUser.setRole(Role.RISK);
        ApprovalHistory history = new ApprovalHistory();
        history.setLoanApplication(application);
        history.setPerformedBy(riskUser);
        history.setDecision(ApprovalDecision.RISK_APPROVE);
        history.setNotes("Customer is eligible");
        history.setCreatedAt(OffsetDateTime.now());

        when(securityUtil.currentUser()).thenReturn(agent);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));
        when(calculationRepository.findTopByLoanApplicationIdOrderByCreatedAtDesc(application.getId())).thenReturn(Optional.of(calculation));
        when(eligibilityRepository.findByLoanApplicationId(application.getId())).thenReturn(Collections.singletonList(eligibility));
        when(approvalHistoryRepository.findByLoanApplicationIdOrderByCreatedAtAsc(application.getId())).thenReturn(Collections.singletonList(history));

        // Act
        LoanApplicationDetailResponse detail = service.detail(application.getId());

        // Assert
        assertEquals(application.getId(), detail.getId());
        assertEquals("Budi Santoso", detail.getCustomer().getName());
        assertEquals("Personal Loan", detail.getLoanProduct().getName());
        assertEquals(new BigDecimal("1550000.00"), detail.getCalculation().getMonthlyInstallment());
        assertEquals("MINIMUM_INCOME", detail.getEligibilityResults().get(0).getRuleCode());
        assertEquals("APPROVED", detail.getApprovalHistories().get(0).getDecision());
    }

    @Test
    void agentCannotAccessAnotherAgentApplicationDetail() {
        // Arrange
        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setRole(Role.AGENT);

        User otherAgent = new User();
        otherAgent.setId(UUID.randomUUID());
        otherAgent.setRole(Role.AGENT);

        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setAgent(owner);
        when(securityUtil.currentUser()).thenReturn(otherAgent);
        when(applicationRepository.findById(application.getId())).thenReturn(Optional.of(application));

        // Act and Assert
        assertThrows(ForbiddenException.class, () -> service.detail(application.getId()));
        verifyNoInteractions(calculationRepository, eligibilityRepository, approvalHistoryRepository);
    }
}
