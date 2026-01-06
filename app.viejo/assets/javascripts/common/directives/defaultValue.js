/**
 * Directive to set an attribute to a default value
 */
define(['angular'], function(angular) {
    'use strict';

    var mod = angular.module('common.directives.defaultValue', []);
    mod.directive('defaultValue', [function() {
        return {
            restrict : 'A',
            scope: {
                ngModel: '=',
                defaultValue: '='
            },
            link: function(scope, element) {
                element.bind('blur', function() {
                    if (scope.ngModel === undefined || scope.ngModel === null || scope.ngModel.length === 0) {
                        scope.$evalAsync(function () {
                            scope.ngModel = scope.defaultValue;
                        });
                    }
                });

            }
        };
    }]);
    return mod;
});