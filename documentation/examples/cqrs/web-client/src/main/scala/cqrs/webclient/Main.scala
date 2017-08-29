package cqrs.webclient

import java.time.Instant
import java.util.UUID

import scala.scalajs.js.JSApp
import cats.implicits._
import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import mhtml.{Var, mount}
import mhtml.cats._
import org.scalajs.dom
import faithful.cats.Instances._
import cqrs.queries.Meter
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement

import scala.util.Try

object Main extends JSApp {

  val metersVar: Var[Map[UUID, Meter]] = Var(Map.empty)

  def main(): Unit = {
    //#list-meters-invocation
    documented.PublicEndpoints.listMeters(()).map { fetchedMeters =>
      metersVar := fetchedMeters.map(meter => meter.id -> meter).toMap
    }
    //#list-meters-invocation

    val newMeterId = UUID.randomUUID().toString

    val onNewMeterClicked: Event => Unit = _ => {
      val input = dom.document.getElementById(newMeterId).asInstanceOf[HTMLInputElement]
      val name = input.value.trim
      if (name.isEmpty) dom.window.alert("Please type a name for the meter to create")
      else {
        documented.PublicEndpoints.createMeter(CreateMeter(name)).map { createdMeter =>
          metersVar := metersVar.value + (createdMeter.id -> createdMeter)
          input.value = ""
        }.handleError(error => dom.window.alert(s"Unable to create the meter (error is $error)"))
        ()
      }
    }

    def onAddValueClicked(meter: Meter): Event => Unit = _ => {
      val input = dom.document.getElementById(s"value-${meter.id.toString}").asInstanceOf[HTMLInputElement]
      val value = input.value.trim
      if (value.isEmpty) dom.window.alert("Please type a value to add")
      else {
        Try(BigDecimal(value)).toOption match {
          case None => dom.window.alert("Unable to parse the value as a number")
          case Some(decimal) =>
            documented.PublicEndpoints.addRecord((meter.id, AddRecord(Instant.now(), decimal))).map { updatedMeter =>
              metersVar := metersVar.value + (updatedMeter.id -> updatedMeter)
            }.handleError(error => dom.window.alert(s"Unable to add the value (error is $error)"))
            ()
        }
      }
    }

    val app =
      <article>
        <h1>Meters</h1>
        <p>
          <input type="text" placeholder="New meter name" id={ newMeterId } required="required" />
          <button onclick={ onNewMeterClicked }>Create</button>
        </p>
        {
          metersVar.map { meters =>
            if (meters.isEmpty) {
              <p>No meters yet!</p>
            } else {
              <div>
                {
                  meters.to[Seq].map { case (_, meter) =>
                    <section>
                      <h2>{ meter.label }</h2>
                      <p>
                        <table>
                          <thead>
                            <th>Time</th><th>Value</th>
                          </thead>
                          <tbody>
                            {
                              meter.timeSeries.to[Seq].map { case (instant, value) =>
                                <tr><td>{ instant.toString }</td><td>{ value.toString }</td></tr>
                              }
                            }
                          </tbody>
                        </table>
                        <input placeholder="New value" required="required" id={ s"value-${meter.id.toString}" } />
                        <button onclick={ onAddValueClicked(meter) }>Add</button>
                      </p>
                    </section>
                  }
                }
              </div>
            }
          }
        }
      </article>

    mount(dom.document.body, app)
  }
}
