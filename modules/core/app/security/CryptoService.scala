package security

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger

abstract class CryptoService {
  def giveRsaKeys: (Array[Byte], Array[Byte])
  def giveTotpSecret: String
  def encrypt(plainText: Array[Byte], credentials: AuthenticatedPair): Array[Byte]
  def decrypt(cipherText: Array[Byte], credentials: AuthenticatedPair): Array[Byte]
  def generateDerivatedCredentials(password: String): AuthenticatedPair
  def generateRandomCredentials(): AuthenticatedPair
}

// TODO: Migrar implementación real desde app/security/CryptoService.scala
// El original usaba play.api.libs.iteratee (Enumeratee) que ya no existe en Play 3
// Necesita reescribirse usando streams de Akka o APIs directas de javax.crypto
@Singleton
class CryptoServiceImpl @Inject() (@Named("keyLength") val keyLength: Int) extends CryptoService {

  private val logger = Logger(this.getClass)

  override def giveRsaKeys: (Array[Byte], Array[Byte]) = {
    logger.warn("CryptoService.giveRsaKeys es un STUB")
    (Array.empty, Array.empty)
  }

  override def giveTotpSecret: String = {
    logger.warn("CryptoService.giveTotpSecret es un STUB")
    "STUB_SECRET"
  }

  override def encrypt(plainText: Array[Byte], credentials: AuthenticatedPair): Array[Byte] = {
    logger.warn("CryptoService.encrypt es un STUB - retorna plainText sin encriptar")
    plainText
  }

  override def decrypt(cipherText: Array[Byte], credentials: AuthenticatedPair): Array[Byte] = {
    logger.warn("CryptoService.decrypt es un STUB - retorna cipherText sin desencriptar")
    cipherText
  }

  override def generateDerivatedCredentials(password: String): AuthenticatedPair = {
    logger.warn("CryptoService.generateDerivatedCredentials es un STUB")
    AuthenticatedPair("stub_verifier", "stub_key", "stub_iv")
  }

  override def generateRandomCredentials(): AuthenticatedPair = {
    logger.warn("CryptoService.generateRandomCredentials es un STUB")
    AuthenticatedPair("stub_verifier", "stub_key", "stub_iv")
  }
}
