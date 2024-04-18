define([], function() {
	'use strict';

	function UpdateUserCtrl ($scope, modalInstance, userService, selectedUser, roleService, alertService) {
		$scope.regexEmail = new RegExp("^[_\\w-\\+]+(\\.[_\\w-]+)*@[\\w-]+(\\.\\w+)*(\\.[A-Za-z]{2,})$");
		$scope.regexTelNum= new RegExp(/^[0-9\-\+]{7,15}$/);
		$scope.selectedUser = selectedUser;
		
		roleService.getRoles().then(function(response) {
			$scope.roles = response.data;
		});
		
		$scope.updateUser = function(){
			console.log($scope.selectedUser);
			userService.updateUser($scope.selectedUser).then(function(response){
				console.log(response);
				if (response.data.status) {
					alertService.success({message: 'Fue modificado con exito'});
					$scope.updateSuccess = true;
					$scope.closeModal();
				} else {
					alertService.error({message: 'No pudo ser modificado'});
				}
			});
		};
		
		$scope.closeModal = function(){
			if($scope.updateSuccess){
				modalInstance.close($scope.selectedUser);
			}else{
				modalInstance.close();
			}
		};		

	}
	
	return UpdateUserCtrl ;

});