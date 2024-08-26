package inbox

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder

class NotificationModule extends AbstractModule {
  override protected def configure() {

    bind(classOf[NotificationService]).to(classOf[NotificationServiceImpl])
    bind(classOf[NotificationRepository]).to(classOf[SlickNotificationRepository])

    ()
  }
  
}