define(['angular', './controllers/reportingController', 'common'],
    function(angular, reportingController) {
        'use strict';

        angular
            .module('pdg.reporting', ['pdg.common'])
            .controller('reportingController', ['$scope', 'cryptoService', 'alertService', reportingController])
            .config(['$routeProvider', function($routeProvider) {
                $routeProvider.when('/reportes-main', {
                    templateUrl: '/assets/javascripts/reporting/views/mainReporting.html',
                    controller: 'reportingController'
                }).when('/profilesReporting', {
                    templateUrl: '/assets/javascripts/reporting/views/profileReportingFilters.html',
                    controller: 'reportingController'
                }) ;
            }]);

        return undefined;

    });