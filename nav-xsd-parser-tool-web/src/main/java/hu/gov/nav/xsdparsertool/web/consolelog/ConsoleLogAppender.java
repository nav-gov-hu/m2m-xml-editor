package hu.gov.nav.xsdparsertool.web.consolelog;
import jakarta.annotation.PostConstruct; import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory; import org.springframework.stereotype.Component;
import ch.qos.logback.classic.Logger; import ch.qos.logback.classic.spi.ILoggingEvent; import ch.qos.logback.core.AppenderBase;
/**
 * A web modul alkalmazási területének közös alkalmazási típusa.
 *
 * <p>A {@code ConsoleLogAppender} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Component
public class ConsoleLogAppender extends AppenderBase<ILoggingEvent> {
 private final ConsoleLogService service;
 /**
  * Létrehozza a {@code ConsoleLogAppender} példányt, és eltárolja a működéshez szükséges együttműködő komponenseket.
  *
  * <p>A konstruktor nem indít önálló üzleti folyamatot; a kapott függőségeket a későbbi kérések és szolgáltatási műveletek használják.</p>
  * @param service a művelet bemeneti {@code service} értéke
  */
 public ConsoleLogAppender(ConsoleLogService service){this.service=service;setName("WEB_CONSOLE_BUFFER");}
 /**
  * A {@code attach} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  */
 @PostConstruct public void attach(){setContext((ch.qos.logback.classic.LoggerContext)LoggerFactory.getILoggerFactory());start();((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(this);service.append("INFO",getClass().getName(),"Webes konzolnapló csatlakoztatva.");}
 /**
  * A {@code detach} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  */
 @PreDestroy public void detach(){((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(this);stop();}
 /**
  * A {@code append} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param event a művelet bemeneti {@code event} értéke
  */
 @Override protected void append(ILoggingEvent event){String msg=event.getFormattedMessage(); if(event.getThrowableProxy()!=null) msg += " | " + event.getThrowableProxy().getClassName()+": "+event.getThrowableProxy().getMessage(); service.append(event.getLevel().toString(),event.getLoggerName(),msg);}
}
