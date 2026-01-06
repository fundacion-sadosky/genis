define(['angular', './controllers/traceController', './services/traceService', './controllers/traceModalController', 'common','./controllers/tracePedigreeController'],
    function(angular, TraceController, TraceService, TraceModalController,common,TracePedigreeModalController) {
        'use strict';

        angular
            .module('pdg.trace', ['pdg.common'])
            .service('traceService', ['playRoutes', TraceService])
            .controller('traceController', ['$scope', 'traceService', 'userService', '$routeParams', 'profileDataService', '$modal', TraceController])
            .controller('traceModalController', ['$scope', 'trace', 'traceService', TraceModalController])
            .controller('tracePedigreeController', ['$scope', 'traceService', 'userService', '$routeParams', 'profileDataService', '$modal','pedigreeService', TracePedigreeModalController])
            .config(['$routeProvider', function($routeProvider) {
                $routeProvider
                    .when('/trace/:profileId', {templateUrl: '/assets/javascripts/trace/views/trace.html', controller: 'traceController' })
                    .when('/trace-pedigree/:courtCaseId/:pedigreeId', {templateUrl: '/assets/javascripts/trace/views/tracePedigree.html', controller: 'tracePedigreeController' });
            }]);

        return undefined;

    });