package hu.gov.nav.xsdparsertool.core.security;

/**
 * A Spring Security metódus-jogosultsági kifejezéseinek központi gyűjteménye.
 *
 * <p>A web és szolgáltatási rétegek ezekre a konstansokra hivatkozhatnak a
 * {@code @PreAuthorize} szabályokban, így a szerepkörmátrix egy helyen tartható karban.</p>
 */
public final class AuthorizationRules {
    public static final String AUTHENTICATED_READ = "hasAnyRole('ADMIN','OPERATOR','VIEWER')";
    public static final String OPERATOR_WRITE = "hasAnyRole('ADMIN','OPERATOR')";
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";
    public static final String FILE_DELETE = "hasAnyRole('ADMIN','FILE_DELETE')";
    public static final String XML_INDEX_CONFIG_MANAGE = "hasAnyRole('ADMIN','XML_INDEX_CONFIG_MANAGE')";
    public static final String M2M_SUBMIT = "hasAnyRole('ADMIN','M2M_SUBMITTER')";

    /**
     * Privát konstruktor; a AuthorizationRules segédosztály példányosítását megakadályozza.
     */
    private AuthorizationRules() {
    }
}
