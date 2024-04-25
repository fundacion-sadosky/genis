define([], function() {
    'use strict';

    function LocusUpdateController($scope, locusService, alertService) {
        console.log('update');

        $scope.closeModal = function(){
            $scope.modalInstance.close();
        };

        $scope.save = function(){
            $scope.isProcessing = true;
            if(!$scope.selectedLocusCopy.chromosome || $scope.selectedLocusCopy.chromosome==='XY' || $scope.selectedLocusCopy.chromosome === 'MT'){

                delete $scope.selectedLocusCopy.minAlleleValue;
                delete $scope.selectedLocusCopy.maxAlleleValue;

            }
            var fullLocus = {};
            fullLocus.locus = $scope.selectedLocusCopy;
            fullLocus.alias = $scope.selectedAliasCopy;
            fullLocus.links = [];
            locusService.update(fullLocus).then(function() {
                $scope.closeModal();
                alertService.success({message: $.i18n.t('alerts.locus.updated')});
                $scope.loadLocus();
                $scope.isProcessing = false;
            }, function(response) {
                $scope.closeModal();
                alertService.error(response.data);
                $scope.isProcessing = false;
            });
        };
    }

    return LocusUpdateController;

});