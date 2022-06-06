define(['angular'], function(angular) {
'use strict';

function OperationLogLotController($scope, $routeParams, operationLogLotService, roleService, $filter) {

    $scope.results = [{value: undefined, label: ''},{value: true, label: 'Ok'},{value: false, label: 'Error'}];

    var initialize = function() {
        $scope.pageSizeLog = 30;
        $scope.currentPageLog = 1;

        $scope.datepickers = {
            hourFrom : false,
            hourUntil : false
        };

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
        localStorage.removeItem("nuevo");

        $scope.dateOptions = {
            initDate : new Date()
        };

        $scope.filters = {};
        
        $scope.sortField = 'id';
        $scope.ascending = false;
    };

	$scope.doTheBack = function() {
		window.history.back();
	};

	$scope.logLotInit = function() {
        initialize();
        searchLogs({});
	};

	$scope.changePage = function() {
        searchLogs($scope.previousFilters);
    };

    $scope.searchWithParameters = function() {
        $scope.currentPageLog = 1;
        $scope.sortField = 'id';
        $scope.ascending = false;
        searchLogs($scope.filters);
    };
    
    $scope.sortBy = function(field) {
        $scope.currentPageLog = 1;
        $scope.sortField = field;
        $scope.ascending = !$scope.ascending;
        searchLogs($scope.previousFilters);
    };

    var createSearchObject = function(filters) {
        $scope.previousFilters = angular.copy(filters);

        var searchObject = {};
        if (filters.operation) {
            searchObject.operations = $scope.operations.filter(function (op) {
                return op.description.toLowerCase().indexOf(filters.operation.toLowerCase()) !== -1;
            })
                .map(function (op) {
                    return op.operation;
                });
            delete searchObject.operation;
        }
        if (filters.user && filters.user.length > 0) {
            searchObject.user = filters.user;
        }
        searchObject.result = filters.result;
        if (filters.hourFrom) {
            searchObject.hourFrom = filters.hourFrom;
            searchObject.hourFrom.setHours(0);
            searchObject.hourFrom.setMinutes(0);
            searchObject.hourFrom.setSeconds(0);
        }
        if (filters.hourUntil) {
            searchObject.hourUntil = filters.hourUntil;
            searchObject.hourUntil.setHours(23);
            searchObject.hourUntil.setMinutes(59);
            searchObject.hourUntil.setSeconds(59);
        }
        searchObject.page = $scope.currentPageLog - 1;
        searchObject.pageSize = $scope.pageSizeLog;
        searchObject.lotId = Number($routeParams.lotId);
        searchObject.sortField = $scope.sortField;
        searchObject.ascending = $scope.ascending;
        return searchObject;
    };

    var searchLogs = function(filters) {
        var searchObject = createSearchObject(filters);

        operationLogLotService.getTotalLogs(searchObject).then(function(response){
            $scope.totalItemsLog = response.headers('X-LOGS-LENGTH');
            operationLogLotService.searchLogs(searchObject).then(function(response) {
                $scope.entries = response.data;
            });
        });

	};
	
	$scope.getOperations = function() {
        roleService.getOperations().then(function(response) {
            $scope.operations = response.data.map(function(op) {
                    return {operation: op, description: getOperationDescription(op)};
                });
        });
    };

    var getOperationDescription = function(operation) {
        return $filter('i18next')('resource.' + operation);
    };

	$scope.toggleDatePicker = function($event, witch) {
		$event.preventDefault();
		$event.stopPropagation();

		$scope.datepickers[witch] = !$scope.datepickers[witch];
	};

}

return OperationLogLotController;

});