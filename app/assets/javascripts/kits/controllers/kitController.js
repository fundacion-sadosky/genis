define([], function() {
    'use strict';

    function KitController($scope, kitService, helper) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("nuevo");
        
        $scope.loadKits = function() {
            $scope.isProcessing = true;
            return kitService.listFull().then(function(response) {
                $scope.kits = response.data;
                $scope.kitsByAnalysisType = helper.groupBy($scope.kits, 'type');
                $scope.isProcessing = false;
            }, function() {
                $scope.isProcessing = false;
            });
        };
        
    }

    return KitController;

});