define([], function() {
    'use strict';

    function LocusController($scope, locusService, helper, analysisTypeService) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.loadLocus = function() {
            $scope.isProcessing = true;
            return locusService.listFull().then(function(response) {
                $scope.locus = response.data;
                $scope.locusByAnalysisType = helper.groupBy($scope.locus, 'locus.analysisType');
                $scope.isProcessing = false;
            }, function() {
                $scope.isProcessing = false;
            });
        };      
        
        analysisTypeService.listById().then(function(response) {
            $scope.analysisTypes = response;
        });
        
    }
        
    return LocusController;

});
