package security

import org.scalatest.FlatSpec
import javax.crypto.spec.SecretKeySpec
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import org.scalatest.Ignore
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import java.net.URI

//@Ignore
class CryptoTest extends FlatSpec {

  val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
  /*
  val ivAp = "d39aba1c939f45504d48e35f5190418f"
  val keyAp = "b2ca57b757efd60d587d5da95af71e69"
  val uriCypherText = "/SEbAyY0ZVj0oAPulpA7Jiw=="
  val real = "/getFilesId"
    */
  //Js  
  val ivAp = "a064e342893498766153f0af75e35086"
  val keyAp = "d8fe879914be93a647b2192620423b80"
  val uriCypherText = "/_vqaZqEKPEgL0HQ9Ks2WOK8DQ8WbiMNQ8ldXq6KkMw5o1DUEojwP-Uy0RNh2EQPecfJOwblRn8fGl1IsHfil-FI04RGrJ2xAyJ5DoeZEDroJNBAsIFgsrapR63U2Z01_xr1YcMTsk0L3OF4dd8ep2A1alp88tJoA9p-nPx-13tamdc9igdURYfuqPkLS_pM-"
  
  val real = "/randomMatchProbabilities?frecuencyTableName=nombre&matchingResultId=b05534d1-2bf7-4074-b6e2-cc927b735f16&probabilityModel=NRCII41"  
    
    
  val verifierAp = "d11969212aa96c801554d04f68e6587d"  
  
  val authenticatedPair = new AuthenticatedPair(verifierAp,keyAp,ivAp)  
    
  "crypto" must
  	"decript a cypherText" in{
    
        val key = new SecretKeySpec(Hex.decodeHex(authenticatedPair.key.toCharArray()), "AES");
        val iv = new IvParameterSpec(Hex.decodeHex(authenticatedPair.iv.toCharArray()))

        cipher.init(Cipher.DECRYPT_MODE, key, iv);

        val uriToDecrypt = uriCypherText.substring(1)

        //println("decrypting " + uriToDecrypt)

        val decryptedUriBytes = cipher.doFinal(Base64.decodeBase64(uriToDecrypt.getBytes()))

        val decryptedUri = new String(decryptedUriBytes)

        //println("decryptedUri is " + decryptedUri)

        assert(decryptedUri === real)            
  }
  
  "crypto" must
   "encript a text" in {
    
       val key = new SecretKeySpec(Hex.decodeHex(authenticatedPair.key.toCharArray()), "AES");
       val iv = new IvParameterSpec(Hex.decodeHex(authenticatedPair.iv.toCharArray()))

    cipher.init(Cipher.ENCRYPT_MODE, key,iv)
    
    val uriToEncrypt = real.getBytes()
       
    val encryptedUriBytes = cipher.doFinal(uriToEncrypt)
    
    val encryptedUri = new String(Base64.encodeBase64URLSafe(encryptedUriBytes))
    //println(encryptedUri)
    assert("/"+encryptedUri === uriCypherText )
  }
  
}