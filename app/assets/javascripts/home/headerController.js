define(['JSZip'], function() {
'use strict';

function HeaderController($scope, userService, categoriesService, kitService, $location, $modal, hotkeys, appConf, alertService) {

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

	$scope.exportConfiguration = function() {
		// Pedimos ambas exportaciones al mismo tiempo
		Promise.all([
			categoriesService.exportCategories(),
			kitService.exportKits()
		]).then(function(responses) {
			var categoriesData = responses[0].data;
			var kitsData = responses[1].data;

			var zip = new JSZip();
			zip.file("categories.json", JSON.stringify(categoriesData, null, 2));
			zip.file("kits.json", JSON.stringify(kitsData, null, 2));

			zip.generateAsync({ type: "blob" })
				.then(function(content) {
					var downloadUrl = URL.createObjectURL(content);
					var a = document.createElement('a');
					a.href = downloadUrl;
					a.download = 'configuration.zip';
					document.body.appendChild(a);
					a.click();
					document.body.removeChild(a);
				});

		}).catch(function(error) {
			console.error('Error al exportar la configuración:', error);
			alertService.error('Ocurrió un error al exportar la configuración.');
		});
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
			alertService.error('Ocurrió un error al exportar las categorías.');
		});
	};

	$scope.exportKits = function() {
		kitService.exportKits().then(function(response) {
			var jsonData = JSON.stringify(response.data, null, 2); // Convierte a JSON con formato legible
			var blob = new Blob([jsonData], { type: 'application/json' });
			var downloadUrl = URL.createObjectURL(blob);
			var a = document.createElement('a');
			a.href = downloadUrl;
			a.download = 'kits.json';
			document.body.appendChild(a);
			a.click();
			document.body.removeChild(a);
		}, function(error) {
			console.error('Error al exportar los kits:', error);
			alertService.error('Ocurrió un error al exportar los kits.');
		});
	};

	$scope.triggerFileInput = function() {
		document.getElementById('categoryFile').click();
	};


	$scope.importConfiguration = function() {
		$scope.importCategories();
	};

	$scope.importCategories = function(file) {
		if (!file) return;

		var formData = new FormData();
		formData.append("file", file);

		categoriesService.importCategories(formData).then(function(response) {
			alertService.success({message: 'Categorías importadas con éxito'});
		}, function(error) {
			alertService.error("Error al importar categorías: " + error.data);
		});
	};

}

return HeaderController;

});
