package com.stxr.lenscull.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmpRatingTest {
  private val existing = """
    <?xml version="1.0" encoding="UTF-8"?>
    <x:xmpmeta xmlns:x="adobe:ns:meta/">
      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
               xmlns:xmp="http://ns.adobe.com/xap/1.0/"
               xmlns:dc="http://purl.org/dc/elements/1.1/">
        <rdf:Description rdf:about="" xmp:Rating="2" dc:format="image/jpeg" />
      </rdf:RDF>
    </x:xmpmeta>
  """.trimIndent().toByteArray()

  @Test fun `reads and updates rating while preserving unknown fields`() {
    assertEquals(2, XmpRating.read(existing))
    val merged = XmpRating.merge(existing, 5)
    assertEquals(5, XmpRating.read(merged))
    assertTrue(merged.decodeToString().contains("dc:format=\"image/jpeg\""))
  }

  @Test fun `creates a standards based packet when none exists`() {
    val packet = XmpRating.merge(null, 4)
    assertEquals(4, XmpRating.read(packet))
    assertTrue(packet.decodeToString().contains("rdf:RDF"))
  }

  @Test fun `adds the xmp namespace when an existing packet does not declare it`() {
    val packet = """
      <x:xmpmeta xmlns:x="adobe:ns:meta/">
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
          <rdf:Description rdf:about="" />
        </rdf:RDF>
      </x:xmpmeta>
    """.trimIndent().toByteArray()
    val merged = XmpRating.merge(packet, 3)
    assertEquals(3, XmpRating.read(merged))
    assertTrue(merged.decodeToString().contains("xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\""))
  }

  @Test(expected = XmpFormatException::class)
  fun `does not silently replace malformed XMP`() {
    XmpRating.merge("<broken".toByteArray(), 3)
  }
}
