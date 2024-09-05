define(['angular','./statsService','./statsController','./statsDbController', './fminController', 'common'], 
function(angular, StatsService, StatsController, StatsDbController, FminController, $http) {
'use strict';

angular
	.module('pdg.stats', ['pdg.common'])
       .service('statsService',['playRoutes', '$q', '$http', StatsService])
       .controller('statsController',['$scope', '$location', '$log', 'statsService','Upload','$modal','cryptoService', 'userService', 'alertService', StatsController])
       .controller('statsDbController',['$scope', '$location','statsService',StatsDbController])
       .controller('fminController',['$scope', 'statsService', '$modalInstance', 'data', FminController])
       .config(['$routeProvider', function($routeProvider) {
           $routeProvider.when('/stats', {
               templateUrl: '/assets/javascripts/stats/stats.html', 
               controller: 'statsController' })
           .when('/stats/:name', {
               templateUrl: '/assets/javascripts/stats/allele-freq-db.html', 
               controller: 'statsDbController' });
       }]);

return undefined;

});