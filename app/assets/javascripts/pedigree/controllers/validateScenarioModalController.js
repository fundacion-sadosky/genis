define([], function() {
    'use strict';

    function ValidateScenarioModalController($scope, $modalInstance, data, pedigreeService) {

        $scope.scenario = data.scenario;
        $scope.pedigreeActivo = false;

        $scope.close = function() {
            $modalInstance.close({ status: "Cancel" });
        };

        $scope.changePedigreeSelection = function(valor) {
            $scope.pedigreeActivo = valor;
        };

        $scope.validate = function(){
            pedigreeService.confirmScenario($scope.scenario, 'Validated', $scope.pedigreeActivo).then(function() {
                    $modalInstance.close({ status: "Success" });
                },
                function(response) {
                    $modalInstance.close({ status: "Error", error: response.data });
                }
            );
        };
    }

    return ValidateScenarioModalController;
});