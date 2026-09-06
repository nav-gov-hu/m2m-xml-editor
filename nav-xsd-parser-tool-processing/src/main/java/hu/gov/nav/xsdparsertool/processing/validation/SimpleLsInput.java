package hu.gov.nav.xsdparsertool.processing.validation;

import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;


/**
 * Egyszerű {@link LSInput} implementáció lokális XSD-erőforrások byte streamként történő átadásához.
 *
 * <p>A validációhoz szükséges publicId, systemId és byte stream értékeket kezeli; a többi
 * LSInput adatforrás ebben az implementációban nem használt.</p>
 */
public class SimpleLsInput implements LSInput {
    private String publicId;
    private String systemId;
    private InputStream byteStream;

/**
 * Létrehozza az LSInput példányt.
 * @param publicId az erőforrás publikus azonosítója
 * @param systemId az erőforrás rendszerazonosítója
 * @param byteStream az XSD tartalmát szolgáltató byte stream
 */
    public SimpleLsInput(String publicId, String systemId, InputStream byteStream) {
        this.publicId = publicId;
        this.systemId = systemId;
        this.byteStream = byteStream;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ez az implementáció karakterfolyamot nem használ, ezért mindig {@code null}.</p>
     */
    @Override public Reader getCharacterStream() { return null; }
    /**
     * {@inheritDoc}
     *
     * <p>A karakterfolyam nincs használatban; a beállítás szándékosan hatástalan.</p>
     */
    @Override public void setCharacterStream(Reader characterStream) { }
    /**
     * {@inheritDoc}
     *
     * @return a lokális XSD-erőforrás byte streamje
     */
    @Override public InputStream getByteStream() { return byteStream; }
    /**
     * {@inheritDoc}
     *
     * @param byteStream a használni kívánt XSD byte stream
     */
    @Override public void setByteStream(InputStream byteStream) { this.byteStream = byteStream; }
    /**
     * {@inheritDoc}
     *
     * <p>Szöveges adatforrás nincs használatban, ezért mindig {@code null}.</p>
     */
    @Override public String getStringData() { return null; }
    /**
     * {@inheritDoc}
     *
     * <p>Szöveges adatforrás nincs használatban; a beállítás szándékosan hatástalan.</p>
     */
    @Override public void setStringData(String stringData) { }
    /**
     * {@inheritDoc}
     *
     * @return a feloldott XSD-erőforrás rendszerazonosítója
     */
    @Override public String getSystemId() { return systemId; }
    /**
     * {@inheritDoc}
     *
     * @param systemId az XSD-erőforrás rendszerazonosítója
     */
    @Override public void setSystemId(String systemId) { this.systemId = systemId; }
    /**
     * {@inheritDoc}
     *
     * @return az XSD-erőforrás publikus azonosítója
     */
    @Override public String getPublicId() { return publicId; }
    /**
     * {@inheritDoc}
     *
     * @param publicId az XSD-erőforrás publikus azonosítója
     */
    @Override public void setPublicId(String publicId) { this.publicId = publicId; }
    /**
     * {@inheritDoc}
     *
     * <p>Külön bázis URI-t ez az objektum nem tárol, ezért {@code null}.</p>
     */
    @Override public String getBaseURI() { return null; }
    /**
     * {@inheritDoc}
     *
     * <p>Külön bázis URI-t ez az objektum nem tárol; a beállítás szándékosan hatástalan.</p>
     */
    @Override public void setBaseURI(String baseURI) { }
    /**
     * {@inheritDoc}
     *
     * @return az erőforrás deklarált kódolása, ebben az implementációban mindig UTF-8
     */
    @Override public String getEncoding() { return "UTF-8"; }
    /**
     * {@inheritDoc}
     *
     * <p>A kódolás rögzített UTF-8; a setter szándékosan hatástalan.</p>
     */
    @Override public void setEncoding(String encoding) { }
    /**
     * {@inheritDoc}
     *
     * @return mindig {@code false}, mert az input nem minősített szövegként kerül átadásra
     */
    @Override public boolean getCertifiedText() { return false; }
    /**
     * {@inheritDoc}
     *
     * <p>A certified-text jelző nincs használatban; a setter szándékosan hatástalan.</p>
     */
    @Override public void setCertifiedText(boolean certifiedText) { }
}
