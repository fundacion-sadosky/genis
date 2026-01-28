package controllers

import specs.PdgSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Results
import play.api.libs.json.JsString
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import security.AuthService
import org.mockito.Mockito.when
import stubs.Stubs
import scala.concurrent.Future
import security.RequestToken
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.FakeRequest
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.test.Helpers.OK
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.contentType
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import security.AuthenticatedPair

class AuthenticationTest extends PdgSpec with MockitoSugar with Results {

  val reqToken = Stubs.requestToken
  val otpToken = Stubs.totpToken
  val returnReqToken = Future.successful(Some(reqToken))
  val fullUser = Stubs.fullUser
  val returnAuth = Future.successful(Some(fullUser))
  val authenticationRequest = Stubs.userPassword
  
  "Authentication Controller" must{
    
   "Authenticate a user and give him the credentials" in {
     
     val authServiceMock = mock[AuthService]
     when(authServiceMock.authenticate("user", "pass", otpToken)).thenReturn(returnAuth)
     
     val request = FakeRequest().withBody(Json.toJson(authenticationRequest))
     val target = new Authentication(authServiceMock)
     val result: Future[Result] = target.login().apply(request)
     
     status(result) mustBe OK
     contentType(result).get mustBe "application/json"
     
     val jsonToken = contentAsJson(result)
     jsonToken mustBe Json.toJson(fullUser)
   }
  }
  
}