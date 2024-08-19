define(['angular', './geneticistsService', './geneticistsController', './updateController', 'common'], 
function(angular, geneticistsService, geneticistsController, updateController, $http) {
'use strict';

angular
	.module('pdg.geneticists', ['pdg.common'])
	.service('geneticistsService', ['playRoutes', geneticistsService])
	.controller('geneticistsController', ['$scope', 'geneticistsService', 'laboratoriesService', '$modal', 'alertService', geneticistsController])
	.controller('geneticistsUpdateController', ['$scope', '$modalInstance', 'geneticistsService', 'modalGeneticist', 'laboratories' , 'parentScope', 'alertService', updateController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/geneticist', {
			templateUrl: '/assets/javascripts/geneticists/geneticists.html', 
			controller: 'geneticistsController'
		});
	}]);

return undefined;

});