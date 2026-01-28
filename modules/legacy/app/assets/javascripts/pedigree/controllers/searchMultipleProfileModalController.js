define(['lodash'], function(_) {
'use strict';

function SearchMultipleProfileModalController($scope, $modalInstance, pedigreeService, data, profileDataService) {
	$scope.isProcessing = false;
	$scope.previous = data.previousSearch;
    $scope.selectAllCheckToggle = false;
    $scope.idCase = data.idCourtCase;
    $scope.tabActive = data.tabActive;
    $scope.tipo = data.tipo;
    $scope.pageSizeModal = 10;
    $scope.currentPageNumberModal = 1;

    $scope.totalItemsModal = 0;
    $scope.countProfiles = function(tipo) {

        $scope.isProcessing = false;
        if(!_.isUndefined($scope.searchText) && $scope.searchText.trim().length > 0){
            $scope.isProcessing = true;
            pedigreeService.count($scope.searchText,$scope.idCase,$scope.tabActive === 3,tipo).then(function(response){
                $scope.totalItemsModal= response.data;
            });
        }else{
            $scope.results = [];
            $scope.isProcessing = false;
        }
    };
    $scope.selectAllProfiles = function(){
        $scope.selectAllCheckToggle = !$scope.selectAllCheckToggle;
        if($scope.results){
            $scope.results.forEach(function (element) {
                if(!element.associated){
                    element.selected = $scope.selectAllCheckToggle;
                }
            });
        }
    };
   $scope.searchProfile = function() {
       var tipo = ($scope.tipo === "DVI")? 3 : 2;

       $scope.countProfiles(tipo);

       $scope.isProcessing = false;
       if(!_.isUndefined($scope.searchText) && $scope.searchText.trim().length > 0){
            $scope.isProcessing = true;
            pedigreeService.filter($scope.searchText,$scope.idCase,$scope.tabActive === 3,tipo,$scope.currentPageNumberModal -1,$scope.pageSizeModal).then(function(response){
                $scope.results = response.data;
                $scope.results.forEach(function(result){
                    result.categoryName = $scope.getSubCategoryName(result.category);
                });
                $scope.isProcessing = false;
            });
       }else{
           $scope.results = [];
           $scope.isProcessing = false;
       }
	};
	
	$scope.selectProfile = function() {
        $modalInstance.close($scope.results.filter(function(pd){return pd.selected;}));
	};
    $scope.cancel = function() {
        $modalInstance.close([]);
    };
    $scope.getCategories = function(){
		return profileDataService.getCategories().then(function(categories){
			$scope.categories = categories.data;
		});
	};

	$scope.getSubCategoryName = function(id){
		return pedigreeService.getSubCatName($scope.categories, id);
	};

    $scope.initialize = function(){
        $scope.getCategories().then(function(){
            if ($scope.previous !== ""){
                $scope.searchText = $scope.previous;
                $scope.searchProfile();
            }
        });
    };

    $scope.initialize();
}

return SearchMultipleProfileModalController;

});