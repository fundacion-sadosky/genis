package security

import java.security.KeyPairGenerator
import java.security.SecureRandom

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS

import org.apache.commons.codec.binary.Hex
import org.jboss.aerogear.security.otp.api.Base32

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Input
import play.api.libs.iteratee.Iteratee

abstract class CryptoService {
  def giveRsaKeys: (Array[Byte], Array[Byte])
  def giveTotpSecret: String
  def encrypt(plainText: Array[Byte], credentials: AuthenticatedPair): Array[Byte]
  def decrypt(cipherText: Array[Byte], credentials: AuthenticatedPair): Array[Byte]
  def generateDerivatedCredentials(password: String): AuthenticatedPair
  def generateRandomCredentials(): AuthenticatedPair
  def encryptEnumeratee(credentials: AuthenticatedPair): Enumeratee[Array[Byte], Array[Byte]]
  def decryptEnumeratee(credentials: AuthenticatedPair): Enumeratee[Array[Byte], Array[Byte]]
}

@Singleton
class CryptoServiceImpl @Inject() (@Named("keyLength") val keyLength: Int) extends CryptoService {

  val logger = Logger(this.getClass())

  private val cipherAlgorithm = "AES/CBC/PKCS5Padding"
  private val secretKeyType = "AES"

  private val keyPairType = "RSA"

  private val randomGeneratorAlgorithm = "SHA1PRNG"

  private val derivatedKeyAlgorithm = "PBKDF2WithHmacSHA1"

  private val hmacAlgorithm = "HmacSHA256"

  abstract class CipherMode
  case object ENCRYPT extends CipherMode
  case object DECRYPT extends CipherMode

  override def giveRsaKeys: (Array[Byte], Array[Byte]) = {
    val keyGen = KeyPairGenerator.getInstance(keyPairType)
    keyGen initialize keyLength
    val keys = keyGen.genKeyPair
    (keys.getPublic.getEncoded, keys.getPrivate.getEncoded)
  }

  override def generateRandomCredentials(): AuthenticatedPair = {
    val key: Array[Byte] = new Array[Byte](48)
    SecureRandom.getInstance(randomGeneratorAlgorithm).nextBytes(key)
    generateCredentials(key)
  }

  private def generateCredentials(bytes: Array[Byte]): AuthenticatedPair = {
    val verifier = Hex.encodeHexString(bytes.take(16))
    val key = Hex.encodeHexString(bytes.drop(16).take(16))
    val iv = Hex.encodeHexString(bytes.drop(32).take(16))
    AuthenticatedPair(verifier, key, iv)
  }

  override def giveTotpSecret: String = Base32.random

  override def encrypt(plainText: Array[Byte], credentials: AuthenticatedPair): Array[Byte] = {
    val encryptIteratee = Enumerator(plainText) |>> encryptEnumeratee(credentials) &>> Iteratee.consume[Array[Byte]]()
    val result = Iteratee.flatten(encryptIteratee)
    Await.result(result.run, Duration.Inf)
  }

  override def decrypt(cipherText: Array[Byte], credentials: AuthenticatedPair): Array[Byte] = {
    val decryptIteratee = Enumerator(cipherText) |>> decryptEnumeratee(credentials) &>> Iteratee.consume[Array[Byte]]()
    val result = Iteratee.flatten(decryptIteratee)
    Await.result(result.run, Duration.Inf)
  }

  override def generateDerivatedCredentials(password: String): AuthenticatedPair = {
    val salt: Array[Byte] = "agentsalt".getBytes
    val derivedKeyLength = 48 * 8
    val iterations = 20000
    val spec = new PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength)
    val keyBytes = SecretKeyFactory.getInstance(derivatedKeyAlgorithm).generateSecret(spec).getEncoded
    generateCredentials(keyBytes)
  }

  private def sign(entry: Array[Byte], key: Array[Byte]): Array[Byte] = {
    val secretKey = new SecretKeySpec(key, "");
    val mac = Mac.getInstance(hmacAlgorithm)
    mac.init(secretKey);
    val hmac = mac.doFinal(entry)
    hmac
  }

  private def doCipher(cipher: Cipher) = {
    val func: PartialFunction[Input[Array[Byte]], Enumerator[Array[Byte]]] = {
      case Input.El(bytes) => Option(cipher.update(bytes)).fold[Enumerator[Array[Byte]]] {
        Enumerator.enumInput(Input.Empty)
      } {
        Enumerator(_)
      }
      case Input.Empty => Enumerator.enumInput(Input.Empty)
      case Input.EOF   => Enumerator(cipher.doFinal()) andThen Enumerator.enumInput(Input.EOF)
    }
    func
  }

  override def encryptEnumeratee(credentials: AuthenticatedPair): Enumeratee[Array[Byte], Array[Byte]] = {
    val key = new SecretKeySpec(Hex.decodeHex(credentials.key.toCharArray()), secretKeyType)
    val iv = new IvParameterSpec(Hex.decodeHex(credentials.iv.toCharArray()))
    val cipher = Cipher.getInstance(cipherAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    Enumeratee.mapInputFlatten { doCipher(cipher) }
  }

  override def decryptEnumeratee(credentials: AuthenticatedPair): Enumeratee[Array[Byte], Array[Byte]] = {
    val key = new SecretKeySpec(Hex.decodeHex(credentials.key.toCharArray()), secretKeyType)
    val iv = new IvParameterSpec(Hex.decodeHex(credentials.iv.toCharArray()))
    val cipher = Cipher.getInstance(cipherAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    Enumeratee.mapInputFlatten { doCipher(cipher) }
  }

} 