# Plan de Migración del Módulo Inbox a GENis 6.0

## 📊 Estado Actual

### En `_LEGACY_BACKUP/app.viejo/inbox/`
```
Notification.scala           - Modelos principales
NotificationType.scala       - Tipos de notificaciones
NotificationService.scala    - Lógica de negocio
NotificationRepository.scala - Acceso a datos
NotificationSearch.scala     - Búsqueda avanzada
NotificationModule.scala     - Inyección de dependencias
```

### En `_LEGACY_BACKUP/app.viejo/assets/javascripts/inbox/`
```
controllers/         - Controladores AngularJS
services/            - Servicios AngularJS
views/               - Plantillas HTML
main.js              - Inicialización
```

### Rutas en `conf/routes.viejo`
```
GET  /notifications                    - Obtener notificaciones
POST /notifications/search             - Buscar notificaciones
POST /notifications/total              - Contar notificaciones
DELETE /notifications/:id              - Eliminar notificación
POST /notifications/flag/:id           - Marcar/desmarcar notificación
POST /notifications/:id/pending        - Cambiar estado pending
```

---

## 🎯 Objetivo de Migración

Implementar el módulo Inbox en el nuevo marco de trabajo:
- **Backend**: Scala + Play 3.0 + Slick ORM
- **Frontend**: React o mantener AngularJS (requiere decisión)
- **BD**: PostgreSQL (genisdb)
- **Arquitectura**: Controllers → Services → Repositories

---

## 📝 Paso 1: Preparar la Rama

```bash
# Estando en /home/cdiaz/Descargas/genis
git checkout -b feature/inbox-migration
git status
```

---

## 🗂️ Paso 2: Estructura de Directorios en el Nuevo Proyecto

```
app/
├── controllers/
│   └── NotificationController.scala     (nuevo)
├── models/
│   └── notification/
│       ├── Notification.scala           (nuevo)
│       ├── NotificationType.scala       (nuevo)
│       └── NotificationSearch.scala     (nuevo)
├── services/
│   └── NotificationService.scala        (nuevo)
├── repositories/
│   └── NotificationRepository.scala     (nuevo)
└── modules/
    └── NotificationModule.scala         (nuevo)

conf/
└── evolutions/
    └── default/
        └── 4__notification_tables.sql   (nuevo - migraciones)
```

---

## 📋 Paso 3: Modelos de Datos

### 3.1 NotificationType.scala
```scala
package models.notification

object NotificationType extends Enumeration {
  type NotificationType = Value
  
  val matching = Value("matching")
  val bulkImport = Value("bulkImport")
  val userNotification = Value("userNotification")
  val profileData = Value("profileData")
  val profileDataAssociation = Value("profileDataAssociation")
  val pedigreeMatching = Value("pedigreeMatching")
  val pedigreeLR = Value("pedigreeLR")
  val inferiorInstancePending = Value("inferiorInstancePending")
  val hitMatch = Value("hitMatch")
  val discardMatch = Value("discardMatch")
  val deleteProfile = Value("deleteProfile")
  val collapsing = Value("collapsing")
  val pedigreeConsistency = Value("pedigreeConsistency")
  val profileUploaded = Value("profileUploaded")
  val approvedProfile = Value("approvedProfile")
  val rejectedProfile = Value("rejectedProfile")
  val deletedProfileInSuperiorInstance = Value("deletedProfileInSuperiorInstance")
  val deletedProfileInInferiorInstance = Value("deletedProfileInInferiorInstance")
  val profileChangeCategory = Value("profileChangeCategory")
}
```

### 3.2 Notification.scala
```scala
package models.notification

import java.time.LocalDateTime
import play.api.libs.json._

case class Notification(
  id: Long,
  userId: String,
  createdAt: LocalDateTime,
  updatedAt: Option[LocalDateTime],
  flagged: Boolean,
  pending: Boolean,
  notificationType: NotificationType.Value,
  title: String,
  description: String,
  url: String,
  metadata: Option[JsObject]  // JSONB para datos adicionales
)

object Notification {
  implicit val writes: Writes[Notification] = Json.writes[Notification]
  implicit val reads: Reads[Notification] = Json.reads[Notification]
}

case class NotificationSearch(
  userId: Option[String] = None,
  notificationType: Option[NotificationType.Value] = None,
  pending: Option[Boolean] = None,
  flagged: Option[Boolean] = None,
  page: Int = 0,
  pageSize: Int = 25
)
```

---

## 🗄️ Paso 4: Esquema de Base de Datos

Crear archivo: `conf/evolutions/default/4__notification_tables.sql`

```sql
-- Evolutions -- !Ups

CREATE TABLE notifications (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP,
  flagged BOOLEAN NOT NULL DEFAULT FALSE,
  pending BOOLEAN NOT NULL DEFAULT TRUE,
  notification_type VARCHAR(100) NOT NULL,
  title VARCHAR(500) NOT NULL,
  description TEXT NOT NULL,
  url TEXT,
  metadata JSONB,
  FOREIGN KEY (user_id) REFERENCES users(username) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(notification_type);
CREATE INDEX idx_notifications_pending ON notifications(pending);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- Evolutions -- !Downs

DROP TABLE notifications;
```

---

## 💾 Paso 5: Repository

Crear: `app/repositories/NotificationRepository.scala`

```scala
package repositories

import models.notification.{Notification, NotificationSearch, NotificationType}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationRepository @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]
  
  import dbConfig.profile.api._
  import dbConfig.db

  private class NotificationsTable(tag: Tag) 
    extends Table[Notification](tag, "notifications") {
    
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[String]("user_id")
    def createdAt = column[java.time.LocalDateTime]("created_at")
    def updatedAt = column[Option[java.time.LocalDateTime]]("updated_at")
    def flagged = column[Boolean]("flagged")
    def pending = column[Boolean]("pending")
    def notificationType = column[String]("notification_type")
    def title = column[String]("title")
    def description = column[String]("description")
    def url = column[Option[String]]("url")
    def metadata = column[Option[play.api.libs.json.JsObject]]("metadata")

    def * = (id, userId, createdAt, updatedAt, flagged, pending, 
             notificationType, title, description, url, metadata) <> 
             ((n) => Notification(n._1, n._2, n._3, n._4, n._5, n._6, 
                                  NotificationType.withName(n._7), n._8, n._9, n._10, n._11),
              (n: Notification) => Some((n.id, n.userId, n.createdAt, n.updatedAt, n.flagged, 
                                        n.pending, n.notificationType.toString, n.title, 
                                        n.description, n.url, n.metadata)))
  }

  private val notifications = TableQuery[NotificationsTable]

  def create(notification: Notification): Future[Notification] = db.run {
    (notifications returning notifications.map(_.id) 
      into ((_, id) => notification.copy(id = id))) += notification
  }

  def findById(id: Long): Future[Option[Notification]] = db.run {
    notifications.filter(_.id === id).result.headOption
  }

  def findByUserId(userId: String): Future[Seq[Notification]] = db.run {
    notifications
      .filter(_.userId === userId)
      .sortBy(_.createdAt.desc)
      .result
  }

  def search(search: NotificationSearch): Future[Seq[Notification]] = db.run {
    var query = notifications.asInstanceOf[Query[NotificationsTable, Notification, Seq]]
    
    if (search.userId.isDefined) {
      query = query.filter(_.userId === search.userId.get)
    }
    if (search.notificationType.isDefined) {
      query = query.filter(_.notificationType === search.notificationType.get.toString)
    }
    if (search.pending.isDefined) {
      query = query.filter(_.pending === search.pending.get)
    }
    if (search.flagged.isDefined) {
      query = query.filter(_.flagged === search.flagged.get)
    }

    query
      .sortBy(_.createdAt.desc)
      .drop(search.page * search.pageSize)
      .take(search.pageSize)
      .result
  }

  def count(userId: String): Future[Int] = db.run {
    notifications.filter(_.userId === userId).filter(_.pending === true).length.result
  }

  def update(id: Long, flagged: Option[Boolean], pending: Option[Boolean]): Future[Int] = db.run {
    val query = notifications.filter(_.id === id)
    val updateFields = Seq(
      flagged.map(f => notifications.map(_.flagged) -> f),
      pending.map(p => notifications.map(_.pending) -> p)
    ).flatten

    if (updateFields.nonEmpty) {
      query.update(Notification(
        id = 0, userId = "", createdAt = java.time.LocalDateTime.now(), 
        updatedAt = Some(java.time.LocalDateTime.now()), flagged = flagged.getOrElse(false),
        pending = pending.getOrElse(false), notificationType = NotificationType.userNotification,
        title = "", description = "", url = None, metadata = None
      )).map(_ => 1)
    } else {
      DBIO.successful(0)
    }
  }

  def delete(id: Long): Future[Int] = db.run {
    notifications.filter(_.id === id).delete
  }
}
```

---

## 🔧 Paso 6: Service

Crear: `app/services/NotificationService.scala`

```scala
package services

import models.notification.{Notification, NotificationSearch, NotificationType}
import repositories.NotificationRepository
import javax.inject.{Inject, Singleton}
import java.time.LocalDateTime
import play.api.libs.json.JsObject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationService @Inject() (
  notificationRepository: NotificationRepository
)(implicit ec: ExecutionContext) {

  def createNotification(
    userId: String,
    notificationType: NotificationType.Value,
    title: String,
    description: String,
    url: Option[String] = None,
    metadata: Option[JsObject] = None
  ): Future[Notification] = {
    val notification = Notification(
      id = 0,
      userId = userId,
      createdAt = LocalDateTime.now(),
      updatedAt = None,
      flagged = false,
      pending = true,
      notificationType = notificationType,
      title = title,
      description = description,
      url = url.getOrElse(""),
      metadata = metadata
    )
    notificationRepository.create(notification)
  }

  def getNotifications(userId: String): Future[Seq[Notification]] = {
    notificationRepository.findByUserId(userId)
  }

  def searchNotifications(search: NotificationSearch): Future[Seq[Notification]] = {
    notificationRepository.search(search)
  }

  def countPendingNotifications(userId: String): Future[Int] = {
    notificationRepository.count(userId)
  }

  def markAsRead(id: Long): Future[Int] = {
    notificationRepository.update(id, flagged = None, pending = Some(false))
  }

  def toggleFlag(id: Long, flagged: Boolean): Future[Int] = {
    notificationRepository.update(id, flagged = Some(flagged), pending = None)
  }

  def deleteNotification(id: Long): Future[Int] = {
    notificationRepository.delete(id)
  }
}
```

---

## 🎮 Paso 7: Controller

Crear: `app/controllers/NotificationController.scala`

```scala
package controllers

import models.notification.{NotificationSearch, NotificationType}
import services.NotificationService
import play.api.mvc._
import play.api.libs.json._
import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class NotificationController @Inject() (
  cc: ControllerComponents,
  notificationService: NotificationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def getNotifications: Action[AnyContent] = Action.async { request =>
    val userId = request.session.get("userId").getOrElse("unknown")
    notificationService.getNotifications(userId).map { notifications =>
      Ok(Json.toJson(notifications))
    }
  }

  def search: Action[JsValue] = Action.async(parse.json) { request =>
    val userId = request.session.get("userId").getOrElse("unknown")
    
    val notificationType = (request.body \ "notificationType").asOpt[String]
      .flatMap(t => try Some(NotificationType.withName(t)) catch { case _: Exception => None })
    val pending = (request.body \ "pending").asOpt[Boolean]
    val flagged = (request.body \ "flagged").asOpt[Boolean]
    val page = (request.body \ "page").asOpt[Int].getOrElse(0)
    val pageSize = (request.body \ "pageSize").asOpt[Int].getOrElse(25)

    val search = NotificationSearch(
      userId = Some(userId),
      notificationType = notificationType,
      pending = pending,
      flagged = flagged,
      page = page,
      pageSize = pageSize
    )

    notificationService.searchNotifications(search).map { notifications =>
      Ok(Json.toJson(notifications))
    }
  }

  def count: Action[AnyContent] = Action.async { request =>
    val userId = request.session.get("userId").getOrElse("unknown")
    notificationService.countPendingNotifications(userId).map { count =>
      Ok(Json.obj("count" -> count))
    }
  }

  def markAsRead(id: Long): Action[AnyContent] = Action.async { request =>
    notificationService.markAsRead(id).map { result =>
      if (result > 0) Ok(Json.obj("success" -> true))
      else NotFound(Json.obj("error" -> "Notification not found"))
    }
  }

  def toggleFlag(id: Long, flag: Boolean): Action[AnyContent] = Action.async { request =>
    notificationService.toggleFlag(id, flag).map { result =>
      if (result > 0) Ok(Json.obj("success" -> true))
      else NotFound(Json.obj("error" -> "Notification not found"))
    }
  }

  def delete(id: Long): Action[AnyContent] = Action.async { request =>
    notificationService.deleteNotification(id).map { result =>
      if (result > 0) Ok(Json.obj("success" -> true))
      else NotFound(Json.obj("error" -> "Notification not found"))
    }
  }
}
```

---

## 🔌 Paso 8: Inyección de Dependencias

Crear/Actualizar: `app/modules/ApplicationModule.scala`

```scala
package modules

import play.api.{Configuration, Environment}
import play.api.inject.Module
import services.NotificationService
import repositories.NotificationRepository

class ApplicationModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    // Notifications
    bind[NotificationRepository].toSelf,
    bind[NotificationService].toSelf
  )
}
```

---

## 🛣️ Paso 9: Rutas

Agregar a `conf/routes` (crear si no existe):

```
# Notifications
GET      /notifications                           controllers.NotificationController.getNotifications
POST     /notifications/search                    controllers.NotificationController.search
POST     /notifications/total                     controllers.NotificationController.count
POST     /notifications/:id/read                  controllers.NotificationController.markAsRead(id: Long)
POST     /notifications/:id/flag                  controllers.NotificationController.toggleFlag(id: Long, flag: Boolean)
DELETE   /notifications/:id                       controllers.NotificationController.delete(id: Long)
```

---

## 🎨 Paso 10: Frontend (Opcional - Mantener AngularJS)

Si quieres mantener AngularJS (simplemente copiar):

```bash
cp -r _LEGACY_BACKUP/app.viejo/assets/javascripts/inbox/views/* \
    app/views/inbox/

cp -r _LEGACY_BACKUP/app.viejo/assets/javascripts/inbox/controllers/* \
    app/assets/javascripts/inbox/controllers/

cp -r _LEGACY_BACKUP/app.viejo/assets/javascripts/inbox/services/* \
    app/assets/javascripts/inbox/services/
```

O migrar a React/Vue (decisión pendiente).

---

## 🚀 Paso 11: Ejecución

```bash
# 1. Compilar
sbt compile

# 2. Aplicar migraciones
sbt "run -Dplay.evolutions.autoApply=true"

# 3. Ejecutar proyecto
sbt -Dconfig.file=./conf/application-moderno.conf run

# 4. Testear endpoints
curl http://localhost:9000/notifications
```

---

## ✅ Checklist de Migración

- [ ] Crear rama `feature/inbox-migration`
- [ ] Copiar modelos (Notification, NotificationType)
- [ ] Crear esquema de BD (evolutions)
- [ ] Implementar Repository
- [ ] Implementar Service
- [ ] Implementar Controller
- [ ] Agregar inyección de dependencias
- [ ] Agregar rutas
- [ ] Copiar/migrar frontend
- [ ] Testear endpoints
- [ ] Hacer commit: `git commit -m "feat: Migrate inbox module to GENis 6.0"`
- [ ] Hacer push: `git push origin feature/inbox-migration`
- [ ] Crear PR a rama `dev`

---

## 📝 Notas Importantes

1. **JSONB en PostgreSQL**: Úsalo para `metadata` (datos flexible)
2. **Timestamps**: Cambié `java.util.Date` a `java.time.LocalDateTime` (mejor práctica)
3. **Evolutions**: Las migraciones de BD son automáticas con Play
4. **Testing**: Agregar tests unitarios para Service y Repository
5. **Seguridad**: Validar que `userId` coincida con usuario autenticado en Controller

---

## 🔗 Referencias

- Módulo Legacy: `_LEGACY_BACKUP/app.viejo/inbox/`
- Rutas Legacy: `_LEGACY_BACKUP/conf/routes.viejo` (líneas 183-190)
- Documentación GENis: `GENIS_ARQUITECTURA.md`

