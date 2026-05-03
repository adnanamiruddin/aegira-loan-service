package com.aegira.loan.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomiser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SnakeCaseOpenApiSchemaCustomiser implements OpenApiCustomiser {
    @Override
    public void customise(OpenAPI openApi) {
        if (openApi == null || openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        Set<Schema<?>> visited = Collections.newSetFromMap(new IdentityHashMap<Schema<?>, Boolean>());
        for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
            renameSchemaFields(schema, visited);
        }
    }

    private void renameSchemaFields(Schema<?> schema, Set<Schema<?>> visited) {
        if (schema == null || !visited.add(schema)) {
            return;
        }
        renameProperties(schema, visited);
        renameRequiredFields(schema);
        renameSchemaFields(schema.getItems(), visited);
    }

    private void renameProperties(Schema<?> schema, Set<Schema<?>> visited) {
        Map<String, Schema> properties = schema.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        Map<String, Schema> renamedProperties = new LinkedHashMap<String, Schema>();
        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            renamedProperties.put(toSnakeCase(entry.getKey()), entry.getValue());
            renameSchemaFields(entry.getValue(), visited);
        }
        schema.setProperties(renamedProperties);
    }

    private void renameRequiredFields(Schema<?> schema) {
        List<String> required = schema.getRequired();
        if (required == null || required.isEmpty()) {
            return;
        }
        List<String> renamedRequired = new ArrayList<String>(required.size());
        for (String field : required) {
            renamedRequired.add(toSnakeCase(field));
        }
        schema.setRequired(renamedRequired);
    }

    static String toSnakeCase(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(current));
            } else {
                result.append(current);
            }
        }
        return result.toString().toLowerCase(Locale.ENGLISH);
    }
}
