package coursier.core

import coursier.util.Xml

import scala.xml.{ MetaData, Null }

package object compatibility {

  implicit class RichChar(val c: Char) extends AnyVal {
    def letterOrDigit = c.isLetterOrDigit
    def letter = c.isLetter
  }

  def xmlParse(s: String): Either[String, Xml.Node] = {
    def parse =
      try Right(scala.xml.XML.loadString(s))
      catch { case e: Exception => Left(e.getMessage) }

    def fromNode(node: scala.xml.Node): Xml.Node =
      new Xml.Node {
        lazy val attributes = {
          def helper(m: MetaData): Stream[(String, String)] =
            m match {
              case Null => Stream.empty
              case attr =>
                val value = attr.value.collect {
                  case scala.xml.Text(t) => t
                }.mkString("")
                (attr.key -> value) #:: helper(m.next)
            }

          helper(node.attributes).toVector
        }
        def label = node.label
        def children = node.child.map(fromNode)
        def isText = node match { case _: scala.xml.Text => true; case _ => false }
        def textContent = node.text
        def isElement = node match { case _: scala.xml.Elem => true; case _ => false }
      }

    parse.right
      .map(fromNode)
  }

  def encodeURIComponent(s: String): String =
    new java.net.URI(null, null, null, -1, s, null, null) .toASCIIString

}
