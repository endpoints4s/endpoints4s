package cqrs.webclient

import java.time.Instant
import java.util.UUID
import com.raquo.laminar.api.L._
import cats.implicits._
import cqrs.publicserver.commands.{AddRecord, CreateMeter}
import org.scalajs.dom
import faithful.cats.Instances._
import cqrs.queries.Meter
import faithful.Future
import org.scalajs.dom.raw.HTMLInputElement
import scala.util.Try

object Main {

  val metersVar: Var[Map[UUID, Meter]] = Var(Map.empty)

  def main(args: Array[String]): Unit = {
    //#list-meters-invocation
    PublicEndpoints.listMeters(()).map { fetchedMeters =>
      metersVar.set(fetchedMeters.map(meter => meter.id -> meter).toMap)
    }
    //#list-meters-invocation

    val newMeterId = UUID.randomUUID().toString

    val onNewMeterClicked: Observer[Unit] = Observer { _ =>
      val input =
        dom.document.getElementById(newMeterId).asInstanceOf[HTMLInputElement]
      val name = input.value.trim
      if (name.isEmpty)
        dom.window.alert("Please type a name for the meter to create")
      else {
        //#webapps-invocation
        val eventuallyCreatedMeter: Future[Meter] =
          PublicEndpoints.createMeter(CreateMeter(name))
        //#webapps-invocation
        eventuallyCreatedMeter
          .map { createdMeter =>
            metersVar.update(_ + (createdMeter.id -> createdMeter))
            input.value = ""
          }
          .handleError(error => dom.window.alert(s"Unable to create the meter (error is $error)"))
        ()
      }
    }

    val onAddValueClicked: Observer[Meter] = Observer { meter =>
      val input = dom.document
        .getElementById(s"value-${meter.id.toString}")
        .asInstanceOf[HTMLInputElement]
      val value = input.value.trim
      if (value.isEmpty) dom.window.alert("Please type a value to add")
      else {
        Try(BigDecimal(value)).toOption match {
          case None => dom.window.alert("Unable to parse the value as a number")
          case Some(decimal) =>
            PublicEndpoints
              .addRecord((meter.id, AddRecord(Instant.now(), decimal)))
              .map { updatedMeter =>
                metersVar.update(_ + (updatedMeter.id -> updatedMeter))
              }
              .handleError(error => dom.window.alert(s"Unable to add the value (error is $error)"))
            ()
        }
      }
    }

    val app = article(
      h1("Meters"),
      p(
        input(
          tpe := "text",
          placeholder := "New meter name",
          idAttr := newMeterId,
          required := true
        ),
        button(onClick.mapTo(()) --> onNewMeterClicked, "Create")
      ),
      child <-- metersVar.signal.map { meters =>
        if (meters.isEmpty) {
          p("No meters yet!")
        } else {
          div(
            meters.toSeq.sortBy(_._2.label).map { case (_, meter) =>
              section(
                h2(meter.label),
                p(
                  table(
                    thead(
                      th("Time"),
                      th("Value")
                    ),
                    tbody(
                      meter.timeSeries.toSeq.map { case (instant, value) =>
                        tr(
                          td(instant.toString),
                          td(value.toString())
                        )
                      }
                    ),
                    input(
                      placeholder := "New Value",
                      required := true,
                      idAttr := s"value-${meter.id.toString}"
                    ),
                    button(
                      onClick.mapTo(meter) --> onAddValueClicked,
                      "Add"
                    )
                  )
                )
              )
            }
          )
        }
      }
    )

    render(dom.document.body, app)
    ()
  }
}
