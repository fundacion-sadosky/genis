package controllers

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.twirl.api.Html

import matching.{Algorithm, Stringency}
import security.{AuthService, AuthorisationOperation}
import types.SampleCode
import user.UserStatus

@Singleton
class ApplicationController @Inject()(
  cc: ControllerComponents,
  config: Configuration,
  authService: AuthService
) extends AbstractController(cc):

  private val cssMode = config.getOptional[String]("genis.cssMode").getOrElse("main")

  def index: Action[AnyContent] = Action:
    Ok(views.html.index(cssMode))

  def pedigree: Action[AnyContent] = Action:
    Ok(views.html.pedigreeView())

  def jsRoutes: Action[AnyContent] = Action:
    Ok(views.html.jsroutes()).as(JAVASCRIPT)

  def sensitiveOper: Action[AnyContent] = Action:
    given operationWrites: Writes[AuthorisationOperation] =
      import play.api.libs.json.*
      import play.api.libs.functional.syntax.*
      (
        (__ \ "resource").write[String] and
        (__ \ "action").write[String]
      )((a: AuthorisationOperation) => (a.resource, a.action))

    val ops = authService.getSensitiveOperations()
    Ok(views.html.sensitiveOp(Json.stringify(Json.toJson(ops)))).as(JAVASCRIPT)

  def appConf: Action[AnyContent] = Action:
    val labCode    = config.get[String]("laboratory.code")
    val country    = config.get[String]("laboratory.country")
    val province   = config.get[String]("laboratory.province")
    val ppgcd      = config.getOptional[String]("protoprofile.globalCode.dummy").getOrElse("XX-X-XXXX-")
    val version    = config.getOptional[String]("application.version").getOrElse("develop")

    val defaultMutationRateI            = config.getOptional[String]("mt.defaultMutationRateI").getOrElse("0.001628")
    val defaultMutationRateF            = config.getOptional[String]("mt.defaultMutationRateF").getOrElse("0.000463")
    val defaultMutationRateM            = config.getOptional[String]("mt.defaultMutationRateM").getOrElse("0.001584")
    val defaultMutationRange            = config.getOptional[String]("mt.defaultMutationRange").getOrElse("0.5")
    val defaultMutationRateMicrovariant = config.getOptional[String]("mt.defaultMutationRateMicrovariant").getOrElse("0.0000005")

    val stringencies     = Stringency.values.filter(_ != Stringency.ImpossibleMatch).map(_.toString)
    val userStatusList   = UserStatus.values.map(_.toString)
    val matchingAlgos    = Algorithm.values.map(_.toString)
    val sampleCodeRegex  = SampleCode.validationRe.regex

    val appJson = Json.obj(
      "labCode"                        -> labCode,
      "country"                        -> country,
      "province"                       -> province,
      "stringencies"                   -> stringencies,
      "userStatusList"                 -> userStatusList,
      "matchingAlgorithms"             -> matchingAlgos,
      "protoProfileGlobalCodeDummy"    -> ppgcd,
      "version"                        -> version,
      "defaultMutationRateI"           -> defaultMutationRateI,
      "defaultMutationRateF"           -> defaultMutationRateF,
      "defaultMutationRateM"           -> defaultMutationRateM,
      "defaultMutationRange"           -> defaultMutationRange,
      "defaultMutationRateMicrovariant"-> defaultMutationRateMicrovariant,
      "sampleCodeRegex"                -> sampleCodeRegex
    )
    Ok(views.html.application(Json.stringify(appJson))).as(JAVASCRIPT)