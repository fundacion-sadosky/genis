define(['angular'], function(ng) {
'use strict';

function GroupModalController($scope, categoryService, group, mode, alertService) {
	$scope.groupRegex = new RegExp(/^\w{4,}$/);
	$scope.mode = mode;
	$scope.group = group;
	
	$scope.add = function () {
		categoryService.createGroup($scope.group).then(
			function() {
				$scope.$close(ng.extend($scope.group, { isOpen: true, categories:[] }));
			},
			function(response) {
				alertService.error({message: response.data});
			}
		);
	};
	
	$scope.remove = function () {
		categoryService.removeGroup($scope.group).then(
			function() {
				$scope.$close($scope.group.id);
			},
			function(response) {
				alertService.error({message: response.data});
			}
		);
	};

	$scope.update = function () {
		categoryService.updateGroup($scope.group).then(
			function() {
				$scope.$close($scope.group);
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

return GroupModalController;

});