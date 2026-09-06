package hu.gov.nav.xsdparsertool.print.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import hu.gov.nav.xsdparsertool.core.model.bundle.SchemaBundle;
import hu.gov.nav.xsdparsertool.core.model.form.*;
import hu.gov.nav.xsdparsertool.core.model.processing.ProcessingResult;
import hu.gov.nav.xsdparsertool.print.model.PrintOptions;
import hu.gov.nav.xsdparsertool.processing.form.DefaultFormDataBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.DefaultFormDefinitionBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.FormDataBuilderService;
import hu.gov.nav.xsdparsertool.processing.form.FormDefinitionBuilderService;
import hu.gov.nav.xsdparsertool.processing.service.DefaultXmlProcessingService;
import hu.gov.nav.xsdparsertool.processing.service.XmlProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Az {@link XmlPrintService} alapértelmezett megvalósítása, amely XML dokumentumból
 * nyomtatásra optimalizált HTML- és PDF-kimenetet készít.
 *
 * <p>A feldolgozási folyamat először az XML-hez tartozó dokumentumdefiníciót és
 * {@link SchemaBundle} objektumot oldja fel, majd ezekből {@link FormDefinition} és
 * {@link FormData} modellt épít. A HTML-kimenet ezeknek a modelleknek a felhasználásával készül;
 * a PDF-generálás ugyanezt a HTML-t alakítja át PDF dokumentummá az OpenHTMLToPDF segítségével.</p>
 *
 * <p>A generált dokumentum metaadatai között szerepel az XML típusa, elérési útja,
 * SHA3-512 lenyomata, létrehozási és nyomtatási ideje, valamint az alkalmazás verziója.
 * A megjelenítés A4-es nyomtatásra és többoldalas táblázatok kezelésére optimalizált.</p>
 */
public class DefaultXmlPrintService implements XmlPrintService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultXmlPrintService.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withLocale(Locale.ROOT)
                    .withZone(ZoneId.systemDefault());

    private final XmlProcessingService xmlProcessingService = new DefaultXmlProcessingService();
    private final FormDefinitionBuilderService formDefinitionBuilderService = new DefaultFormDefinitionBuilderService();
    private final FormDataBuilderService formDataBuilderService = new DefaultFormDataBuilderService();

    /**
     * {@inheritDoc}
     *
     * <p>Ha a {@link PrintOptions#getUiModelOverrideFile()} egy létező, szabályos és olvasható
     * állományra mutat, akkor az felülírja a feldolgozás során feloldott UIModel állományt.
     * A metódus ezt követően felépíti az űrlapdefiníciót és az XML-hez tartozó adatmodellt,
     * majd elkészíti a teljes HTML-dokumentumot.</p>
     */
    @Override
    public String generateHtml(Path xmlFile,
                               Path schemaRootDir,
                               Path generalXsdDir,
                               Path uiModelDir,
                               PrintOptions options) {
        PrintOptions effectiveOptions = options == null ? new PrintOptions() : options;

        ProcessingResult inspection =
                xmlProcessingService.inspect(xmlFile, schemaRootDir, generalXsdDir, uiModelDir);

        SchemaBundle schemaBundle = inspection.getSchemaBundle();
        Path uiModelOverrideFile = effectiveOptions.getUiModelOverrideFile();

        if (isRegularReadableFile(uiModelOverrideFile)) {
            schemaBundle.setUiModelFile(uiModelOverrideFile.toAbsolutePath().normalize());
            LOGGER.info("Using UI model override for print generation.");
        }



        FormDefinition formDefinition =
                formDefinitionBuilderService.build(inspection.getDocumentDefinition(), schemaBundle);
        FormData formData = formDataBuilderService.build(formDefinition, xmlFile);

        return renderHtml(formDefinition, formData, schemaBundle, effectiveOptions, xmlFile);
    }

    /**
     * Ellenőrzi, hogy a megadott útvonal közvetlenül elérhető, szabályos és olvasható fájl-e.
     *
     * <p>Az ellenőrzés nem követ szimbolikus linket. Hozzáférési hiba esetén a metódus
     * naplózza a problémát és sikertelen ellenőrzéssel tér vissza.</p>
     *
     * @param file az ellenőrizendő fájl elérési útja
     * @return {@code true}, ha a fájl szabályos és olvasható; egyébként {@code false}
     */
    private boolean isRegularReadableFile(Path file) {
        if (file == null) {
            return false;
        }

        try {
            Path normalizedFile = file.toAbsolutePath().normalize();
            return Files.isRegularFile(normalizedFile, LinkOption.NOFOLLOW_LINKS)
                    && Files.isReadable(normalizedFile);
        } catch (SecurityException ex) {
            LOGGER.warn("Could not access UI model override file.", ex);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>A PDF-generálás a {@link #generateHtml(Path, Path, Path, Path, PrintOptions)} által
     * előállított HTML-t használja bemenetként. A renderelés gyors módban, közvetlenül
     * memóriába írt kimenettel történik.</p>
     */
    @Override
    public byte[] generatePdf(Path xmlFile,
                              Path schemaRootDir,
                              Path generalXsdDir,
                              Path uiModelDir,
                              PrintOptions options) {
        String html = generateHtml(xmlFile, schemaRootDir, generalXsdDir, uiModelDir, options);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF from rendered HTML", e);
        }
    }
    /**
     * A feldolgozott űrlapdefinícióból és űrlapadatból összeállítja a teljes nyomtatható HTML-dokumentumot.
     *
     * <p>A metódus létrehozza a dokumentum metaadat-fejlécét, bejárja a füleket, szekciókat és
     * sorokat, kezeli az ismétlődő sorpéldányokat, valamint beágyazza az A4-es nyomtatáshoz szükséges
     * CSS-szabályokat.</p>
     *
     * @param formDefinition a megjelenítendő űrlap szerkezeti definíciója
     * @param formData az XML-ből felépített űrlapadat
     * @param schemaBundle a feldolgozáshoz feloldott séma- és UIModel-információk
     * @param options a nyomtatási beállítások
     * @param xmlFile a forrás XML állomány elérési útja
     * @return a teljes HTML-dokumentum
     */
    private String renderHtml(FormDefinition formDefinition,
                              FormData formData,
                              SchemaBundle schemaBundle,
                              PrintOptions options,
                              Path xmlFile) {

        String documentTitle = firstNonBlank(formDefinition.getTitle(), schemaBundle.getDocumentType(), "Nyomtatási kép");
        String documentType = firstNonBlank(schemaBundle.getDocumentType(), "ismeretlen");
        String xmlPath = xmlFile == null ? "-" : xmlFile.toAbsolutePath().normalize().toString();
        String xmlHash = computeSha3_512(xmlFile);
        String createdAt = resolveCreatedAt(xmlFile);
        String printedAt = formatInstant(Instant.now());
        String appVersion = firstNonBlank(options.getAppVersion(), "1.0.0-SNAPSHOT-20260420103318");

        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>")
                .append("<html lang=\"hu\">")
                .append("<head>")
                .append("<meta charset=\"utf-8\" />")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\" />")
                .append("<title>").append(escape(documentTitle)).append("</title>")
                .append("<style>")
                .append("@page{size:A4 portrait;margin:12mm;}")
                .append("*{box-sizing:border-box;}")
                .append("html,body{margin:0;padding:0;background:#fff;color:#111;font-family:Arial,Helvetica,sans-serif;font-size:10pt;line-height:1.35;}")
                .append("body{padding:0;}")
                .append(".document{width:100%;max-width:186mm;margin:0 auto;}")
                .append(".doc-title{font-size:14pt;font-weight:700;text-align:center;margin:0 0 4mm 0;}")
                .append(".doc-meta{margin:0 0 7mm 0;font-size:10pt;}")
                .append(".meta-line{margin:0 0 1.5mm 0;}")
                .append(".meta-label{font-weight:700;text-decoration:underline;}")
                .append(".meta-value{word-break:break-all;overflow-wrap:anywhere;}")
                .append(".section{margin:0 0 6mm 0;page-break-inside:auto;}")
                .append(".section-title{font-size:12pt;font-weight:700;margin:0 0 2mm 0;padding-bottom:1mm;border-bottom:1px solid #666;}")
                //.append(".row-block{margin:0 0 4mm 0;page-break-inside:avoid;}")
                ///////////////////////
                .append(".row-block{margin:0 0 4mm 0;}")
                .append(".print-table tr{page-break-inside:avoid;}")
                .append(".visual-table{width:100%;border-collapse:collapse;table-layout:fixed;margin:0;}")
                .append(".visual-table thead{display:table-header-group;}")
                .append(".visual-table tr{page-break-inside:avoid;break-inside:avoid;}")
                .append(".visual-table th,.visual-table td{border:1px solid #a9a9a9;padding:1.6mm 2mm;vertical-align:top;}")
                .append(".visual-table th{font-weight:700;background:#eef3f9;text-align:left;}")
                .append(".visual-row-no{width:10%;text-align:right;white-space:nowrap;}")
                .append(".visual-description{width:65%;overflow-wrap:anywhere;}")
                .append(".visual-value{width:25%;overflow-wrap:anywhere;white-space:pre-wrap;}")
                .append(".section-block{page-break-inside:avoid;}")
                .append(".section-title{page-break-after:avoid;}")
                //////////////////////
                .append(".row-title{font-weight:700;margin:0 0 1.5mm 0;color:#222;}")
                .append(".print-table{width:100%;border-collapse:collapse;table-layout:fixed;margin:0;}")
                .append(".print-table td{border:1px solid #a9a9a9;padding:2mm;vertical-align:top;}")
                .append(".label-cell{width:25%;font-weight:700;white-space:normal;word-break:break-all;overflow-wrap:anywhere;}")
                .append(".value-cell{width:25%;word-break:break-word;overflow-wrap:anywhere;white-space:pre-wrap;}")
                .append(".full-label-cell{width:25%;font-weight:700;white-space:normal;word-break:break-all;overflow-wrap:anywhere;}")
                .append(".full-value-cell{width:75%;word-break:break-word;overflow-wrap:anywhere;white-space:pre-wrap;}")
                .append(".empty-value{color:#888;}")
                .append("@media print{button{display:none !important;}}")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class=\"document\">");

        html.append("<div class=\"doc-title\">")
                .append(escape(documentTitle))
                .append("</div>");

        html.append("<div class=\"doc-meta\">")
                .append(metaLine("Dokumentum típusa:", documentType))
                .append(metaLine("XML fájl elérési útja:", xmlPath))
                .append(metaLine("XML állomány SHA3-512 hash lenyomata:", xmlHash))
                .append(metaLine("Létrehozás dátuma:", createdAt))
                .append(metaLine("Nyomtatás dátuma:", printedAt))
                .append(metaLine("Alkalmazás verziója:", appVersion))
                .append("</div>");

        for (FormTabDefinition tab : formDefinition.getTabs()) {
            for (FormSectionDefinition section : tab.getSections()) {
                html.append("<section class=\"section\">")
                        .append("<div class=\"section-block\">")
                        .append("<div class=\"section-title\">")
                        .append(escape(firstNonBlank(section.getTitle(), section.getId(), "Szekció")))
                        .append("</div>");

                for (FormRowDefinition row : section.getRows()) {
                    if (row.isRepeatable()) {
                        List<FormRowInstance> instances = formData.getRowInstancesByRowId().get(row.getId());
                        if (instances == null || instances.isEmpty()) {
                            continue;
                        }
                        int idx = 1;
                        for (FormRowInstance instance : instances) {
                            appendRow(html, row, row.getTitle() + " #" + idx++, instance, options, null);
                        }
                    } else {
                        appendRow(html, row, row.getTitle(), null, options, formData);
                    }
                }

                html.append("</div>") // <<< section-block zárása
                .append("</section>");
            }
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    /**
     * Egy címke-érték párból a dokumentum metaadat-fejlécében használható HTML-sort készít.
     *
     * @param label a metaadat megnevezése
     * @param value a metaadat értéke
     * @return a HTML-formátumú metaadatsor
     */
    private String metaLine(String label, String value) {
        return "<div class=\"meta-line\"><span class=\"meta-label\">"
                + escape(label)
                + "</span> <span class=\"meta-value\">"
                + escape(firstNonBlank(value, "-"))
                + "</span></div>";
    }

    /**
     * Egy űrlapsor nyomtatható reprezentációját fűzi a készülő HTML-hez.
     *
     * <p>A metódus összegyűjti a megjelenítendő mezőket, alkalmazza az üres mezők elhagyására
     * és a technikai mezőazonosítók megjelenítésére vonatkozó beállításokat, majd a sort
     * hagyományos mezőrácsként vagy vizuális táblázatként rendereli.</p>
     *
     * @param html a készülő HTML tartalmát gyűjtő objektum
     * @param row a feldolgozandó űrlapsor definíciója
     * @param rowTitle a nyomtatásban megjelenítendő sorcím
     * @param instance ismétlődő sor aktuális példánya, vagy {@code null} nem ismétlődő sor esetén
     * @param options a nyomtatási beállítások
     * @param formData a teljes űrlapadat nem ismétlődő sorokhoz; ismétlődő példány esetén lehet {@code null}
     */
    private void appendRow(StringBuilder html,
                           FormRowDefinition row,
                           String rowTitle,
                           FormRowInstance instance,
                           PrintOptions options,
                           FormData formData) {

        List<RenderedField> renderedFields = new ArrayList<>();
        for (FormFieldDefinition field : row.getFields()) {
            FormValue value = instance != null
                    ? instance.getValuesByFieldId().get(field.getId())
                    : (formData != null ? formData.getValuesByFieldId().get(field.getId()) : null);

            String rawValue = value == null ? null : value.getValue();
            if (options.isOnlyFilledFields() && (rawValue == null || rawValue.isBlank())) {
                continue;
            }

            String displayValue = formatValue(field, rawValue);
            String label = firstNonBlank(field.getLabel(), field.getXmlName(), field.getId(), "Mező");

            if (options.isShowFieldIds()) {
                StringBuilder meta = new StringBuilder();
                meta.append(field.getId() == null ? "" : field.getId());
                if (field.getXmlName() != null && !field.getXmlName().isBlank()) {
                    if (meta.length() > 0) {
                        meta.append(" / ");
                    }
                    meta.append(field.getXmlName());
                }
                if (meta.length() > 0) {
                    if (displayValue == null || displayValue.isBlank()) {
                        displayValue = meta.toString();
                    } else {
                        displayValue = displayValue + " [" + meta + "]";
                    }
                }
            }

            renderedFields.add(new RenderedField(label, displayValue));
        }

        if (renderedFields.isEmpty()) {
            return;
        }

        if (isVisualTableRow(row, renderedFields)) {
            appendVisualTable(html, row, rowTitle, renderedFields);
            return;
        }

        html.append("<div class=\"row-block\">")
                .append("<div class=\"row-title\">")
                .append(escape(firstNonBlank(rowTitle, row.getId(), "Blokk")))
                .append("</div>")
                .append("<table class=\"print-table\">");

        int pairCountInCurrentRow = 0;
        html.append("<tr>");

        for (RenderedField renderedField : renderedFields) {
            boolean fullWidth = isFullWidthField(renderedField);

            if (fullWidth) {
                if (pairCountInCurrentRow > 0) {
                    while (pairCountInCurrentRow < 2) {
                        html.append("<td class=\"label-cell\"></td><td class=\"value-cell\"></td>");
                        pairCountInCurrentRow++;
                    }
                    html.append("</tr>");
                    pairCountInCurrentRow = 0;
                }

                html.append("<tr>")
                        .append("<td class=\"full-label-cell\">")
                        .append(escape(renderedField.label()))
                        .append("</td>")
                        .append("<td class=\"full-value-cell\" colspan=\"3\">")
                        .append(formatCellValue(renderedField.value()))
                        .append("</td>")
                        .append("</tr>")
                        .append("<tr>");
                continue;
            }

            html.append("<td class=\"label-cell\">")
                    .append(escape(renderedField.label()))
                    .append("</td>")
                    .append("<td class=\"value-cell\">")
                    .append(formatCellValue(renderedField.value()))
                    .append("</td>");

            pairCountInCurrentRow++;
            if (pairCountInCurrentRow == 2) {
                html.append("</tr>");
                pairCountInCurrentRow = 0;
                html.append("<tr>");
            }
        }

        if (pairCountInCurrentRow > 0) {
            while (pairCountInCurrentRow < 2) {
                html.append("<td class=\"label-cell\"></td><td class=\"value-cell\"></td>");
                pairCountInCurrentRow++;
            }
        }

        html.append("</tr></table></div>");
    }

    /**
     * Heurisztikusan eldönti, hogy egy űrlapsort háromoszlopos vizuális táblázatként kell-e megjeleníteni.
     *
     * <p>A döntés a sorszámozott és hosszú címkék számára, pénzügyi jellegű megnevezésekre,
     * valamint a UIModelben megadott négyoszlopos mezőszélességek mintázatára támaszkodik.</p>
     *
     * @param row a vizsgált űrlapsor
     * @param fields a megjelenítésre előkészített mezők
     * @return {@code true}, ha a sor vizuális táblázatként jelenítendő meg
     */
    private boolean isVisualTableRow(FormRowDefinition row, List<RenderedField> fields) {
        if (row == null || fields == null || fields.size() < 3) {
            return false;
        }

        int numbered = 0;
        int longLabels = 0;
        int amountLike = 0;
        int fourColumnFields = 0;

        for (int i = 0; i < row.getFields().size(); i++) {
            FormFieldDefinition field = row.getFields().get(i);
            String label = firstNonBlank(field.getUiLabel(), field.getXsdLabel(), field.getLabel(), field.getXmlName(), field.getId(), "");
            if (label != null && label.matches("^\\d{1,3}\\.\\s+.*")) {
                numbered++;
            }
            if (label != null && label.length() >= 24) {
                longLabels++;
            }
            if (label != null && label.toLowerCase(Locale.ROOT).matches(".*(összeg|bevétel|adó|érték|forint).*")) {
                amountLike++;
            }
            Integer width = field.getLayoutWidth();
            if (width != null && width == 4) {
                fourColumnFields++;
            }
        }

        boolean numberedLongSeries = numbered >= 2 && longLabels >= 2;
        boolean financialSeries = fields.size() >= 4 && amountLike >= 2;
        boolean threeColumnUiSeries = row.getFields().size() >= 3
                && fourColumnFields == row.getFields().size()
                && numbered >= 2;
        return numberedLongSeries || financialSeries || threeColumnUiSeries;
    }

    /**
     * A mezőket „Sor – Megnevezés – Érték” szerkezetű vizuális táblázatként fűzi a HTML-hez.
     *
     * @param html a készülő HTML tartalmát gyűjtő objektum
     * @param row a táblázatként megjelenített űrlapsor definíciója
     * @param rowTitle a táblázat felett megjelenítendő cím
     * @param renderedFields a táblázat soraivá alakítandó mezők
     */
    private void appendVisualTable(StringBuilder html,
                                   FormRowDefinition row,
                                   String rowTitle,
                                   List<RenderedField> renderedFields) {
        html.append("<div class=\"row-block visual-table-block\">")
                .append("<div class=\"row-title\">")
                .append(escape(firstNonBlank(rowTitle, row.getId(), "Táblázat")))
                .append("</div>")
                .append("<table class=\"visual-table\">")
                .append("<thead><tr><th class=\"visual-row-no\">Sor</th><th class=\"visual-description\">Megnevezés</th><th class=\"visual-value\">Érték</th></tr></thead><tbody>");

        for (int index = 0; index < renderedFields.size(); index++) {
            RenderedField field = renderedFields.get(index);
            ParsedTableLabel parsed = parseVisualTableLabel(field.label(), index);
            html.append("<tr>")
                    .append("<td class=\"visual-row-no\">").append(escape(parsed.rowNo())).append("</td>")
                    .append("<td class=\"visual-description\">").append(escape(parsed.description())).append("</td>")
                    .append("<td class=\"visual-value\">").append(formatCellValue(field.value())).append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table></div>");
    }

    /**
     * A vizuális táblázat mezőcímkéjéből elkülöníti a sorszámot és a leírást.
     *
     * <p>Ha a címke nem tartalmaz felismerhető, ponttal lezárt numerikus sorszámot,
     * a metódus a mező pozíciója alapján generál kétjegyű sorszámot.</p>
     *
     * @param label a feldolgozandó mezőcímke
     * @param index a mező nullától induló pozíciója
     * @return a különválasztott sorszámot és leírást tartalmazó értékobjektum
     */
    private ParsedTableLabel parseVisualTableLabel(String label, int index) {
        String text = firstNonBlank(label, "");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d{1,3}\\.)\\s*(.*)$").matcher(text);
        if (matcher.matches()) {
            return new ParsedTableLabel(matcher.group(1), firstNonBlank(matcher.group(2), text));
        }
        return new ParsedTableLabel(String.format(Locale.ROOT, "%02d.", index + 1), text);
    }

    /**
     * Meghatározza, hogy egy mező a teljes rendelkezésre álló értékszélességet igényelje-e.
     *
     * <p>Teljes szélességűként kezeli a hash-t tartalmazó címkéjű mezőket, valamint a
     * nyolcvan karakternél hosszabb értékeket.</p>
     *
     * @param field a vizsgált renderelt mező
     * @return {@code true}, ha a mezőt teljes szélességben kell megjeleníteni
     */
    private boolean isFullWidthField(RenderedField field) {
        if (field == null) {
            return false;
        }
        String label = field.label();
        String value = field.value();

        if (label != null && label.toLowerCase(Locale.ROOT).contains("hash")) {
            return true;
        }
        return value != null && value.length() > 80;
    }

    /**
     * A mezőértéket HTML-cellában megjeleníthető formára alakítja.
     *
     * @param value a megjelenítendő érték
     * @return HTML-ben biztonságosan felhasználható érték; üres érték esetén vizuális helyőrző
     */
    private String formatCellValue(String value) {
        if (value == null || value.isBlank()) {
            return "<span class=\"empty-value\">-</span>";
        }
        return escape(value);
    }

    /**
     * A nyers XML-mezőértéket nyomtatási megjelenítésre normalizálja.
     *
     * <p>Jelölőnégyzet típusú mezőknél több elterjedt igaz/hamis reprezentációt magyar
     * „Igen” vagy „Nem” értékre alakít. Más mezőtípusok esetén a levágott nyers értéket adja vissza.</p>
     *
     * @param field a mező definíciója
     * @param rawValue az XML-ből kiolvasott nyers érték
     * @return a nyomtatásban megjelenítendő érték, vagy {@code null}, ha a bemenet {@code null}
     */
    private String formatValue(FormFieldDefinition field, String rawValue) {
        if (rawValue == null) {
            return null;
        }

        if ("checkbox".equalsIgnoreCase(field.getType())) {
            String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("true")
                    || normalized.equals("1")
                    || normalized.equals("igen")
                    || normalized.equals("x")) {
                return "Igen";
            }
            if (normalized.equals("false")
                    || normalized.equals("0")
                    || normalized.equals("nem")) {
                return "Nem";
            }
        }

        return rawValue.trim();
    }

    /**
     * Meghatározza a forrás XML állomány létrehozási idejének nyomtatásban megjelenő értékét.
     *
     * <p>Elsődlegesen a fájlrendszer létrehozási idejét használja. Ha ez nem áll rendelkezésre,
     * az utolsó módosítás idejére esik vissza; sikertelen metaadat-olvasás esetén „-” értéket ad.</p>
     *
     * @param xmlFile a forrás XML állomány
     * @return a formázott létrehozási idő vagy „-”, ha nem határozható meg
     */
    private String resolveCreatedAt(Path xmlFile) {
        if (xmlFile == null) {
            return "-";
        }
        try {
            BasicFileAttributes attributes = Files.readAttributes(xmlFile, BasicFileAttributes.class);
            Instant created = attributes.creationTime() != null ? attributes.creationTime().toInstant() : null;
            if (created == null || created.equals(Instant.EPOCH)) {
                created = attributes.lastModifiedTime().toInstant();
            }
            return formatInstant(created);
        } catch (Exception ex) {
            try {
                return formatInstant(Files.getLastModifiedTime(xmlFile).toInstant());
            } catch (Exception ignored) {
                return "-";
            }
        }
    }

    /**
     * Időpontot a nyomtatási metaadatok egységes dátum-idő formátumára alakít.
     *
     * @param instant a formázandó időpont
     * @return a formázott időpont vagy „-”, ha az időpont nincs megadva
     */
    private String formatInstant(Instant instant) {
        return instant == null ? "-" : DATE_TIME_FORMATTER.format(instant);
    }

    /**
     * Kiszámítja a forrás XML állomány SHA3-512 hash lenyomatát.
     *
     * <p>A fájl ellenőrzése és feloldása után a tartalom streaming módon kerül a hash
     * számításába, így a teljes XML állományt nem szükséges memóriába tölteni. Hiba esetén
     * a probléma naplózásra kerül, a nyomtatási metaadat értéke pedig „-” lesz.</p>
     *
     * @param file a hash-elendő XML állomány
     * @return a kisbetűs hexadecimális SHA3-512 lenyomat vagy „-”, ha nem számítható ki
     */
    private String computeSha3_512(Path file) {
        if (file == null) {
            return "-";
        }

        try {
            Path safeFile = resolveReadableXmlFile(file);

            MessageDigest digest = MessageDigest.getInstance("SHA3-512");

            try (InputStream inputStream = Files.newInputStream(safeFile, StandardOpenOption.READ)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }

            byte[] hash = digest.digest();
            StringBuilder result = new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                result.append(String.format(Locale.ROOT, "%02x", b));
            }

            return result.toString();
        } catch (Exception ex) {
            LOGGER.warn("Could not compute SHA3-512 for XML file: {}", file, ex);
            return "-";
        }
    }

    /**
     * Biztonságosan feloldja és ellenőrzi a hash-számításhoz használható XML állományt.
     *
     * <p>A metódus abszolút, normalizált valós elérési utat képez szimbolikus link követése nélkül,
     * majd ellenőrzi, hogy az útvonal szabályos és olvasható, {@code .xml} kiterjesztésű fájlra mutat.</p>
     *
     * @param file az ellenőrizendő XML állomány elérési útja
     * @return az ellenőrzött valós fájlútvonal
     * @throws IllegalArgumentException ha az útvonal nem oldható fel, nem szabályos fájl,
     *                                  nem olvasható vagy nem XML kiterjesztésű
     */
    private Path resolveReadableXmlFile(Path file) {
        Path normalizedPath = file.toAbsolutePath().normalize();

        Path realPath;
        try {
            realPath = normalizedPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (Exception ex) {
            throw new IllegalArgumentException("XML file path does not exist or cannot be resolved: " + normalizedPath, ex);
        }

        if (!isRegularFileSafe(realPath)) {
            throw new IllegalArgumentException("XML file path is not a regular file: " + realPath);
        }

        if (!isReadableFileSafe(realPath)) {
            throw new IllegalArgumentException("XML file is not readable: " + realPath);
        }

        String fileName = realPath.getFileName() == null ? "" : realPath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".xml")) {
            throw new IllegalArgumentException("Only XML files can be hashed: " + realPath);
        }

        return realPath;
    }

    /**
     * Hozzáférési hibával szemben védetten ellenőrzi, hogy az útvonal szabályos fájlra mutat-e.
     *
     * @param file az ellenőrizendő útvonal
     * @return {@code true}, ha az útvonal szabályos fájl; egyébként {@code false}
     */
    private boolean isRegularFileSafe(Path file) {
        if (file == null) {
            return false;
        }

        try {
            return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
        } catch (SecurityException ex) {
            LOGGER.warn("Could not check whether XML file is regular: {}", file, ex);
            return false;
        }
    }

    /**
     * Hozzáférési hibával szemben védetten ellenőrzi a fájl olvashatóságát.
     *
     * @param file az ellenőrizendő fájl
     * @return {@code true}, ha a fájl olvasható; egyébként {@code false}
     */
    private boolean isReadableFileSafe(Path file) {
        if (file == null) {
            return false;
        }

        try {
            return Files.isReadable(file);
        } catch (SecurityException ex) {
            LOGGER.warn("Could not check whether XML file is readable: {}", file, ex);
            return false;
        }
    }

    /**
     * A paraméterek közül visszaadja az első nem üres szöveges értéket.
     *
     * @param values a prioritási sorrendben vizsgálandó értékek
     * @return az első nem üres, levágott érték, vagy {@code null}, ha nincs ilyen
     */
    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * HTML-kimenethez escape-eli a szöveg alapvető speciális karaktereit.
     *
     * @param value az escape-elendő szöveg
     * @return a HTML-ben biztonságosan beilleszthető szöveg
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * A nyomtatás előtt előkészített mező megjelenítési címkéjét és értékét fogja össze.
     *
     * @param label a megjelenítendő mezőcímke
     * @param value a megjelenítendő mezőérték
     */
    private record RenderedField(String label, String value) {
    }

    /**
     * A vizuális táblázat számára feldolgozott sorszámot és mezőleírást tartalmazza.
     *
     * @param rowNo a nyomtatásban megjelenő sorszám
     * @param description a sorszámtól megtisztított mezőleírás
     */
    private record ParsedTableLabel(String rowNo, String description) {
    }
}