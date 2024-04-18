define([ 'angular', 'jquery'], function(angular, $) {
    'use strict';

    var mod = angular.module('common.directives.tagsInputModal', []);
        
    mod.directive('tagsInputModal', [function () {

        return {
            restrict: 'E',
            scope: {
                collection: '=ngModel',
                separators: '=?',
                placeholder: '@',
                repeat: '@'
            },
            template: '<div id="tagsInputModal" class="div" ng-style="stDiv"><span class="tag" ng-style="stTag" ng-repeat="element in collection track by $index">{{element}}<span class="remove" ng-style="stRemove" ng-click="remove(element)">&times;</span></span><input class="input" ng-style="stInput" placeholder="{{placeholder}}" type="text" id="newElementModal" ng-model="newElementModal"/></div>',

            link: function (scope) {
                if (!scope.separators) {
                    scope.separators = [9, 13];
                }

                scope.add = function (element, event) {
                    if (element &&
                        element.length > 0 &&
                        (scope.repeat || scope.collection.indexOf(element) === -1)) {
                        scope.collection.push(element);
                        scope.newElementModal = undefined;
                        scope.refresh(event);
                    }
                };

                scope.pop = function (event) {
                    if (!scope.newElementModal || scope.newElementModal.length === 0) {
                        scope.collection.pop();
                        scope.refresh(event);
                    }
                };

                scope.remove = function (element) {
                    scope.collection.splice(scope.collection.indexOf(element), 1);
                };

                scope.refresh = function (element) {
                    $('#newElementModal').focus();

                    scope.$apply();

                    element.preventDefault();
                };

                $('#tagsInputModal').on('keydown', '#newElementModal', function (event) {
                    var keyCode = event.keyCode || event.which;

                    // Add new element using the separators
                    if (scope.separators.indexOf(keyCode) > -1) {
                        scope.add(scope.newElementModal, event);
                    }

                    // Discard last element using back space
                    if (keyCode === 8) {
                        scope.pop(event);
                    }
                });

                // Add new element on input blur
                $('#newElementModal').blur(function (event) {
                    scope.add(scope.newElementModal, event);
                    $('#tagsInputModal').removeClass('focus');
                });

                $('#tagsInputModal').click(function () {
                    $('#newElementModal').focus();
                });
                
                $('#newElementModal').focus(function () {
                    $('#tagsInputModal').addClass('focus');
                });
                
                // Styles
                
                scope.stDiv = {
                    'border': '1px solid #ccc', 
                    'display': 'inline-block', 
                    'width': '100%',
                    'padding': '4px 6px', 
                    'vertical-align': 'middle',
                    'line-height': '22px',
                    'cursor': 'text'
                };

                scope.stTag = {
                    'padding': '.2em .6em .3em',
                    'line-height': '1',
                    'text-align': 'center',
                    'white-space': 'nowrap',
                    'vertical-align': 'baseline',
                    'background-color': 'transparent',
                    'margin-right': '5px',
                    'color': '#3f3e3e',
                    'display': 'inline-block',
                    'border': '1px solid #ccc',
                    'border-radius': '.25em',
                    'font-family': 'Open Sans, sans-serif',
                    'font-size': '14px'
                };
                
                scope.stRemove = {
                    'margin-left': '2px',
                    'cursor': 'pointer',
                    'color': '#000000'
                };
                
                scope.stInput = {
                    'border': 'none',
                    'size': '2px',
                    'box-shadow': 'none',
                    'background-color': 'transparent',
                    'outline': 'none',
                    'padding': '0 6px',
                    'margin': '0',
                    'width': 'auto',
                    'max-width': 'inherit'
                };

            }
        };

    }]);

    return mod;
    
});