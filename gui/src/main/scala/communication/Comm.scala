package communication

import java.util.UUID

import scala.concurrent.Future
import scala.concurrent.Promise
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{JSONBody, PlainTextBody}
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success, Try}
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.reactive.Observer
import upickle.Js._

/**
  * Created by kristofer on 2017-01-04.
  */
object Comm {

  def sendRequest(
        mess: upickle.Js.Value,
        header: upickle.Js.Obj = upickle.Js.Obj("empty"-> upickle.Js.Str("header"))
        ): Future[String] = {

    val url = org.scalajs.dom.window.location.href
    val request = HttpRequest(url).withPath("/request")
    val p = Promise[String]

    // upd header

    import fr.hmil.roshttp.body.Implicits._
    val json = fr.hmil.roshttp.body.JSONBody.JSONObject(
      "header"->upickle.default.write(header),
      "body" -> upickle.default.write(mess)
    )

    request.post(json).onComplete {
      case x: Success[SimpleHttpResponse] =>
        //check for errors here later
        p.complete(Try{x.value.body})
      case x: Failure[SimpleHttpResponse] =>
        p.failure(x.exception)

    }

    p.future

  }





}
