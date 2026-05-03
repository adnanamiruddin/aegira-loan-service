package com.aegira.loan.common.config;

import com.aegira.loan.common.openapi.SnakeCaseOpenApiSchemaCustomiser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI aegiraOpenApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8080").description("Local development"))
                .info(new Info()
                        .title("Aegira Loan Service API")
                        .version("v1")
                        .description("Mini Loan Origination System flow: 1. Agent creates customer. "
                                + "2. Agent creates loan application. 3. Agent submits loan application. "
                                + "4. Backend calculates installment and DSR. 5. Backend checks eligibility. "
                                + "6. Risk Officer reviews application. 7. HO approves if required. "
                                + "DSR calculation uses current DSR = existing monthly installment / monthly income * 100 "
                                + "and projected DSR = (existing monthly installment + new monthly installment) / monthly income * 100. "
                                + "Loan submission can use DATABASE or MOCK data through feature flag loan.data-source.mode. "
                                + "Submit and approval endpoints require Idempotency-Key and use Redis-based idempotency. "
                                + "CustomerID is used as correlationId for debugging and audit traceability."));
    }

    @Bean
    public OpenApiCustomiser snakeCaseSchemaProperties() {
        return new SnakeCaseOpenApiSchemaCustomiser();
    }
}
