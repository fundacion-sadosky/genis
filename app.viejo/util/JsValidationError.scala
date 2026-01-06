package util

import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath

object JsValidationError {

  private def jsErrorMsg(error: ValidationError): (String, Seq[String]) = {
    error.message match {
     case "error.required" => ("error.E3001", Seq())
     case "error.expected.int" => ("error.E3002", Seq())
     case "error.expected.long" => ("error.E3003", Seq())
     case "error.expected.float" => ("error.E3004", Seq())
     case "error.expected.double" => ("error.E3005", Seq())
     case "error.expected.boolean" => ("error.E3006", Seq())
     case "error.expected.jsstring" => ("error.E3007", Seq())
     case "error.expected.jsnumber" => ("error.E3008", Seq())
     case "error.expected.jsboolean" => ("error.E3009", Seq())
     case "error.expected.jsarray" => ("error.E3010", Seq())
     case "error.expected.jsobject" => ("error.E3011", Seq())
     case "error.minLength" => ("error.E3012", Seq())
     case "error.maxLength" => ("error.E3013", Seq())
     case "error.pattern" => ("error.E3014", Seq())
     case "error.invalid" => ("error.E3015", Seq())
     case "error.enum" => ("error.E3016", Seq())
     case "error.invalid.date" => ("error.E3017", Seq())
     case "error.invalid.datetime" => ("error.E3018", Seq())
     case "error.invalid.email" => ("error.E3019", Seq())
     case _ => ("error.E3000", Seq(error.message))
    }
  }

  def toJsonResult(errors: Seq[(JsPath, Seq[ValidationError])]): Seq[(String, Seq[String])] = {
    errors
      .flatMap {
        case (_, error) => error
      }
      .map(
        x => jsErrorMsg(x)
      )
  }
}
