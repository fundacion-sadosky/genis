define(['angular',
        './services/profileApprovalService',
        './controllers/profileApprovalController',
        '../users/userService'], // Add the path to your userService
    function(angular, profileApprovalService, profileApprovalController, userService) { // Add userService to the arguments
        'use strict';

        angular
            .module('pdg.profileapproval', ['pdg.common', 'jm.i18next', 'ui.sortable'])
            .controller('profileApprovalController', [
                '$scope',
                'profileApprovalService',
                'alertService',
                '$q',
                '$modal',
                'bulkuploadService',
                'locusService',
                'profileService',
                'cryptoService',
                '$filter',
                'userService', // Inject userService into the controller
                profileApprovalController
            ])
            .service('profileApprovalService', ['playRoutes', profileApprovalService])
            .service('userService', ['playRoutes', '$cookies', '$window', '$log', userService]) //Register userService
            .config(['$routeProvider', function($routeProvider) {
                $routeProvider.when('/profile-approval', {
                    templateUrl: '/assets/javascripts/profileapproval/views/main.html',
                    controller: 'profileApprovalController'
                });
            }]);

        return undefined;

    });
