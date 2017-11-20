package no.mnemonic.act.platform.dao.elastic.document;

import org.junit.Test;

import static no.mnemonic.act.platform.dao.elastic.document.DocumentTestUtils.assertObjectDocument;
import static no.mnemonic.act.platform.dao.elastic.document.DocumentTestUtils.createObjectDocument;
import static org.junit.Assert.assertNotSame;

public class ObjectDocumentTest {

  @Test
  public void testCloneDocument() {
    ObjectDocument original = createObjectDocument();
    ObjectDocument clone = original.clone();

    assertNotSame(original, clone);
    assertObjectDocument(original, clone);
  }

}
