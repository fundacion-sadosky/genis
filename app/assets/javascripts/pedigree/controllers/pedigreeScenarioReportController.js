define(['jquery'], function() {
    'use strict';

    var PedigreeScenarioReportCtrl = function($scope, matchesService) {
        
        var chunks = function(arr, len) {
            var result = [],
                i = 0,
                n = arr.length;

            while (i < n) {
                result.push(arr.slice(i, i += len));
            }

            return result;
        };

        var profiles = $scope.scenario.genogram.filter(function(ind) { return ind.globalCode; });
        
        var profilesAndAliases = profiles.map(function(ind) { return {globalCode: ind.globalCode, alias: ind.alias}; });
        
        matchesService.getComparedMixtureGene(profiles.map(function(p){return p.globalCode;})).then(function(response) {
            $scope.comparison = response.data;
        });

        $scope.profileChunks = chunks(profilesAndAliases, 5);
        
    };

    return PedigreeScenarioReportCtrl;

});