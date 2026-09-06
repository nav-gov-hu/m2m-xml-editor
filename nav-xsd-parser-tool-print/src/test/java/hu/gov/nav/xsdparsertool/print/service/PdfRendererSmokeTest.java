package hu.gov.nav.xsdparsertool.print.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PdfRendererSmokeTest {
    @Test
    void openHtmlToPdfProducesPdfBytesFromPrintCompatibleHtml() throws Exception {
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/><style>@page{size:A4 portrait;margin:12mm;} table{border-collapse:collapse;} thead{display:table-header-group;} tr{break-inside:avoid;}</style></head><body><h1>Nyomtatási próba</h1><table><thead><tr><th>Sor</th><th>Érték</th></tr></thead><tbody><tr><td>1.</td><td>Árvíztűrő tükörfúrógép</td></tr></tbody></table></body></html>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode(); builder.withHtmlContent(html, null); builder.toStream(out); builder.run();
        byte[] pdf = out.toByteArray();
        assertTrue(pdf.length > 500);
        assertEquals((byte) '%', pdf[0]); assertEquals((byte) 'P', pdf[1]); assertEquals((byte) 'D', pdf[2]); assertEquals((byte) 'F', pdf[3]); assertEquals((byte) '-', pdf[4]);
    }
}
