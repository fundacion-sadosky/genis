define(['angular'], function(angular) {
'use strict';

function searchController($scope, $log, profiledataService, searchService, $modal, alertService, $location, userService, appConf) {
    $scope.currentPage = 1;
    $scope.pageSize = 30;
	$scope.search = {input: '', active: true, inactive: false,notUploaded: false};
    $scope.lab = "-"+appConf.labCode+"-";

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	profiledataService.getSubCategories().then(function(response) {
		$scope.categories = response.data;
	});

	var modalInstance = null;


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
		$scope.search = {input: '', active: true, inactive: false};
        $scope.getProfiles($scope.search);
	};

	var createSearchObject = function(filters) {
		$scope.previousFilters = angular.copy(filters);

        var searchObject = angular.copy(filters);
		searchObject.page = $scope.currentPage - 1;
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
                    $scope.isProcessing = false;
                });
			}
		});
	};

    $scope.changePage = function() {
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
                    alertService.error({message: 'Ha ocurrido un error ' + response.data.message});
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


}

return searchController;

});