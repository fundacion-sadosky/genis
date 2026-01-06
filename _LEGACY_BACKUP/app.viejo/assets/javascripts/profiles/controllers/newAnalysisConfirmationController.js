define([], function() {
    'use strict';

    function NewAnalysisConfirmationController($scope, newAnalysis, controller) {
        
        $scope.newAnalysis = newAnalysis;

        $scope.close = function(value) {
            controller.closeNewAnalysisConfirmationModal(value, newAnalysis);
        };

    }
        
    return NewAnalysisConfirmationController;

});


