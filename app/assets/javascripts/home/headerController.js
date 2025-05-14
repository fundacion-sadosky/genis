define(['jszip'], function(JSZip) {
'use strict';

function HeaderController($scope, userService, categoriesService, kitService, profileService, profileDataService, locusService, $location, $modal, hotkeys, appConf, alertService) {

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
	function downloadJSON(content) {
		var downloadUrl = URL.createObjectURL(content);
		var a = document.createElement('a');
		a.href = downloadUrl;
		a.download = 'configuration.zip';
		document.body.appendChild(a);
		a.click();
		document.body.removeChild(a);
	}

	$scope.exportConfiguration = function() {
		// Pedimos ambas exportaciones al mismo tiempo
		Promise.all([
			categoriesService.exportCategories(),
			kitService.exportKits(),
			locusService.exportLocus()
		]).then(function(responses) {
			var categoriesData = responses[0].data;
			var kitsData = responses[1].data;
			var locusData = responses[2].data;

			var zip = new JSZip();
			zip.file("categories.json", JSON.stringify(categoriesData, null, 2));
			zip.file("kits.json", JSON.stringify(kitsData, null, 2));
			zip.file("locus.json", JSON.stringify(locusData, null, 2));
			
			zip.generateAsync({ type: "blob" })
				.then(downloadJSON);

		}).catch(function(error) {
			console.error('Error al exportar la configuración:', error);
			alertService.error({message: 'Ocurrió un error al exportar la configuración.'});
		});
	};
	

	$scope.triggerFileInput = function() {
		document.getElementById('configFile').click();
	};


	$scope.importConfiguration = function(fileInput) {
		if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
			alertService.error({message: 'No se seleccionó ningún archivo.'});
			return;
		}

		var file = fileInput.files[0];
		var reader = new FileReader();

		reader.onload = function(event) {
			JSZip.loadAsync(event.target.result).then(function(zip) {
				var categoriesFile = zip.file("categories.json");
				var kitsFile = zip.file("kits.json");

				var importPromises = [];

				if (categoriesFile) {
					importPromises.push(
						categoriesFile.async("blob").then(function(blob) {
							var file = new File([blob], "categories.json", { type: "application/json" });
							return $scope.importCategories(file); // Reuse the working importCategories function
						})
					);
				} else {
					alertService.error({message: "El archivo no contiene categorías."});
				}

				if (kitsFile) {
					importPromises.push(
						kitsFile.async("blob").then(function(blob) {
							var file = new File([blob], "kits.json", { type: "application/json" });
							return $scope.importKits(file); // Add a new importKits function
						})
					);
				} else {
					alertService.error({message: "El archivo no contiene kits."});
				}

				return Promise.all(importPromises);
			}).then(function() {
				alertService.success({message: 'Configuración importada con éxito'});
			}).catch(function(error) {
				console.error('Error al importar la configuración:', error);
				alertService.error({message: "Ocurrió un error al importar la configuración."});
			});
		};

		reader.onerror = function() {
			alertService.error({message: 'No se pudo leer el archivo.'});
		};

		reader.readAsArrayBuffer(file);
	};

	$scope.importKits = function(file) {
		if (!file) return;

		var formData = new FormData();
		formData.append("file", file);

		kitService.importKits(formData).then(function(response) {
			console.log({ message: 'Kits importados con éxito' });
		}, function(error) {
			console.log("Error al importar kits: " + error.data);
		});
	};

	$scope.importCategories = function(file) {
		if (!file) return;

		var formData = new FormData();
		formData.append("file", file);

		categoriesService.importCategories(formData).then(function(response) {
			console.log({message: 'Categorías importadas con éxito'});
		}, function(error) {
			console.log("Error al importar categorías: " + error.data);
		});
	};

}

return HeaderController;

});
