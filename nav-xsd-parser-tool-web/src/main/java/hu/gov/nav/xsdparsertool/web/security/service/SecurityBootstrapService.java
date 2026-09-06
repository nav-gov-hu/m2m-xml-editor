package hu.gov.nav.xsdparsertool.web.security.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.security.SecurityBootstrapProperties;
import hu.gov.nav.xsdparsertool.web.security.SecurityMode;
import hu.gov.nav.xsdparsertool.web.security.SecurityModeProperties;
import hu.gov.nav.xsdparsertool.web.security.entity.AppRoleEntity;
import hu.gov.nav.xsdparsertool.web.security.entity.AppUserEntity;
import hu.gov.nav.xsdparsertool.web.security.repository.AppRoleRepository;
import hu.gov.nav.xsdparsertool.web.security.repository.AppUserRepository;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code SecurityBootstrapService} osztály a web modul biztonsági és jogosultságkezelési területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
@Order(50)
public class SecurityBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityBootstrapService.class);
    private static final List<String> DEFAULT_ROLES = List.of("ADMIN", "OPERATOR", "VIEWER", "FILE_DELETE", "M2M_SUBMITTER", "XML_INDEX_CONFIG_MANAGE");

    private final SecurityModeProperties securityModeProperties;
    private final SecurityBootstrapProperties bootstrapProperties;
    private final AppRoleRepository roleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    /**
     * Létrehozza a {@code SecurityBootstrapService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
     *
     * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
     * @param securityModeProperties a művelethez szükséges konfigurációs adatok
     * @param bootstrapProperties a művelethez szükséges konfigurációs adatok
     * @param roleRepository a művelet bemeneti {@code roleRepository} értéke
     * @param userRepository a művelet felhasználói kontextusa vagy felhasználóneve
     * @param passwordEncoder a művelet bemeneti {@code passwordEncoder} értéke
     * @param auditLogService a művelet bemeneti {@code auditLogService} értéke
     */
    public SecurityBootstrapService(
            SecurityModeProperties securityModeProperties,
            SecurityBootstrapProperties bootstrapProperties,
            AppRoleRepository roleRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {
        this.securityModeProperties = securityModeProperties;
        this.bootstrapProperties = bootstrapProperties;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    /**
     * A {@code run} művelet elindítja vagy végrehajtja a kapcsolódó alkalmazási folyamatot.
     *
     * <p>A művelet a biztonsági és jogosultságkezelési komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
     * @param args a művelet bemeneti {@code args} értéke
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureDefaultRoles();
        if (securityModeProperties.getSecurityMode() != SecurityMode.MULTI_USER) {
            return;
        }
        if (userRepository.count() > 0) {
            return;
        }
        if (!bootstrapProperties.isEnabled()) {
            log.warn("MULTI_USER mod aktiv, de nincs felhasznalo es a bootstrap admin nincs engedelyezve.");
            auditLogService.log("SECURITY_BOOTSTRAP_SKIPPED", "system", "WARNING",
                    "MULTI_USER modban nincs felhasznalo, a bootstrap admin nincs engedelyezve.");
            return;
        }
        AppRoleEntity adminRole = roleRepository.findByRoleCode("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN szerepkor nem talalhato."));

        AppUserEntity admin = new AppUserEntity();
        admin.setUsername(bootstrapProperties.getUsername());
        admin.setPasswordHash(passwordEncoder.encode(bootstrapProperties.getPassword()));
        admin.setDisplayName(bootstrapProperties.getDisplayName());
        admin.setEmail(bootstrapProperties.getEmail());
        admin.setEnabled(true);
        admin.setPasswordChangeRequired(true);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setCreatedBy("system");
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.warn("Bootstrap admin felhasznalo letrehozva: {}. Az elso belepes utan jelszocsere javasolt.",
                bootstrapProperties.getUsername());
        auditLogService.log("SECURITY_BOOTSTRAP_ADMIN_CREATED", bootstrapProperties.getUsername(), "SUCCESS",
                "Bootstrap admin felhasznalo letrejott.");
    }

    /**
     * A {@code ensureDefaultRoles} művelet ellenőrzi a művelethez tartozó feltételeket és invariánsokat.
     *
     * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
     */
    private void ensureDefaultRoles() {
        for (String roleCode : DEFAULT_ROLES) {
            roleRepository.findByRoleCode(roleCode).orElseGet(() -> {
                AppRoleEntity role = new AppRoleEntity();
                role.setRoleCode(roleCode);
                role.setRoleName(roleCode);
                return roleRepository.save(role);
            });
        }
    }
}
