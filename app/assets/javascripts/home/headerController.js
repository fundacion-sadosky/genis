define(['jszip'], function(JSZip) {
'use strict';

function HeaderController($scope, userService, categoriesService, kitService, profileService, profileDataService, locusService, roleService, $location, $modal, hotkeys, appConf, alertService) {

	var modalInstance = null;

	$scope.version = appConf.version;

	$scope.hasProfiles = false;
	profileDataService.countProfiles().then(function(response) {
		$scope.hasProfiles = response.data > 0;
	});
	
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
		console.log("Importando configuración")
		if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
			alertService.error({ message: 'No se seleccionó ningún archivo.' });
			return;
		}

		var file = fileInput.files[0];
		var reader = new FileReader();
		console.debug("Leyendo archivos")

		reader.onload = function(event) {
			console.debug("Leyendo archivos: reader abierto")
			JSZip.loadAsync(event.target.result).then(function(zip) {

				console.debug("Descomprimiendo archivos")
				var groupsFile                   = zip.file("groups.json");
				var categoriesFile               = zip.file("categories.json");
				var categoryConfigurationsFile   = zip.file("category-configurations.json");
				var categoryAssociationsFile     = zip.file("category-associations.json");
				var categoryAliasFile            = zip.file("category-alias.json");
				var categoryMatchingRulesFile    = zip.file("category-matching-rules.json");
				var categoryModificationsFile    = zip.file("category-modifications.json");
				var categoryMappingsFile         = zip.file("category-mappings.json");

				var locusFile = zip.file("locus.json");
				var kitsFile  = zip.file("kits.json");
				console.debug("Validando archivos")

				// Validación temprana
				if (
					!groupsFile ||
					!categoriesFile ||
					!categoryConfigurationsFile ||
					!categoryAssociationsFile ||
					!categoryAliasFile ||
					!categoryMatchingRulesFile ||
					!categoryModificationsFile ||
					!categoryMappingsFile
				) {
					alertService.error({
						message: "El archivo no contiene todos los archivos obligatorios de categorías."
					});
					console.debug("Archivos validados")
					return Promise.reject();
				}

				console.debug("Importar configuración: archivos abiertos")
				// CADENA: Categorías (completo) → Locus → Kits
				return Promise.all([
					groupsFile.async("blob"),
					categoriesFile.async("blob"),
					categoryConfigurationsFile.async("blob"),
					categoryAssociationsFile.async("blob"),
					categoryAliasFile.async("blob"),
					categoryMatchingRulesFile.async("blob"),
					categoryModificationsFile.async("blob"),
					categoryMappingsFile.async("blob")
				]).then(function(blobs) {
					console.debug("Importando configuración de categorías y sus grupos")
					var formData = new FormData();

					formData.append("groups",
						new File([blobs[0]], "groups.json", { type: "application/json" })
					);
					formData.append("categories",
						new File([blobs[1]], "categories.json", { type: "application/json" })
					);
					formData.append("categoryConfigurations",
						new File([blobs[2]], "category-configurations.json", { type: "application/json" })
					);
					formData.append("categoryAssociations",
						new File([blobs[3]], "category-associations.json", { type: "application/json" })
					);
					formData.append("categoryAlias",
						new File([blobs[4]], "category-alias.json", { type: "application/json" })
					);
					formData.append("categoryMatchingRules",
						new File([blobs[5]], "category-matching-rules.json", { type: "application/json" })
					);
					formData.append("categoryModifications",
						new File([blobs[6]], "category-modifications.json", { type: "application/json" })
					);
					formData.append("categoryMappings",
						new File([blobs[7]], "category-mappings.json", { type: "application/json" })
					);

					return $scope.importGroupsAndCategories(formData);
				}).then(function() {
					console.debug("Importando configuración de loci")

					// Locus
					if (!locusFile) {
						alertService.error({ message: "El archivo no contiene loci." });
						return Promise.reject();
					}

					return locusFile.async("blob").then(function(blob) {
						return $scope.importLocus(
							new File([blob], "locus.json", { type: "application/json" })
						);
					});

				}).then(function() {
					console.debug("Importando configuración de kits")

					// Kits
					if (!kitsFile) {
						alertService.error({ message: "El archivo no contiene kits." });
						return Promise.reject();
					}

					return kitsFile.async("blob").then(function(blob) {
						return $scope.importKits(
							new File([blob], "kits.json", { type: "application/json" })
						);
					});
				})
					.then(function() {
						alertService.success({ message: 'Configuración importada con éxito' });
					})
					.catch(function(error) {
						if (error) {
							console.error('Error al importar la configuración:', error);
							alertService.error({
								message: "Ocurrió un error al importar la configuración."
							});
						}
					});
			});
		};
		reader.onerror = function() {
			alertService.error({ message: 'No se pudo leer el archivo.' });
		};
		reader.readAsArrayBuffer(file);
	};

	$scope.importGroupsAndCategories = function(formData) {

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
