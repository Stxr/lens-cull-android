package com.stxr.lenscull.metadata

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element

class XmpFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

object XmpRating {
  private const val XMP_META_NS = "adobe:ns:meta/"
  private const val RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  private const val XMP_NS = "http://ns.adobe.com/xap/1.0/"
  private const val ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD"
  private const val ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema"
  private const val ACCESS_EXTERNAL_STYLESHEET = "http://javax.xml.XMLConstants/property/accessExternalStylesheet"

  fun read(packet: ByteArray?): Int? {
    if (packet == null || packet.isEmpty()) return null
    val document = parse(packet)
    val descriptions = document.getElementsByTagNameNS(RDF_NS, "Description")
    for (index in 0 until descriptions.length) {
      val element = descriptions.item(index) as? Element ?: continue
      val attribute = element.getAttributeNS(XMP_NS, "Rating")
      attribute.toIntOrNull()?.let { return it.takeIf(::isValid) }
    }
    val ratings = document.getElementsByTagNameNS(XMP_NS, "Rating")
    for (index in 0 until ratings.length) {
      ratings.item(index).textContent.trim().toIntOrNull()?.let { return it.takeIf(::isValid) }
    }
    return null
  }

  @Throws(XmpFormatException::class)
  fun merge(packet: ByteArray?, rating: Int): ByteArray {
    require(isValid(rating)) { "Rating must be between 0 and 5" }
    val document = if (packet == null || packet.isEmpty()) newDocument() else parse(packet)
    val description = firstDescription(document) ?: appendDescription(document)
    if (description.lookupNamespaceURI("xmp") != XMP_NS) {
      description.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xmp", XMP_NS)
    }
    description.setAttributeNS(XMP_NS, "xmp:Rating", rating.toString())
    return serialize(document)
  }

  private fun isValid(value: Int): Boolean = value in 0..5

  private fun parse(packet: ByteArray): Document = try {
    builderFactory().newDocumentBuilder().parse(ByteArrayInputStream(packet))
  } catch (error: Exception) {
    throw XmpFormatException("Invalid XMP packet", error)
  }

  private fun newDocument(): Document {
    val document = builderFactory().newDocumentBuilder().newDocument()
    val meta = document.createElementNS(XMP_META_NS, "x:xmpmeta")
    meta.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:x", XMP_META_NS)
    document.appendChild(meta)
    val rdf = document.createElementNS(RDF_NS, "rdf:RDF")
    rdf.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:rdf", RDF_NS)
    rdf.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xmp", XMP_NS)
    meta.appendChild(rdf)
    return document
  }

  private fun firstDescription(document: Document): Element? =
    document.getElementsByTagNameNS(RDF_NS, "Description").item(0) as? Element

  private fun appendDescription(document: Document): Element {
    val rdf = document.getElementsByTagNameNS(RDF_NS, "RDF").item(0) as? Element
      ?: throw XmpFormatException("XMP packet has no rdf:RDF element")
    val description = document.createElementNS(RDF_NS, "rdf:Description")
    description.setAttributeNS(RDF_NS, "rdf:about", "")
    rdf.appendChild(description)
    return description
  }

  private fun serialize(document: Document): ByteArray = try {
    val output = ByteArrayOutputStream()
    val factory = TransformerFactory.newInstance()
    runCatching { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
    runCatching { factory.setAttribute(ACCESS_EXTERNAL_DTD, "") }
    runCatching { factory.setAttribute(ACCESS_EXTERNAL_STYLESHEET, "") }
    factory.newTransformer().apply {
      setOutputProperty(OutputKeys.ENCODING, "UTF-8")
      setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
      setOutputProperty(OutputKeys.INDENT, "yes")
    }.transform(DOMSource(document), StreamResult(output))
    output.toByteArray()
  } catch (error: Exception) {
    throw XmpFormatException("Unable to serialize XMP packet", error)
  }

  private fun builderFactory(): DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply {
      isNamespaceAware = true
      runCatching { isXIncludeAware = false }
      runCatching { setExpandEntityReferences(false) }
      runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
      runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
      runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
      runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
      runCatching { setAttribute(ACCESS_EXTERNAL_DTD, "") }
      runCatching { setAttribute(ACCESS_EXTERNAL_SCHEMA, "") }
    }
}
