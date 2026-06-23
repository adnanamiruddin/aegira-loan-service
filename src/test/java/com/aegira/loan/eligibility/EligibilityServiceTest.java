package com.aegira.loan.eligibility;

import com.aegira.loan.calculation.entity.LoanCalculation;
import com.aegira.loan.customer.entity.Customer;
import com.aegira.loan.eligibility.entity.EligibilityResult;
import com.aegira.loan.eligibility.entity.EligibilityRule;
import com.aegira.loan.eligibility.repository.EligibilityResultRepository;
import com.aegira.loan.eligibility.service.EligibilityService;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanapplication.entity.RiskLevel;
import com.aegira.loan.loanapplication.provider.LoanDataProvider;
import com.aegira.loan.loanapplication.provider.LoanDataProviderResolver;
import com.aegira.loan.loanproduct.entity.LoanProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {
    @Mock
    private LoanDataProviderResolver loanDataProviderResolver;
    @Mock
    private LoanDataProvider loanDataProvider;
    @Mock
    private EligibilityResultRepository eligibilityResultRepository;
    @InjectMocks
    private EligibilityService service;

    @Test
    void passAllRules() {
        givenCustomerHasNoActiveApplication();
        List<EligibilityResult> results = service.evaluate(application(new BigDecimal("5000000.00"), 12, new BigDecimal("5000000.00"), 30), calculation("35.0000"));
        assertTrue(results.stream().allMatch(EligibilityResult::getPassed));
    }

    @Test
    void failMinimumIncome() {
        givenCustomerHasNoActiveApplication();
        List<EligibilityResult> results = service.evaluate(application(new BigDecimal("5000000.00"), 12, new BigDecimal("2000000.00"), 30), calculation("35.0000"));
        assertFailed(results, EligibilityRule.MINIMUM_INCOME);
    }

    @Test
    void failDsrLimit() {
        givenCustomerHasNoActiveApplication();
        List<EligibilityResult> results = service.evaluate(application(new BigDecimal("5000000.00"), 12, new BigDecimal("5000000.00"), 30), calculation("45.0000"));
        assertFailed(results, EligibilityRule.DSR_LIMIT);
    }

    @Test
    void failAgeRule() {
        givenCustomerHasNoActiveApplication();
        List<EligibilityResult> results = service.evaluate(application(new BigDecimal("5000000.00"), 12, new BigDecimal("5000000.00"), 20), calculation("35.0000"));
        assertFailed(results, EligibilityRule.MINIMUM_AGE);
    }

    @Test
    void riskLevelLow() {
        assertEquals(RiskLevel.LOW, service.riskLevel(new BigDecimal("30.0000")));
    }

    @Test
    void riskLevelMedium() {
        assertEquals(RiskLevel.MEDIUM, service.riskLevel(new BigDecimal("35.0000")));
    }

    @Test
    void riskLevelHigh() {
        assertEquals(RiskLevel.HIGH, service.riskLevel(new BigDecimal("40.0001")));
    }

    private void assertFailed(List<EligibilityResult> results, EligibilityRule rule) {
        assertTrue(results.stream().anyMatch(result -> result.getRuleName() == rule && !result.getPassed()));
    }

    private void givenCustomerHasNoActiveApplication() {
        when(loanDataProviderResolver.resolve()).thenReturn(loanDataProvider);
        when(loanDataProvider.hasActiveApplication(org.mockito.ArgumentMatchers.any(UUID.class))).thenReturn(false);
    }

    private LoanApplication application(BigDecimal amount, int tenure, BigDecimal income, int age) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setDateOfBirth(LocalDate.now().minusYears(age));
        customer.setMonthlyIncome(income);
        LoanProduct product = new LoanProduct();
        product.setMinimumIncome(new BigDecimal("3000000.00"));
        product.setMinAmount(new BigDecimal("5000000.00"));
        product.setMaxAmount(new BigDecimal("100000000.00"));
        product.setMinTenure(6);
        product.setMaxTenure(36);
        product.setMaximumDsr(new BigDecimal("40.0000"));
        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setCustomer(customer);
        application.setLoanProduct(product);
        application.setRequestedAmount(amount);
        application.setRequestedTenure(tenure);
        return application;
    }

    private LoanCalculation calculation(String projectedDsr) {
        LoanCalculation calculation = new LoanCalculation();
        calculation.setProjectedDsr(new BigDecimal(projectedDsr));
        return calculation;
    }
}
