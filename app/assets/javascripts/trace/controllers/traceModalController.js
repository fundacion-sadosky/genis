define([], function() {
'use strict';

function TraceModalController($scope, trace, traceService) {
	$scope.trace = trace;
    
    traceService.getFullDescription(trace.id).then(function(response) {
        $scope.fullDescription = response.data;
    });

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};
}

return TraceModalController;

});