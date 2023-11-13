define(['angular', './services/matchesService', './services/hypothesesService',
        './matchesController', './controllers/comparisonController', 
        './controllers/statsOptionModalController', './directives/pdgProfileDataInfo', './directives/pdgProfileDataInfoPopover',
        './directives/pdgStatisticalOptions', 'common'],
function(angular, matchesService, hypothesesService, matchesController, comparisonController, statsOptionModalController,
		pdgProfileDataInfo, pdgProfileDataInfoPopover, pdgStatisticalOptions) {
'use strict';

angular
	.module('pdg.matches', ['pdg.common', 'pdg.stats', 'jm.i18next'])
	.run(['$anchorScroll',function ($anchorScroll) {
        $anchorScroll.yOffset = 100;
    }])
	.service('matchesService', ['playRoutes', 'userService', matchesService])
	.service('hypothesesService', ['playRoutes', hypothesesService])
	.controller('matchesController', ['$scope', '$routeParams' , 'matchesService', 'profileDataService', 'alertService','laboratoriesService', 'profileService', 'locusService', 'helper', '$anchorScroll','$timeout',  matchesController])
	.controller('comparisonController', ['$scope', '$routeParams', '$modal', '$timeout', '$filter', 'matchesService', 'profileDataService', 'profileService', '$sce', 'statsService', 'alertService', 'cryptoService', 'appConf', 'analysisTypeService', 'locusService','$window', comparisonController])
	.controller('statsOptionModalController', ['$scope', '$modal', '$modalInstance', 'selectedOptions', 'mix', 'statsService', 'alertService', 'profileData', 'scenarioService',  statsOptionModalController])
	.directive('pdgProfileDataInfo', [ 'profileDataService', pdgProfileDataInfo])
	.directive('pdgProfileDataInfoPopover', ['profileDataService', 'matchesService', pdgProfileDataInfoPopover])
	.directive('pdgStatisticalOptions', [pdgStatisticalOptions])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/matches', {templateUrl: '/assets/javascripts/matches/matcher-manager.html', controller: 'matchesController'})
			.when('/comparison/:profileId/matchedProfileId/:matchedProfileId/matchingId/:matchingId', 
					{templateUrl: '/assets/javascripts/matches/views/comparison.html', controller: 'comparisonController'})
			.when('/comparison/:profileId/matchedProfileId/:matchedProfileId/matchingId/:matchingId/pedigreeMatch/:isPedigreeMatch',
				{templateUrl: '/assets/javascripts/matches/views/comparison.html', controller: 'comparisonController'})
            .when('/comparison/:profileId/matchedProfileId/:matchedProfileId/matchingId/:matchingId/collapsingMatch/:isCollapsingMatch',
                {templateUrl: '/assets/javascripts/matches/views/comparison.html', controller: 'comparisonController'});
	}]);

return undefined;

});