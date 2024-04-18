/**
 * A common directive. It would also be ok to put all directives into one file,
 * or to define one RequireJS module that references them all.
 */
define([ 'angular' ], function(angular) {
	'use strict';

	var mod = angular.module('common.directives', []);

	mod.directive('alleleValidation', ['$log', function($log) {
		return {
			restrict: 'AE',
			//require:  '^form',
			link: function (/*scope, el, attrs, formCtrl*/) {
				
				$log.info('validaciooooon!');
//			// find the text box element, which has the 'name' attribute
//			var inputEl   = el[0].querySelector("[name]");
//			// convert the native text box element to an angular element
//			var inputNgEl = angular.element(inputEl);
//			// get the name on the text box so we know the property to check
//			// on the form controller
//			var inputName = inputNgEl.attr('name');
//			
//			// only apply the has-error class after the user leaves the text box
//			inputNgEl.bind('blur', function() {
//					el.toggleClass('has-error', formCtrl[inputName].$invalid);
//				});
				}
			};
	}]);
	

		mod.directive('otherValidation', [
			'$log',
			function($log) {
				return {
					restrict : 'AE',
					link : function($scope, el, $attrs/* , ctrl */) {

						$attrs.$observe('otherValidation', function() {
							$log.info('mi directiva: otherValidation!' + $attrs.name);
							$log.info($attrs.$$element.find("#alelo1").val());
							$log.info($attrs.$$element.find("#alelo2").val());
						});
					}
				};
			} ]);
	
	return mod;
});