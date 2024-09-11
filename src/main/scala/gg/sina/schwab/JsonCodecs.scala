package gg.sina.schwab

import org.joda.time.{DateTime, LocalDate}
import zio.json._

object JsonCodecs {
  implicit val jodaDateTimeDecoder: JsonDecoder[DateTime] = JsonDecoder[String]
    .map(DateTime.parse) <> JsonDecoder[Long].map(new DateTime(_))
  implicit val jodaLocalDateDecoder: JsonDecoder[LocalDate] = JsonDecoder[String]
    .map(LocalDate.parse) <> JsonDecoder[Long].map(new LocalDate(_))
  

}
