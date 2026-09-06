package hu.gov.nav.xsdparsertool.web.certificate.api;
import hu.gov.nav.xsdparsertool.core.security.AuthorizationRules;import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;import hu.gov.nav.xsdparsertool.web.certificate.dto.NetworkTestRequest;import hu.gov.nav.xsdparsertool.web.secret.service.SystemSecretService;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.core.env.Environment;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
import java.net.*;import java.net.http.*;import java.time.Duration;import java.util.Map;
/**
 * A webes végpontokat kiszolgáló vezérlő, amely a HTTP-kéréseket a megfelelő alkalmazási szolgáltatásokhoz irányítja.
 *
 * <p>A {@code NetworkTestController} osztály a web modul REST API területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@RestController @RequestMapping("/api/admin/network") @PreAuthorize(AuthorizationRules.ADMIN_ONLY)
public class NetworkTestController{
 private static final Logger log=LoggerFactory.getLogger(NetworkTestController.class);private final Environment env;private final SystemSecretService secrets;private final AuditLogService audit;
 /**
  * Létrehozza a {@code NetworkTestController} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param env a művelet bemeneti {@code env} értéke
  * @param secrets a művelet bemeneti {@code secrets} értéke
  * @param audit a művelet bemeneti {@code audit} értéke
  */
 public NetworkTestController(Environment env,SystemSecretService secrets,AuditLogService audit){this.env=env;this.secrets=secrets;this.audit=audit;}
 /**
  * A {@code test} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a REST API komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param req a művelet bemeneti {@code req} értéke
  * @param auth a művelet bemeneti {@code auth} értéke
  * @return a feldolgozás során felépített kulcs-érték leképezés
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 @PostMapping("/test") public Map<String,Object> test(@RequestBody NetworkTestRequest req,Authentication auth)throws Exception{
  URI uri=URI.create(req.url());HttpClient.Builder b=HttpClient.newBuilder().connectTimeout(Duration.ofMillis(env.getProperty("nav.xsdparsertool.network.connect-timeout-ms",Long.class,10000L))).followRedirects(HttpClient.Redirect.NORMAL);
  if(env.getProperty("nav.xsdparsertool.network.proxy.enabled",Boolean.class,false)){String host=env.getProperty("nav.xsdparsertool.network.proxy.host","");int port=env.getProperty("nav.xsdparsertool.network.proxy.port",Integer.class,8080);if(!host.isBlank())b.proxy(ProxySelector.of(new InetSocketAddress(host,port)));String user=env.getProperty("nav.xsdparsertool.network.proxy.username","");String pass=secrets.read("nav.xsdparsertool.network.proxy.password").orElse("");if(!user.isBlank())b.authenticator(new Authenticator(){  /**
   * A {@code getPasswordAuthentication} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
   *
   * <p>A felhasználói és jogosultsági kontextust szerveroldali kontrollként kezeli; a kliensoldali állapot nem helyettesíti ezt az ellenőrzést.</p>
   * @return a feloldott vagy lekért érték
   */
  protected PasswordAuthentication getPasswordAuthentication(){return new PasswordAuthentication(user,pass.toCharArray());}});}
  long start=System.nanoTime();HttpResponse<Void> response=b.build().send(HttpRequest.newBuilder(uri).timeout(Duration.ofMillis(env.getProperty("nav.xsdparsertool.network.read-timeout-ms",Long.class,30000L))).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding());long ms=(System.nanoTime()-start)/1_000_000;
  String user=auth==null?"system":auth.getName();audit.log("NETWORK_CONNECTION_TEST",user,"SUCCESS","HTTP HEAD request completed");log.info("Hálózati kapcsolat teszt sikeres.");return Map.of("success",true,"status",response.statusCode(),"elapsedMs",ms,"url",uri.toString());
 }
}
