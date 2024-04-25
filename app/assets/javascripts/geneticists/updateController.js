define([], function() {
	'use strict';

	function UpdateCtrl ($scope, modalInstance, geneticistsService, modalGeneticist, laboratories, parentScope, alertService) {
		$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
		$scope.modalGeneticist = modalGeneticist;
		$scope.laboratories = laboratories;
		
		$scope.updateGeneticist = function(){
			geneticistsService.updateGeneticist($scope.modalGeneticist).then(function(response){
				if (response.data > 0) {
					alertService.success({message: $.i18n.t('geneticists.modifySuccess')});
				} else {
					alertService.error({message: $.i18n.t('geneticists.modifyError')});
				}
				modalInstance.close();
			});
		};
		
		$scope.closeModal = function(){
			modalInstance.close();
		};		

	}
	
	return UpdateCtrl ;

});