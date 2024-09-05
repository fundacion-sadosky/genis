define(['angular', './controllers/profileController', './controllers/labelsModalController', './controllers/stringencyModalController',
        './services/profileHelper', './services/profileService', './services/associationLabelService',
        './controllers/associateProfilesModalController', './controllers/newAnalysisController',
		'./controllers/dnaController', './controllers/newAnalysisConfirmationController','./controllers/profileComparisonController',
		'./filters/filters', 'common'],
function(angular, ProfileController, LabelsModalController, StringencyModalController,
		ProfileHelper, ProfileService, AssociationLabelService,
		AssociateProfilesModalController, NewAnalysisController, DnaController,
        NewAnalysisConfirmationController,ProfileComparisonController, filters, $http) {
'use strict';

angular
	.module('pdg.profile', ['pdg.common'])
	.service('profileHelper', ['profileService', '$http',ProfileHelper])
	.service('profileService', ['playRoutes', '$log', '$q', 'userService', '$hhtp', ProfileService])
	.service('associationLabelService', ['playRoutes', '$http', AssociationLabelService])
	.controller('profileController', ['$scope', '$rootScope','$routeParams', '$log', 'profileService', 'analysisTypeService', '$route', '$location', 'profileHelper', '$modal' ,'cryptoService', 'alertService', 'locusService', 'kitService', 'Upload', 'resourcesHelper','appConf',ProfileController])
	.controller('newAnalysisController', ['$sce','$scope','$routeParams', '$log', 'profileService', 'Upload', 'resourcesHelper', '$location', '$modal' ,'cryptoService', 'alertService','appConf', NewAnalysisController])
    .controller('newAnalysisConfirmationController', ['$scope', 'newAnalysis', 'controller', NewAnalysisConfirmationController])
    .controller('dnaController', ['$scope', '$rootScope','$routeParams', '$log', 'profileService', '$route', '$location', '$modal', 'alertService', 'resourcesHelper','appConf', DnaController])
	.controller('labelsModalController', ['$scope', '$modalInstance', 'labelsSets', LabelsModalController])
	.controller('associateProfilesModalController',  ['$scope', '$modalInstance', '$log', 'data', 'searchService', 'appConf', 'associationLabelService', 'alertService', AssociateProfilesModalController])
	.controller('stringencyModalController', ['$scope', '$modalInstance', 'data', 'profileService', 'alertService', StringencyModalController])
    .controller('profileComparisonController',  ['$scope', 'matchesService','locusService','$routeParams','profileService','$window', 'alertService','searchService', ProfileComparisonController])
	.filter('filterByLocusType', [filters.filterByLocusType])
    .filter('filterByKitType', [filters.filterByKitType])
    .filter('isRange', [filters.isRange])
    .filter('isRegion', [filters.isRegion])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/profile/:profileId', {templateUrl: '/assets/javascripts/profiles/profiles.html', controller: 'profileController' })
            .when('/profile-comparison', {templateUrl: '/assets/javascripts/profiles/views/profileComparison.html', controller: 'profileComparisonController' });
	}]);

return undefined;

});