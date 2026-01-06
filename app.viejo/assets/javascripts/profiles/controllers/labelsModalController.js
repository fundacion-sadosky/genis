define([], function() {
'use strict';

function LabelsModalController($scope, $modalInstance, labelsSets) {

	$scope.labelsSets = labelsSets;

}

return LabelsModalController;

});