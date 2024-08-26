define([], function() {
    'use strict';

    function DisassociateController($scope, $modalInstance, node) {

        $scope.node = node;

        $scope.close = function(value) {
            $modalInstance.close(value);
        };

    }

    return DisassociateController;

});