package types

sealed trait Mode

object Mode {

  case object Prod extends Mode

  case object Sandbox extends Mode

  case object SandboxAmarillo extends Mode

  case object SandboxVerde extends Mode

  case object SandboxAzul extends Mode

}