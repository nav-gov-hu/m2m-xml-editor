package hu.gov.nav.xsdparsertool.web.github;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubTokenQuickConfigDialogContractTest {

    @Test
    void missingTokenDialogContainsPasswordInputAndSaveAction() throws Exception {
        String html = new ClassPathResource("static/github-templates.html")
                .getContentAsString(StandardCharsets.UTF_8);
        assertTrue(html.contains("id=\"githubTokenQuickInput\""));
        assertTrue(html.contains("type=\"password\""));
        assertTrue(html.contains("autocomplete=\"new-password\""));
        assertTrue(html.contains("id=\"saveGithubTokenQuickButton\""));
        assertTrue(html.contains("Token mentése"));
    }
}
