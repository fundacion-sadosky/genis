define(['angular', './controllers/mutationController','./controllers/mutationInsertController','./controllers/mutationUpdateController', './services/mutationService'],
function(angular, mutationController,mutationInsertController,mutationUpdateController, mutationService, $http) {
'use strict';

angular
    .module('pdg.mutations', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('mutationController', ['$scope','mutationService','alertService','$location', mutationController])
    .controller('mutationInsertController', ['$scope', 'mutationService','alertService','$location','locusService','appConf', mutationInsertController])
    .controller('mutationUpdateController', ['$scope', 'mutationService','alertService','$location','$routeParams','locusService','appConf', mutationUpdateController])
    .service('mutationService', ['playRoutes','$q', '$http', mutationService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/mutation-models',  {templateUrl: '/assets/javascripts/mutations/views/mutations.html', controller: 'mutationController'})
            .when('/new-mutation-model',  {templateUrl: '/assets/javascripts/mutations/views/insert.html', controller: 'mutationInsertController'})
            .when('/update-mutation-model',  {templateUrl: '/assets/javascripts/mutations/views/update.html', controller: 'mutationUpdateController'})
        ;
    }]);
    
return undefined;

});