define(['angular', './controllers/scenarioController', './controllers/genotypificationController',
	'./controllers/resultsController', './controllers/hypothesisController',
    './controllers/scenariosController','./services/scenarioService',
	'./controllers/saveModalController', 'common'],
function(angular, scenarioController, genotypificationController, resultsController, hypothesisController, scenariosController, scenarioService, saveModalController, $http) {
'use strict';

angular
	.module('pdg.scenarios', ['pdg.common', 'pdg.stats', 'jm.i18next'])
    .controller('scenarioController', ['$scope', '$routeParams', 'matchesService', 'profileDataService', 'scenarioService', 'laboratoriesService', scenarioController])
	.controller('genotypificationController', ['$scope', 'analysisTypeService', 'locusService', genotypificationController])
	.controller('resultsController', ['$scope', '$modal', 'alertService', 'scenarioService', '$route', '$location', '$filter', resultsController])
    .controller('hypothesisController', ['$scope', 'statsService', hypothesisController])
    .controller('scenariosController', ['$scope', '$routeParams', '$location', 'scenarioService', 'alertService', scenariosController])
    .controller('saveModalController', ['$scope', 'scenario', 'results', 'validated', 'restricted', 'scenarioService', 'alertService', 'userService', saveModalController])
    .service('scenarioService', ['playRoutes', '$q', '$filter', '$http', scenarioService])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/scenarios/scenario.html',
				{templateUrl: '/assets/javascripts/scenarios/views/scenario.html', controller: 'scenarioController'})
			.when('/scenarios/scenarios.html',
				{templateUrl: '/assets/javascripts/scenarios/views/scenarios.html', controller: 'scenariosController'});

	}]);

return undefined;

});