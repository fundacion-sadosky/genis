define([], function() {
    'use strict';

    function TracePedigreeController($scope, traceService, userService, $routeParams, profileDataService, $modal,pedigreeService) {
        $scope.courtCaseId = parseInt($routeParams.courtCaseId);
        $scope.pedigreeId = parseInt($routeParams.pedigreeId);
        var user = userService.getUser();
        $scope.profileId = $routeParams.profileId;
        $scope.pedigreeMetadata = {};
        $scope.initialize = function() {
            pedigreeService.getPedigree($scope.pedigreeId).then(function(response){
                if (response.status !== 204) {
                    $scope.pedigree = response.data.pedigreeGenogram;
                    $scope.pedigreeMetadata.pedigreeMetadata = response.data.pedigreeMetaData;
                    //$scope.courtCaseId = $scope.pedigreeMetadata;
                    console.log("pedigreeMetaData",$scope.pedigreeMetadata);
                    if($scope.pedigree === undefined) {
                        $scope.pedigree = pedigreeService.defaultPedigree();
                    }
                }
            });
        };
        $scope.initialize();
        $scope.pageSize = 30;
        $scope.currentPage = 1;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        var createSearchObject = function() {
            var searchObject = {};
            searchObject.pedigreeId = $scope.pedigreeId;
            searchObject.user = user.name;
            searchObject.isSuperUser = user.superuser;
            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = $scope.pageSize;
            return searchObject;
        };
        
        $scope.searchTrace = function() {
            var searchObject = createSearchObject();
            traceService.countPedigree(searchObject).then(function(response){
                $scope.totalItems = response.headers('X-TRACE-LENGTH');
                $scope.isProcessing = true;
                traceService.searchPedigree(searchObject).then(function(response) {
                    $scope.traces = response.data;
                    $scope.isProcessing = false;
                    $scope.noResult = $scope.traces.length === 0;
                }, function() {
                    $scope.isProcessing = false;
                });
            });

        };

        $scope.searchTrace();
        
        $scope.openModal = function(trace) {
            $modal.open({
                templateUrl:'/assets/javascripts/trace/views/traceModal.html',
                controller : 'traceModalController',
                resolve : {
                    trace: function() {
                        return trace;
                    }
                }
            });
            
        };
        
    }
    return TracePedigreeController;

});