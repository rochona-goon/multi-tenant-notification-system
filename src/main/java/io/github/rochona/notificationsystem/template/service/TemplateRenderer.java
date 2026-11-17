package io.github.rochona.notificationsystem.template.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_]*)}}");

    public String render(String content, Map<String, Object> variables) {
        if (content == null) {
            throw new IllegalArgumentException("Template content cannot be null");
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);

            if (variables == null || !variables.containsKey(variableName)
                    || variables.get(variableName) == null) {
                throw new IllegalArgumentException(
                        "Missing template variable: " + variableName);
            }

            String replacement = String.valueOf(variables.get(variableName));

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement)
            );
        }

        matcher.appendTail(result);

        String renderedContent = result.toString();

        // Reject unresolved or malformed placeholders.
        if (renderedContent.contains("{{") || renderedContent.contains("}}")) {
            throw new IllegalArgumentException(
                    "Template contains malformed or unresolved placeholders");
        }

        return renderedContent;
    }
}
