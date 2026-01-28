define([], function() {
'use strict';

function LoadingAppController($scope) {

	$scope.$on('pdg.loaded', function() {
		$scope.isLoading = false;
	});

	$scope.isLoading = true;

}

return LoadingAppController;

});
