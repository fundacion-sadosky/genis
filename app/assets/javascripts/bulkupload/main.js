define(['angular', './bulkuploadService', './step1Controller','./step2Controller',
        './protoProfileDataService', '../profiledata/profileDataController', 'common','./addLoteController' ],
function(angular,BulkuploadService,Step1Controller,Step2Controller, ProtoProfileDataService, ProtoProfileDataController,Common, AddLoteController, $http) {
'use strict';

angular.module('pdg.bulkupload', ['pdg.common','pdg.locus'])
	.service('bulkuploadService',['playRoutes','$http', BulkuploadService])
	.service('protoProfileDataService',['playRoutes','$http', ProtoProfileDataService])
	.controller('protoProfileDataController', ['$scope', '$log', 'protoProfileDataService', 'profileDataCommonService', '$filter', 'rp', 'alertService', '$modalInstance', ProtoProfileDataController])
	.controller('step1Controller',['$scope', '$location', 'bulkuploadService', 'Upload', 'profileDataService','cryptoService', '$log', '$modal','appConf', 'alertService', '$q','locusService', Step1Controller])
	.controller('step2Controller',['$scope', '$routeParams', 'bulkuploadService', 'helper', '$log', '$modal', 'alertService', '$q', 'userService','locusService','profileDataService', Step2Controller])
    .controller('addLoteController',['$scope', 'alertService','$filter', AddLoteController])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/profiles/bulkupload-step1', {templateUrl: '/assets/javascripts/bulkupload/step1.html', controller: 'step1Controller' })
			.when('/profiles/bulkupload-step2', {templateUrl: '/assets/javascripts/bulkupload/step2.html', controller: 'step2Controller' })
			.when('/profiles/bulkupload-step2/protoprofile/:protoprofileId', {templateUrl: '/assets/javascripts/bulkupload/step2.html', controller: 'step2Controller' });
	}]);

return undefined;

});