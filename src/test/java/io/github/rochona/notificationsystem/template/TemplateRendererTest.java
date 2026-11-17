package io.github.rochona.notificationsystem.template;

import io.github.rochona.notificationsystem.template.service.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

        private TemplateRenderer templateRenderer;

        @BeforeEach
        void setUp() {
            templateRenderer = new TemplateRenderer();
        }

        @Test
        void shouldReplaceSingleVariable() {
            String content = "Hello {{userName}}";

            String result = templateRenderer.render(
                    content,
                    Map.of("userName", "Rochona")
            );

            assertThat(result).isEqualTo("Hello Rochona");
        }

        @Test
        void shouldReplaceMultipleVariables() {
            String content = "Hello {{userName}}, your booking {{bookingId}} is confirmed.";

            String result = templateRenderer.render(
                    content,
                    Map.of(
                            "userName", "Rochona",
                            "bookingId", "BK123"
                    )
            );

            assertThat(result)
                    .isEqualTo("Hello Rochona, your booking BK123 is confirmed.");
        }

        @Test
        void shouldReplaceRepeatedOccurrencesOfSameVariable() {
            String content = "Hello {{userName}}. Welcome back, {{userName}}!";

            String result = templateRenderer.render(
                    content,
                    Map.of("userName", "Rochona")
            );

            assertThat(result)
                    .isEqualTo("Hello Rochona. Welcome back, Rochona!");
        }

        @Test
        void shouldThrowExceptionWhenVariableIsMissing() {
            String content = "Hello {{userName}}";

            assertThatThrownBy(() ->
                    templateRenderer.render(content, Map.of())
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Missing template variable: userName");
        }

        @Test
        void shouldThrowExceptionWhenVariableValueIsNull() {
            String content = "Hello {{userName}}";

            Map<String, Object> variables = new java.util.HashMap<>();
            variables.put("userName", null);

            assertThatThrownBy(() ->
                    templateRenderer.render(content, variables)
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Missing template variable: userName");
        }

        @Test
        void shouldThrowExceptionWhenContentIsNull() {
            assertThatThrownBy(() ->
                    templateRenderer.render(null, Map.of())
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Template content cannot be null");
        }

        @Test
        void shouldInsertSpecialCharactersLiterally() {
            String content = "Your reference is {{reference}}";

            String result = templateRenderer.render(
                    content,
                    Map.of("reference", "$booking\\123")
            );

            assertThat(result).isEqualTo("Your reference is $booking\\123");
        }

        @Test
        void shouldThrowExceptionWhenPlaceholderIsMalformed() {
            String content = "Hello {{userName";

            assertThatThrownBy(() ->
                    templateRenderer.render(content, Map.of("userName", "Rochona"))
            )
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Template contains malformed or unresolved placeholders");
        }

        @Test
        void shouldReturnOriginalContentWhenNoPlaceholdersExist() {
            String content = "Your booking has been confirmed.";

            String result = templateRenderer.render(content, Map.of());

            assertThat(result).isEqualTo(content);
        }
}

