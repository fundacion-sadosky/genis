define([], function() {
'use strict';

function RoleService(playRoutes, $http) {

	this.getRoles = function() {
		return $http.get('/roles');
		//return playRoutes.controllers.Roles.getRoles().get();
	};

	this.getRolesForSignUp = function() {
		return $http.get('/rolesForSU');
		//return playRoutes.controllers.Roles.getRolesForSignUp().get();
	};

	this.getPermissions = function() {
		return $http.get('/permissions');
		//return playRoutes.controllers.Roles.listPermissions().get();
	};

	this.getOperations = function() {
		return $http.get('/operations');
		//return playRoutes.controllers.Roles.listOperations().get();
	};
	
	this.getFullPermissions = function() {
		return $http.get('/permissionsfull');
		//return playRoutes.controllers.Roles.listFullPermissions().get();
	};
	
	this.upsertRole = function(role, mode) {
		if (mode === 'add') {
			console.log('ADD ROLE');
			return  $http.post('/roles', role);
			//return playRoutes.controllers.Roles.addRole().post(role);
		}
		if (mode === 'edit') {
			return  $http.put('/roles', role);
			//return playRoutes.controllers.Roles.updateRole().put(role);
		}
	};
	
	this.deleteRole = function(role) {
		return $http.delete('/roles/' + role.id);
		//return playRoutes.controllers.Roles.deleteRole(role.id).delete();
	};
}
	
return RoleService;

});