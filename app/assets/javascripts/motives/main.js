define(['angular', './controllers/motiveController', './services/motiveService'],
function(angular, motiveController, motiveService) {
'use strict';

angular
    .module('pdg.motives', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('motiveController', ['$scope', '$modal','motiveService','alertService', motiveController])
    .service('motiveService', ['playRoutes', motiveService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/motives',  {templateUrl: '/assets/javascripts/motives/views/motives.html', controller: 'motiveController'});
    }]);
    
return undefined;

});