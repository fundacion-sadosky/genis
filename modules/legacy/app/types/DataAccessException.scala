package types

case class DataAccessException(msg: String, error: Throwable) extends RuntimeException(msg,error) 