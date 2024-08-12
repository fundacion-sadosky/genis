package controllers

import java.security.MessageDigest
import org.apache.commons.codec.binary.Hex

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import matching.Algorithm
import matching.Stringency
import play.api.Play
import play.api.Routes
import play.api.Application
import play.api.Play.current
import play.api.cache.Cache
import play.api.i18n.{Lang, Messages}
import play.api.libs.functional.syntax.functionalCanBuildApplicative
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads.functorReads
import play.api.libs.json.Reads.minLength
import play.api.libs.json.Writes
import play.api.libs.json.__
import play.api.mvc.{Action, AnyContent, Controller, DiscardingCookie}
import play.twirl.api.Html
import security.AuthorisationOperation
import security.StaticAuthorisationOperation
import types.{Mode, Permission, SampleCode}
import user.UserStatus
import profiledata.ProfileDataService

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
@Singleton
class Application @Inject() (
  app: play.api.Application,
  @Named("genisManifest") manifest: Map[String, String],
  mode: Mode,
  profileData:ProfileDataService
) extends Controller {

  def manifestAsFixedTable(manifest: Map[String, String]): Html = {
    val lineLength = 71
    val header = "<!-- ####################    Manifest file contents     #################### -->\n"
    val footer = "\n<!-- ####################################################################### -->"
    val text = manifest
      .map {
        case (k, v) =>
          val entry = (k + ": " + v).take(lineLength)
          val padding = " " * (lineLength - entry.length())
          "<!-- " + entry + padding + " -->"
      }
      .toList
      .sorted
      .mkString("\n")
    Html(header + text + footer)
  }

  /** Serves the index page, see views/index.scala.html */
  def index: Action[AnyContent] = Action {
    val cssFileName = mode match {
      case Mode.Prod => "main"
      case Mode.Sandbox => "mainTest"
      case Mode.SandboxAzul => "mainAzul"
      case Mode.SandboxAmarillo => "mainAmarillo"
      case Mode.SandboxVerde => "mainVerde"
    }
    Ok(views.html.index(manifestAsFixedTable(manifest), cssFileName))
  }

  def pedigree: Action[AnyContent] = Action {
    Ok(views.html.pedigree())
  }

  def appParam = Action {
    val mtRcrs = Await.result(this.profileData.getMtRcrs(),  Duration.Inf);

    val labCode = app.configuration.getString("laboratory.code").get
    val country = app.configuration.getString("laboratory.country").get
    val province = app.configuration.getString("laboratory.province").get
    val ppgcd = app.configuration.getString("protoprofile.globalCode.dummy").get
    val defaultMutationRateI = app.configuration.getString("mt.defaultMutationRateI").get
    val defaultMutationRateF = app.configuration.getString("mt.defaultMutationRateF").get
    val defaultMutationRateM = app.configuration.getString("mt.defaultMutationRateM").get
    val defaultMutationRange = app.configuration.getString("mt.defaultMutationRange").get
    val defaultMutationRateMicrovariant = app.configuration.getString("mt.defaultMutationRateMicrovariant").get
    import collection.JavaConversions._
    val mtRegionsConf = app.configuration.getObject("mt.regions").get
    val regions = mtRegionsConf.keySet().toSeq
//    val hvVariables = regions.map(key => s"${key}_RANGE" -> mtRegionsConf.toConfig.getIntList(key).toList.map(_.toInt))

    val version = (for {
      iver <- manifest.get("Implementation-Version")
      //gcom <- manifest.get("Git-Head-Rev")
    } yield {
      //s"$iver r${gcom.take(9)}"
      s"$iver"
    }).getOrElse("develop")

    var appJson = Json.obj(
      "manifest" -> manifest,
      "labCode" -> labCode,
      "country" -> country,
      "province" -> province,
      "stringencies" -> Stringency.values.filter { _ != Stringency.ImpossibleMatch },
      "userStatusList" -> UserStatus.values,
      "matchingAlgorithms" -> Algorithm.values,
      "protoProfileGlobalCodeDummy" -> ppgcd,
      "version" -> version,
      "defaultMutationRateI" -> defaultMutationRateI,
      "defaultMutationRateF" -> defaultMutationRateF,
      "defaultMutationRateM" -> defaultMutationRateM,
      "defaultMutationRange" -> defaultMutationRange,
      "defaultMutationRateMicrovariant" -> defaultMutationRateMicrovariant,
      "sampleCodeRegex" -> SampleCode.validationRe.regex,
      "mtRCRS" -> mtRcrs)

//    val mtRegionsConf2 = app.configuration.getObject("mt.regions2").get
//    val hvVariables2 = mtRegionsConf2.keySet().toSeq.map(key => s"${key}_2_RANGE" -> mtRegionsConf2.toConfig.getIntList(key).toList.map(_.toInt))

//    (hvVariables ++ hvVariables2).foreach { case (hv, range) => appJson = appJson + (hv -> Json.toJson(range))}

    Ok(views.html.application(Json.stringify(appJson))).as("text/javascript")
  }

  def senseOper = Action {
    val sensitiveOperations: String = getSensitiveOperations()
    Ok(views.html.sensitiveOp(sensitiveOperations)).as("text/javascript")
  }

  /**
   * Retrieves all routes via reflection.
   * http://stackoverflow.com/questions/12012703/less-verbose-way-of-generating-play-2s-javascript-router
   * @todo If you have controllers in multiple packages, you need to add each package here.
   */
  val jsRouter = {
    val routeCache = {
      val jsRoutesClass = classOf[routes.javascript]
      val controllers = jsRoutesClass.getFields.map(_.get(null))
      controllers.flatMap { controller =>
        controller.getClass.getDeclaredMethods.map { action =>
          action.invoke(controller).asInstanceOf[play.core.Router.JavascriptReverseRoute]
        }
      }
    }

    Routes.javascriptRouter("jsRoutes", None, "", routeCache: _*)
  }

  val jsRouterMd5 = {
    val md5 = MessageDigest.getInstance("MD5").digest(jsRouter.body.getBytes)
    new String(Hex.encodeHex(md5))
  }

  def jsRoutes = Action { request =>
    request.headers
      .get(IF_NONE_MATCH)
      .fold {
        Ok(jsRouter).as(JAVASCRIPT).withHeaders(ETAG -> jsRouterMd5)
      } { etags =>
        if (etags.split(',').exists(_.trim == jsRouterMd5)) {
          NotModified
        } else {
          Ok(jsRouter).as(JAVASCRIPT).withHeaders(ETAG -> jsRouterMd5)
        }
      }
  }

  def getSensitiveOperations(): String = {
    implicit val staticAuthorisationOperationWrites: Writes[AuthorisationOperation] = (
      (__ \ "resource").write[String] ~
      (__ \ "action").write[String])((a: AuthorisationOperation) => (a.resource, a.action))

    val permissionToOperationSet = Permission.list.foldLeft(
      Set[StaticAuthorisationOperation]())((opsSet, permission) => opsSet ++ permission.operations)
      .filter(x => x.isSensitive).map(x => new AuthorisationOperation(x.resource.toString, x.action.toString))

    Json.stringify(Json.toJson(permissionToOperationSet))
  }

  def changeLanguage(lang: String): Action[AnyContent] = Action {
      implicit request => {
        val pLang = request.cookies.get("PLAY_LANG").map(_.value)
        val l = implicitly[Lang]
        val s0 = Messages("error.E0304")
        Redirect("/")
          .withLang(Lang(lang))
      }
  }
}
