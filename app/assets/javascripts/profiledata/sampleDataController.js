/**
 * profiledata controllers.
 */
define([ 'jquery' ], function() {
	'use strict';

	function SampleDataController($scope, profileDataService, alertService) {

		$scope.dateOptions = {
			initDate : new Date()
        };

        $scope.checkMax = function(fieldName) {
			var aux = $scope.$parent.profileData[fieldName];
			var today = new Date();

			if(today-aux < 0 ){
                alertService.error({message: $.i18n.t('alerts.date.before')});
				$scope.$parent.profileData[fieldName] = undefined;

            }
        };

        $scope.checkMin = function (fieldName) {
            var aux = $scope.$parent.profileData[fieldName];
            var today = new Date();

			if(today-aux > 0){
                alertService.error({message: $.i18n.t('alerts.date.afterNow')});
				$scope.$parent.profileData[fieldName] = undefined;
                }
        };

        $scope.maxDate = $scope.maxDate ? null : new Date();
		$scope.minDate = $scope.minDate ? null : new Date();

		profileDataService.getBioMaterialTypes().then(function(response) {
			$scope.bioMaterialTypes = response.data;
		});

		profileDataService.getLaboratories().then(function(response) {
			$scope.laboratories = response.data;
		});

		$scope.giveGeneticist = function(lab) {
			profileDataService.getGeneticist(lab).then(function(response) {
				$scope.$parent.geneticists = response.data;
			});
		};

		$scope.getGeneticist = function(id){
			profileDataService.getGeneticist(id).then(function(response) {
				$scope.$parent.geneticists = response.data;
			});
		};

		$scope.datepickers = {
			sampleDate : false,
			sampleEntryDate : false,
			profileExpirationDate : false
		};

		$scope.toggleDatePicker = function($event, witch) {
			$event.preventDefault();
			$event.stopPropagation();

			$scope.datepickers[witch] = !$scope.datepickers[witch];
		};

	}

	return SampleDataController;

});