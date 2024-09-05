define(['angular', './laboratoriesService', './laboratoriesController', './saveController', 'common'],
function(angular, laboratoriesService, LaboratoriesController, SaveController, $http) {
'use strict';

angular
	.module('pdg.laboratories', ['pdg.common'])
	.service('laboratoriesService', ['playRoutes', '$http', laboratoriesService])
	.controller('laboratoriesController', ['$scope', '$modal', 'laboratoriesService', LaboratoriesController])
	.controller('laboratoriesSaveController', ['$scope', '$log', 'laboratoriesService', 'alertService', SaveController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/laboratories', {
			templateUrl: '/assets/javascripts/laboratories/laboratories.html', 
			controller: 'laboratoriesController'
		});
	}]);

return undefined;

});