package hu.gov.nav.xsdparsertool.web.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import hu.gov.nav.xsdparsertool.web.security.SecurityMode;
import hu.gov.nav.xsdparsertool.web.security.PasswordPolicyProperties;
import hu.gov.nav.xsdparsertool.web.security.service.DatabaseUserDetailsService;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.security.apikey.ApiKeyAuthenticationFilter;
import hu.gov.nav.xsdparsertool.web.security.apikey.ApiKeySecurityProperties;
import hu.gov.nav.xsdparsertool.web.setup.SetupRequiredFilter;
import hu.gov.nav.xsdparsertool.web.setup.SetupStateService;

/**
 * A web modul kapcsolódó infrastruktúrájának Spring-konfigurációját biztosító típus.
 *
 * <p>A {@code SecurityConfiguration} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(PasswordPolicyProperties.class)
public class SecurityConfiguration {

    private final SecurityModeProperties securityModeProperties;
    private final AuditingAuthenticationHandlers auditingAuthenticationHandlers;
    private final ApiKeySecurityProperties apiKeySecurityProperties;

    /**
     * Létrehozza a {@code SecurityConfiguration} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     * @param auditingAuthenticationHandlers a művelet bemeneti {@code auditingAuthenticationHandlers} értéke
     * @param apiKeySecurityProperties a művelethez szükséges konfigurációs adatok
     */
    public SecurityConfiguration(SecurityModeProperties securityModeProperties,
                                 AuditingAuthenticationHandlers auditingAuthenticationHandlers,
                                 ApiKeySecurityProperties apiKeySecurityProperties) {
        this.securityModeProperties = securityModeProperties;
        this.auditingAuthenticationHandlers = auditingAuthenticationHandlers;
        this.apiKeySecurityProperties = apiKeySecurityProperties;
    }

    /**
     * A {@code passwordEncoder} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @return a művelet feldolgozási eredménye
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * A {@code localAuthenticationProvider} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param userDetailsService a művelet felhasználói kontextusa vagy felhasználóneve
     * @param passwordEncoder a művelet bemeneti {@code passwordEncoder} értéke
     * @param credentialHolder a művelet bemeneti {@code credentialHolder} értéke
     * @return a művelet feldolgozási eredménye
     */
    @Bean
    public DaoAuthenticationProvider localAuthenticationProvider(DatabaseUserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
                                                                    VerifiedLoginCredentialHolder credentialHolder) {
        DaoAuthenticationProvider provider = new CapturingDaoAuthenticationProvider(credentialHolder);
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * A {@code securityFilterChain} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param http a művelet bemeneti {@code http} értéke
     * @param localAuthenticationProvider a művelet bemeneti {@code localAuthenticationProvider} értéke
     * @param setupStateService a feldolgozandó elemek kollekciója
     * @return a művelet feldolgozási eredménye
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider localAuthenticationProvider, SetupStateService setupStateService) throws Exception {
        if (securityModeProperties.getSecurityMode() == SecurityMode.STANDALONE) {
            configureStandalone(http, localAuthenticationProvider, setupStateService);
        } else {
            configureMultiUser(http, localAuthenticationProvider, setupStateService);
        }
        return http.build();
    }

    /**
     * A {@code configureStandalone} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A konfigurációs értékeket a web modul érvényes beállításaihoz igazítja, és az esetleges alapértelmezéseket csak a komponensben definiált szabályok szerint alkalmazza.</p>
     * @param http a művelet bemeneti {@code http} értéke
     * @param localAuthenticationProvider a művelet bemeneti {@code localAuthenticationProvider} értéke
     * @param setupStateService a feldolgozandó elemek kollekciója
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void configureStandalone(HttpSecurity http, DaoAuthenticationProvider localAuthenticationProvider, SetupStateService setupStateService) throws Exception {
        http.authenticationProvider(localAuthenticationProvider);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'self'; base-uri 'self'; form-action 'self'")))
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login.html?sessionExpired=true"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/setup.html",
                                "/js/pages/setup.js",
                                "/styles/setup.css",
                                "/api/setup/**",
                                "/login.html",
                                "/access-denied.html",
                                "/login",
                                "/favicon.ico",
                                "/images/SET_logo.png",
                                "/images/SET_logo_dark.png",
                                "/styles.css",
                                "/styles/**",
                                "/images/**",
                                "/js/**",
                                "/api/security/mode",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(auditingAuthenticationHandlers)
                        .failureHandler(auditingAuthenticationHandlers)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(auditingAuthenticationHandlers)
                        .logoutSuccessUrl("/login.html?logout=true")
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .addFilterBefore(new SetupRequiredFilter(setupStateService), AnonymousAuthenticationFilter.class)
                .addFilterBefore(
                        new ApiKeyAuthenticationFilter(apiKeySecurityProperties),
                        AnonymousAuthenticationFilter.class);
    }

    /**
     * A {@code configureMultiUser} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     * @param http a művelet bemeneti {@code http} értéke
     * @param localAuthenticationProvider a művelet bemeneti {@code localAuthenticationProvider} értéke
     * @param setupStateService a feldolgozandó elemek kollekciója
     * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
     */
    private void configureMultiUser(HttpSecurity http, DaoAuthenticationProvider localAuthenticationProvider, SetupStateService setupStateService) throws Exception {
        http.authenticationProvider(localAuthenticationProvider);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'self'; base-uri 'self'; form-action 'self'")))
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login.html?sessionExpired=true"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/setup.html",
                                "/js/pages/setup.js",
                                "/styles/setup.css",
                                "/api/setup/**",
                                "/login.html",
                                "/access-denied.html",
                                "/login",
                                "/favicon.ico",
                                "/images/SET_logo.png",
                                "/images/SET_logo_dark.png",
                                "/styles.css",
                                "/styles/**",
                                "/images/**",
                                "/js/**",
                                "/api/security/mode",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers("/xml-index-config.html", "/api/xml-index-config/**")
                        .hasAnyRole("ADMIN", "XML_INDEX_CONFIG_MANAGE")
                        .requestMatchers(
                                "/admin.html",
                                "/configuration.html",
                                "/console-log.html",
                                "/audit-log.html",
                                "/users.html",
                                "/user-edit.html",
                                "/api/admin/**",
                                "/api/github-templates/local-delete",
                                "/api/database/**",
                                "/api/proxy-settings/**",
                                "/api/m2m-proxy-settings/**",
                                "/api/users/**",
                                "/h2-console/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(auditingAuthenticationHandlers)
                        .failureHandler(auditingAuthenticationHandlers)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(auditingAuthenticationHandlers)
                        .logoutSuccessUrl("/login.html?logout=true")
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint())
                        .accessDeniedHandler(new JsonAccessDeniedHandler()))
                .addFilterBefore(new SetupRequiredFilter(setupStateService), AnonymousAuthenticationFilter.class)
                .addFilterBefore(
                        new ApiKeyAuthenticationFilter(apiKeySecurityProperties),
                        AnonymousAuthenticationFilter.class);
    }
}
