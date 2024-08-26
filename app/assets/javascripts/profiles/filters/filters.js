define([ 'angular'], function(angular) {
    'use strict';

    var filterByLocusType =  function() {
        return function(input, type, loci) {
            if (!input) { return input; }
            if (!type) { return input; }
            if (!loci) { return input; }
            var result = {};
            angular.forEach(input, function(value, key) {
                if (loci[key].analysisType === type) {
                    result[key] = value;
                }
            });
            return result;
        };
    };

    var filterByKitType =  function() {
        return function(input, type, kits) {
            if (!input) { return input; }
            if (!type) { return input; }
            if (!kits) { return input; }
            var result = [];
            angular.forEach(input, function(value) {
                if (value.type) {
                  if (value.type === type) {
                        result.push(value);
                  }
                } else if (kits[value.kit].type === type) {
                    result.push(value);
                }
            });
            return result;
        };
    };
    var isRange = function() {
        return function(nodes) {
            return nodes.filter(function(n){return angular.isString(n.id) && n.id.endsWith("_RANGE");});
        };
    };
    var isRegion = function() {
        return function(nodes) {
            return nodes.filter(function(n){return angular.isString(n.id) && !n.id.endsWith("_RANGE");});
        };
    };
    return {filterByLocusType: filterByLocusType, filterByKitType: filterByKitType,isRange:isRange,isRegion:isRegion};
});