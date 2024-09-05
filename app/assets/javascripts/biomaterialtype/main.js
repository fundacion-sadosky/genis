define(['angular', './bioMaterialTypeService', './bioMaterialTypeController', './controllers/bmtModalController', 'common'], 
function(angular, bioMaterialTypeService, bioMaterialTypeController, bmtModalController, $http) {
'use strict';

angular
	.module('pdg.biomaterialtype', ['pdg.common'])
	.service('bioMaterialTypeService', ['playRoutes','$http', bioMaterialTypeService])
	.controller('bioMaterialTypeController', ['$scope', 'bioMaterialTypeService', '$modal', 'alertService', bioMaterialTypeController])
	.controller('bmtModalController', ['$scope', 'bmt', 'mode', bmtModalController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/biomaterialtype', {
			templateUrl: '/assets/javascripts/biomaterialtype/biomaterialtype.html', 
			controller: 'bioMaterialTypeController'
		});
	}]);

return undefined;

});