package inbox

import com.google.inject.AbstractModule

class NotificationModule extends AbstractModule:
  override def configure(): Unit =
    bind(classOf[NotificationRepository]).to(classOf[SlickNotificationRepository])
    bind(classOf[NotificationService]).to(classOf[NotificationServiceImpl])