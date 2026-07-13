define([], function() {
    'use strict';

    function PedigreeMatchingParameterController($scope, mutationModelService, alertService) {

        $scope.isProcessing = false;

        $scope.init = function() {
            $scope.isProcessing = true;
            mutationModelService.getMaxMendelianExclusions().then(function(response) {
                $scope.maxMendelianExclusions = response.data.maxMendelianExclusions;
                $scope.isProcessing = false;
            }, function() {
                alertService.error({message: 'Error'});
                $scope.isProcessing = false;
            });
        };

        $scope.init();

        $scope.save = function() {
            $scope.isProcessing = true;
            mutationModelService.updateMaxMendelianExclusions($scope.maxMendelianExclusions).then(function(response) {
                $scope.maxMendelianExclusions = response.data.maxMendelianExclusions;
                $scope.isProcessing = false;
                alertService.success({message: 'Parámetro actualizado correctamente.'});
            }, function(error) {
                alertService.error({message: (error.data && error.data.message) || 'Error'});
                $scope.isProcessing = false;
            });
        };

    }

    return PedigreeMatchingParameterController;

});