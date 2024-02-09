define(
	[
		'angular', 
		'./pedigreeService', 
		'./controllers/originalController',
		'./controllers/pedigreeController',
		'./controllers/disassociateController',
		'./controllers/courtcasesController',
		'./controllers/courtcaseDataController', 
		'./controllers/searchProfileModalController', 
		'./controllers/scenariosDeleteModalController',
		'./controllers/pedigreeScenarioController',
		'./controllers/scenarioNameModalController',
		'./controllers/pedigreeScenarioReportController',
		'./controllers/pedigreeCaseSummaryController',
		'./controllers/pedigreeGenotypificationController',
		'./controllers/validateScenarioModalController',
		'./filters',
		'common',
		'./controllers/courtcaseController',
		'./controllers/profileCourtcaseController',
		'./controllers/searchMultipleProfileModalController',
		'./controllers/searchBatchModalController',
		'./controllers/pedigreesDataController',
		'./controllers/activarPedigreeModalController',
		'./controllers/metadataModalController',
		'./controllers/courtCaseCloseModalController',
		'./controllers/groupedProfilesModal',
		'../matches/matchesController',
		'../matchesGroups/controllers/matchesGroupsController',
		'./controllers/pedigreeConsistencyController',
		'./controllers/pedigreeMatchesCourtCaseController'
	],
	function(
		angular,
		pedigreeService, 
		originalController,
		pedigreeController,
		disassociateController,
		courtcasesController,
		courtcaseDataController,
		searchProfileModalController,
		scenariosDeleteModalController,
		pedigreeScenarioController,
		scenarioNameModalController,
		pedigreeScenarioReportController,
		pedigreeCaseSummaryController,
		pedigreeGenotypificationController,
		validateScenarioModalController,
		filters,
		common,
		courtcaseController,
		profileCourtcaseController,
		searchMultipleProfileModalController,
		searchBatchModalController,
		pedigreesDataController,
		activarPedigreeModalController,
		metadataModalController,
		courtCaseCloseModalController,
		groupedProfilesModal,
		matchesController,
		matchesGroupsController,
		pedigreeConsistencyController,
		pedigreeMatchesCourtCaseController
	) {
		'use strict';
		angular
			.module('pdg.pedigree', ['pdg.common'])
			.run(['$anchorScroll',function ($anchorScroll) {$anchorScroll.yOffset = 100;}])
			.service('pedigreeService', [ 'playRoutes', 'userService', pedigreeService])
			.filter('isFemale', filters.isFemale)
			.filter('isMale', filters.isMale)
			.controller('pedigreeController', ['$scope', '$filter', 'pedigreeService', '$routeParams', '$modal', 'statsService', 'alertService', '$timeout', '$route','$location', pedigreeController])
			.controller('courtcaseController', ['$scope', '$filter', 'pedigreeService', '$routeParams', '$modal', 'statsService', 'alertService', '$timeout', '$route', '$location', courtcaseController])
			.controller('profileCourtcaseController', ['$scope', '$filter', 'pedigreeService', '$routeParams', '$modal', 'statsService', 'alertService', '$timeout', '$route', '$location','bulkuploadService','locusService', profileCourtcaseController])
			.controller('originalController', ['$scope', 'pedigreeService', '$log', '$routeParams', '$modal', '$route', 'alertService', '$timeout', 'hotkeys', originalController])
			.controller('pedigreesDataController', ['$scope', '$filter', 'pedigreeService', '$routeParams', '$modal', 'statsService', 'alertService', '$timeout', '$route', '$location', pedigreesDataController])
			.controller('disassociateController', [ '$scope', '$modalInstance', 'node', disassociateController])
			.controller('scenarioNameModalController', [ '$scope', '$modalInstance', scenarioNameModalController])
			.controller('validateScenarioModalController', [ '$scope', '$modalInstance', 'data', 'pedigreeService', validateScenarioModalController])
			.controller('scenariosDeleteModalController', [ '$scope', '$modalInstance', scenariosDeleteModalController])
			.controller('courtcasesController', [ '$scope', 'pedigreeService', '$location', '$filter', '$modal', 'alertService', courtcasesController])
			.controller('courtcaseDataController', [ '$scope', 'userService', 'pedigreeService', '$routeParams', '$location', 'profileDataService', '$route', 'alertService','$modal', courtcaseDataController])
			.controller('searchProfileModalController',  ['$scope', '$modalInstance', 'pedigreeService', 'data', 'profileDataService', searchProfileModalController])
			.controller('searchMultipleProfileModalController',  ['$scope', '$modalInstance', 'pedigreeService', 'data', 'profileDataService', searchMultipleProfileModalController])
			.controller('searchBatchModalController',  ['$scope', '$modalInstance', 'pedigreeService', 'data', 'profileDataService', searchBatchModalController])
			.controller('pedigreeScenarioController',  ['$scope', 'alertService', 'pedigreeService', '$modal', '$timeout','$window','$routeParams', '$location', pedigreeScenarioController])
			.controller('pedigreeGenotypificationController',  ['$scope', 'matchesService','locusService','$routeParams','pedigreeService','$window', 'alertService', pedigreeGenotypificationController])
			.controller('pedigreeScenarioReportController',  ['$scope', 'matchesService', pedigreeScenarioReportController])
			.controller('pedigreeCaseSummaryController', [ '$scope', '$rootScope', 'pedigreeMatchesService', 'pedigreeService', 'profileService', 'matchesService', pedigreeCaseSummaryController])
			.controller('activarPedigreeModalController',['$scope','$modalInstance','data',activarPedigreeModalController])
			.controller('metadataModalController',['$scope', 'alertService','$modalInstance','data','pedigreeService',metadataModalController])
			.controller('courtCaseCloseModalController', [ '$scope', '$modalInstance', 'info', courtCaseCloseModalController])
			.controller('groupedProfilesModal',['$scope', 'pedigreeService', '$modalInstance', 'alertService','data',groupedProfilesModal])
			.controller('matchesController', ['$scope', '$routeParams', 'matchesService',  'profileDataService','alertService','laboratoriesService', 'profileService', 'locusService', 'helper', '$anchorScroll','$timeout', matchesController])
			.controller('matchesGroupsController', ['$scope', '$routeParams', 'matchesService', 'profileService', 'profileDataService', '$filter', '$location', 'scenarioService', 'analysisTypeService', 'locusService', 'helper', 'alertService', 'userService', matchesGroupsController])
			.controller('pedigreeConsistencyController',  ['$scope', 'matchesService','locusService','$routeParams','pedigreeService','$window', 'alertService','$timeout','$filter', pedigreeConsistencyController])
			.controller('pedigreeMatchesCourtCaseController', ['$scope', 'pedigreeMatchesService', 'matchesService','alertService','profileDataService','$routeParams','pedigreeService','pedigreeMatchesGroupsService','userService', pedigreeMatchesCourtCaseController])
			.config(
				[
					'$routeProvider',
					function($routeProvider) {
						$routeProvider
							.when('/pedigree/:courtcaseId/:pedigreeId', {templateUrl: '/assets/javascripts/pedigree/views/pedigree.html', controller: 'pedigreeController'})
							.when('/pedigree-consistency/:courtcaseId/:pedigreeId', {templateUrl: '/assets/javascripts/pedigree/views/pedigree-consistency.html', controller: 'pedigreeConsistencyController'})
							.when('/pedigree/:courtcaseId', {templateUrl: '/assets/javascripts/pedigree/views/pedigree.html', controller: 'pedigreeController'})
							.when('/court-case/:id', {templateUrl: '/assets/javascripts/pedigree/views/courtcase.html', controller: 'courtcaseController'})
							.when('/court-case/', {templateUrl: '/assets/javascripts/pedigree/views/courtcase.html', controller: 'courtcaseController'})
							.when('/courtcases', {templateUrl: '/assets/javascripts/pedigree/courtcases.html', controller: 'courtcasesController' })
							.when('/courtcasesdata', {templateUrl: '/assets/javascripts/pedigree/views/courtcase-data.html', controller: 'courtcaseDataController' })
							.when('/courtcasesdata/:id',{templateUrl: '/assets/javascripts/pedigree/views/courtcase-data.html',controller:'courtcaseDataController'})
							.when('/manual-collapsing',{templateUrl: '/assets/javascripts/pedigree/views/pedigree-genotypifcation.html',controller: 'pedigreeGenotypificationController'});
					}
				]
			);
		return undefined;
	}
);