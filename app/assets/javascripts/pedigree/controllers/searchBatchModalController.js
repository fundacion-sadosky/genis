define(['lodash'], function(_) {
'use strict';

function SearchBatchController($scope, $modalInstance, pedigreeService, data, profileDataService) {
    $scope.isProcessing = false;
    $scope.previous = data.previousSearch;
    $scope.selectAllCheckToggle = false;
    $scope.idCase = data.idCourtCase;
    $scope.caseType = data.caseType;
    console.log(profileDataService);
    $scope.selectAllBatchs = function(){
        $scope.selectAllCheckToggle = !$scope.selectAllCheckToggle;
        if($scope.results){
            $scope.results.forEach(function (element) {
                if(!element.associated && element.totalProfilesToImport !== 0){
                    element.selected = $scope.selectAllCheckToggle;
                }
            });
        }
    };
    $scope.searchBatchs = function() {
        if(!_.isUndefined($scope.searchText) && $scope.searchText.trim().length > 0){
            $scope.isProcessing = true;
            pedigreeService.getBatchSearchModalViewByIdOrLabel($scope.searchText, parseInt(data.idCourtCase)).then(function(response){
                $scope.results = response.data;
                $scope.isProcessing = false;
            });
        }else{
            $scope.results = [];
            $scope.isProcessing = false;
        }
    };

    $scope.selectBatch = function() {
        $modalInstance.close($scope.results.filter(function(pd){return pd.selected;}));
    };
    $scope.cancel = function() {
        $modalInstance.close([]);
    };

    $scope.initialize = function(){
        if ($scope.previous !== ""){
            $scope.searchText = $scope.previous;
            $scope.searchBatchs();
        }
    };


    $scope.initialize();
}

return SearchBatchController;

});