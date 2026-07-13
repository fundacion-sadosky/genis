define(['angular', './controllers/mutationController','./controllers/mutationInsertController','./controllers/mutationUpdateController', './controllers/pedigreeMatchingParameterController', './services/mutationService'],
function(angular, mutationController,mutationInsertController,mutationUpdateController, pedigreeMatchingParameterController, mutationService) {
'use strict';

angular
    .module('pdg.mutations', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('mutationController', ['$scope','mutationService','alertService','$location', mutationController])
    .controller('mutationInsertController', ['$scope', 'mutationService','alertService','$location','locusService','appConf', mutationInsertController])
    .controller('mutationUpdateController', ['$scope', 'mutationService','alertService','$location','$routeParams','locusService','appConf', mutationUpdateController])
    .controller('pedigreeMatchingParameterController', ['$scope', 'mutationService', 'alertService', pedigreeMatchingParameterController])
    .service('mutationService', ['playRoutes','$q', mutationService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/mutation-models',  {templateUrl: '/assets/javascripts/mutations/views/mutations.html', controller: 'mutationController'})
            .when('/new-mutation-model',  {templateUrl: '/assets/javascripts/mutations/views/insert.html', controller: 'mutationInsertController'})
            .when('/update-mutation-model',  {templateUrl: '/assets/javascripts/mutations/views/update.html', controller: 'mutationUpdateController'})
            .when('/pedigree-matching-parameter',  {templateUrl: '/assets/javascripts/mutations/views/pedigree-matching-parameter.html', controller: 'pedigreeMatchingParameterController'})
        ;
    }]);

return undefined;

});