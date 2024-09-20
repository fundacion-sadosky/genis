package connections

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.ning.http.client.AsyncHttpClientConfig
import play.api.Configuration
import play.api.libs.ws._
import play.api.libs.ws.WSClient

import play.api.libs.ws._
import play.api.{Configuration, Environment}
import javax.inject.{Inject, Singleton}

import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder

class InterconnectionModule(conf: Configuration,wsConf: Configuration) extends AbstractModule {
  override protected def configure() {

    val protocol = conf.getString("protocol").get
    val status = conf.getString("status").get
    val categoryTreeCombo = conf.getString("categoryTreeCombo").get
    val insertConnection = conf.getString("insertConnection").get
    val localUrl = conf.getString("localUrl").get
    val uploadProfile = conf.getString("uploadProfile").get
    val retryInterval = conf.getString("retryInterval").get

    val timeOutOnDemand = conf.getString("timeOutOnDemand").get
    val timeOutQueue = conf.getString("timeOutQueue").get
    val timeActorSendRequestGet = conf.getString("timeActorSendRequestGet").get
    val timeActorSendRequestPutPostDelete = conf.getString("timeActorSendRequestPutPostDelete").get
    val timeOutHolder = conf.getInt("timeOutHolder").get

    bind(classOf[String]).annotatedWith(Names.named("protocol")).toInstance(protocol)
    bind(classOf[String]).annotatedWith(Names.named("status")).toInstance(status)
    bind(classOf[String]).annotatedWith(Names.named("categoryTreeCombo")).toInstance(categoryTreeCombo)
    bind(classOf[String]).annotatedWith(Names.named("insertConnection")).toInstance(insertConnection)
    bind(classOf[String]).annotatedWith(Names.named("uploadProfile")).toInstance(uploadProfile)
    bind(classOf[String]).annotatedWith(Names.named("localUrl")).toInstance(localUrl)
    bind(classOf[String]).annotatedWith(Names.named("retryInterval")).toInstance(retryInterval)
    bind(classOf[String]).annotatedWith(Names.named("timeOutOnDemand")).toInstance(timeOutOnDemand)
    bind(classOf[String]).annotatedWith(Names.named("timeOutQueue")).toInstance(timeOutQueue)
    bind(classOf[String]).annotatedWith(Names.named("timeActorSendRequestGet")).toInstance(timeActorSendRequestGet)
    bind(classOf[String]).annotatedWith(Names.named("timeActorSendRequestPutPostDelete")).toInstance(timeActorSendRequestPutPostDelete)
    bind(classOf[Int]).annotatedWith(Names.named("timeOutHolder")).toInstance(timeOutHolder)

    bind(classOf[ConnectionRepository]).to(classOf[SlickConnectionRepository])
    bind(classOf[InferiorInstanceRepository]).to(classOf[SlickInferiorInstanceRepository])
    bind(classOf[SuperiorInstanceProfileApprovalRepository]).to(classOf[SlickSuperiorInstanceProfileApprovalRepository])

    bind(classOf[InterconnectionService]).to(classOf[InterconnectionServiceImpl])

    val clientConfig = new PlayWSConfigParser(wsConf,getClass.getClassLoader)
    //val clientConfig = new DefaultWSConfigParser(wsConf,getClass.getClassLoader)
    val secureDefaults:com.ning.http.client.AsyncHttpClientConfig = new AsyncHttpClientConfig.Builder(clientConfig()).build()
    //val secureDefaults:com.ning.http.client.AsyncHttpClientConfig = new NingAsyncHttpClientConfigBuilder(clientConfig.parse()).build()
    // You can directly use the builder for specific options once you have secure TLS defaults...
    val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder(secureDefaults)
    builder.setCompressionEnforced(true)
    //builder.setCompressionEnabled(true)

    val secureDefaultsWithSpecificOptions:com.ning.http.client.AsyncHttpClientConfig = builder.build()
    implicit val implicitClient = new play.api.libs.ws.ning.NingWSClient(secureDefaultsWithSpecificOptions)
    bind(classOf[WSClient]).toInstance(implicitClient)
    bind(classOf[RetryScheduler]).asEagerSingleton()
    ()
  }
}