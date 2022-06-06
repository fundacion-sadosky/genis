define([], function() {
	'use strict';
	function HypothesisController($scope, statsService) {
        $scope.selectAll = { selectedByP: false,
                             selectedByD: false };
        $scope.isProcessing = false;
        
        $scope.$watch('scenario.stats.frequencyTable', function(db) {
            if (db && db !== null && $scope.freqTables && $scope.freqTables[db]) {
                $scope.scenario.stats.theta = $scope.freqTables[db].theta;
                $scope.scenario.stats.probabilityModel = $scope.freqTables[db].model;
            }
        }, true);
        
        $scope.changeSelection = function(selection) {
            $scope.options.forEach(function(m) { m[selection] = $scope.selectAll[selection]; });
        };

        $scope.getFrequencyTables = function() {
            statsService.getActiveTables().then(function(tables) {
                $scope.freqTables = tables;
                for(var name in tables) {
                    if (tables.hasOwnProperty(name) && tables[name].default) {
                        $scope.scenario.stats.frequencyTable = name;
                    }
                }
            });
        };

        $scope.calculate = function() {
            $scope.fillScenario();
            $scope.isProcessing = true;
            $scope.getLRMix($scope.scenario).then(
                function(){
                    $scope.isProcessing = false;
                },
                function() {
                    $scope.isProcessing = false;
                });
        };
        
        $scope.getMarks = function(item) {
            return {'associated': item.associated, 'hit': !$scope.scenarioId && $scope.isRestricted && item.globalCode !== $scope.matchingCode && !item.associated};
        };
        
        $scope.anyMarks = function(mark) {
            return  $scope.options !== undefined &&
                    $scope.options.length !== 0 &&
                    $scope.options.filter(function(o) {
                        return $scope.getMarks(o)[mark];
                    }).length !== 0;
        };
	}
	
	return HypothesisController;
});

