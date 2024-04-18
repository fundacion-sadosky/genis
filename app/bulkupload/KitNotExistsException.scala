package bulkupload

final case class KitNotExistsException(kit:String) extends Exception(kit)
