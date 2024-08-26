define([], function() {
	'use strict';
	function GenotypificationController($scope, analysisTypeService, locusService) {
        $scope.showDifferences = false;
        $scope.showMatches = false;
        $scope.selectedOptions = [];

        analysisTypeService.listById().then(function(response) {
            $scope.analysisTypes = response;
        });

        locusService.list().then(function(response) {
            $scope.locusById = {};
            response.data.forEach(function(l) {
                $scope.locusById[l.id] = l;
            });
        });

        $scope.availableOptions = function() {
            if ($scope.options) {
                return $scope.options.filter(function (op) {
                    return $scope.selectedOptions.indexOf(op) === -1;
                });
            }
        };

        $scope.show = function(option) {
            if (option && ($scope.selectedOptions.length < 4)) {
                $scope.selectedOptions.push(option);
            }
        };

        $scope.hide = function(option) {
            if (option) {
                var index = $scope.selectedOptions.indexOf(option);
                if (index > -1) {
                    $scope.selectedOptions.splice(index, 1);
                }
            }
        };

        $scope.displayDifferences = function() {
            $scope.showMatches = false;
            $scope.showDifferences = !$scope.showDifferences;
        };

        $scope.displayMatches = function() {
            $scope.showDifferences = false;
            $scope.showMatches = !$scope.showMatches;
        };


	}
	
	return GenotypificationController;
});

