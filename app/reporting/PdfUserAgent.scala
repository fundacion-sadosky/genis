package reporting

import java.io._

import com.itextpdf.text.Image
import org.xhtmlrenderer.pdf.{ITextFSImage, ITextOutputDevice, ITextUserAgent}
import org.xhtmlrenderer.resource.{CSSResource, ImageResource, XMLResource}

import play.api.Play

/**
  * Custom iText user agent implementation.
  *
  * @param outputDevice the abstract output device for PDF generation.
  */
class PdfUserAgent(outputDevice: ITextOutputDevice) extends ITextUserAgent(outputDevice) {

  /**
    * Get an image resource given its URI.
    *
    * @param uri the image resource URI.
    * @return Image resource object.
    */
  override def getImageResource(uri: String): ImageResource = {
    Play.current.resourceAsStream(toClasspathPath(uri)).fold(super.getImageResource(uri)) { toImageResource(uri) }
  }

  /**
    * Get a CSS resource given its URI.
    *
    * @param uri the CSS resource URI.
    * @return CSS resource object.
    */
  override def getCSSResource(uri: String): CSSResource = {
    Play.current.resourceAsStream(toClasspathPath(uri)).fold(super.getCSSResource(uri)) { toCssResource }
  }

  /**
    * Get an XML resource given its URI.
    *
    * @param uri the XML resource URI.
    * @return XML resource object.
    */
  override def getXMLResource(uri: String): XMLResource = {
    Play.current.resourceAsStream(toClasspathPath(uri)).fold(super.getXMLResource(uri)) { XMLResource.load }
  }

  /**
    * Get a binary resource given its URI.
    *
    * @param uri the binary resource URI.
    * @return Binary resource as bytearray.
    */
  override def getBinaryResource(uri: String): Array[Byte] = {
    Play.current.resourceAsStream(toClasspathPath(uri)).fold(super.getBinaryResource(uri)) { toByteArray }
  }

  /**
    * Converts a full asset URI (e.g. http://host/assets/images/logo.png)
    * to a classpath-relative path (e.g. public/images/logo.png).
    * Falls back to the original URI if it cannot be parsed as a URL.
    *
    * @param uri the resource URI.
    * @return Classpath-relative path.
    */
  private def toClasspathPath(uri: String): String = {
    val path = try new java.net.URL(uri).getPath catch { case _: Exception => return uri }
    if (path.startsWith("/assets/")) "public" + path.drop("/assets".length)
    else path
  }

  /**
    * Converts input stream with URI reference to image resource.
    *
    * @param uri the image resource URI.
    * @param stream the input stream to convert.
    * @return Flying Saucer's Image resource.
    */
  private def toImageResource(uri: String)(stream: InputStream): ImageResource = {
    val image = Image.getInstance(toByteArray(stream))
    scaleToOutputResolution(image)
    new ImageResource(uri, new ITextFSImage(image))
  }

  /**
    * Converts input stream to CSS resource.
    *
    * @param stream the input stream to convert.
    * @return Flying Saucer's CSS resource.
    */
  private def toCssResource(stream: InputStream): CSSResource = new CSSResource(stream)

  /**
    * Converts input stream to bytearray.
    *
    * @param stream the input stream to convert.
    * @return Converted input stream as bytearray.
    */
  private def toByteArray(stream: InputStream): Array[Byte] = {
    val buffer = new BufferedInputStream(stream)
    Stream.continually(buffer.read).takeWhile(-1 !=).map(_.toByte).toArray
  }

  /**
    * Scale images to output resolution.
    *
    * @param image the Image object to scale.
    */
  private def scaleToOutputResolution(image: Image): Unit = {
    val factor: Float = getSharedContext.getDotsPerPixel
    image.scaleAbsolute(image.getPlainWidth * factor, image.getPlainHeight * factor)
  }

}