define([], function() {
    'use strict';

    function TraceController($scope, traceService, userService, $routeParams, profileDataService, $modal) {

        var user = userService.getUser();
        $scope.profileId = $routeParams.profileId;
        
        $scope.pageSize = 30;
        $scope.currentPage = 1;

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        profileDataService.getProfileData($scope.profileId).then(function(response) {
            $scope.profile = response.data;
        });

        var createSearchObject = function() {
            var searchObject = {};
            searchObject.profile = $scope.profileId;
            searchObject.user = user.name;
            searchObject.isSuperUser = user.superuser;
            searchObject.page = $scope.currentPage - 1;
            searchObject.pageSize = $scope.pageSize;
            return searchObject;
        };
        
        $scope.searchTrace = function() {
            var searchObject = createSearchObject();
            traceService.count(searchObject).then(function(response){
                $scope.totalItems = response.headers('X-TRACE-LENGTH');
                $scope.isProcessing = true;
                traceService.search(searchObject).then(function(response) {
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
    return TraceController;

});