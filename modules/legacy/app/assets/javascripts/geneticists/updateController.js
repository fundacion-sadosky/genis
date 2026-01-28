define([], function() {
	'use strict';

	function UpdateCtrl ($scope, modalInstance, geneticistsService, modalGeneticist, laboratories, parentScope, alertService) {
		$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
		$scope.modalGeneticist = modalGeneticist;
		$scope.laboratories = laboratories;
		
		$scope.updateGeneticist = function(){
			geneticistsService.updateGeneticist($scope.modalGeneticist).then(function(response){
				if (response.data > 0) {
					alertService.success({message: 'Fue modificado con exito'});
				} else {
					alertService.error({message: 'No pudo ser modificado'});
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