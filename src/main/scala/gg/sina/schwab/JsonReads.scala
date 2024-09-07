package gg.sina.schwab

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{JsValue, Reads}

object JsonReads {
  implicit val jodaDateTimeReads: Reads[DateTime] = (json: JsValue) => json
    .validate[String]
    .map(DateTime.parse)
    .orElse(json.validate[Long])
    .map(new DateTime(_))
  implicit val jodaLocalDateReads: Reads[LocalDate] = (json: JsValue) => json
    .validate[String]
    .map(LocalDate.parse)
    .orElse(json.validate[Long])
    .map(new LocalDate(_))
}
