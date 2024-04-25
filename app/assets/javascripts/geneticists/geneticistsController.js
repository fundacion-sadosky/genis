define([], function() {
	'use strict';

	function GeneticistsCtrl ($scope, geneticistsService, laboratoriesService, $modal, alertService) {
		var modalInstance = null;
		$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
		$scope.geneticist = {};

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
		
		$scope.modalResponse = false;
		$scope.modalUpdateResult = false;
		
		var giveUpdateModal = function(id){
			geneticistsService.getGeneticist(id).then(function(response){
				
				$scope.modalGeneticist = response.data;
				
				modalInstance = $modal.open({
					templateUrl:'/assets/javascripts/geneticists/update.html',
					controller: 'geneticistsUpdateController',
					resolve: {
						modalGeneticist: function () {
							return $scope.modalGeneticist;
						},
						laboratories: function () {
							return $scope.laboratories;
						},
						parentScope: function (){
							return $scope;
						}
					}
				});
				
				modalInstance.result.then(
					function () {
						$scope.giveGeneticist($scope.modalGeneticist.laboratory);
					}, 
					function () {//dismissed
					}
				);
				
			});
		};
		
		$scope.doUpdate = function(id){
			giveUpdateModal(id);
		};
		
		$scope.giveGeneticist = function(lab){
			geneticistsService.getGeneticists(lab).then(function(response){
                $scope.selected.laboratory = lab;
				$scope.geneticists = response.data;
			});
		};
		
		$scope.cancelForm = function(){
			$scope.geneticist = {};
			$scope.genForm.$setPristine();
		};
		
		laboratoriesService.getLaboratories().then(function(response){
			$scope.laboratories = response.data;
		});
		
		$scope.saveGeneticist = function(){
			geneticistsService.saveGeneticist($scope.geneticist).then(function(response){
				if (response.data > 0) {
					alertService.success({message: $.i18n.t('geneticists.newSuccess')});
				} else {
					alertService.error({message: $.i18n.t('geneticists.newError')});
				}
				$scope.geneticist = {};
				$scope.genForm.$setPristine();
			},function(error){
				console.log(error);
				alertService.error({message: ' ' + $.i18n.t('geneticists.newError')+': ' + error.data.error});
			});
		};
	}
	
	return GeneticistsCtrl;

});