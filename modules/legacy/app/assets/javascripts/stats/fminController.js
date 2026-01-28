define(['angular'], function(angular) {
	'use strict';

	function FminController($scope, statsService, $modalInstance, data) {
		$scope.loci = data.loci;
		$scope.calcOptions = statsService.getFminCalcOptions();
		$scope.result = {};
		var reInt = new RegExp(/^[1-9][0-9]*$/);
		var reProb = new RegExp(/^0\.\d*[1-9]$/);
		$scope.regEx = {'C': reInt, 'fmin': reProb, 'alpha': reProb, 'N': reInt};
		
		$scope.closeModal = function(apply){
			if (apply) {
				$modalInstance.close($scope.result);
			} else {
				$modalInstance.dismiss();
			}
		};
		
		$scope.setAll = function(locus) {
			if (!$scope.result.config[locus]){return;}
			
			var configToAplly = $scope.result.config[locus];
			$scope.loci.forEach(function(l){
				$scope.result.config[l] = angular.copy(configToAplly);
			});
		};
	}

	return FminController;

});	