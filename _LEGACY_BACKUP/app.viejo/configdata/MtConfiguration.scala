package configdata

case class MtRegion(
  name: String,
  range: (Int, Int)
)

case class MtConfiguration(
  regions: List[MtRegion],
  ignorePoints: List[Int]
)