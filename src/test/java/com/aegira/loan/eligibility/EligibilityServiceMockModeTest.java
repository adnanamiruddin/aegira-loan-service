package com.aegira.loan.eligibility;

import com.aegira.loan.calculation.entity.LoanCalculation;
import com.aegira.loan.common.config.DataSourceMode;
import com.aegira.loan.common.config.LoanDataSourceProperties;
import com.aegira.loan.eligibility.entity.EligibilityResult;
import com.aegira.loan.eligibility.repository.EligibilityResultRepository;
import com.aegira.loan.eligibility.service.EligibilityService;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanapplication.provider.DatabaseLoanDataProvider;
import com.aegira.loan.loanapplication.provider.LoanDataProviderResolver;
import com.aegira.loan.loanapplication.provider.MockLoanDataProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EligibilityServiceMockModeTest {
    @Test
    void shouldPassAllEligibilityRulesWhenUsingMockData() {
        // Arrange: gunakan provider mock sungguhan karena data mock-nya yang ingin dites.
        MockLoanDataProvider mockProvider = new MockLoanDataProvider();
        LoanDataSourceProperties properties = new LoanDataSourceProperties();
        properties.setMode(DataSourceMode.MOCK);

        // Database provider dan repository tidak dipakai pada MOCK mode, jadi cukup dibuat mock.
        LoanDataProviderResolver resolver = new LoanDataProviderResolver(
                properties,
                mock(DatabaseLoanDataProvider.class),
                mockProvider
        );
        EligibilityResultRepository repository = mock(EligibilityResultRepository.class);
        EligibilityService service = new EligibilityService(repository, resolver);

        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LoanApplication application = new LoanApplication();
        application.setId(UUID.randomUUID());
        application.setCustomer(mockProvider.getCustomerById(customerId));
        application.setLoanProduct(mockProvider.getActiveLoanProductById(productId));
        application.setRequestedAmount(new BigDecimal("25000000.00"));
        application.setRequestedTenure(12);
        LoanCalculation calculation = new LoanCalculation();
        calculation.setProjectedDsr(new BigDecimal("38.7500"));

        // Act
        List<EligibilityResult> results = service.evaluate(application, calculation);

        // Assert: terdapat tujuh rule dan semua rule lulus untuk data mock.
        assertEquals(7, results.size());
        assertTrue(results.stream().allMatch(EligibilityResult::getPassed));
    }
}
