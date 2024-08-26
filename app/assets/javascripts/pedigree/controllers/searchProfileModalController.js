define(['lodash'], function(_) {
'use strict';

function SearchProfileModalController($scope, $modalInstance, pedigreeService, data, profileDataService) {
	$scope.isProcessing = false;
    $scope.totalItems = 0;
    $scope.currentPageNumber = 1;
    $scope.pageSize = 10;
    $scope.noEsEscenario= data.noEsEscenario;
    $scope.caseType= data.caseType;
    $scope.profile = data.profiles;

    if(!_.isUndefined(data.previousSearch)&&data.previousSearch.length>0){
        $scope.previous = data.previousSearch;
    }

   $scope.searchProfile = function() {
		$scope.isProcessing = true;
		var search = {};
       search.idCourtCase = data.idCourtCase;
       search.pageSize = $scope.pageSize;
       search.profilesCod= [];

       $scope.profile.forEach(function (profile) {
           if(profile.globalCode){
           search.profilesCod.push(profile.globalCode);
           }
       });


       if($scope.currentPageNumber){
           search.page  = $scope.currentPageNumber -1;
       }else{
           search.page = 0;
       }
       if(!_.isUndefined($scope.searchText)&&$scope.searchText.length>0){
           search.searchText = $scope.searchText;
       }

       if($scope.noEsEscenario !== false ){
           $scope.noEsEscenario = true;
       }
       if($scope.noEsEscenario){
           search.statusProfile = "Active";
       }
       if($scope.caseType === "MPI" && $scope.noEsEscenario === false ){
                search.idCourtCase= 0;
                     }

       pedigreeService.getTotalProfilesNodeAssociation(search, $scope.noEsEscenario).then(function (response) {
           $scope.totalItems = response.data;
            });

       pedigreeService.getProfilesNodeAssociation(search,$scope.noEsEscenario).then(function(response){
			console.log('data',response.data);
			$scope.results = response.data;
            $scope.results.forEach(function(result){
                result.categoryName = $scope.getSubCategoryName(result.category);
            });

            $scope.isProcessing = false;

		});

	};



	$scope.selectProfile = function(pd) {
        pd.previousSearch = $scope.searchText;
        $modalInstance.close(pd);
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
            if (!_.isUndefined($scope.previous)){
                $scope.searchText = $scope.previous;
            }
            $scope.searchProfile();
        });
    };

    $scope.initialize();
}

return SearchProfileModalController;

});