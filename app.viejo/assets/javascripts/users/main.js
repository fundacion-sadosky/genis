define(['angular', './userService', './userController', './updateUserController', 'common'], 
function(angular, userService, userController, updateUserCtrl) {
'use strict';

angular
	.module('pdg.users', ['pdg.common'])
	.service('userService', ['playRoutes','$cookies','$window', '$log', userService])
	.controller('userController', ['$scope', 'userService', 'appConf', '$modal', 'roleService', 'alertService', userController])
	.controller('userUpdateController',[ '$scope', '$modalInstance', 'userService', 'selectedUser', 'roleService', 'alertService', updateUserCtrl])
	.config(['$routeProvider', function($routeProvider) {
		$routeProvider
			.when('/users', {templateUrl: '/assets/javascripts/users/users.html', controller: 'userController' });
	}]);

return undefined;

});
