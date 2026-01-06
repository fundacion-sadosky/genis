package util

import security.CryptoService
import security.CryptoServiceImpl

object GenerateEncryptedTotp {
  
  def main(args: Array[String]): Unit = {
    val cryptoService: CryptoService = new CryptoServiceImpl(2048)
    val totpSecret = args(0)
    val password = args(1)
    val derivatedKey = cryptoService.generateDerivatedCredentials(password)
    val result = cryptoService.encrypt(totpSecret.getBytes, derivatedKey)
    println(result)
  }
  
}