define(['angular'], function(angular) {
    'use strict';
    function ScenariosController($scope, $routeParams, $location, scenarioService, alertService) {
        $scope.profile = $routeParams.p;
        $scope.states = [{value: undefined, label: ''},{value: true, label: $.i18n.t('generics.validated')},{value: false, label: $.i18n.t('generics.pending')}];
        var initialize = function() {
            $scope.datepickers = {
                hourFrom : false,
                hourUntil : false
            };

            $scope.dateOptions = {
                initDate : new Date()
            };

            $scope.filters = {};

            $scope.sortField = 'date';
            $scope.ascending = false;
        };

        $scope.doTheBack = function() {
            window.history.back();
        };

        $scope.listScenarios = function() {
            initialize();
            searchScenarios({});
        };

        $scope.searchWithParameters = function() {
            $scope.sortField = 'date';
            $scope.ascending = false;
            searchScenarios($scope.filters);
        };

        $scope.sortBy = function(field) {
            $scope.sortField = field;
            $scope.ascending = !$scope.ascending;
            searchScenarios($scope.previousFilters);
        };

        var createSearchObject = function(filters) {
            $scope.previousFilters = angular.copy(filters);

            var searchObject = {};
            if (filters.name) {
                searchObject.name = filters.name;
            }

            if (filters.state !== undefined) {
                searchObject.state = filters.state;
            }

            if (filters.hourFrom) {
                searchObject.hourFrom = filters.hourFrom;
                searchObject.hourFrom.setHours(0);
                searchObject.hourFrom.setMinutes(0);
                searchObject.hourFrom.setSeconds(0);
                searchObject.hourFrom = new Date(searchObject.hourFrom.toISOString());
            }
            if (filters.hourUntil) {
                searchObject.hourUntil = filters.hourUntil;
                searchObject.hourUntil.setHours(23);
                searchObject.hourUntil.setMinutes(59);
                searchObject.hourUntil.setSeconds(59);
                searchObject.hourUntil = new Date(searchObject.hourUntil.toISOString());
            }

            searchObject.profile = $scope.profile;
            searchObject.sortField = $scope.sortField;
            searchObject.ascending = $scope.ascending;
            return searchObject;
        };

        var searchScenarios = function(filters) {
            var searchObject = createSearchObject(filters);

            scenarioService.search(searchObject).then(function(response) {
                $scope.scenarios = response.data;
            });
        };

        $scope.toggleDatePicker = function($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };

        $scope.edit = function(id){
            $location.url('/scenarios/scenario.html').search({ s:id.$oid });
        };

        $scope.delete = function(id){
            scenarioService.delete(id).then(
                function(response) {
                    if (response.data.length > 0) {
                        searchScenarios($scope.previousFilters);
                        alertService.success({message: $.i18n.t('alerts.scenario.deleted')});
                    } else {
                        alertService.error({message: $.i18n.t('alerts.scenario.unregisterError')});
                    }
                },
                function(response) {
                    alertService.error({message: response.data});
                }
            );
        };
    }

    return ScenariosController;
});
