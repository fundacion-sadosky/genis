define(['angular', './services/superiorInstanceService', './controllers/superiorInstanceController'],
function(angular, superiorInstanceService, superiorInstanceController, $http) {
'use strict';

angular
    .module('pdg.superiorinstance', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('superiorInstanceController', ['$scope', 'superiorInstanceService','alertService', superiorInstanceController])
    .service('superiorInstanceService', ['playRoutes', '$http', superiorInstanceService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/superior-instance',  {templateUrl: '/assets/javascripts/superiorinstance/views/superiorinstance.html', controller: 'superiorInstanceController'})
            .when('/menu-interconnect',  {templateUrl: '/assets/javascripts/superiorinstance/views/menu-interconnection.html', controller: 'superiorInstanceController'});
    }]);
    
return undefined;

});