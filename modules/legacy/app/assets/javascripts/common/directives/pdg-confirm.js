define([ 'angular'], function(angular) {
	'use strict';

	var mod = angular.module('common.directives.pdgConfirm', []);
	
	mod.directive('pdgConfirm', ['$modal', function($modal) {

		return {
			restrict : 'A',
			scope: {
				content: "@pdgConfirmContent",
				title: "@pdgConfirmTitle",
				onConfirmBase: "&pdgOnConfirm",
				hidePopUp: '=',
				param: '@param',
				prueba:'@prueba'
			},
			link : function(scope, el/*, attrs*/) {
				if (!scope.hidePopUp) {
					var modalInstance = null;

					scope.close = function (response) {
						modalInstance.close();
						if (typeof(scope.onConfirmBase()) === "function") {
							if (scope.param){
								scope.onConfirmBase()(response, scope.param);
							} else {
								scope.onConfirmBase()(response);
							}
						}
					};

					el.on('click', function () {
						if(scope.prueba === undefined || scope.prueba === "true"){
						modalInstance = $modal.open({
							templateUrl: '/assets/javascripts/common/directives/pdg-confirm.html',
							scope: scope
						});
                        }
					});
				}else{
					el.on('click', function () {
						if (typeof(scope.onConfirmBase()) === "function") {
							scope.onConfirmBase()(true);
						}
					});

				}
			}
		};
	} ]);

	return mod;
});