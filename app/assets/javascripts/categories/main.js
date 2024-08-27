define(['angular', './categoriesService', './categoriesController', './associationsController', './matchingRulesController', './categoryModalController', './groupModalController', 'common'], 
function(angular, categoriesService, categoriesController, associationsController, matchingRulesController, categoryModalController, groupModalController, $http) {
'use strict';

angular
	.module('pdg.categories', ['pdg.common'])
	.service('categoriesService', ['$q', 'playRoutes', categoriesService])
	.controller('groupModalController', ['$scope', 'categoriesService', 'group', 'mode', 'alertService', groupModalController])
	.controller('categoryModalController', ['$scope', 'categoriesService', 'category', 'mode', 'alertService', categoryModalController])
	.controller('associationsController', ['$scope', associationsController])
	.controller('matchingRulesController', ['$scope', 'appConf', '$filter', matchingRulesController])
	.controller('categoriesController', ['$scope', 'categoriesService', '$modal', 'alertService', 'analysisTypeService', categoriesController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider.when('/categories', {
			templateUrl: '/assets/javascripts/categories/categories.html', 
			controller: 'categoriesController'
		});
	}]);

return undefined;

});