define(['angular', './services/profileExporterToLimsService', './controllers/profileExporterToLimsController'],
    function(angular, profileExporterToLimsService, profileExporterToLimsController, $http) {
        'use strict';
        angular
    .module('pdg.profileExporterToLims', ['pdg.common'])
            .service('profileExporterToLimsService', ['playRoutes','userService', '$http', profileExporterToLimsService])
            .controller('profileExporterToLimsController', ['$scope','profileExporterToLimsService','alertService','cryptoService', profileExporterToLimsController])
            .config(['$routeProvider', function($routeProvider) {
                $routeProvider.when('/profile-exporterToLims',  {templateUrl: '/assets/javascripts/profileExporterToLims/views/main.html', controller: 'profileExporterToLimsController'});
            }]);

return undefined;

});