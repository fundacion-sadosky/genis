package user

import com.unboundid.ldap.sdk.LDAPConnectionPool
import javax.inject.{Inject, Singleton}

import scala.util.Try

trait LdapHealthService:
  def checkStatus(): Try[(String, String)]

@Singleton
class LdapHealthServiceImpl @Inject()(pool: LDAPConnectionPool) extends LdapHealthService:
  override def checkStatus(): Try[(String, String)] =
    Try(pool.getRootDSE).map(dse => ("UP", dse.getVendorName))
