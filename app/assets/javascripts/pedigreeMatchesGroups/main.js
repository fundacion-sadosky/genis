define(['angular', './services/pedigreeMatchesGroupsService', './controllers/pedigreeMatchesGroupsController',
	'./controllers/groupController'],
function(angular, pedigreeMatchesGroupsService, pedigreeMatchesGroupsController, groupController, $http) {
'use strict';

angular
	.module('pdg.pedigreeMatchesGroups', ['pdg.common', 'pdg.stats', 'jm.i18next'])
	.service('pedigreeMatchesGroupsService', ['playRoutes', 'userService', pedigreeMatchesGroupsService])
	.controller('pedigreeMatchesGroupsController', ['$scope', '$routeParams', 'matchesService', '$location', 'analysisTypeService', 'pedigreeMatchesGroupsService', pedigreeMatchesGroupsController])
    .controller('groupController', ['$scope', 'pedigreeMatchesGroupsService', 'userService', 'alertService','$location','profileDataService', 'cryptoService', groupController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			//.when('/pedigreeMatchesGroups/groups.html', {templateUrl: '/assets/javascripts/pedigreeMatchesGroups/views/groups.html', controller: 'pedigreeMatchesGroupsController'});
			.when('/pedigreeMatchesGroups/groups.html', {templateUrl: '/assets/javascripts/pedigreeMatchesGroups/views/grupo.html', controller: 'pedigreeMatchesGroupsController'});

    }]);

return undefined;

});