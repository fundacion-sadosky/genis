define(['angular', './services/profileExporterService', './controllers/profileExporterController'],
    function(angular, profileExporterService, profileExporterController) {
        'use strict';
        angular
    .module('pdg.profileExporter', ['pdg.common'])
            .service('profileExporterService', ['playRoutes','userService', profileExporterService])
            .controller('profileExporterController', ['$scope','profileExporterService','alertService','cryptoService', 'userService', profileExporterController])
            .config(['$routeProvider', function($routeProvider) {
                $routeProvider.when('/profile-exporter',  {templateUrl: '/assets/javascripts/profileExporter/views/main.html', controller: 'profileExporterController'});
            }]);

return undefined;

});