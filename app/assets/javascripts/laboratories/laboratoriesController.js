define([], function() {
	'use strict';

	function LaboratoriesCtrl ($scope, $modal, laboratoriesService) {
		$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
		$scope.modalInstance = null;
		$scope.modalResponse = false;
		$scope.modalUpdateResult = false;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("nuevo");

        $scope.clearLaboratory = function(){
            $scope.laboratory = { dropIn: 0.05, dropOut: 0.1 };
            $scope.isNew = true;
        };

        $scope.clearLaboratory();

		$scope.refreshLabs = function() {
			laboratoriesService.getLaboratoriesDescriptive().then(
				function(response){
					$scope.laboratories = response.data;
				});
		};

		$scope.refreshLabs();

		var giveUpdateModal = function(code){
			$scope.laboratory.code = code;
			$scope.modalInstance = $modal.open({
				templateUrl:'/assets/javascripts/laboratories/update.html',
				scope: $scope,
				keyboard: false
			});
		};

		$scope.doUpdate = function(code){
			giveUpdateModal(code);
		};
	}
	
	return LaboratoriesCtrl;
});