package sp.server

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import upickle.default.{Reader, Writer, readJs, writeJs}
import upickle.{Js, json}

import scala.reflect.ClassTag

///**
//  * Automatic to and from JSON marshalling/unmarshalling using *upickle* protocol.
//  */
//object UpickleSupport extends UpickleSupport
//
///**
//  * Automatic to and from JSON marshalling/unmarshalling using *upickle* protocol.
//  */
//trait UpickleSupport {
//
//  private val jsonStringUnmarshaller =
//    Unmarshaller.byteStringUnmarshaller
//      .forContentTypes(`application/json`)
//      .mapWithCharset {
//        case (ByteString.empty, _) => throw Unmarshaller.NoContentException
//        case (data, charset)       => data.decodeString(charset.nioCharset.name)
//      }
//
//  private val jsonStringMarshaller =
//    Marshaller.stringMarshaller(`application/json`)
//
//  /**
//    * HTTP entity => `A`
//    *
//    * @param reader reader for `A`
//    * @tparam A type to decode
//    * @return unmarshaller for `A`
//    */
//  implicit def upickleUnmarshaller[A](implicit reader: Reader[A]
//                                     ): FromEntityUnmarshaller[A] =
//    jsonStringUnmarshaller.map(data => FixedType.read[A](data))
//
//  /**
//    * `A` => HTTP entity
//    *
//    * @param writer writer for `A`
//    * @tparam A type to encode
//    * @return marshaller for any `A` value
//    */
//  implicit def upickleMarshaller[A](
//                                     implicit writer: Writer[A]
//                                   ): ToEntityMarshaller[A] =
//    jsonStringMarshaller.compose(FixedType.write[A](_))
//}

