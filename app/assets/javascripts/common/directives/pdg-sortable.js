define(['angular'], function(angular) {
    'use strict';

    var mod = angular.module('common.directives.pdgSortable', []);

    mod.directive('pdgSortable', [function () {

        return {
            restrict: 'E',
            scope: {
                name: '@',
                predicate: '=',
                reverse: '=',
                sortField: '@',
                numeric: '=?',
                eventHandler: '&sortAction'
            },
            templateUrl: '/assets/javascripts/common/directives/pdg-sortable.html'
        };

    }]);

    return mod;

});