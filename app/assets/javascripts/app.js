define(['angular', 'sensitiveOper', 'appConf',  './httpInterceptors/requestAuthorizationInterceptor', './httpInterceptors/secureRequestInterceptor', './httpInterceptors/responseErrorInterceptor','home', 'users', 'login', 'search', 'profiledata', 'geneticists', 'categories', 'laboratories','profiles', 'stats', 'audit', 'matches', 'bulkupload', 'roles', 'pedigree', 'biomaterialtype', 'scenarios', 'matchesGroups', 'inbox', 'locus', 'kits', 'trace', 'pedigreeMatches', 'pedigreeMatchesGroups','superiorinstance','inferiorinstance','profileapproval','motives', 'reporting','mutations','profileExporter', 'profileExporterToLims'],
function(angular, sensitiveOperations, appConf,  requestAuthorizationInterceptor, secureRequestInterceptor, responseErrorInterceptor) {
'use strict';
  
angular
	.module('app', ['pdg.home', 'pdg.users', 'pdg.login', 'pdg.signup', 'pdg.profiledata', 'pdg.geneticists', 'pdg.categories', 'pdg.search', 'pdg.stats', 'pdg.profile', 'pdg.laboratories', 'pdg.audit', 'pdg.matches', 'pdg.bulkupload', 'pdg.roles', 'pdg.pedigree', 'pdg.biomaterialtype', 'pdg.scenarios', 'pdg.matchesGroups', 'pdg.inbox', 'pdg.locus', 'pdg.kits', 'pdg.trace', 'pdg.pedigree.matches', 'pdg.pedigreeMatchesGroups','pdg.superiorinstance','pdg.inferiorinstance','pdg.profileapproval','pdg.motives', 'pdg.reporting','pdg.mutations','pdg.profileExporter', 'pdg.profileExporterToLims'])
	.constant('$log', console)
	.constant('sensitiveOperations', sensitiveOperations)
	.constant('appConf', appConf)
//	.factory('$exceptionHandler', function() {
//		return function(exception, cause) {
//			exception.message += ' (caused by "' + cause + '")';
//			throw exception;
//		};
//	})
	.factory('requestAuthorizationInterceptor', ['$q', '$window', '$log', 'totpPromptService', 'sensitiveOperations', requestAuthorizationInterceptor])
	.factory('secureRequestInterceptor', ['$q', '$window', '$log', '$injector', secureRequestInterceptor])
	.factory('responseErrorInterceptor', ['$q', '$location', '$log', '$cookies', '$injector', 'errorService', responseErrorInterceptor ] )
	.config(['$httpProvider', '$routeProvider', '$compileProvider', function ($httpProvider, $routeProvider, $compileProvider) {
		
		$httpProvider.interceptors.push('requestAuthorizationInterceptor');
		$httpProvider.interceptors.push('secureRequestInterceptor');
		$httpProvider.interceptors.push('responseErrorInterceptor');
		$compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|local|blob):/);
		
		$routeProvider.otherwise( {templateUrl: '/assets/javascripts/home/notFound.html'});

	}])
	.run(['$rootScope', function($rootScope) {

		$rootScope.pageTitle = 'GENIS';
		
	}]);

return undefined;

});
