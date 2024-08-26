define([], function() {
'use strict';

function SaveModalController($scope, scenario, results, validated, restricted, scenarioService, alertService, userService) {
	$scope.scenario = scenario;
	$scope.results = results;
	$scope.validated = validated;
	 
	$scope.save = function () {
		var user = userService.getUser().name;
		if (!$scope.validated) {
			scenarioService.createScenario($scope.scenario, restricted, $scope.name, $scope.description, $scope.results, user).then(
				function (response) {
					alertService.success({message: $.i18n.t('alerts.scenario.registered')});
					$scope.$close(response.data);
				},
				function (response) {
					alertService.error({message: response.data});
				}
			);
		} else{
				var data = {
					id: null,
					name: $scope.name,
					state: null,
					geneticist: user,
					calculationScenario: $scope.scenario,
					isRestricted: false,
					result: $scope.results,
					description: $scope.description
				};

				scenarioService.validate(data).then(function(response) {
					alertService.success({message: $.i18n.t('alerts.scenario.validatedAndSaved')});
					$scope.$close(response.data);
				},
				function(response) {
					if(response.data){
                        alertService.error({message: response.data.message});
                    }
					if(response.data && response.data.id){
                        $scope.$close(response.data.id);
                    }else{
                        $scope.$close();
                    }
                }
			);
		}
	};

	$scope.validateAndSave = function(confirmRes){
		if (!confirmRes) {return;}

		$scope.save();
	};

	$scope.cancel = function () {
		$scope.$dismiss('cancel');
	};
}

return SaveModalController;

});