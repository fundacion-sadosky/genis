define(['angular', './controllers/locusController', './services/locusService', './controllers/newLocusController', './services/analysisTypeService', './controllers/listLocusController', './controllers/locusUpdateController'],
function(angular, locusController, locusService, newLocusController, analysisTypeService, listLocusController,locusUpdateController) {
'use strict';

angular
    .module('pdg.locus', ['pdg.common', 'jm.i18next'])
    .controller('locusController', ['$scope', 'locusService', 'helper', 'analysisTypeService', locusController])
    .controller('newLocusController', ['$scope', 'locusService', 'alertService', newLocusController])
    .controller('listLocusController', ['$scope', 'locusService', 'alertService','$modal', listLocusController])
    .controller('locusUpdateController', ['$scope', 'locusService', 'alertService', locusUpdateController])
    .service('locusService', ['playRoutes', locusService])
    .service('analysisTypeService', ['playRoutes', '$q', analysisTypeService])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/locus',  {templateUrl: '/assets/javascripts/locus/views/locus.html', controller: 'locusController'});
    }]);

return undefined;

});