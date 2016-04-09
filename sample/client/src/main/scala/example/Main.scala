package example

import org.scalajs.dom.document

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport
object Main extends JSApp {

  @JSExport
  def main(): Unit = {
    Api.api.index("Julien").andThen { user =>
      val p = document.createElement("p")
      p.textContent = s"User(${user.name}, ${user.age})"
      document.body.appendChild(p)
      ()
    }

    Api.api.action(ActionParameter()).andThen { result =>
      val p = document.createElement("p")
      p.textContent = s"Result = $result"
      document.body.appendChild(p)
      ()
    }
    ()
  }

}
