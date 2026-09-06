package hu.gov.nav.xsdparsertool.web.consolelog;
import java.time.Instant; import java.util.*; import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
/**
 * A kapcsolódó webes üzleti vagy alkalmazási folyamatokat összefogó szolgáltatás.
 *
 * <p>A {@code ConsoleLogService} osztály a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
 */
@Service
public class ConsoleLogService {
 /**
  * A web modul alkalmazási területének közös alkalmazási típusa.
  *
  * <p>A {@code Entry} rekord a web modul alkalmazási területéhez tartozik. A típus a réteg felelősségi határain belül tartja a hozzá tartozó adatokat és műveleteket, és nem helyettesíti az alacsonyabb szintű modulok üzleti szolgáltatásait.</p>
  */
 public record Entry(long sequence, Instant timestamp, String level, String logger, String message) {}
 private final Deque<Entry> buffer=new ArrayDeque<>(); private final List<SseEmitter> emitters=new CopyOnWriteArrayList<>(); private long sequence=0; private final int max=3000;
 /**
  * A {@code snapshot} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param limit a lapozási vagy mennyiségi korlátot meghatározó érték
  * @return a művelet eredményeként előállított elemek listája
  */
 public synchronized List<Entry> snapshot(int limit){if(limit<1||limit>3000)throw new IllegalArgumentException("A limit értéke 1 és 3000 közötti lehet.");int currentSize=buffer.size();int skip=currentSize>limit?currentSize-limit:0;return buffer.stream().skip(skip).toList();}
 /**
  * A {@code subscribe} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @return a művelet feldolgozási eredménye
  */
 public SseEmitter subscribe(){SseEmitter e=new SseEmitter(0L); emitters.add(e); e.onCompletion(()->emitters.remove(e)); e.onTimeout(()->emitters.remove(e)); try{e.send(SseEmitter.event().name("connected").data(Map.of("status","connected")));}catch(Exception ex){emitters.remove(e);} return e;}
 /**
  * A {@code append} művelet a komponens felelősségi körébe tartozó feldolgozási lépést hajtja végre.
  *
  * <p>A művelet a alkalmazási komponens hívási kontextusában fut; az eredményét a következő réteg közvetlenül használhatja, miközben a komponens saját ellenőrzési és fallback szabályai érvényben maradnak.</p>
  * @param level a művelet bemeneti {@code level} értéke
  * @param logger a művelet bemeneti {@code logger} értéke
  * @param message a művelet bemeneti {@code message} értéke
  */
 public void append(String level,String logger,String message){Entry entry; synchronized(this){entry=new Entry(++sequence,Instant.now(),level,logger,message);buffer.addLast(entry);while(buffer.size()>max)buffer.removeFirst();} for(SseEmitter e:emitters){try{e.send(SseEmitter.event().name("log").id(Long.toString(entry.sequence())).data(entry));}catch(Exception ex){emitters.remove(e);e.complete();}}}
}
