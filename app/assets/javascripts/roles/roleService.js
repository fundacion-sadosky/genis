define([], function() {
'use strict';

function RoleService(playRoutes) {

	this.getRoles = function() {
		return playRoutes.controllers.Roles.getRoles().get();
	};

	this.getRolesForSignUp = function() {
		return playRoutes.controllers.Roles.getRolesForSignUp().get();
	};

	this.getPermissions = function() {
		return playRoutes.controllers.Roles.listPermissions().get();
	};

	this.getOperations = function() {
		return playRoutes.controllers.Roles.listOperations().get();
	};
	
	this.getFullPermissions = function() {
		return playRoutes.controllers.Roles.listFullPermissions().get();
	};
	
	this.upsertRole = function(role, mode) {
		if (mode === 'add') {
			return playRoutes.controllers.Roles.addRole().post(role);
		}
		if (mode === 'edit') {
			return playRoutes.controllers.Roles.updateRole().put(role);
		}
	};
	
	this.deleteRole = function(role) {
		return playRoutes.controllers.Roles.deleteRole(role.id).delete();
	};
}
	
return RoleService;

});