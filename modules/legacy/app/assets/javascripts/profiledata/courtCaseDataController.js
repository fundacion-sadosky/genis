/**
 * profiledata controllers.
 */
define(['jquery'], function() {
'use strict';
	
function CourtCaseDataController($scope, profileDataService) {

	profileDataService.getCrimeTypes().then(function(response) {
		$scope.crimeTypes = response.data;
	});
	
}

return CourtCaseDataController;

});