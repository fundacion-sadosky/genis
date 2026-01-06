define(['jquery', 'angular'], function($, angular) {
'use strict';

function RolesCtrl ($scope, roleService, $modal, alertService, $filter) {
	$scope.mode = '';

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	function selectRole(role) {
		$scope.current = role;
	}
	
	function getRoles() {
		roleService.getRoles().then(function(response){
			$scope.roles = response.data;
			if ($scope.current) {
				selectRole($scope.roles.filter(function(x){return x.id === $scope.current.id;})[0]);
				$scope.mode = 'edit';
			} else if (!$scope.current && $scope.roles.length > 0) { 
				selectRole($scope.roles[0]);
				$scope.mode = 'edit';
			}
		});	
	}
	
	function onModalEditClose(role){
		$scope.mode = 'edit';
		if(angular.isObject(role)) { // update role
			selectRole(role);
			$scope.save();
		} else { // delete role
			selectRole(undefined);
			alertService.success({message: 'El rol se ha eliminado correctamente'});
			getRoles();
		}
	}

	function onModalAddClose(role){
		selectRole(role);
        alertService.warning({message: 'El rol fue generado, debe agregar el/los permiso/s y guardar para finalizar la operacion.'});
	}

	function openRoleModal(role, mode, onModalClose) {
		$scope.mode = mode;
		
		$modal.open({
			templateUrl:'/assets/javascripts/roles/views/roleModal.html',
			controller : 'roleModalController',
			resolve : {
				role: function() {
					return angular.copy(role);
				},
				mode: function() {
					return mode;
				}
			}
		}).result.then(function(role){
			onModalClose(role);
		});
	}

	roleService.getFullPermissions().then(function(response){
		$scope.permissions = response.data;
	});
	getRoles();
	
	$scope.premissionComparator = function(p, search) {
		var s = angular.lowercase(search);
		return angular.lowercase($.i18n.t('permission.' + p +'.name')).indexOf(s) > -1 || 
			angular.lowercase($.i18n.t('permission.' + p +'.description')).indexOf(s) > -1;
	};

	$scope.addRole = function(){
		selectRole({});
		openRoleModal({}, 'add', onModalAddClose);
	};
	
	$scope.editRole = function(role) {
		openRoleModal(role, 'edit', onModalEditClose);
	};
	
	$scope.updateRole = function(role) {
		$scope.mode = 'edit';
		selectRole(role);
	};

	$scope.save = function() {
		roleService.upsertRole($scope.current, $scope.mode).then(
			function(){
                getRoles();
                     alertService.success({message: 'El rol se ha guardado correctamente'});

			},
			function(response){
				alertService.error({message: 'Ha ocurrido un error al guardar los cambios: ' + response.data});
			});
	};

	$scope.cancel = function() {
		getRoles();
	};
	
	$scope.translatePermission = function(p) {
		return $filter('i18next')('permission.' + p.id + '.name');
	};

    $scope.verificaPermisos = function (role) {
		if(role.permissions.length===0) {
			return false;
		}else{
			return true;
		}
	};
}

return RolesCtrl;

});