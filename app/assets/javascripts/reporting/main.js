define(['angular', './controllers/reportingController', 'common'], function(angular, reportingController) {
    'use strict';

    angular
        .module('pdg.reporting', ['pdg.common'])
        .controller('reportingController', ['$scope', 'cryptoService', 'alertService', reportingController])
        .config(['$routeProvider', function($routeProvider) {
            $routeProvider
                .when('/reportes-main', {
                    templateUrl: '/assets/javascripts/reporting/views/mainReporting.html',
                    controller: 'reportingController'
                })
                // Existing route
                .when('/profilesReporting', {
                    templateUrl: '/assets/javascripts/reporting/views/profileReportingFilters.html',
                    controller: 'reportingController'
                })
                // New routes for each page
                .when('/perfiles-por-usuario', {
                    templateUrl: '/assets/javascripts/reporting/views/perfiles-por-usuario.html',
                    controller: 'reportingController'
                })
                .when('/perfiles-activos-baja-por-categoria', {
                    templateUrl: '/assets/javascripts/reporting/views/perfiles-activos-baja-por-categoria.html',
                    controller: 'reportingController'
                })
                .when('/perfiles-enviados-instancia-superior', {
                    templateUrl: '/assets/javascripts/reporting/views/perfiles-enviados-instancia-superior.html',
                    controller: 'reportingController'
                })
                .when('/perfiles-recibidos-instancia-inferior', {
                    templateUrl: '/assets/javascripts/reporting/views/perfiles-recibidos-instancia-inferior.html',
                    controller: 'reportingController'
                })
                .when('/perfiles-cambiaron-categoria', {
                    templateUrl: '/assets/javascripts/reporting/views/perfiles-cambiaron-categoria.html',
                    controller: 'reportingController'
                });
        }]);

    return undefined;
});
