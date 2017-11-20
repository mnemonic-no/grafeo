package no.mnemonic.act.platform.dao.elastic.document;

import org.junit.Test;

import static no.mnemonic.act.platform.dao.elastic.document.DocumentTestUtils.assertFactDocument;
import static no.mnemonic.act.platform.dao.elastic.document.DocumentTestUtils.createFactDocument;
import static org.junit.Assert.assertNotSame;

public class FactDocumentTest {

  @Test
  public void testCloneDocument() {
    FactDocument original = createFactDocument();
    FactDocument clone = original.clone();

    assertNotSame(original, clone);
    assertNotSame(original.getObjects().iterator().next(), clone.getObjects().iterator().next());
    assertFactDocument(original, clone);
  }

}
