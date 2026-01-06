define([], function() {
    'use strict';

    function ScenarioNameModalController($scope, $modalInstance) {

        $scope.close = function(value) {
            $modalInstance.close(value);
        };

    }

    return ScenarioNameModalController;

});