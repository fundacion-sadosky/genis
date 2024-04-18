define([], function() {
'use strict';

function BmtModalController($scope, bmt, mode) {
	$scope.rolIdRegex = new RegExp(/^\w{4,}$/);
	$scope.mode = mode;
	$scope.bmt = bmt;
	
	$scope.add = function () {
		$scope.$close($scope.bmt);
	};
	
	$scope.update = function () {
		$scope.$close($scope.bmt);
	};

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};
	
	$scope.remove = function(){
		$scope.$close($scope.bmt.id);
	};

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");
}

return BmtModalController;

});