package integration.controllers

import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import play.api.libs.json.Json

import scala.concurrent.Future

import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.listener.{InMemoryDirectoryServer, InMemoryDirectoryServerConfig}
import fixtures.{StubDisclaimerService, StubStrKitService, StubUserService}
import disclaimer.DisclaimerService
import motive.{Motive, MotiveService, MotiveType}
import security.{RoleService, StubRoleRepository, StubUserRepository, UserRepository}
import services.UserService
import user.{LdapHealthService, RoleRepository, UsersModule, Role, UserStatus, UserView}
import fixtures.StubLdapHealthService
import kits.{StrKitModule, StrKitService}
import types.Permission

class StubMotiveService extends MotiveService:
  override def getMotives(motiveType: Long, editable: Boolean): Future[List[Motive]] = Future.successful(List.empty)
  override def getMotivesTypes(): Future[List[MotiveType]] = Future.successful(List.empty)
  override def deleteMotiveById(id: Long): Future[Either[String, Unit]] = Future.successful(Right(()))
  override def insert(row: Motive): Future[Either[String, Long]] = Future.successful(Right(0L))
  override def update(row: Motive): Future[Either[String, Unit]] = Future.successful(Right(()))

class StubRoleService extends RoleService:
  var getRolePermissionsResult: Map[String, Set[Permission]] = Map.empty
  var getRolesResult: Future[Seq[Role]] = Future.successful(Seq.empty)
  var getRolesForSignUpResult: Future[Seq[Role]] = Future.successful(Seq.empty)
  var addRoleResult: Future[Boolean] = Future.successful(true)
  var updateRoleResult: Future[Boolean] = Future.successful(true)
  var deleteRoleResult: Future[Either[String, Boolean]] = Future.successful(Right(true))
  var listPermissionsResult: Set[Permission] = Set.empty
  var listOperationsResult: Set[String] = Set.empty
  var translatePermissionResult: String = ""

  override def getRolePermissions(): Map[String, Set[Permission]] = getRolePermissionsResult
  override def getRoles: Future[Seq[Role]] = getRolesResult
  override def getRolesForSignUp(): Future[Seq[Role]] = getRolesForSignUpResult
  override def addRole(role: Role): Future[Boolean] = addRoleResult
  override def updateRole(role: Role): Future[Boolean] = updateRoleResult
  override def deleteRole(id: String): Future[Either[String, Boolean]] = deleteRoleResult
  override def listPermissions(): Set[Permission] = listPermissionsResult
  override def listOperations(): Set[String] = listOperationsResult
  override def translatePermission(action: String, resource: String): String = translatePermissionResult

class UserAndRoleControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private var userServiceStub: StubUserService = _
  private var roleServiceStub: StubRoleService = _

  private def createInMemoryLdapPool(): LDAPConnectionPool = {
    val config = new InMemoryDirectoryServerConfig("dc=test,dc=local")
    config.setListenerConfigs(
      com.unboundid.ldap.listener.InMemoryListenerConfig.createLDAPConfig("default", 0)
    )
    val server = new InMemoryDirectoryServer(config)
    server.startListening()
    server.getConnectionPool(1)
  }

  override def fakeApplication(): Application = {
    userServiceStub = new StubUserService
    roleServiceStub = new StubRoleService
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[StrKitModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[UserService].toInstance(userServiceStub),
        bind[RoleService].toInstance(roleServiceStub),
        bind[StrKitService].toInstance(new StubStrKitService),
        bind[DisclaimerService].toInstance(new StubDisclaimerService),
        bind[MotiveService].toInstance(new StubMotiveService),
        bind[LDAPConnectionPool].toInstance(createInMemoryLdapPool()),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()
  }

  // =========================================================================
  // UserController tests
  // =========================================================================

  "UserController" must {

    "POST /api/v2/user/signup - return 200 with SignupResponse for valid SignupSolicitude" in {
      import user.{SignupResponse, SignupSolicitude}

      val signupResponse = SignupResponse("req-123", "TOTP_SECRET", Seq("jdoe", "j.doe", "doej"))
      userServiceStub.signupRequestResult = Future.successful(Right(signupResponse))

      val body = Json.obj(
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "secret123",
        "email" -> "john@example.com",
        "roles" -> Seq("admin"),
        "geneMapperId" -> "GM01",
        "phone1" -> "1122334455",
        "superuser" -> false
      )

      val request = FakeRequest(POST, "/api/v2/user/signup").withBody(body)
      val result = route(app, request).get

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "signupRequestId").as[String] mustBe "req-123"
      (json \ "totpSecret").as[String] mustBe "TOTP_SECRET"
      (json \ "userNameCandidates").as[Seq[String]] mustBe Seq("jdoe", "j.doe", "doej")
    }

    "POST /api/v2/user/signup - return 400 for invalid JSON" in {
      val body = Json.obj("wrong" -> "data")
      val request = FakeRequest(POST, "/api/v2/user/signup").withBody(body)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "PUT /api/v2/user/signup - return 200 for valid SignupChallenge" in {
      userServiceStub.signupConfirmationResult = Future.successful(Right(0))

      val body = Json.obj(
        "signupRequestId" -> "req-123",
        "choosenUserName" -> 0,
        "challengeResponse" -> "123456"
      )

      val request = FakeRequest(PUT, "/api/v2/user/signup").withBody(body)
      val result = route(app, request).get

      status(result) mustBe OK
    }

    "POST /api/v2/user/clear-password - return 200 for valid ClearPassSolicitud" in {
      import user.ClearPassResponse
      val clearPassResponse = ClearPassResponse("clear-req-1", "TOTP_SECRET_CLEAR")
      userServiceStub.clearPassRequestResult = Future.successful(Right(clearPassResponse))

      val body = Json.obj(
        "userName" -> "jdoe",
        "newPassword" -> "newSecret456"
      )

      val request = FakeRequest(POST, "/api/v2/user/clear-password").withBody(body)
      val result = route(app, request).get

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "clearPasswordRequestId").as[String] mustBe "clear-req-1"
      (json \ "totpSecret").as[String] mustBe "TOTP_SECRET_CLEAR"
    }

    "PUT /api/v2/user/clear-password - return 200 for valid ClearPassChallenge" in {
      userServiceStub.clearPassConfirmationResult = Future.successful(Right(0))

      val body = Json.obj(
        "clearPassRequestId" -> "clear-req-1",
        "challengeResponse" -> "654321"
      )

      val request = FakeRequest(PUT, "/api/v2/user/clear-password").withBody(body)
      val result = route(app, request).get

      status(result) mustBe OK
    }

    "GET /api/v2/users - return list of UserView" in {
      val users = Seq(
        UserView("jdoe", "John", "Doe", "john@example.com", Seq("admin"), UserStatus.active, "GM01", "1122334455", None, false),
        UserView("asmith", "Alice", "Smith", "alice@example.com", Seq("analyst"), UserStatus.pending, "GM02", "9988776655", Some("5544332211"), true)
      )
      userServiceStub.listAllUsersResult = Future.successful(users)

      val request = FakeRequest(GET, "/api/v2/users")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val json = contentAsJson(result)
      val parsed = json.as[Seq[UserView]]
      parsed.size mustBe 2
      parsed.head.userName mustBe "jdoe"
      parsed(1).userName mustBe "asmith"
    }

    "PUT /api/v2/users/:userId/status - return 200 for valid status" in {
      userServiceStub.setStatusResult = Future.successful(Right(0))

      val body = Json.toJson(UserStatus.active)
      val request = FakeRequest(PUT, "/api/v2/users/jdoe/status").withBody(body)
      val result = route(app, request).get

      status(result) mustBe OK
    }

    "PUT /api/v2/users - return 200 for valid UserView" in {
      userServiceStub.updateUserResult = Future.successful(true)

      val userView = UserView("jdoe", "John", "Doe", "john@example.com", Seq("admin"), UserStatus.active, "GM01", "1122334455", None, false)
      val request = FakeRequest(PUT, "/api/v2/users").withBody(Json.toJson(userView))
      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "status").as[Boolean] mustBe true
    }

    "POST /api/v2/user/signup - return 400 when service returns Left" in {
      userServiceStub.signupRequestResult = Future.successful(Left("signup error"))

      val body = Json.obj(
        "firstName" -> "John",
        "lastName" -> "Doe",
        "password" -> "secret123",
        "email" -> "john@example.com",
        "roles" -> Seq("admin"),
        "geneMapperId" -> "GM01",
        "phone1" -> "1122334455",
        "superuser" -> false
      )

      val request = FakeRequest(POST, "/api/v2/user/signup").withBody(body)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "PUT /api/v2/users/:userId/status - return 400 for invalid status string" in {
      val body = Json.toJson("not_a_valid_status")
      val request = FakeRequest(PUT, "/api/v2/users/jdoe/status").withBody(body)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "POST /api/v2/user/clear-password - return 400 for invalid JSON" in {
      val body = Json.obj("wrong" -> "fields")
      val request = FakeRequest(POST, "/api/v2/user/clear-password").withBody(body)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }
  }

  // =========================================================================
  // RoleController tests
  // =========================================================================

  "RoleController" must {

    "GET /api/v2/roles/permissions - return Set[Permission] as JSON" in {
      roleServiceStub.listPermissionsResult = Set(Permission.DNA_PROFILE_CRUD, Permission.USER_CRUD)

      val request = FakeRequest(GET, "/api/v2/roles/permissions")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val permissions = contentAsJson(result).as[Set[Permission]]
      permissions must contain(Permission.DNA_PROFILE_CRUD)
      permissions must contain(Permission.USER_CRUD)
    }

    "GET /api/v2/roles/permissions/full - return permissions with operations detail" in {
      roleServiceStub.listPermissionsResult = Set(Permission.ROLE_CRUD)

      val request = FakeRequest(GET, "/api/v2/roles/permissions/full")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val json = contentAsJson(result)
      // Full permissions serialize each Permission with an id and operations array
      json.as[Seq[play.api.libs.json.JsObject]].size mustBe 1
      val first = json.as[Seq[play.api.libs.json.JsObject]].head
      (first \ "id").as[String] mustBe "ROLE_CRUD"
      (first \ "operations").as[Seq[play.api.libs.json.JsObject]].nonEmpty mustBe true
    }

    "GET /api/v2/roles/operations - return Set[String]" in {
      roleServiceStub.listOperationsResult = Set("GET /profiles", "POST /profiles")

      val request = FakeRequest(GET, "/api/v2/roles/operations")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val ops = contentAsJson(result).as[Set[String]]
      ops must contain("GET /profiles")
      ops must contain("POST /profiles")
    }

    "GET /api/v2/rolesForSU - return roles for signup" in {
      val roles = Seq(
        Role("role1", "Analyst", Set(Permission.DNA_PROFILE_CRUD)),
        Role("role2", "Admin", Set(Permission.USER_CRUD))
      )
      roleServiceStub.getRolesForSignUpResult = Future.successful(roles)

      val request = FakeRequest(GET, "/api/v2/rolesForSU")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val parsed = contentAsJson(result).as[Seq[Role]]
      parsed.size mustBe 2
      parsed.head.id mustBe "role1"
      parsed(1).roleName mustBe "Admin"
    }

    "GET /api/v2/roles - return list of roles" in {
      val roles = Seq(
        Role("admin", "Administrator", Set(Permission.USER_CRUD, Permission.ROLE_CRUD)),
        Role("analyst", "Analyst", Set(Permission.DNA_PROFILE_CRUD))
      )
      roleServiceStub.getRolesResult = Future.successful(roles)

      val request = FakeRequest(GET, "/api/v2/roles")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      val parsed = contentAsJson(result).as[Seq[Role]]
      parsed.size mustBe 2
      parsed.head.id mustBe "admin"
    }

    "POST /api/v2/roles - return 200 with X-CREATED-ID header for valid Role" in {
      roleServiceStub.addRoleResult = Future.successful(true)

      val role = Role("new-role", "New Role", Set(Permission.DNA_PROFILE_CRUD))
      val request = FakeRequest(POST, "/api/v2/roles").withBody(Json.toJson(role))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe true
      header("X-CREATED-ID", result) mustBe Some("new-role")
    }

    "PUT /api/v2/roles - return 200 for valid Role" in {
      roleServiceStub.updateRoleResult = Future.successful(true)

      val role = Role("existing-role", "Updated Role", Set(Permission.USER_CRUD))
      val request = FakeRequest(PUT, "/api/v2/roles").withBody(Json.toJson(role))
      val result = route(app, request).get

      status(result) mustBe OK
      contentAsJson(result).as[Boolean] mustBe true
    }

    "DELETE /api/v2/roles/:id - return result JSON on success" in {
      roleServiceStub.deleteRoleResult = Future.successful(Right(true))

      val request = FakeRequest(DELETE, "/api/v2/roles/role-to-delete")
      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "result").as[Boolean] mustBe true
    }

    "DELETE /api/v2/roles/:id - return result JSON with error on failure" in {
      roleServiceStub.deleteRoleResult = Future.successful(Left("Role is in use"))

      val request = FakeRequest(DELETE, "/api/v2/roles/role-in-use")
      val result = route(app, request).get

      status(result) mustBe OK
      (contentAsJson(result) \ "result").as[Boolean] mustBe false
      (contentAsJson(result) \ "error").as[String] mustBe "Role is in use"
    }

    "GET /api/v2/roles/export - return JSON with Content-Disposition header" in {
      val roles = Seq(Role("admin", "Administrator", Set(Permission.USER_CRUD)))
      roleServiceStub.getRolesResult = Future.successful(roles)

      val request = FakeRequest(GET, "/api/v2/roles/export")
      val result = route(app, request).get

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      header("Content-Disposition", result) mustBe Some("attachment; filename=roles.json")
    }

    "POST /api/v2/roles/import - return 400 when no file is provided" in {
      val request = FakeRequest(POST, "/api/v2/roles/import")
        .withMultipartFormDataBody(
          play.api.mvc.MultipartFormData(
            dataParts = Map.empty,
            files = Seq.empty,
            badParts = Seq.empty
          )
        )
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }

    "POST /api/v2/roles - return 400 for invalid JSON" in {
      val body = Json.obj("wrong" -> "data")
      val request = FakeRequest(POST, "/api/v2/roles").withBody(body)
      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST
    }
  }
}
