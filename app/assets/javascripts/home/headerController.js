define([], function() {
'use strict';

function HeaderController($scope, userService, categoriesService, $location, $modal, hotkeys, appConf) {

	var modalInstance = null;

	$scope.version = appConf.version;
	
	hotkeys.bindTo($scope).add({
		combo : 'ctrl+alt+b',
		allowIn : [ 'INPUT', 'SELECT', 'TEXTAREA' ],
		callback : function() {
			modalInstance = $modal.open({
				templateUrl : '/assets/javascripts/home/views/ee.html',
				scope : $scope
			});
		}
	});

	// Wrap the current user from the service in a watch expression
	$scope.$watch(function() {
		var user = userService.getUser();
		return user;
	}, function(user) {
		$scope.user = user;
	}, true);

	$scope.showAbout = function() {
		modalInstance = $modal.open({
			templateUrl : '/assets/javascripts/home/views/aboutModal.html',
			scope : $scope
		});
	};

	$scope.closeModal = function() {
		modalInstance.close();
	};

	$scope.logout = function() {
		$scope.selectedMenu = undefined;
		userService.logout();
		$scope.user = undefined;
	};

	$scope.exportCategories = function() {
		categoriesService.exportCategories().then(function(response) {
			var jsonData = JSON.stringify(response.data, null, 2); // Convierte a JSON con formato legible
			var blob = new Blob([jsonData], { type: 'application/json' });
			var downloadUrl = URL.createObjectURL(blob);
			var a = document.createElement('a');
			a.href = downloadUrl;
			a.download = 'categories.json';
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
		}, function(error) {
			console.error('Error al exportar las categorías:', error);
			alert('Ocurrió un error al exportar las categorías.'); //cambiar esto para que use el error de la plataforma
		});
	};


}

return HeaderController;

});
