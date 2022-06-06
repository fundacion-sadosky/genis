define(['jquery'], function(jquery) {
'use strict';

function StringencyModalController($scope, $modalInstance, data, profileService, alertService) {

	$scope.stringency = { 
		HighStringency : {key:'HighStringency', text:'Alta'}, 
		ModerateStringency : {key:'ModerateStringency', text:'Media'}, 
		LowStringency : {key:'LowStringency', text:'Baja'} 
	};
	$scope.mismatchesOptions = [0, 1, 2, 3];

	$scope.data = data;
	
	function fillMismatchesByDefault(){
		if (!$scope.mismatches) {
			$scope.mismatches = jquery.extend({}, $scope.subcatsRel);
			Object.keys($scope.mismatches).forEach(function(k){$scope.mismatches[k] = 0;});
		}
	}
	
	profileService.getSubcategories().then(function(response) {
		$scope.subcats = response.data;
	}, 
	function() {
		alertService.error({message: 'Ha ocurrido un error'});
	});
	
	if ($scope.data.subcatsRel && Object.keys($scope.data.subcatsRel).length > 0) {
		$scope.subcatsRel = $scope.data.subcatsRel;
		fillMismatchesByDefault();
	} else {
		profileService.findSubcategoryRelationships(data.subcategory.id).then(function(response) {
			$scope.subcatsRel = response.data;
			fillMismatchesByDefault();
		});
	}
	
	if ($scope.data.mismatches && Object.keys($scope.data.mismatches).length > 0) {
		$scope.mismatches = $scope.data.mismatches;
	}
	
	$scope.closeModal = function(save) {
		if (save) {
			$modalInstance.close({subcatsRel: $scope.subcatsRel, mismatches: $scope.mismatches});
		} else {
			$modalInstance.dismiss('cancel');
		}
	};
	
}
	
return StringencyModalController;

});