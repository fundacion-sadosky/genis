define(
  ['angular', 'lodash'],
  function(angular, _) {
    'use strict';
    function ComparisonReportController(
      $scope,
      matcherService
    ) {
      $scope.parent = $scope.$parent;
      $scope.getSubCatName = function(groups, catId) {
        return matcherService
          .getSubCatName(groups, catId);
      };
      $scope.getLR = function() {
        return $scope.pvalue;
      };
      $scope.stringencyAlleles = function() {
        var alleles = Object
          .entries(
            $scope
              .results
              .reducedStringencies
          );
        return (alleles[0]||[])[1] || 'N/A';
      };
      $scope.selectRanges = function(profile) {
        var ranges = $scope.comparision
          .filter(function(x) {return x.locus.endsWith("_RANGE");})
          .map(function(x) {return x.g[profile];})
          .filter(function(x) {return x !== undefined;});
        return ranges;
      };
      $scope.rangeAsString = function(range) {
        if (range === undefined) {
          return "";
        }
        if (range.length === 2) {
          return "[" + range[0] + " - " + range[1] + "]";
        }
        return "";
      };
      $scope.selectMatchingAlleles = function(profile, matching) {
        var mAlleles = $scope.comparision
          .filter(function(x){return x.locus.startsWith("HV") && !x.locus.endsWith("_RANGE");})
          .flatMap(function(x){return x.g[profile];})
          .filter(function(x){return x!==undefined;})
          .filter(function(x){return matching.includes(x);});
        return mAlleles;
      };
      $scope.selectNonMatchingAlleles = function(profile, matching) {
        var mAlleles = $scope.comparision
          .filter(function(x){return x.locus.startsWith("HV") && !x.locus.endsWith("_RANGE");})
          .flatMap(function(x){return x.g[profile];})
          .filter(function(x){return x!==undefined;})
          .filter(function(x){return !matching.includes(x);});
        return mAlleles;
      };
      $scope.totalNumberOfDifferences = function(profileId, matchedProfileId, matching) {
        var pAlleles = $scope.selectNonMatchingAlleles(profileId, matching);
        var mAlleles = $scope.selectNonMatchingAlleles(matchedProfileId, matching);
        var allAlleles = pAlleles.concat(mAlleles);
        var nDiff = new Set(allAlleles).size;
        return nDiff;
      };
      $scope.getMarker = function(marker, profileId) {
        var markerMap = new Map();
        if (markerMap.size===0) {
          markerMap = new Map(
            $scope
              .comparision
              .filter(function(x) {return !x.locus.startsWith("HV");})
              .map(
                function(x) {
                  return [x.locus, new Map(Object.entries(x.g))];
                }
              )
          );
        }
        return markerMap
          .get(marker)
          .get(profileId)
          .map(function(x){return x+"/"+x;})
          .join(" ");
      };
      $scope.markers = function() {
        var markers = $scope
          .comparision
          .filter(function(x) {return !x.locus.startsWith("HV");})
          .map(function(x) {return x.locus;});
        markers.sort();
        return markers;
      };

    }
    return ComparisonReportController;
  }
);