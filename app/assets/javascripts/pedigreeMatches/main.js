define(['angular', './services/pedigreeMatchesService', './pedigreeMatchesController'],
function(angular, pedigreeMatchesService, pedigreeMatchesController) {
'use strict';

angular
	.module('pdg.pedigree.matches', ['pdg.common', 'pdg.stats', 'jm.i18next'])
	.run(['$anchorScroll',function ($anchorScroll) {
        $anchorScroll.yOffset = 100;
    }])
	.service('pedigreeMatchesService', ['playRoutes', '$q', 'userService', pedigreeMatchesService])
	.controller('pedigreeMatchesController', ['$scope', 'pedigreeMatchesService', 'matchesService','alertService','profileDataService','$anchorScroll', '$timeout', pedigreeMatchesController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			//.when('/pedigreeMatches', {templateUrl: '/assets/javascripts/pedigreeMatches/pedigree-matcher-manager.html', controller: 'pedigreeMatchesController'}),
			.when('/pedigreeMatches', {templateUrl: '/assets/javascripts/pedigreeMatches/pedigree-match-manager.html', controller: 'pedigreeMatchesController'});
	}]);

return undefined;

});