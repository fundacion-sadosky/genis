define(['angular', './operationLogLotService', './operationLogLotsController', './operationLogLotController', 'common'], 
function(angular, OperationLogLotService, OperationLogLotsController, OperationLogLotController) {
'use strict';

angular
	.module('pdg.audit', ['pdg.common'])
	.service('operationLogLotService', ['playRoutes', OperationLogLotService])
	.controller('operationLogLotsController',['$scope', '$routeParams', '$q', '$timeout', 'operationLogLotService', OperationLogLotsController])
	.controller('operationLogLotController', ['$scope', '$routeParams', 'operationLogLotService', 'roleService', '$filter', OperationLogLotController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/audit/operationLogLots', {
				templateUrl: '/assets/javascripts/audit/operationLogLots.html', 
				controller: 'operationLogLotsController' })
			.when('/audit/operationLogLots/:lotId', {
				templateUrl: '/assets/javascripts/audit/operationLogLot.html', 
				controller: 'operationLogLotController' });
	}]);

return undefined;

});