package hu.gov.nav.xsdparsertool.web.xmlfile.repository;

import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileDiffEntryEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileEntity;
import hu.gov.nav.xsdparsertool.web.xmlfile.entity.XmlFileRevisionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class XmlFileRepositoryJpaIntegrationTest {

    @Autowired XmlFileRepository xmlFiles;
    @Autowired XmlFileRevisionRepository revisions;
    @Autowired XmlFileDiffEntryRepository diffs;

    @Test
    void fileNameLookupMustBeCaseInsensitive() {
        XmlFileEntity saved = xmlFiles.save(xml("Invoice.XML", false, LocalDateTime.now()));

        assertTrue(xmlFiles.existsByFileNameIgnoreCase("invoice.xml"));
        assertEquals(saved.getId(), xmlFiles.findByFileNameIgnoreCase("INVOICE.XML").orElseThrow().getId());
    }

    @Test
    void activeAndArchivedQueriesMustFilterAndOrderByCreatedAtDescending() {
        XmlFileEntity olderActive = xmlFiles.save(xml("older.xml", false, LocalDateTime.now().minusHours(3)));
        XmlFileEntity newerActive = xmlFiles.save(xml("newer.xml", false, LocalDateTime.now().minusHours(1)));
        XmlFileEntity archived = xmlFiles.save(xml("archived.xml", true, LocalDateTime.now()));

        List<XmlFileEntity> active = xmlFiles.findByArchivedFalseOrderByCreatedAtDesc();
        List<XmlFileEntity> archivedRows = xmlFiles.findByArchivedTrueOrderByCreatedAtDesc();

        assertEquals(List.of(newerActive.getId(), olderActive.getId()), active.stream().map(XmlFileEntity::getId).toList());
        assertEquals(List.of(archived.getId()), archivedRows.stream().map(XmlFileEntity::getId).toList());
    }

    @Test
    void revisionRepositoryMustReturnDescendingOrderMaxAndCountForSingleXml() {
        XmlFileEntity xml1 = xmlFiles.save(xml("one.xml", false, LocalDateTime.now()));
        XmlFileEntity xml2 = xmlFiles.save(xml("two.xml", false, LocalDateTime.now()));
        revisions.save(revision(xml1, 1, "NEW_VERSION"));
        revisions.save(revision(xml1, 3, "OVERWRITE"));
        revisions.save(revision(xml1, 2, "OVERWRITE"));
        revisions.save(revision(xml2, 9, "OVERWRITE"));

        List<XmlFileRevisionEntity> result = revisions.findByXmlFileIdOrderByRevisionNoDesc(xml1.getId());

        assertEquals(List.of(3, 2, 1), result.stream().map(XmlFileRevisionEntity::getRevisionNo).toList());
        assertEquals(3, safeMaxRevisionNo(xml1.getId()));
        assertEquals(3L, revisions.countByXmlFileId(xml1.getId()));
    }

    @Test
    void maxRevisionNoMustReturnZeroWhenXmlHasNoRevision() {
        XmlFileEntity xml = xmlFiles.save(xml("empty.xml", false, LocalDateTime.now()));

        assertEquals(0, safeMaxRevisionNo(xml.getId()));
        assertEquals(0L, revisions.countByXmlFileId(xml.getId()));
    }

    @Test
    void diffRepositoryMustKeepRevisionAscendingAndXmlDescendingIdOrdering() {
        XmlFileEntity xml = xmlFiles.save(xml("diff.xml", false, LocalDateTime.now()));
        XmlFileRevisionEntity r1 = revisions.save(revision(xml, 1, "OVERWRITE"));
        XmlFileRevisionEntity r2 = revisions.save(revision(xml, 2, "OVERWRITE"));
        XmlFileDiffEntryEntity d1 = diffs.save(diff(xml, r1, "CHANGED", "/Root/A", "1", "2"));
        XmlFileDiffEntryEntity d2 = diffs.save(diff(xml, r1, "ADDED", "/Root/B", null, "x"));
        XmlFileDiffEntryEntity d3 = diffs.save(diff(xml, r2, "REMOVED", "/Root/C", "y", null));

        assertEquals(List.of(d1.getId(), d2.getId()),
                diffs.findByRevisionIdOrderByIdAsc(r1.getId()).stream().map(XmlFileDiffEntryEntity::getId).toList());
        assertEquals(List.of(d3.getId(), d2.getId(), d1.getId()),
                diffs.findByXmlFileIdOrderByIdDesc(xml.getId()).stream().map(XmlFileDiffEntryEntity::getId).toList());
    }

    private int safeMaxRevisionNo(Long xmlFileId) {
        try {
            return revisions.maxRevisionNo(xmlFileId);
        } catch (RuntimeException ex) {
            fail("A revision maximum lekérdezése sikertelen: " + ex.getMessage());
            return 0;
        }
    }

    private XmlFileEntity xml(String name, boolean archived, LocalDateTime createdAt) {
        XmlFileEntity entity = new XmlFileEntity();
        entity.setFileName(name);
        entity.setOriginalFileName(name);
        entity.setFilePath("C:/test/" + name);
        entity.setFileSizeBytes(1L);
        entity.setSourceType("TEST");
        entity.setStatus("REGISTERED");
        entity.setLargeFileMode(false);
        entity.setArchived(archived);
        entity.setCreatedAt(createdAt);
        entity.setCreatedBy("test");
        return entity;
    }

    private XmlFileRevisionEntity revision(XmlFileEntity xml, int no, String type) {
        XmlFileRevisionEntity entity = new XmlFileRevisionEntity();
        entity.setXmlFile(xml);
        entity.setRevisionNo(no);
        entity.setSaveType(type);
        entity.setChangeCount(0);
        entity.setXsdValidationRequested(false);
        entity.setXsdValidationStatus("SKIPPED");
        entity.setCreatedAt(LocalDateTime.now().plusSeconds(no));
        entity.setCreatedBy("test");
        return entity;
    }

    private XmlFileDiffEntryEntity diff(XmlFileEntity xml, XmlFileRevisionEntity revision, String type,
                                        String path, String oldValue, String newValue) {
        XmlFileDiffEntryEntity entity = new XmlFileDiffEntryEntity();
        entity.setXmlFile(xml);
        entity.setRevision(revision);
        entity.setChangeType(type);
        entity.setXmlPath(path);
        entity.setOldValue(oldValue);
        entity.setNewValue(newValue);
        entity.setDisplayLabel(path.substring(path.lastIndexOf('/') + 1));
        return entity;
    }
}
