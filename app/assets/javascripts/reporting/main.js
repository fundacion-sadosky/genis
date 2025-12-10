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
                }).when('/profilesByUser', {
                    templateUrl: '/assets/javascripts/reporting/views/profilesByUser.html',
                    controller: 'reportingController'
                }).when('/activesInactivesByCategory', {
                    templateUrl: '/assets/javascripts/reporting/views/activesInactivesByCategory.html',
                    controller: 'reportingController'
                }).when('/enviadosInstanciaSuperior', {
                    templateUrl: '/assets/javascripts/reporting/views/enviadosInstanciaSuperior.html',
                    controller: 'reportingController'
                }).when('/recibidosInstanciaInferior', {
                    templateUrl: '/assets/javascripts/reporting/views/recibidosInstanciaInferior.html',
                    controller: 'reportingController'
                });
            }]);

        return undefined;

    });