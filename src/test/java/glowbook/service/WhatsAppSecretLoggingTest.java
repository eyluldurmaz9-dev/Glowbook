package glowbook.service;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec item L: static guard proving no log statement in the WhatsApp code paths that
 * handle credentials can leak the access token or app secret. Scans every {@code
 * LOGGER.*(...)}/{@code log.*(...)} call in the files that hold those fields and asserts
 * none of their arguments reference the secret variables (or embed a raw "Bearer "
 * value) — so a future edit that accidentally logs a credential fails this test
 * immediately instead of shipping.
 */
class WhatsAppSecretLoggingTest {

    private static final List<String> FILES_HANDLING_SECRETS = List.of(
            "src/main/java/glowbook/service/MetaWhatsAppSender.java",
            "src/main/java/glowbook/controller/WhatsAppWebhookController.java"
    );

    private static final Pattern LOG_CALL = Pattern.compile(
            "(?:LOGGER|log)\\.(?:info|warn|error|debug|trace)\\(([^;]*)\\);", Pattern.DOTALL);

    @Test
    void noLogStatementReferencesTheAccessTokenOrAppSecretVariables() throws Exception {
        for (String path : FILES_HANDLING_SECRETS) {
            String content = Files.readString(new File(path).toPath());
            Matcher matcher = LOG_CALL.matcher(content);
            boolean sawAtLeastOneLogCall = false;
            while (matcher.find()) {
                sawAtLeastOneLogCall = true;
                String logArguments = matcher.group(1);
                assertThat(logArguments)
                        .as("log call in %s must never reference a secret field directly: %s", path, logArguments)
                        .doesNotContain("accessToken")
                        .doesNotContain("appSecret")
                        .doesNotContainIgnoringCase("bearer ");
            }
            assertThat(sawAtLeastOneLogCall).as("expected at least one log call in %s to check", path).isTrue();
        }
    }
}
