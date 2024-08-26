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
    case MatchTypeInfo.Insert => "Generation"
    case MatchTypeInfo.Update => "Update"
    case MatchTypeInfo.Delete => "Deletion"
  }

  override val description = if(courtCase.isDefined && pedigreeName.isDefined)
    s"$matchTypeDescription of match in pedigree ${pedigreeName.get} of case ${courtCase.get}"
  else {
    s"$matchTypeDescription dof match with pedigree $pedigree"
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
    s"Discard of match in pedigree ${pedigreeName.get} of case ${courtCase.get}"
  else {s"Discard of match in pedigree $pedigree whose responsible is $user."}
}

case class PedigreeConfirmInfo(matchId: String,
                                pedigree: Long,
                                user: String,
                                analysisType: Int,
                                courtCase:Option[String] = None,
                                pedigreeName:Option[String] = None) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeConfirm
  override val description = if(courtCase.isDefined && pedigreeName.isDefined)
    s"Pedigree math confirmation ${pedigreeName.get} of case ${courtCase.get}"
  else {s"Pedigree math confirmation $pedigree whose responsible is $user."}
}

case class PedigreeMatchInfo2(matchId: String,
                              pedigree: Long,
                              profile: String,
                              analysisType: Int,
                              matchType: MatchTypeInfo.Value) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeMatch2
  val matchTypeDescription = matchType match {
    case MatchTypeInfo.Insert => "Generation"
    case MatchTypeInfo.Update => "Update"
    case MatchTypeInfo.Delete => "Deletion"
  }
  override val description = s"$matchTypeDescription of match with profile $profile."
}

case class PedigreeDiscardInfo2(matchId: String,
                                pedigree: Long,
                                profile: String,
                                user: String,
                                analysisType: Int) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeDiscard2
  override val description = s"Discard of match with profile $profile."
}

case class PedigreeConfirmInfo2(matchId: String,
                                pedigree: Long,
                                profile: String,
                                user: String,
                                analysisType: Int) extends TraceInfo with PedigreeMatchActionInfo {
  override val kind = TraceType.pedigreeConfirm2
  override val description = s"Confirmation of match with profile $profile."
}