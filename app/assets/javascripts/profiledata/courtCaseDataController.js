/**
 * profiledata controllers.
 */
define(['jquery'], function() {
	'use strict';
	function CourtCaseDataController($scope, profileDataService) {
		profileDataService.getCrimeTypes().then(function(response) {
			// Almacenar todos los datos de los crímenes
			$scope.allCrimeData = response.data;

			// Extraer los tipos de crímenes como objetos con id y name
			$scope.crimeTypes = Object.values(response.data).map(function(crimeType) {
				return { id: crimeType.id, name: crimeType.name };
			});

			// Vigilar cambios en el tipo de crimen seleccionado
			$scope.$watch('$parent.profileData.crimeType', function(newVal) {
				if (newVal && $scope.allCrimeData[newVal]) {
					// Actualizar la lista de crímenes según el tipo de crimen seleccionado
					$scope.crimesInvolved = $scope.allCrimeData[newVal].crimes;
				} else {
					$scope.crimesInvolved = []; // Reiniciar si no se selecciona un tipo de crimen válido
				}
			});
		});
	}

	return CourtCaseDataController;

});