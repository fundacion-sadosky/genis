define(['angular', './profileDataService', './profileDataController', './mandatoryDataController', 
        './courtCaseDataController', './sampleDataController', './filiationDataController', './profileDataCommonService', 'common'], 
function(angular, ProfileDataService, ProfileDataController, MandatoryDataController, 
		CourtCaseDataController, SampleDataController, FiliationDataController, ProfileDataCommonService) {
'use strict';

angular
	.module('pdg.profiledata', ['pdg.common'])
	.service('profileDataService', ['playRoutes','$log', ProfileDataService])
	.service('profileDataCommonService', ['$q','playRoutes','$log', ProfileDataCommonService])
	.controller('profileDataController', ['$scope', '$log', 'profileDataService', 'profileDataCommonService', '$filter', '$routeParams', 'alertService', ProfileDataController])
	.controller('mandatoryDataController', ['$scope', 'profileDataCommonService', MandatoryDataController])
	.controller('courtCaseDataController', ['$scope', 'profileDataCommonService', CourtCaseDataController])
	.controller('sampleDataController', ['$scope', 'profileDataCommonService','alertService', SampleDataController])
	.controller('filiationDataController', ['$scope', '$log', 'profileDataService', 'Upload' ,'cryptoService', 'alertService', FiliationDataController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/profiledata', {
				templateUrl: '/assets/javascripts/profiledata/profile-data.html', 
				controller: 'profileDataController'
				})
			.when('/profiledata/:samplecode',{
				templateUrl: '/assets/javascripts/profiledata/profile-data.html', 
				controller:'profileDataController'});
	}]);

return undefined;

});
