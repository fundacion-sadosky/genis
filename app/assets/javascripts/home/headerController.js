define(['jszip'], function(JSZip) {
'use strict';

function HeaderController($scope, userService, categoriesService, kitService, profileService, profileDataService, locusService, roleService, $location, $modal, hotkeys, appConf, alertService) {

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
		// Pedimos todas las exportaciones al mismo tiempo
		Promise.all([
			categoriesService.exportCategories(),                    // 0
			kitService.exportKits(),                                  // 1
			locusService.exportLocus(),                                // 2
			categoriesService.exportGroups(),                          // 3

			categoriesService.exportCategoryConfigurations(),          // 4
			categoriesService.exportCategoryAssociations(),            // 5
			categoriesService.exportCategoryAlias(),                   // 6
			categoriesService.exportCategoryMatchingRules(),           // 7
			categoriesService.exportCategoryModifications(),           // 8
			categoriesService.exportCategoryMappings()                 // 9
		]).then(function(responses) {

			var categoriesData              = responses[0].data;
			var kitsData                    = responses[1].data;
			var locusData                   = responses[2].data;
			var groupsData                  = responses[3].data;

			var categoryConfigurationsData  = responses[4].data;
			var categoryAssociationsData    = responses[5].data;
			var categoryAliasData           = responses[6].data;
			var categoryMatchingRulesData   = responses[7].data;
			var categoryModificationsData   = responses[8].data;
			var categoryMappingsData        = responses[9].data;

			var zip = new JSZip();

			zip.file("categories.json", JSON.stringify(categoriesData, null, 2));
			zip.file("kits.json", JSON.stringify(kitsData, null, 2));
			zip.file("locus.json", JSON.stringify(locusData, null, 2));
			zip.file("groups.json", JSON.stringify(groupsData, null, 2));

			zip.file("category-configurations.json", JSON.stringify(categoryConfigurationsData, null, 2));
			zip.file("category-associations.json", JSON.stringify(categoryAssociationsData, null, 2));
			zip.file("category-alias.json", JSON.stringify(categoryAliasData, null, 2));
			zip.file("category-matching-rules.json", JSON.stringify(categoryMatchingRulesData, null, 2));
			zip.file("category-modifications.json", JSON.stringify(categoryModificationsData, null, 2));
			zip.file("category-mappings.json", JSON.stringify(categoryMappingsData, null, 2));

			zip.generateAsync({ type: "blob" })
				.then(downloadJSON);

		}).catch(function(error) {
			console.error('Error al exportar la configuración:', error);
			alertService.error({ message: 'Ocurrió un error al exportar la configuración.' });
		});
	};



	$scope.triggerFileInput = function() {
		document.getElementById('configFile').click();
	};


	$scope.importConfiguration = function(fileInput) {
		if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
			alertService.error({ message: 'No se seleccionó ningún archivo.' });
			return;
		}

		var file = fileInput.files[0];
		var reader = new FileReader();

		reader.onload = function(event) {
			JSZip.loadAsync(event.target.result).then(function(zip) {

				var groupsFile     = zip.file("groups.json");
				var categoriesFile = zip.file("categories.json");
				var locusFile      = zip.file("locus.json");
				var kitsFile       = zip.file("kits.json");

				// CADENA: Groups + Categories → Locus → Kits
				var mainChain = Promise.resolve();

				if (groupsFile && categoriesFile) {
					mainChain = Promise.all([
						groupsFile.async("blob"),
						categoriesFile.async("blob")
					]).then(function(blobs) {

						var groups = new File(
							[blobs[0]],
							"groups.json",
							{ type: "application/json" }
						);

						var categories = new File(
							[blobs[1]],
							"categories.json",
							{ type: "application/json" }
						);

						return $scope.importGroupsAndCategories(groups, categories);
					});
				} else {
					alertService.error({
						message: "El archivo no contiene grupos y/o categorías."
					});
				}

				mainChain = mainChain.then(function() {
					if (locusFile) {
						return locusFile.async("blob").then(function(blob) {
							var file = new File([blob], "locus.json", {
								type: "application/json"
							});
							return $scope.importLocus(file);
						});
					} else {
						alertService.error({ message: "El archivo no contiene loci." });
					}
				}).then(function() {
					if (kitsFile) {
						return kitsFile.async("blob").then(function(blob) {
							var file = new File([blob], "kits.json", {
								type: "application/json"
							});
							return $scope.importKits(file);
						});
					} else {
						alertService.error({ message: "El archivo no contiene kits." });
					}
				});

				return mainChain;

			}).then(function() {
				alertService.success({ message: 'Configuración importada con éxito' });
			}).catch(function(error) {
				console.error('Error al importar la configuración:', error);
				alertService.error({
					message: "Ocurrió un error al importar la configuración."
				});
			});
		};

		reader.onerror = function() {
			alertService.error({ message: 'No se pudo leer el archivo.' });
		};

		reader.readAsArrayBuffer(file);
	};


	$scope.importGroups = function(file) {
		if (!file) return;

		var formData = new FormData();
		formData.append("file", file);

		categoriesService.importGroups(formData).then(function(response) {
			console.log({message: 'Grupos importados con éxito'});
		}, function(error) {
			console.log("Error al importar grupos: " + error.data);
		});
	};

	$scope.importGroupsAndCategories = function(groupsFile, categoriesFile) {
		if (!groupsFile || !categoriesFile) {
			console.log("Faltan archivos de grupos/categorías");
			return;
		}

		var formData = new FormData();
		formData.append("groups", groupsFile);
		formData.append("categories", categoriesFile);

		categoriesService.importGroupsAndCategories(formData).then(
			function(response) {
				console.log("Importación exitosa", response.data);
			},
			function(error) {
				console.log("Error en la importación", error.data);
			}
		);
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

	$scope.importLocus = function(file) {
		if (!file) return;

		var formData = new FormData();
		formData.append("file", file);

		locusService.importLocus(formData).then(function(response) {
			console.log({ message: 'Loci importados con éxito' });
		}, function(error) {
			console.log("Error al importar loci: " + error.data);
		});
	};

	$scope.importRoles = function(file) {
		if (!file) return;

		var formData = new FormData();
		formData.append("file", file);

		roleService.importRoles(formData).then(function(response) {
			console.log({ message: 'Roles importados con éxito' });
		}, function(error) {
			console.log("Error al importar roles: " + error.data);
		});
	};

}

return HeaderController;

});
