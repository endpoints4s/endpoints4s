package sample

import endpoints.algebra.BasicAuthentication.Credentials
import org.scalajs.dom.document
import org.scalajs.dom.AudioContext

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport
object Main extends js.JSApp {

  @JSExport
  def main(): Unit = {
    Api.index(("Julien", 30, "foo&bar+baz")).`then`[Unit] ({ user =>
      val p = document.createElement("p")
      p.textContent = s"User(${user.name}, ${user.age})"
      document.body.appendChild(p)
      ()
    }, js.undefined)

    Api.action(ActionParameter()).`then`[Unit] ({ result =>
      val p = document.createElement("p")
      p.textContent = s"Result = $result"
      document.body.appendChild(p)
      ()
    }, js.undefined)

    Api.assets(Api.asset("medias", "chopin--funeral-march.mp3")).`then`[Unit] ({ arrayBuffer =>
      val audioCtx = new AudioContext
      audioCtx.decodeAudioData(arrayBuffer).`then`[Unit] { audioBuffer =>
        val source = audioCtx.createBufferSource()
        source.buffer = audioBuffer
        source.connect(audioCtx.destination)
        source.start()
        source.stop(audioCtx.currentTime + 10)
        ()
      }
    }, js.undefined)

    Api.auth(Credentials("foo", "bar")).`then`[Unit]({ maybeResponse =>
      println(s"Access granted: ${maybeResponse.isDefined}")
    }, js.undefined)
    ()
  }

}
