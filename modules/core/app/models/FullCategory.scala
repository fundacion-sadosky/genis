package models

case class FullCategory(
  id: String,
  associations: Seq[CategoryAssociationRow] = Seq.empty,
  pedigreeAssociation: Boolean = false
  // Agregar otros campos según uso real
)
