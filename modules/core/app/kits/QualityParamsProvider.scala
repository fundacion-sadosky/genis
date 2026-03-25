package kits

import javax.inject.Singleton
import configdata.{CategoryConfiguration, FullCategory}

trait QualityParamsProvider {
  def minLocusQuantityAllowedPerProfile(category: FullCategory, kit: StrKit): Int
  def maxAllelesPerLocus(category: FullCategory, kit: StrKit): Int
  def maxOverageDeviatedLociPerProfile(category: FullCategory, kit: StrKit): Int
  def multiallelic(category: FullCategory, kit: StrKit): Boolean
}

@Singleton
class QualityParamsProviderStub extends QualityParamsProvider {
  override def minLocusQuantityAllowedPerProfile(category: FullCategory, kit: StrKit): Int = 0
  override def maxAllelesPerLocus(category: FullCategory, kit: StrKit): Int = 2
  override def maxOverageDeviatedLociPerProfile(category: FullCategory, kit: StrKit): Int = 0
  override def multiallelic(category: FullCategory, kit: StrKit): Boolean = false
}
