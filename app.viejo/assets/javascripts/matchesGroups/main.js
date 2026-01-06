define(['angular', './controllers/matchesGroupsController'],
function(angular, matchesGroupsController) {
'use strict';

angular
	.module('pdg.matchesGroups', ['pdg.common', 'pdg.stats', 'jm.i18next'])
	.controller('matchesGroupsController', ['$scope', '$routeParams', 'matchesService', 'profileService', 'profileDataService', '$filter', '$location', 'scenarioService', 'analysisTypeService', 'locusService', 'helper', 'alertService', 'userService', matchesGroupsController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/matchesGroups/groups.html', {templateUrl: '/assets/javascripts/matchesGroups/views/groups.html', controller: 'matchesGroupsController'});
	}]);

return undefined;

});