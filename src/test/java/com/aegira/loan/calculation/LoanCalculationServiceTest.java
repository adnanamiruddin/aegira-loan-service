package com.aegira.loan.calculation;

import com.aegira.loan.calculation.entity.LoanCalculation;
import com.aegira.loan.calculation.repository.LoanCalculationRepository;
import com.aegira.loan.calculation.service.LoanCalculationService;
import com.aegira.loan.common.exception.BadRequestException;
import com.aegira.loan.customer.entity.Customer;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanproduct.entity.LoanProduct;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class LoanCalculationServiceTest {
    @Mock
    private LoanCalculationRepository loanCalculationRepository;
    @InjectMocks
    private LoanCalculationService service;

    @Test
    void calculateTotalInterest() {
        LoanCalculation result = service.calculate(application(new BigDecimal("12000000.00"), 12, new BigDecimal("5000000.00"), new BigDecimal("500000.00")));
        assertEquals(new BigDecimal("1440000.00"), result.getTotalInterest());
    }

    @Test
    void calculateMonthlyInstallment() {
        LoanCalculation result = service.calculate(application(new BigDecimal("12000000.00"), 12, new BigDecimal("5000000.00"), new BigDecimal("500000.00")));
        assertEquals(new BigDecimal("1120000.00"), result.getMonthlyInstallment());
    }

    @Test
    void calculateCurrentDsr() {
        LoanCalculation result = service.calculate(application(new BigDecimal("12000000.00"), 12, new BigDecimal("5000000.00"), new BigDecimal("500000.00")));
        assertEquals(new BigDecimal("10.0000"), result.getCurrentDsr());
    }

    @Test
    void calculateProjectedDsr() {
        LoanCalculation result = service.calculate(application(new BigDecimal("12000000.00"), 12, new BigDecimal("5000000.00"), new BigDecimal("500000.00")));
        assertEquals(new BigDecimal("32.4000"), result.getProjectedDsr());
    }

    @Test
    void rejectDivisionByZeroMonthlyIncome() {
        assertThrows(BadRequestException.class, () ->
                service.calculate(application(new BigDecimal("12000000.00"), 12, BigDecimal.ZERO, new BigDecimal("500000.00"))));
    }

    private LoanApplication application(BigDecimal amount, int tenure, BigDecimal income, BigDecimal existingInstallment) {
        Customer customer = new Customer();
        customer.setDateOfBirth(LocalDate.now().minusYears(30));
        customer.setMonthlyIncome(income);
        customer.setExistingInstallment(existingInstallment);
        LoanProduct product = new LoanProduct();
        product.setAnnualInterestRate(new BigDecimal("0.1200"));
        product.setMaximumDsr(new BigDecimal("40.0000"));
        LoanApplication application = new LoanApplication();
        application.setCustomer(customer);
        application.setLoanProduct(product);
        application.setRequestedAmount(amount);
        application.setRequestedTenure(tenure);
        return application;
    }
}
