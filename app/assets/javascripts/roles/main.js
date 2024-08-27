define(['angular', './roleService', './roleController', './controllers/roleModalController',
        './directives/pdgOperationsPopover'], 
function(angular, roleService, roleController, roleModalController, pdgOperationsPopover, $http) {
'use strict';

angular
	.module('pdg.roles', ['pdg.common'])
	.service('roleService', ['playRoutes', roleService])
	.controller('roleController', ['$scope', 'roleService', '$modal', 'alertService', '$filter', roleController])
	.controller('roleModalController', ['$scope', 'roleService', 'role', 'mode','alertService', roleModalController])
	.directive('pdgOperationsPopover', ['$compile', '$filter', pdgOperationsPopover])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/roles', {
			templateUrl: '/assets/javascripts/roles/roles.html', 
			controller: 'roleController'
		});
	}]);

return undefined;

});