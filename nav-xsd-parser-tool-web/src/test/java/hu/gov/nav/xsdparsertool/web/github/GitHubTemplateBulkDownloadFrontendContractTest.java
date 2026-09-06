package hu.gov.nav.xsdparsertool.web.github;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubTemplateBulkDownloadFrontendContractTest {

    private static String source;

    @BeforeAll
    static void loadSource() throws Exception {
        ClassPathResource resource = new ClassPathResource("static/js/github-templates.js");
        source = resource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void bulkProgressUsesActualSelectedItemCount() {
        assertTrue(source.contains("const total=items.length;"));
        assertTrue(source.contains("for(let index=0;index<total;index+=1)"));
        assertTrue(source.contains("`${position}/${total} kijelölt release`"));
        assertTrue(source.contains("setProgress(Math.round(position*100/total));"));
    }

    @Test
    void eachSelectedReleaseIsSentAsItsOwnDownloadRequest() {
        assertTrue(source.contains("JSON.stringify({items:[item],force})"));
    }

    @Test
    void oneItemFailureIsAggregatedInsteadOfAbortingTheBulkLoop() {
        assertTrue(source.contains("appendDownloadFailure(aggregate,item,e);"));
        assertTrue(source.indexOf("appendDownloadFailure(aggregate,item,e);")
                < source.indexOf("setProgress(Math.round(position*100/total));"));
    }
    @Test
    void missingTokenDialogCanSaveEncryptedTokenWithoutOpeningConfigurationPage() {
        assertTrue(source.contains("saveGithubTokenFromDialog"));
        assertTrue(source.contains("fetch('/api/admin/configuration'"));
        assertTrue(source.contains("nav.xsdparsertool.github-schema-updater.token"));
        assertTrue(source.contains("confirmedSensitiveKeys:[key]"));
        assertTrue(source.contains("input.value=''"));
    }

    @Test
    void successfulCatalogRefreshOffersEveryMissingReleaseThroughManualDownloadFlow() {
        assertTrue(source.contains("buildLatestMissingDownloadItems"));
        assertTrue(source.contains("const downloadableByRelease=new Map();"));
        assertTrue(source.contains("if(!repository||!tag||Boolean(row?.locallyAvailable)) continue;"));
        assertTrue(source.contains("const key=`${repository}@@${tag}`;"));
        assertTrue(source.contains("downloadableByRelease.set(key,{repository,tag})"));
        assertTrue(source.contains("showRefreshResult(d,state.pendingRefreshDownloadItems.length)"));
        assertTrue(source.contains("state.selected.set(`${item.repository}@@${item.tag}`,item)"));
        assertTrue(source.contains("await download([...state.selected.values()])"));
    }

    @Test
    void localDeleteUsesDedicatedAdminEndpointAndOnlySelectedLocalRows() {
        assertTrue(source.contains("selectedLocalDeleteItems"));
        assertTrue(source.contains("row.locallyAvailable"));
        assertTrue(source.contains("fetch('/api/github-templates/local-delete'"));
        assertTrue(source.contains("JSON.stringify({items})"));
    }

}
