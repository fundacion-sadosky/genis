package configdata

import play.api.libs.json.Json

case class Country(
    code: String,
    name: String
)

object Country {
  implicit val countryFormat = Json.format[Country]
}

case class Province(
    code: String,
    name: String
)

object Province {
  implicit val provinceFormat = Json.format[Province]
}