define(['angular', './services/profileApprovalService', './controllers/profileApprovalController'],
function(angular, profileApprovalService, profileApprovalController, $http) {
'use strict';
angular
    .module('pdg.profileapproval', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('profileApprovalController', ['$scope', 'profileApprovalService','alertService','$q','$modal','bulkuploadService','locusService','profileService','cryptoService', profileApprovalController])
    .service('profileApprovalService', ['playRoutes', profileApprovalService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/profile-approval',  {templateUrl: '/assets/javascripts/profileapproval/views/main.html', controller: 'profileApprovalController'});
    }]);
    
return undefined;

});