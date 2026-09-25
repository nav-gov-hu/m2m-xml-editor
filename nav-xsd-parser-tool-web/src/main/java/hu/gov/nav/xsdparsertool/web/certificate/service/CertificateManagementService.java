package hu.gov.nav.xsdparsertool.web.certificate.service;

import hu.gov.nav.xsdparsertool.web.support.RepositoryAccess;
import hu.gov.nav.xsdparsertool.web.network.SystemTableM2mProxySettingsService;
import hu.nav.m2m.submitter.domain.ProxySettings;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.CredentialsProviderBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;

import hu.gov.nav.xsdparsertool.web.audit.AuditLogService;
import hu.gov.nav.xsdparsertool.web.certificate.dto.CertificateDto;
import hu.gov.nav.xsdparsertool.web.certificate.entity.TrustedCertificateEntity;
import hu.gov.nav.xsdparsertool.web.certificate.repository.TrustedCertificateRepository;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization; import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import javax.net.ssl.*; import java.io.*; import java.net.Socket; import java.security.*; import java.security.cert.*; import java.time.Instant; import java.util.*;

/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code CertificateManagementService} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class CertificateManagementService {
 private static final Logger log=LoggerFactory.getLogger(CertificateManagementService.class);
 private final TrustedCertificateRepository repository; private final AuditLogService audit; private final TrustedCertificateSslContextInitializer sslContextInitializer; private final SystemTableM2mProxySettingsService proxySettingsService;
 /**
  * Létrehozza a {@code CertificateManagementService} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param repository a művelet bemeneti {@code repository} értéke
  * @param audit a művelet bemeneti {@code audit} értéke
  * @param sslContextInitializer a központi TLS trust context újratöltő komponense
  */
 public CertificateManagementService(TrustedCertificateRepository repository,AuditLogService audit,TrustedCertificateSslContextInitializer sslContextInitializer,SystemTableM2mProxySettingsService proxySettingsService){this.repository=repository;this.audit=audit;this.sslContextInitializer=sslContextInitializer;this.proxySettingsService=proxySettingsService;}
 /**
  * A {@code list} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet eredményeként előállított elemek listája
  */
 public List<CertificateDto> list(){return RepositoryAccess.findAll(repository).stream().sorted(Comparator.comparing(TrustedCertificateEntity::getValidUntil)).map(this::dto).toList();}
 /**
  * A {@code importFile} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
  * @param file a feldolgozásban részt vevő fájl vagy elérési út
  * @param password a művelet bemeneti {@code password} értéke
  * @param username a művelet felhasználói kontextusa vagy felhasználóneve
  * @return a művelet eredményeként előállított elemek listája
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 @Transactional public List<CertificateDto> importFile(MultipartFile file,String password,String username) throws Exception{
  if(file==null||file.isEmpty())throw new IllegalArgumentException("Tanúsítványfájl megadása kötelező.");
  String name=safeImportedFileName(file.getOriginalFilename()); byte[] bytes=file.getBytes(); List<X509Certificate> certs=new ArrayList<>();
  if(name.toLowerCase(Locale.ROOT).endsWith(".jks")||name.toLowerCase(Locale.ROOT).endsWith(".p12")||name.toLowerCase(Locale.ROOT).endsWith(".pfx")){
   String type=name.toLowerCase(Locale.ROOT).endsWith(".jks")?"JKS":"PKCS12"; KeyStore ks=KeyStore.getInstance(type);ks.load(new ByteArrayInputStream(bytes),password==null?new char[0]:password.toCharArray());
   Enumeration<String> aliases=ks.aliases();while(aliases.hasMoreElements()){String alias=aliases.nextElement();java.security.cert.Certificate c=ks.getCertificate(alias);if(c instanceof X509Certificate x)certs.add(x);}
  }else{
   CertificateFactory cf=CertificateFactory.getInstance("X.509");Collection<? extends java.security.cert.Certificate> parsed=cf.generateCertificates(new ByteArrayInputStream(bytes));for(var c:parsed)if(c instanceof X509Certificate x)certs.add(x);
  }
  if(certs.isEmpty())throw new IllegalArgumentException("A fájl nem tartalmaz X.509 tanúsítványt.");
  List<CertificateDto> result=new ArrayList<>();int i=1;for(X509Certificate cert:certs)result.add(dto(storeWithMetadata(cert,name+"-"+i++,null,null,username)));
  scheduleSslContextReload();audit.log("CERTIFICATE_IMPORT",username,"SUCCESS","Tanúsítvány importálva, darabszám="+result.size());log.info("Tanúsítvány importálva. count={}",result.size());return result;
 }
 /**
  * A {@code fetchRemote} művelet lekéri vagy feloldja a kért adatot a rendelkezésre álló forrásokból.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param host a művelet bemeneti {@code host} értéke
  * @param port a művelet bemeneti {@code port} értéke
  * @param alias a művelet bemeneti {@code alias} értéke
  * @param doImport a művelet bemeneti {@code doImport} értéke
  * @param username a művelet felhasználói kontextusa vagy felhasználóneve
  * @return a művelet eredményeként előállított elemek listája
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 public List<CertificateDto> fetchRemote(String host,int port,String alias,boolean doImport,String username) throws Exception{
  if(host==null||host.isBlank())throw new IllegalArgumentException("A host megadása kötelező."); if(port<1||port>65535)throw new IllegalArgumentException("Érvénytelen port.");
  log.info("TLS tanúsítványlánc lekérése indul. port={}",port); List<X509Certificate> certs=capture(host.trim(),port);List<CertificateDto> result=new ArrayList<>();int i=1;
  for(X509Certificate c:certs){if(doImport)result.add(dto(storeWithMetadata(c,(alias==null||alias.isBlank()?host:alias)+"-"+i++,host,port,username)));else result.add(preview(c,(alias==null||alias.isBlank()?host:alias)+"-"+i++,host,port));}
  if(doImport)scheduleSslContextReload();audit.log(doImport?"REMOTE_CERTIFICATE_IMPORT":"REMOTE_CERTIFICATE_FETCH",username,"SUCCESS","TLS tanúsítványlánc feldolgozva, darabszám="+result.size());log.info("TLS tanúsítványlánc lekérése sikeres. port={}, count={}, imported={}",port,result.size(),doImport);return result;
 }
 /**
  * A {@code delete} művelet lezárja, felszabadítja vagy eltávolítja a kijelölt erőforrást a vonatkozó szabályok szerint.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param id a célobjektum vagy erőforrás azonosítója
  * @param username a művelet felhasználói kontextusa vagy felhasználóneve
  */
 @Transactional public void delete(Long id,String username){TrustedCertificateEntity e=RepositoryAccess.findById(repository, id).orElseThrow(()->new IllegalArgumentException("A tanúsítvány nem található."));repository.delete(e);scheduleSslContextReload();audit.log("CERTIFICATE_DELETE",username,"SUCCESS","Tanúsítvány törölve: "+e.getSha256Fingerprint());log.info("Tanúsítvány törölve. fingerprint={}, user={}",e.getSha256Fingerprint(),username);}

 /**
  * A tanúsítványtár módosítása után a tranzakció sikeres commitját követően újraépíti a JVM TLS trust contextjét.
  * Tranzakción kívüli import esetén az újratöltés azonnal megtörténik.
  */
 private void scheduleSslContextReload(){
  if(TransactionSynchronizationManager.isActualTransactionActive()){
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
    @Override public void afterCommit(){sslContextInitializer.reload();}
   });
  }else{
   sslContextInitializer.reload();
  }
 }

 /**
  * A {@code safeImportedFileName} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A fájl- és útvonalkezelést a konfigurált tárhely és a biztonsági korlátok figyelembevételével végzi; a hívó számára csak a feloldott eredményt adja tovább.</p>
  * @param raw a művelet bemeneti {@code raw} értéke
  * @return a művelet feldolgozási eredménye
  */
 private static String safeImportedFileName(String raw){
  String value=raw==null?"certificate":raw;
  value=value.replace('\\','/');int slash=value.lastIndexOf('/');if(slash>=0)value=value.substring(slash+1);
  String sanitized=value.replaceAll("[^A-Za-z0-9._-]","_");
  if(sanitized.isBlank())return "certificate";
  return sanitized.length()>128?sanitized.substring(0,128):sanitized;
 }
 /**
  * A {@code capture} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param host a művelet bemeneti {@code host} értéke
  * @param port a művelet bemeneti {@code port} értéke
  * @return a művelet eredményeként előállított elemek listája
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 private List<X509Certificate> capture(String host,int port)throws Exception{
  List<X509Certificate> captured=new ArrayList<>();
  X509TrustManager tm=new X509TrustManager(){
   public X509Certificate[] getAcceptedIssuers(){return new X509Certificate[0];}
   public void checkClientTrusted(X509Certificate[] c,String a){}
   public void checkServerTrusted(X509Certificate[] c,String a){captured.addAll(Arrays.asList(c));}
  };
  SSLContext ctx=SSLContext.getInstance("TLS");ctx.init(null,new TrustManager[]{tm},new SecureRandom());
  ProxySettings settings=proxySettingsService.getEntity();
  RequestConfig.Builder requestConfig=RequestConfig.custom()
          .setConnectTimeout(Timeout.ofSeconds(10))
          .setConnectionRequestTimeout(Timeout.ofSeconds(10))
          .setResponseTimeout(Timeout.ofSeconds(10));
  var clientBuilder=HttpClients.custom();
  if(settings!=null&&settings.isEnabled()){
   String proxyHost=normalizeProxyHost(settings.getProxyUrl());
   Integer proxyPort=settings.getProxyPort();
   if(proxyHost==null||proxyHost.isBlank()||proxyPort==null||proxyPort<1||proxyPort>65535)throw new IllegalStateException("A tanúsítvány lekéréséhez beállított általános proxy host/port hibás.");
   requestConfig.setProxy(new HttpHost("http",proxyHost,proxyPort));
   if(settings.getUsername()!=null&&!settings.getUsername().isBlank()&&settings.getPassword()!=null&&!settings.getPassword().isBlank()){
    clientBuilder.setDefaultCredentialsProvider(CredentialsProviderBuilder.create()
            .add(new AuthScope(proxyHost,proxyPort),new UsernamePasswordCredentials(settings.getUsername(),settings.getPassword().toCharArray()))
            .build());
    log.info("TLS tanúsítványlánc lekérése az általános, hitelesített proxyn keresztül. proxyPort={}",proxyPort);
   }else{
    log.info("TLS tanúsítványlánc lekérése az általános proxyn keresztül. proxyPort={}",proxyPort);
   }
  }
  var connectionManager=PoolingHttpClientConnectionManagerBuilder.create()
          .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                  .setSslContext(ctx)
                  .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                  .build())
          .build();
  clientBuilder.setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig.build());
  String url="https://"+host+":"+port+"/";
  try(CloseableHttpClient client=clientBuilder.build()){
   HttpGet request=new HttpGet(url);
   request.addHeader("User-Agent","M2M-XML-EDITOR-CERTIFICATE-FETCH");
   client.execute(request,response->{
    if(response.getCode()==407){
     String challenge=response.getFirstHeader("Proxy-Authenticate")==null?"":response.getFirstHeader("Proxy-Authenticate").getValue();
     throw new IOException("A proxy hitelesítést kért a tanúsítvány lekérésekor (HTTP 407, Proxy-Authenticate="+challenge+").");
    }
    return null;
   });
  }catch(Exception ex){
   if(captured.isEmpty())throw ex;
   log.debug("A TLS lánc lekérése után a HTTP kérés hibával zárult, de a tanúsítványlánc már rendelkezésre áll: {}",ex.getMessage());
  }
  if(captured.isEmpty())throw new IOException("A távoli szolgáltatás nem adott vissza X.509 tanúsítványláncot.");
  return captured;
 }

 private String normalizeProxyHost(String raw){
  if(raw==null||raw.isBlank())return null;
  String value=raw.trim();
  try{java.net.URI uri=value.contains("://")?java.net.URI.create(value):java.net.URI.create("http://"+value);if(uri.getHost()!=null&&!uri.getHost().isBlank())return uri.getHost();}catch(IllegalArgumentException ignored){}
  String host=value.replace("http://","").replace("https://","");int slash=host.indexOf('/');if(slash>=0)host=host.substring(0,slash);int colon=host.indexOf(':');if(colon>=0)host=host.substring(0,colon);return host;
 }
 /**
  * A {@code safeStoredCertificateText} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param raw a művelet bemeneti {@code raw} értéke
  * @param maxLength a művelet bemeneti {@code maxLength} értéke
  * @return a művelet feldolgozási eredménye
  */
 private static String safeStoredCertificateText(String raw,int maxLength){if(raw==null||raw.isBlank())return null;String value=raw.trim();String sanitized=value.replaceAll("[\r\n\u0000]","");if(!sanitized.equals(value)||sanitized.length()>maxLength)throw new IllegalArgumentException("Érvénytelen tanúsítvány metaadat.");return sanitized;}
 /**
  * A {@code storeWithMetadata} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param cert a művelet bemeneti {@code cert} értéke
  * @param alias a művelet bemeneti {@code alias} értéke
  * @param host a művelet bemeneti {@code host} értéke
  * @param port a művelet bemeneti {@code port} értéke
  * @param username a művelet felhasználói kontextusa vagy felhasználóneve
  * @return a művelet feldolgozási eredménye
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 private TrustedCertificateEntity storeWithMetadata(X509Certificate cert,String alias,String host,Integer port,String username)throws Exception{String fp=fingerprint(cert);Optional<TrustedCertificateEntity> existing=repository.findBySha256Fingerprint(fp);if(existing.isPresent())return existing.get();String storedAlias=safeStoredCertificateText(alias,255);if(storedAlias==null)storedAlias="certificate-"+fp.replace(":","").substring(0,12);String storedHost=safeStoredCertificateText(host,253);return storeCertificate(cert,fp,storedAlias,storedHost,port,username);}
 /**
  * A {@code storeCertificate} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param cert a tárolandó X.509 tanúsítvány
  * @param fp a tanúsítvány SHA-256 ujjlenyomata
  * @param alias a nem null tanúsítvány alias
  * @param sourceHost a távoli forrás host neve, ha ismert
  * @param sourcePort a távoli forrás portja, ha ismert
  * @param username a művelet felhasználói kontextusa vagy felhasználóneve
  * @return a művelet feldolgozási eredménye
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 private TrustedCertificateEntity storeCertificate(X509Certificate cert,String fp,String alias,String sourceHost,Integer sourcePort,String username)throws Exception{TrustedCertificateEntity e=new TrustedCertificateEntity();e.setAlias(alias);e.setSubjectDn(cert.getSubjectX500Principal().getName());e.setIssuerDn(cert.getIssuerX500Principal().getName());e.setSerialNumber(cert.getSerialNumber().toString(16));e.setSha256Fingerprint(fp);e.setValidFrom(cert.getNotBefore().toInstant());e.setValidUntil(cert.getNotAfter().toInstant());e.setSourceHost(sourceHost);e.setSourcePort(sourcePort);e.setStatus(cert.getNotAfter().toInstant().isBefore(Instant.now())?"EXPIRED":cert.getNotAfter().toInstant().isBefore(Instant.now().plusSeconds(30L*86400))?"EXPIRING":"VALID");e.setCertificateDer(cert.getEncoded());e.setCreatedAt(Instant.now());e.setCreatedBy(username);return repository.save(e);}
 /**
  * A {@code preview} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param c a művelet bemeneti {@code c} értéke
  * @param alias a művelet bemeneti {@code alias} értéke
  * @param host a művelet bemeneti {@code host} értéke
  * @param port a művelet bemeneti {@code port} értéke
  * @return a művelet feldolgozási eredménye
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 private CertificateDto preview(X509Certificate c,String alias,String host,Integer port)throws Exception{return new CertificateDto(null,alias,c.getSubjectX500Principal().getName(),c.getIssuerX500Principal().getName(),c.getSerialNumber().toString(16),fingerprint(c),c.getNotBefore().toInstant(),c.getNotAfter().toInstant(),host,port,"PREVIEW",null,null);}
 /**
  * A {@code fingerprint} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param c a művelet bemeneti {@code c} értéke
  * @return a művelet feldolgozási eredménye
  * @throws Exception ha a művelet a deklarált technikai vagy üzleti feltétel miatt nem hajtható végre
  */
 private String fingerprint(X509Certificate c)throws Exception{byte[] d=MessageDigest.getInstance("SHA-256").digest(c.getEncoded());StringBuilder s=new StringBuilder();for(byte b:d){if(s.length()>0)s.append(':');s.append(String.format("%02X",b));}return s.toString();}
 /**
  * A {@code dto} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param e a művelet bemeneti {@code e} értéke
  * @return a művelet feldolgozási eredménye
  */
 private CertificateDto dto(TrustedCertificateEntity e){return new CertificateDto(e.getId(),e.getAlias(),e.getSubjectDn(),e.getIssuerDn(),e.getSerialNumber(),e.getSha256Fingerprint(),e.getValidFrom(),e.getValidUntil(),e.getSourceHost(),e.getSourcePort(),e.getStatus(),e.getCreatedAt(),e.getCreatedBy());}
}
