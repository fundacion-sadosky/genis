define(['angular', './searchController', './searchService', 'common'],
function(angular, searchController, SearchService, $http) {
'use strict';

angular
	.module('pdg.search', ['pdg.common'])
     .service('searchService',[ 'playRoutes', 'userService', '$http', SearchService ])
     .controller('searchController',[ '$scope', '$log', 'profileDataService', 'searchService', '$modal', 'alertService', '$location', 'userService','appConf', searchController ])
     .config([ '$routeProvider', function($routeProvider) {
         $routeProvider.when('/search/profiledata', {
             templateUrl : '/assets/javascripts/search/profiles.html',
             controller : 'searchController'
         });
     }]);

return undefined;

});