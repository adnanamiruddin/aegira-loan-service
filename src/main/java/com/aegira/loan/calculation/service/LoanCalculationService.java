package com.aegira.loan.calculation.service;

import com.aegira.loan.calculation.dto.LoanCalculationResponse;
import com.aegira.loan.calculation.entity.LoanCalculation;
import com.aegira.loan.calculation.repository.LoanCalculationRepository;
import com.aegira.loan.common.exception.BadRequestException;
import com.aegira.loan.common.exception.NotFoundException;
import com.aegira.loan.customer.entity.Customer;
import com.aegira.loan.loanapplication.entity.LoanApplication;
import com.aegira.loan.loanproduct.entity.LoanProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanCalculationService {
    private static final BigDecimal TWELVE = new BigDecimal("12");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final LoanCalculationRepository loanCalculationRepository;

    public LoanCalculation calculateAndSave(LoanApplication application) {
        LoanCalculation calculation = calculate(application);
        log.info("loan calculation completed applicationId={} projectedDsr={} eligible={}",
                application.getId(), calculation.getProjectedDsr(), calculation.getEligible());
        
        // log.warn("hai");
        return loanCalculationRepository.save(calculation);
    }

    public LoanCalculation calculate(LoanApplication application) {
        Customer customer = application.getCustomer();
        LoanProduct product = application.getLoanProduct();
        if (customer.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Monthly income must be greater than zero");
        }
        BigDecimal loanAmount = money(application.getRequestedAmount());
        BigDecimal totalInterest = money(loanAmount.multiply(product.getAnnualInterestRate())
                .multiply(new BigDecimal(application.getRequestedTenure()))
                .divide(TWELVE, 2, RoundingMode.HALF_UP));
        BigDecimal totalPayment = money(loanAmount.add(totalInterest));
        BigDecimal monthlyInstallment = money(totalPayment.divide(new BigDecimal(application.getRequestedTenure()), 2, RoundingMode.HALF_UP));
        BigDecimal currentDsr = ratio(customer.getExistingInstallment().divide(customer.getMonthlyIncome(), 8, RoundingMode.HALF_UP).multiply(ONE_HUNDRED));
        BigDecimal projectedDsr = ratio(customer.getExistingInstallment().add(monthlyInstallment)
                .divide(customer.getMonthlyIncome(), 8, RoundingMode.HALF_UP).multiply(ONE_HUNDRED));

        LoanCalculation calculation = new LoanCalculation();
        calculation.setLoanApplication(application);
        calculation.setLoanAmount(loanAmount);
        calculation.setAnnualInterestRate(product.getAnnualInterestRate());
        calculation.setTenureMonth(application.getRequestedTenure());
        calculation.setTotalInterest(totalInterest);
        calculation.setTotalPayment(totalPayment);
        calculation.setMonthlyInstallment(monthlyInstallment);
        calculation.setExistingInstallment(money(customer.getExistingInstallment()));
        calculation.setMonthlyIncome(money(customer.getMonthlyIncome()));
        calculation.setCurrentDsr(currentDsr);
        calculation.setProjectedDsr(projectedDsr);
        calculation.setMaximumDsr(product.getMaximumDsr());
        calculation.setEligible(projectedDsr.compareTo(product.getMaximumDsr()) <= 0);
        return calculation;
    }

    public LoanCalculationResponse latest(UUID applicationId) {
        return loanCalculationRepository.findTopByLoanApplicationIdOrderByCreatedAtDesc(applicationId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Calculation not found"));
    }

    public LoanCalculationResponse toResponse(LoanCalculation calculation) {
        return LoanCalculationResponse.builder()
                .id(calculation.getId())
                .loanApplicationId(calculation.getLoanApplication().getId())
                .loanAmount(calculation.getLoanAmount())
                .annualInterestRate(calculation.getAnnualInterestRate())
                .tenureMonth(calculation.getTenureMonth())
                .totalInterest(calculation.getTotalInterest())
                .totalPayment(calculation.getTotalPayment())
                .monthlyInstallment(calculation.getMonthlyInstallment())
                .existingInstallment(calculation.getExistingInstallment())
                .monthlyIncome(calculation.getMonthlyIncome())
                .currentDsr(calculation.getCurrentDsr())
                .projectedDsr(calculation.getProjectedDsr())
                .maximumDsr(calculation.getMaximumDsr())
                .eligible(calculation.getEligible())
                .createdAt(calculation.getCreatedAt())
                .build();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
