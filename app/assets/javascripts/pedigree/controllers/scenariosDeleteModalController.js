define([], function() {
    'use strict';

    function ScenariosDeleteModalController($scope, $modalInstance) {

        $scope.close = function(value) {
            $modalInstance.close(value);
        };

    }

    return ScenariosDeleteModalController;

});