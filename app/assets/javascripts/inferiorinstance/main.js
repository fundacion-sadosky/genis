define(['angular', './services/inferiorInstanceService', './controllers/inferiorInstanceController'],
function(angular, inferiorInstanceService, inferiorInstanceController) {
'use strict';

angular
    .module('pdg.inferiorinstance', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('inferiorInstanceController', ['$scope', 'inferiorInstanceService','alertService', inferiorInstanceController])
    .service('inferiorInstanceService', ['playRoutes', inferiorInstanceService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/inferior-instances',  {templateUrl: '/assets/javascripts/inferiorinstance/views/inferiorinstance.html', controller: 'inferiorInstanceController'});
    }]);
    
return undefined;

});