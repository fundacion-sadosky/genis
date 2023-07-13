define(['angular', './controllers/newKitController','./controllers/saveController' ,'./services/kitService', './controllers/listKitController', './controllers/kitController', './directives/pdgLocusPopover'],
function(angular, newKitController,saveController, kitService, listKitController, kitController, pdgLocusPopover) {
'use strict';

angular
    .module('pdg.kits', ['pdg.common', 'jm.i18next', 'ui.sortable'])
    .controller('newKitController', ['$scope', 'analysisTypeService', 'kitService', 'alertService', 'locusService', newKitController])
    .controller('listKitController', ['$scope','$modal', 'kitService', 'alertService', 'analysisTypeService', listKitController])
    .controller('kitController', ['$scope', 'kitService', 'helper', kitController])
    .controller('kitsSaveController', ['$scope', '$log', 'kitService', 'alertService', saveController])
    .service('kitService', ['playRoutes', kitService])
    .directive('pdgLocusPopover', ['$compile', 'locusService', pdgLocusPopover])
    .config(['$routeProvider', function($routeProvider) {
        $routeProvider.when('/kits',  {templateUrl: '/assets/javascripts/kits/views/kits.html', controller: 'kitController'});
    }]);
    
return undefined;

});