define([ 'angular'], function(angular) {
    'use strict';

    var mod = angular.module('common.directives.formValidation', []);

    mod.directive('formValidation', [function () {

        return {
            restrict: 'E',
            scope: {
                input: '=',
                pattern: '@',
                min: '@',
                max: '@'
            },
            templateUrl: '/assets/javascripts/common/directives/formValidation.html'
        };

    }]);

    return mod;

});