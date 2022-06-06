define(['angular'], function(ng) {
'use strict';

function UserController($scope, userService, appConf, $modal, roleService, alertService) {
	var modalInstance = null;
	$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");

	$scope.roles = roleService.getRoles();

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	userService.listUsers().then(function(response) {
		$scope.users = response.data;
	});

	$scope.statusList = appConf.userStatusList;
	
	$scope.updateUserStatus = function(userName, status) {
		userService.updateUserSatus(userName, status).then(function(){
			alertService.success({message: 'Fue modificado con exito'});
		}, function(data){
			alertService.error({message: 'No pudo ser modificado' + ((data.data)? ': ' + data.data: '')});
		});
	};
	
	$scope.updateUser = function(user){
		var user2 = ng.copy(user);
		modalInstance = $modal.open({
			templateUrl:'/assets/javascripts/users/view/updateUser.html',
			controller: 'userUpdateController',
			scope: $scope,
			resolve: {
				selectedUser: function () {
                   return user2;
                }}
		});
		
		modalInstance.result.then(
				function (posibleUser) {
					if(posibleUser){
						user.firstName = posibleUser.firstName;
						user.lastName = posibleUser.lastName;
						user.email = posibleUser.email;
						user.geneMapperId = posibleUser.geneMapperId;
						user.phone1 = posibleUser.phone1;
						user.phone2 = posibleUser.phone2;
						user.roles = posibleUser.roles;
						user.superuser = posibleUser.superuser;
					}
				}, 
				function () {//dismissed
				}
			);
	};

}

return UserController;

});