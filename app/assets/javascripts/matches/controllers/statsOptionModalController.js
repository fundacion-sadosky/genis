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
				alertService.error({message: $.i18n.t('match.noBaseFrequency')});
				return;
			}
			
			if (!$scope.selectedOptions.probabilityModel){
				alertService.error({message: $.i18n.t('match.noProbabilityModel')});
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
				$scope.printableProsecutor = $.i18n.t('matches.oneContributorCommon');
				$scope.printableDefense = $.i18n.t('matches.independentMixtures');
			} else {
				$scope.printableProsecutor = printHypothesis('prosecutor');
				$scope.printableDefense = printHypothesis('defense');
			}
        });
        
        
        
		
	}
	
	return StatsOptionModalController ;

});