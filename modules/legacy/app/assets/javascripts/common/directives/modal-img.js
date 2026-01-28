/**
 * A common directive. It would also be ok to put all directives into one file,
 * or to define one RequireJS module that references them all.
 */
define([ 'angular' ], function(angular) {
	'use strict';

	var mod = angular.module('common.directives.modalImg', []);
	mod.directive('modalImg', [ '$log', '$modal', function($log, $modal) {
		return {
			restrict : 'A',
			link : function(scope, el, attrs) {
				var modalScope = scope.$new(true);
				var src = attrs.src || attrs.ngSrc; 
				$log.debug('modal img for ' + src);
				modalScope.src = src;
				el.on('click', function() {
					$modal.open({
						templateUrl: '/assets/javascripts/common/directives/modal-img.html',
						size: 'lg',
						scope: modalScope
					});
				});	
			}
		};
	} ]);

	return mod;
});
