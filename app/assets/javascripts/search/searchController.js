define(['angular'], function(angular) {
'use strict';

function searchController($scope, $log, profiledataService, searchService, $modal, alertService, $location, userService, appConf) {

    $scope.pageSize = 30;
	$scope.search = {input: '', active: true, inactive: false,notUploaded: false, category:''};
    $scope.lab = "-"+appConf.labCode+"-";

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	profiledataService.getSubCategories().then(function(response) {
		$scope.categories = response.data;
	});

	searchService.getCategoriesWithProfiles().then(function (response) {
        $scope.categoriesWithProfiles = response.data;
    });

	var modalInstance = null;

    if(localStorage.length > 0 && localStorage.getItem('search')){
        $scope.search=JSON.parse(localStorage.getItem('search'));
        localStorage.removeItem('search');
    }
    if(localStorage.length > 0 && localStorage.getItem('page')){
        $scope.page=JSON.parse(localStorage.getItem('page'));
        localStorage.removeItem('page');
    }else{
        $scope.page = 0;
    }
    console.log('page:' + $scope.page);

	var giveDeleteProfileModal = function(pd, editMode) {
		$scope.pdToDeleted = pd;
		$scope.editMode = editMode;
        $scope.showMotiveTextArea = false;
        searchService.getMotives().then(function(response) {
            $scope.motives = response.data;
        }, function() {
            $scope.motives = [];
        });
		modalInstance = $modal
			.open({
				templateUrl : '/assets/javascripts/search/views/modalDeleteProfile.html',
				scope : $scope
			});
	};
    $scope.onChangeMotive = function(selectedMotive){

        $scope.showMotiveTextArea = false;
        for (var i = 0; i < $scope.motives.length; i++) {
            if($scope.motives[i].id === parseInt(selectedMotive) && $scope.motives[i].freeText){
                $scope.showMotiveTextArea = true;
                $scope.motiveText = "";
            }
        }

    };
    $scope.goToProfile = function() {
        $location.url('/profiledata');
    };

	$scope.clean = function() {
		$scope.search = {input: '', active: true, inactive: false, category:''};
		// las dos lineas siguientes no harian falta
		//localStorage.removeItem('search');
		//localStorage.removeItem('page');
		$scope.page=0;
        $scope.getProfiles($scope.search);
	};

	$scope.doSearch = function(){
        $scope.page=0;
        $scope.getProfiles($scope.search);
    };

    $scope.store_search = function(){
       localStorage.setItem('search',JSON.stringify($scope.search));
       localStorage.setItem('page',JSON.stringify($scope.page));
    };

	var createSearchObject = function(filters) {
		$scope.previousFilters = angular.copy(filters);
        var searchObject = angular.copy(filters);
		searchObject.page = $scope.page;
		searchObject.pageSize = $scope.pageSize;
		return searchObject;
	};

	$scope.getProfiles = function(filters) {
		$scope.isProcessing = true;
        var searchObject = createSearchObject(filters);

		searchService.searchTotal(searchObject).then(function(response){
			$scope.totalItems = response.headers('X-PROFILES-LENGTH');
			$scope.totalDataBaseItem = response.headers('X-PROFILES-TOTAL-LENGTH');
			if ($scope.totalItems === '0') {
				$scope.noResult = true;
                $scope.isProcessing = false;
			} else {
				$scope.noResult = false;
                searchService.search(searchObject).then(function(response) {
                    $scope.results = response.data;
					$scope.initializeProfileMatches();
					$scope.isProcessing = false;
                });
			}
			$scope.currentPage=$scope.page+1;
		});
	};

    $scope.changePage = function() {
        $scope.page=$scope.currentPage-1;
        console.log('change_page:' + $scope.page);
        $scope.getProfiles($scope.previousFilters);
    };

	$scope.showMotive = function(sampleCode) {
		profiledataService.getMotive(sampleCode).then(
				function(response) {
					$scope.deleted = response.data;
					giveDeleteProfileModal(sampleCode, true);
				});
	};

	$scope.doDelete = function(pd) {
		if ($scope.deleted) {
			$scope.deleted = {};
		}
		giveDeleteProfileModal(pd, false);
	};

	$scope.closeModal = function() {
		modalInstance.close();
	};

	$scope.cancelForm = function() {
		$scope.deleted = undefined;
		$scope.genDelForm.$setPristine();
	};

	// En searchController.js
	$scope.deleteProfile = function(deleted) {
		for (var i = 0; i < $scope.motives.length; i++) {
			if($scope.motives[i].id === parseInt(deleted.selectedMotive) && !$scope.motives[i].freeText){
				deleted.motive = $scope.motives[i].description;
			}
		}
		var deletedRequest = {};

		deletedRequest.selectedMotive = parseInt(deleted.selectedMotive);
		deletedRequest.solicitor = deleted.solicitor;
		deletedRequest.motive  = deleted.motive;

		profiledataService.deleteProfile($scope.pdToDeleted.globalCode, deletedRequest).then(
			function(){
				alertService.success({message: 'La operación se realizó exitosamente'});
				$scope.closeModal();
				$scope.pdToDeleted.deleted = true;
			},
			function(response) {
				if (response.status !== 499) {
					var errorMessage = 'Ha ocurrido un error ' + response.data.message;
					if (response.data.message.includes("Failed to notify deletion due to timeout")) {
						errorMessage = "Timeout occurred while deleting profile.  Profile deleted but deletion status update may not have been sent to remote instance.";
					}
					alertService.error({message: errorMessage});
					$scope.closeModal();
					$scope.pdToDeleted.deleted = false;
				}
			}
		);
	};


	$scope.hasPermission = function(permission) {
		return userService.hasPermission(permission);
	};

	$scope.allowManualLoading = function(pd) {

		console.log('Categoria del perfil: ' + pd.category);
		if( pd && pd.category ){
			for(var i in $scope.categories){
				if( $scope.categories[i].id === pd.category ){
					return $scope.categories[i].manualLoading;
				}
			}
			return false;

		}else{
			return false;
		}
	};

	$scope.goToProfileTab = function(code) {
		$location.url('/profile/' + code + '?tab=add' );
	};

	$scope.profileMatches = {};

	$scope.initializeProfileMatches = function (){
		console.log("## La lista de perfiles: ", $scope.results);
		$scope.results.forEach(function (profileObj) {
			searchService.searchMatchesProfile(profileObj.globalCode).then(function (response) {
				$scope.profileMatches[profileObj.globalCode] = response.data.length > 0;
				console.log("El resultado de search matches profile: ", response.data);
			});
		});
	};

	$scope.getRowColor = function(profileCode) {
		// habrá problema si no hay profiles? porque no lo estoy definiendo en ese caso, pero supongo que no entraría acá tampoco
		console.log("Search - los matches del perfil", profileCode ," son: ", $scope.profileMatches[profileCode]);

		if($scope.profileMatches[profileCode]){
			return '#d500f9';
		} else {
			return '#e4e7ec';
		}

	};


}

return searchController;

});