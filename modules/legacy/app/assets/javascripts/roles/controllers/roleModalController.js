define(['angular', 'jquery'], function(ng, $) {
'use strict';

function RoleModalController($scope, roleService, role, mode, alertService) {
	$scope.rolIdRegex = new RegExp(/^\w{4,}$/);
	$scope.mode = mode;
	$scope.role = ng.extend({permissions: []}, role);
	//Cuando se genera una nueva clase es lo 1° que hace
	$scope.add = function () {
		$scope.mode = 'add';
		$scope.$close($scope.role);
	};
	
	$scope.remove = function () {
		roleService.deleteRole($scope.role).then(function(response){
			if ($.parseJSON(response.data.result)) {
				$scope.$close($scope.role.id);
			} else {
				alertService.error({message: 'Ha ocurrido un error: ' + response.data.error});
			}
		}, 
		function(response){
			alertService.error({message: 'Ha ocurrido un error al guardar los cambios: ' + response.data});
		});
	};
	
	$scope.update = function () {
		$scope.$close($scope.role);
	};

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};
}

return RoleModalController;

});