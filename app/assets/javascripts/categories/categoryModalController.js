define(['angular'], function(ng) {
'use strict';

function CategoryModalController($scope, categoryService, category, mode, alertService) {
	$scope.catIdRegex = new RegExp(/^\w{4,}$/);
	$scope.mode = mode;
	$scope.category = ng.extend({associations: {}}, category);

	$scope.add = function () {
		categoryService.createCategory($scope.category).then(
			function(response) {
				$scope.$close(response.data);
			},
			function(response) {
				alertService.error({message: response.data});
			}
		);
	};
	
	$scope.remove = function () {
		categoryService.removeCategory($scope.category).then(
			function() {
                $scope.$close($scope.category.id);
			},
			function(response) {
				alertService.error({message: response.data});
			}
		);
	};

	$scope.update = function () {
		categoryService.updateCategory($scope.category).then(
			function() {
				$scope.$close($scope.category);
			},
			function(err) {
				alertService.error({message: err});
			}
		);
	};

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};
}

return CategoryModalController;

});