package org.endpoints4s.paradox.coordinates

import com.lightbend.paradox.markdown.InlineDirective
import org.pegdown.Printer
import org.pegdown.ast._

object CoordinatesDirective extends InlineDirective("coordinates") {
  def render(node: DirectiveNode, visitor: Visitor, printer: Printer): Unit = {
    val binarySuffix = if (node.attributes.values("platform").contains("js")) "sjs1_2.13" else "2.13"
    val artifact = node.contents
    new ExpLinkNode(
      s"Artifact $artifact",
      s"https://index.scala-lang.org/endpoints4s/endpoints4s/$artifact",
      new ExpImageNode(
        "Artifact coordinates",
        s"https://img.shields.io/maven-central/v/org.endpoints4s/${artifact}_$binarySuffix?label=$artifact&style=for-the-badge",
        new TextNode("") // FIXME How to have no child nodes?
      )
    ).accept(visitor)
  }
}
