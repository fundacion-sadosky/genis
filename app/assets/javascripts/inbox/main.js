define(['angular', './controllers/inboxController', './controllers/advancedSearchController', './services/inboxService', './controllers/homeController'],
function(angular, inboxController, advancedSearchController, inboxService, homeController, $http) {
'use strict';

angular
    .module('pdg.inbox', ['pdg.common', 'jm.i18next'])
    .controller('inboxController', ['$scope', '$location', 'inboxService', 'alertService', '$q', 'userService', inboxController])
    .controller('advancedSearchController', ['$scope','alertService', advancedSearchController])
    .controller('homeController', ['$scope', 'userService', homeController])
    .service('inboxService', ['playRoutes', inboxService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/home',  {templateUrl: '/assets/javascripts/inbox/views/home.html', controller: 'homeController'});
    }]);

return undefined;

});