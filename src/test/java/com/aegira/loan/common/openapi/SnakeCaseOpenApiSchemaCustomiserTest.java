package com.aegira.loan.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnakeCaseOpenApiSchemaCustomiserTest {
    private final SnakeCaseOpenApiSchemaCustomiser customiser = new SnakeCaseOpenApiSchemaCustomiser();

    @Test
    void convertsSchemaPropertiesAndRequiredFieldsToSnakeCase() {
        Schema<?> schema = new Schema<Object>()
                .addProperties("customerId", new Schema<Object>().type("string"))
                .addProperties("requestedAmount", new Schema<Object>().type("number"));
        schema.setRequired(Arrays.asList("customerId", "requestedAmount"));
        OpenAPI openApi = new OpenAPI()
                .components(new Components().addSchemas("LoanApplicationRequest", schema));

        customiser.customise(openApi);

        assertTrue(schema.getProperties().containsKey("customer_id"));
        assertTrue(schema.getProperties().containsKey("requested_amount"));
        assertFalse(schema.getProperties().containsKey("customerId"));
        assertTrue(schema.getRequired().contains("customer_id"));
        assertTrue(schema.getRequired().contains("requested_amount"));
    }

    @Test
    void convertsNestedObjectAndArrayItemSchemas() {
        Schema<?> nested = new Schema<Object>().addProperties("monthlyIncome", new Schema<Object>().type("number"));
        Schema<?> item = new Schema<Object>().addProperties("createdAt", new Schema<Object>().type("string"));
        Schema<?> root = new Schema<Object>()
                .addProperties("customerDetail", nested)
                .addProperties("approvalHistories", new ArraySchema().items(item));
        OpenAPI openApi = new OpenAPI()
                .components(new Components().addSchemas("LoanApplicationDetailResponse", root));

        customiser.customise(openApi);

        assertTrue(root.getProperties().containsKey("customer_detail"));
        assertTrue(root.getProperties().containsKey("approval_histories"));
        assertTrue(nested.getProperties().containsKey("monthly_income"));
        assertTrue(item.getProperties().containsKey("created_at"));
    }

    @Test
    void ignoresMissingComponents() {
        assertDoesNotThrow(() -> customiser.customise(new OpenAPI()));
    }
}
