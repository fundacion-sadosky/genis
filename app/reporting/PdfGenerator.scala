package reporting

import java.io._
import scala.collection.mutable.ArrayBuffer

import javax.inject.{Inject, Singleton}
import com.itextpdf.text.pdf.BaseFont
import nu.validator.htmlparser.dom.HtmlDocumentBuilder
import org.apache.commons.io.{FilenameUtils, IOUtils}
import org.w3c.tidy.Tidy
import org.xhtmlrenderer.pdf.ITextRenderer

import play.api.Play
import play.api.mvc.{Result, Results}
import play.twirl.api.Html

/**
  * PDF generator service.
  */
@Singleton
class PdfGenerator() {

  /**
    * val xhtml true to set XHTML strict parsing (i.e. leave disabled for HTML5 templates).
    */
  val xhtml: Boolean = false
  /** HTML tidy checker / prettyprinter instance for XHTML strict parsing */

  val fontPath = "app/assets/stylesheets/report/DejaVuSans.ttf"
  private val renderer = new ITextRenderer()
  renderer.getFontResolver.addFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)

  private lazy val tidyParser = {
    val t = new Tidy()
    t.setXHTML(true)
    t
  }

  /** Default fonts */
  private var defaultFonts = ArrayBuffer[String]()

  /**
    * Load a list of fonts as temporary fonts (will be deleted when application exits) for PDF generation.
    * @note Existing default fonts collection will be cleared / emptied.
    *
    * @param fonts the list of font filenames to load.
    */
  def loadTemporaryFonts(fonts: Seq[String]): Unit = {
    defaultFonts.clear()
    addTemporaryFonts(fonts)
  }

  /**
    * Add a list of fonts as temporary fonts (will be deleted when application exits) for PDF generation.
    *
    * @param fonts the list of font filenames to add.
    */
  def addTemporaryFonts(fonts: Seq[String]): Unit = {
    fonts.foreach { font =>
      val stream = Play.current.resourceAsStream(font)

      stream.map { s =>
        val file = File.createTempFile("tmp_" + FilenameUtils.getBaseName(font), "." + FilenameUtils.getExtension(font))
        file.deleteOnExit()
        val output = new FileOutputStream(file)
        IOUtils.copy(s, output)
        defaultFonts += file.getAbsolutePath
      }
    }
  }

  /**
    * Load a list of local fonts for PDF generation.
    * @note Existing default fonts collection will be cleared / emptied.
    *
    * @param fonts the list of font filenames to load.
    */
  def loadLocalFonts(fonts: Seq[String]): Unit = {
    defaultFonts.clear()
    addLocalFonts(fonts)
  }

  /**
    * Add a list of local fonts for PDF generation.
    *
    * @param fonts the list of font filenames to load.
    */
  def addLocalFonts(fonts: Seq[String]): Unit = defaultFonts ++= fonts

  /**
    * Returns PDF result from Twirl HTML.
    *
    * @param html the Twirl HTML.
    * @param documentBaseUrl the document / page base URL.
    * @param fonts the external / additional fonts to load.
    * @return Generated PDF result (with "application/pdf" MIME type).
    */
  def ok(html: Html, documentBaseUrl: String, fonts: Seq[String] = defaultFonts): Result = {
    Results.Ok(toBytes(parseString(html), documentBaseUrl, fonts)).as("application/pdf")
  }

  /**
    * Generate PDF bytearray given Twirl HTML.
    *
    * @param html the Twirl HTML.
    * @param documentBaseUrl the document / page base URL.
    * @param fonts the external / additional fonts to load.
    * @return Generated PDF as bytearray.
    */
  def toBytes(html: Html, documentBaseUrl: String, fonts: Seq[String]): Array[Byte] = {
    // NOTE: Use default value assignment in method body,
    //       as Scala compiler does not like overloaded methods with default params
    val loadedFonts = if (fonts.isEmpty) defaultFonts else fonts
    toBytes(parseString(html), documentBaseUrl, loadedFonts)
  }

  /**
    * Generate PDF bytearray given HTML string.
    *
    * @param string the HTML string.
    * @param documentBaseUrl the document / page base URL.
    * @param fonts the external / additional fonts to load.
    * @return Generated PDF as bytearray.
    */
  def toBytes(string: String, documentBaseUrl: String, fonts: Seq[String]): Array[Byte] = {
    // NOTE: Use default value assignment in method body,
    //       as Scala compiler does not like overloaded methods with default params
    val loadedFonts = if (fonts.isEmpty) defaultFonts else fonts
    val output = new ByteArrayOutputStream()
    toStream(output)(string, documentBaseUrl, loadedFonts)
//    output.writeTo(new FileOutputStream("/home/pdg/ejemplo.pdf"))
    return output.toByteArray
  }

  /**
    * Generate and write PDF to an existing OutputStream given HTML string.
    *
    * @param output the OutputStream to write the generated PDF to.
    * @param string the HTML string.
    * @param documentBaseUrl the document / page base URL.
    * @param fonts the external / additional fonts to load.
    */
  def toStream(output: OutputStream)(string: String, documentBaseUrl: String, fonts: Seq[String] = defaultFonts): Unit = {
    val input = new ByteArrayInputStream(string.getBytes("UTF-8"))
    val renderer = new ITextRenderer()
    fonts.foreach { font => renderer.getFontResolver.addFont(font, BaseFont.IDENTITY_H, BaseFont.EMBEDDED) }
    val userAgent = new PdfUserAgent(renderer.getOutputDevice)
    userAgent.setSharedContext(renderer.getSharedContext)
    renderer.getSharedContext.setUserAgentCallback(userAgent)
    val document = new HtmlDocumentBuilder().parse(input)
    renderer.setDocument(document, documentBaseUrl)
    renderer.layout()
    renderer.createPDF(output)
  }

  /**
    * Parse Twirl HTML into HTML string.
    *
    * @param html the generated Twirl HTML.
    * @return HTML as string.
    */
  def parseString(html: Html): String = {
    if (xhtml) {
      val reader = new StringReader(html.body)
      val writer = new StringWriter()
      tidyParser.parse(reader, writer)
      writer.getBuffer.toString
    } else html.body
  }

}