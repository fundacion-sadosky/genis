define(
  ['jquery'],
  function() {
    'use strict';
    var PedigreeScenarioReportCtrl = function($scope, matchesService) {
      var hasMitocondrialData = function($scope) {
        return Object.entries($scope.mitoRanges).length > 0;
      };
      var getMitoAlleles = function (comparisonData) {
        var alelles = comparisonData
          .filter(function (x){return !x.locus.endsWith("RANGE") && x.locus.startsWith("HV");})
          .map(function(x){return Object.entries(x.g);})
          .flat()
          .reduce(
            function(acc, pIdAlleles) {
              var pId = pIdAlleles[0];
              var allelesArr = pIdAlleles[1];
              allelesArr.forEach(
                function (allele) {
                  if (!acc.hasOwnProperty(pId)) {
                    acc[pId] = [];
                  }
                  acc[pId].push(allele);
                }
              );
              return acc;
            },
            {}
          );
        return alelles;
      };
      var getMitoRegions = function(comparisonData) {
        var mergeRangeArrays = function(acc, pIdRanges) {
          var pid = pIdRanges[0];
          var ranges = pIdRanges[1];
          if (!acc[0].hasOwnProperty(pid)) {
            acc[0][pid] = [];
          }
          acc[0][pid].push(ranges);
          return acc;
        };
        var sortObjectValues =  function (obj) {
          Object
            .entries(obj)
            .forEach(function(values){values[1].sort();});
          return obj;
        };
        var regions = comparisonData
          .filter(function(x){return x.locus.endsWith("RANGE");})
          .map(function(x){return Object.entries(x.g);})
          .flat()
          .reduce(mergeRangeArrays, [{}])
          .map(sortObjectValues)[0];
        return regions;
      };
      var cromosomicMarkers = function(comparisonData) {
        var markers = comparisonData
          .filter(function(x){return !x.locus.startsWith("HV");})
          .map(function(x){return x.locus;})
          .sort();
        return markers;
      };
      var cromosomicAlleles = function (comparisonData) {
        var alleles = comparisonData
          .reduce(
            function (acc, x) { 
              var entries = Object.entries(x.g);
              entries = entries
                .map(function(x){return [x[0],x[1][0]+"/"+x[1][1]];});
              acc[x.locus] = Object.fromEntries(entries);
              return acc;
            },
            {}
          );
        return alleles;
      };
      var getSexMarkers = function(comparisonData) {
        var extractLocusAndMergeAlleles = function(elem) {
          var areAllXorY = Array
            .from(Object.entries(elem.g))
            .map(function(x) {return x[1];})
            .reduce(function (a, x) {return a.concat(x);})
            .map(function(x) {return x ==="X" || x === "Y";})
            .reduce(function(a, x) {return a && x;});
          return [elem.locus, areAllXorY];
        };
        var sexMarker = comparisonData
          .map(extractLocusAndMergeAlleles)
          .filter(function(x) {return x[1];})
          .map(function(x) {return x[0];})[0];
        return sexMarker;
      };
      var chunks = function(arr, len) {
        var result = [],
          i = 0,
          n = arr.length;
        while (i < n) {
          result.push(arr.slice(i, i += len));
        }
        return result;
      };
      var profiles = $scope
        .scenario
        .genogram
        .filter(function(ind) { return ind.globalCode; });
      var profilesAndAliases = profiles
        .map(
          function(ind) {
            return {globalCode: ind.globalCode, alias: ind.alias};
          }
        );
      matchesService
        .getComparedMixtureGene(
          profiles.map(function(p){return p.globalCode;})
        )
        .then(
          function(response) {
            $scope.comparison = response.data;
            $scope.sexMarker = getSexMarkers($scope.comparison);
            $scope.cromosomicMarkers = cromosomicMarkers($scope.comparison);
            $scope.cromosomicAlleles = cromosomicAlleles($scope.comparison);
            $scope.mitoRanges = getMitoRegions($scope.comparison);
            $scope.mitoAlleles = getMitoAlleles($scope.comparison);
            $scope.hasMito = hasMitocondrialData($scope);
          }
      );
      $scope.profileChunks = chunks(profilesAndAliases, 5);
    };
    return PedigreeScenarioReportCtrl;
  }
);