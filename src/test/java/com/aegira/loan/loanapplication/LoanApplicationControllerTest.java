package com.aegira.loan.loanapplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.aegira.loan.calculation.service.LoanCalculationService;
import com.aegira.loan.common.exception.ForbiddenException;
import com.aegira.loan.common.exception.GlobalExceptionHandler;
import com.aegira.loan.eligibility.service.EligibilityService;
import com.aegira.loan.loanapplication.controller.LoanApplicationController;
import com.aegira.loan.loanapplication.dto.LoanApplicationResponse;
import com.aegira.loan.loanapplication.entity.ApplicationStatus;
import com.aegira.loan.loanapplication.service.LoanApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoanApplicationControllerTest {
    @Mock
    private LoanApplicationService loanApplicationService;
    @Mock
    private LoanCalculationService loanCalculationService;
    @Mock
    private EligibilityService eligibilityService;
    @InjectMocks
    private LoanApplicationController controller;
    
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Controller dibuat otomatis oleh @InjectMocks; service dependency dibuat oleh @Mock.
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void get_shouldReturnLoanApplicationWhenServiceFindsData() throws Exception {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        LoanApplicationResponse application = LoanApplicationResponse.builder()
                .id(applicationId)
                .applicationNumber("APP-001")
                .status(ApplicationStatus.WAITING_RISK_REVIEW)
                .build();
        when(loanApplicationService.findById(applicationId)).thenReturn(application);

        // Act and Assert
        mockMvc.perform(get("/api/v1/loan-applications/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.data.application_number").value("APP-001"))
                .andExpect(jsonPath("$.data.status").value("WAITING_RISK_REVIEW"));

        verify(loanApplicationService).findById(applicationId);
    }

    @Test
    void detail_shouldReturnForbiddenWhenServiceRejectsAccess() throws Exception {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        when(loanApplicationService.detail(applicationId))
                .thenThrow(new ForbiddenException("Agent can only view own loan applications"));

        // Act and Assert
        mockMvc.perform(get("/api/v1/loan-applications/{id}/detail", applicationId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Agent can only view own loan applications"));

        verify(loanApplicationService).detail(applicationId);
    }
}
