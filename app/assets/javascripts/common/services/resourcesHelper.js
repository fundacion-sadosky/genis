define([ 'angular', 'common' ], function(angular) {
	'use strict';

	var mod = angular.module('common.resourcesHelper', []);

	mod.factory('resourcesHelper', [
			'playRoutes',
			'$log',
			function(playRoutes, $log) {

				return {
					getFilesId : function() {
						$log.info('resourcesHelper: calling for token for images');
						return playRoutes.controllers.Resources.getFilesId().get();
					}
				};
				
			}]);

	return mod;
});