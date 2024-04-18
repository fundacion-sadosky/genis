define([], function() {
	'use strict';

	function OperationLogLotsController($scope, $routeParams, $q, $timeout, operationLogLotService) {

		$scope.pageSize = 10;
		$scope.currentPage = 1;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("nuevo");

		operationLogLotService.getTotalLots().then(function(response) {
			$scope.totalItems = response.headers('X-LOTS-LENGTH');
		});

		$scope.listLots = function() {
			operationLogLotService.getLotsNames($scope.currentPage - 1,
					$scope.pageSize).then(function(response) {
				$scope.lots = response.data;
			});
		};

		$scope.checkLot = function(lotId) {
			$scope.verificationResult = null;
			$scope.showSpinner = true;
			var p1 = operationLogLotService.checkLot(lotId);
			var p2 = $timeout(600);
			$q.all([ p1, p2 ]).then(function(responses) {
				$scope.verificationResult = responses[0].data;
			}).finally(function() {
				$scope.showSpinner = false;
			});
		};
	}

	return OperationLogLotsController;

});