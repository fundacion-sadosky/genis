package user

import javax.inject.{Inject, Singleton}

import scala.util.Try

trait LdapHealthService:
  def checkStatus(): Try[(String, String)]

@Singleton
class LdapHealthServiceImpl @Inject()(ldap: LdapConnectionProvider) extends LdapHealthService:
  override def checkStatus(): Try[(String, String)] =
    Try(ldap.withConnection(_.getRootDSE)).map(dse => ("UP", dse.getVendorName))
