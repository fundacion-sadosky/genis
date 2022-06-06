package trace

trait PedigreeMatchActionInfo {
  val pedigree: Long
  val analysisType: Int
}

case class PedigreeMatchInfo(
  matchId: String,
  pedigree: Long,
  analysisType: Int,
  matchType: MatchTypeInfo.Value,
  courtCase:Option[String] = None,
  pedigreeName:Option[String] = None) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeMatch
  val matchTypeDescription = matchType match {
    case MatchTypeInfo.Insert => "Generación"
    case MatchTypeInfo.Update => "Actualización"
    case MatchTypeInfo.Delete => "Baja"
  }

  override val description = if(courtCase.isDefined && pedigreeName.isDefined)
    s"$matchTypeDescription de coincidencia con el pedigrí ${pedigreeName.get} del caso ${courtCase.get}"
  else {
    s"$matchTypeDescription de coincidencia con el pedigrí $pedigree"
  }
}

case class PedigreeDiscardInfo(matchId: String,
  pedigree: Long,
  user: String,
  analysisType: Int,
  courtCase:Option[String] = None,
  pedigreeName:Option[String] = None) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeDiscard
  override val description = if(courtCase.isDefined && pedigreeName.isDefined)
    s"Descarte del match del pedigrí ${pedigreeName.get} del caso ${courtCase.get}"
  else {s"Descarte del match del pedigrí $pedigree cuyo responsable es $user."}
}

case class PedigreeConfirmInfo(matchId: String,
                                pedigree: Long,
                                user: String,
                                analysisType: Int,
                                courtCase:Option[String] = None,
                                pedigreeName:Option[String] = None) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeConfirm
  override val description = if(courtCase.isDefined && pedigreeName.isDefined)
    s"Confirmación del match del pedigrí ${pedigreeName.get} del caso ${courtCase.get}"
  else {s"Confirmación del match del pedigrí $pedigree cuyo responsable es $user."}
}

case class PedigreeMatchInfo2(matchId: String,
                              pedigree: Long,
                              profile: String,
                              analysisType: Int,
                              matchType: MatchTypeInfo.Value) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeMatch2
  val matchTypeDescription = matchType match {
    case MatchTypeInfo.Insert => "Generación"
    case MatchTypeInfo.Update => "Actualización"
    case MatchTypeInfo.Delete => "Baja"
  }
  override val description = s"$matchTypeDescription de coincidencia con el perfil $profile."
}

case class PedigreeDiscardInfo2(matchId: String,
                                pedigree: Long,
                                profile: String,
                                user: String,
                                analysisType: Int) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeDiscard2
  override val description = s"Descarte del match con el perfil $profile."
}

case class PedigreeConfirmInfo2(matchId: String,
                                pedigree: Long,
                                profile: String,
                                user: String,
                                analysisType: Int) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeConfirm2
  override val description = s"Confirmación del match con el perfil $profile."
}