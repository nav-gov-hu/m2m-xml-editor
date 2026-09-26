package hu.gov.nav.xsdparsertool.web.githubproxy;

import hu.gov.nav.xsdparsertool.web.githubupdater.domain.GitHubProxySettings;
import hu.gov.nav.xsdparsertool.web.githubupdater.spi.GitHubNetworkSettingsProvider;
import hu.gov.nav.xsdparsertool.web.network.SystemTableM2mProxySettingsService;
import hu.nav.m2m.submitter.domain.ProxySettings;
import org.springframework.stereotype.Component;

/**
 * A GitHub kliens számára az alkalmazás általános hálózati proxy/TLS konfigurációját adja át.
 * A korábbi GitHub-specifikus proxy konfiguráció megmarad, de az updater hálózati forgalma
 * az általános proxybeállítást használja.
 */
@Component
public class SystemTableGitHubNetworkSettingsProvider implements GitHubNetworkSettingsProvider {
    private final SystemTableM2mProxySettingsService proxySettingsService;

    public SystemTableGitHubNetworkSettingsProvider(SystemTableM2mProxySettingsService proxySettingsService) {
        this.proxySettingsService = proxySettingsService;
    }

    @Override
    public GitHubProxySettings load() {
        ProxySettings source = proxySettingsService.getEntity();
        GitHubProxySettings target = new GitHubProxySettings();
        target.setEnabled(source.isEnabled());
        target.setProxyUrl(source.getProxyUrl());
        target.setProxyPort(source.getProxyPort());
        target.setUsername(source.getUsername());
        target.setPassword(source.getPassword());
        target.setSslVerificationDisabled(source.isSslVerificationDisabled());
        target.setTrustStorePath(source.getTrustStorePath());
        target.setTrustStoreType(source.getTrustStoreType());
        target.setTrustStorePassword(source.getTrustStorePassword());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }
}
