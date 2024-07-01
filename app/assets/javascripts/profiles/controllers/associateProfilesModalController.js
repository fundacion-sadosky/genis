define([], function() {
'use strict';

function AssociateProfilesModalController($scope, $modalInstance, $log, data, searchService, appConf, associationLabelService, alertService) {
	$scope.genotypification = data.genotypification;
	$scope.subcategory = data.subcategory;
	$scope.isProcessing = false;
	$scope.profileId = undefined;

	$scope.verify = function() {
		var re = new RegExp(appConf.sampleCodeRegex);
		if (!re.test($scope.profileId)) {
			alertService.error({message:  $.i18n.t('alerts.profile.codeError')});
			return;
		}
		
		$scope.isProcessing = true;
		associationLabelService.verifyMixtureAssociation($scope.genotypification, $scope.profileId, $scope.subcategory.id).then(
			function(response) {
				var res = response.data;
				
				if (res.result) {
					$scope.labels = res.associations;

					if (res.mergedLoci) {
						$scope.mergedLoci = res.mergedLoci;
					} else {
						$modalInstance.close($scope.labels);	
					}
				} else {
					alertService.error({message: res.error});
				}
				$scope.isProcessing = false;
			}, 
			function() {
				alertService.error({message: $.i18n.t('error.common2')});
				$scope.isProcessing = false;
			});
	};
	
	$scope.searchProfile = function() {
		var text = $scope.profileId;
		$scope.isProcessing = true;
		
		searchService.searchProfilesAssociable(text,$scope.subcategory.id).then(function(response){
			$scope.results = response.data;
			$scope.isProcessing = false;
		});			
	};
	
	$scope.setProfileSelected = function(sampleCode) {
		$scope.profileId = sampleCode;
		$scope.results = null;
	};
	
}

return AssociateProfilesModalController;

});