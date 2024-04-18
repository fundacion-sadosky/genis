define([], function() {
	'use strict';

	function StatsOptionModalController ($scope, $modal, modalInstance, selectedOptions, mix, populationBaseService, alertService, profileData, scenarioService, contributors) {
		$scope.selectedOptions = selectedOptions;
		$scope.mix = mix;
		$scope.profileData = profileData;
		$scope.contributors = contributors;


		populationBaseService.getActiveTables().then(function(tables) {
			$scope.freqTables = tables;
		});
		
		$scope.getRandomMatchProbabilitiesByLocus = function(freqTable, statModel, theta, dropIn, dropOut) {
			$scope.selectedOptions = { 'frequencyTable': freqTable, 'probabilityModel': statModel, 'theta': theta, 'dropIn': dropIn, 'dropOut': dropOut};
			
			if (!$scope.selectedOptions.frequencyTable){
				alertService.error({message: 'No existe ninguna base de frecuencias seleccionada o por default. Por favor, seleccione una base de frecuencias.'});
				return;
			}
			
			if (!$scope.selectedOptions.probabilityModel){
				alertService.error({message: 'No existe ningún modelo seleccionado o asociado. Por favor, seleccione un modelo estadístico.'});
				return;
			}
			
			modalInstance.close($scope.selectedOptions); 
		};
		
		$scope.closeStatModal = function(){
			modalInstance.dismiss();
		};
        
        var printHypothesis = function(hypothesis) {
            return scenarioService.printHypothesis($scope.scenario[hypothesis], $scope.profileData);
        };
        
        var profiles = Object.keys($scope.profileData);
        
        scenarioService.getDefaultScenario(profiles[0], profiles[1], selectedOptions).then(function(response) {
            $scope.scenario = response.data;
			if($scope.scenario.isMixMix) {
				$scope.printableProsecutor = 'Un aportante en comun';
				$scope.printableDefense = 'Mezclas independientes';
			} else {
				$scope.printableProsecutor = printHypothesis('prosecutor');
				$scope.printableDefense = printHypothesis('defense');
			}
        });
        
        
        
		
	}
	
	return StatsOptionModalController ;

});