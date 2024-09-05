define(['angular', './services/profileExporterService', './controllers/profileExporterController'],
    function(angular, profileExporterService, profileExporterController, $http) {
        'use strict';
        angular
    .module('pdg.profileExporter', ['pdg.common'])
            .service('profileExporterService', ['playRoutes','userService', '$hhtp', profileExporterService])
            .controller('profileExporterController', ['$scope','profileExporterService','alertService','cryptoService', 'userService', profileExporterController])
            .config(['$routeProvider', function($routeProvider) {
                $routeProvider.when('/profile-exporter',  {templateUrl: '/assets/javascripts/profileExporter/views/main.html', controller: 'profileExporterController'});
            }]);

return undefined;

});