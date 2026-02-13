package security

import java.security.{KeyPairGenerator, SecureRandom}
import javax.crypto.{Cipher, Mac, SecretKeyFactory}
import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.inject.{Inject, Named, Singleton}

import org.apache.commons.codec.binary.Hex
import org.jboss.aerogear.security.otp.api.Base32
import play.api.Logger

abstract class CryptoService {
  def giveRsaKeys: (Array[Byte], Array[Byte])
  def giveTotpSecret: String
  def encrypt(plainText: Array[Byte], credentials: AuthenticatedPair): Array[Byte]
  def decrypt(cipherText: Array[Byte], credentials: AuthenticatedPair): Array[Byte]
  def generateDerivatedCredentials(password: String): AuthenticatedPair
  def generateRandomCredentials(): AuthenticatedPair
}

@Singleton
class CryptoServiceImpl @Inject() (@Named("keyLength") val keyLength: Int) extends CryptoService {

  private val logger = Logger(this.getClass)

  private val cipherAlgorithm = "AES/CBC/PKCS5Padding"
  private val secretKeyType = "AES"
  private val keyPairType = "RSA"
  private val randomGeneratorAlgorithm = "SHA1PRNG"
  private val derivatedKeyAlgorithm = "PBKDF2WithHmacSHA1"

  override def giveRsaKeys: (Array[Byte], Array[Byte]) = {
    val keyGen = KeyPairGenerator.getInstance(keyPairType)
    keyGen.initialize(keyLength)
    val keys = keyGen.genKeyPair()
    (keys.getPublic.getEncoded, keys.getPrivate.getEncoded)
  }

  override def generateRandomCredentials(): AuthenticatedPair = {
    val bytes: Array[Byte] = new Array[Byte](48)
    SecureRandom.getInstance(randomGeneratorAlgorithm).nextBytes(bytes)
    generateCredentials(bytes)
  }

  private def generateCredentials(bytes: Array[Byte]): AuthenticatedPair = {
    val verifier = Hex.encodeHexString(bytes.take(16))
    val key = Hex.encodeHexString(bytes.slice(16, 32))
    val iv = Hex.encodeHexString(bytes.slice(32, 48))
    AuthenticatedPair(verifier, key, iv)
  }

  override def giveTotpSecret: String = Base32.random()

  override def encrypt(plainText: Array[Byte], credentials: AuthenticatedPair): Array[Byte] = {
    val key = new SecretKeySpec(Hex.decodeHex(credentials.key), secretKeyType)
    val iv = new IvParameterSpec(Hex.decodeHex(credentials.iv))
    val cipher = Cipher.getInstance(cipherAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    cipher.doFinal(plainText)
  }

  override def decrypt(cipherText: Array[Byte], credentials: AuthenticatedPair): Array[Byte] = {
    val key = new SecretKeySpec(Hex.decodeHex(credentials.key), secretKeyType)
    val iv = new IvParameterSpec(Hex.decodeHex(credentials.iv))
    val cipher = Cipher.getInstance(cipherAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    cipher.doFinal(cipherText)
  }

  override def generateDerivatedCredentials(password: String): AuthenticatedPair = {
    val salt: Array[Byte] = "agentsalt".getBytes("UTF-8")
    val derivedKeyLength = 48 * 8
    val iterations = 20000
    val spec = new PBEKeySpec(password.toCharArray, salt, iterations, derivedKeyLength)
    val keyBytes = SecretKeyFactory.getInstance(derivatedKeyAlgorithm).generateSecret(spec).getEncoded
    generateCredentials(keyBytes)
  }
}
