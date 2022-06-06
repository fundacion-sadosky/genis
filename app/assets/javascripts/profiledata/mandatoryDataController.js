/**
 * profiledata controllers.
 */
define(['jquery'], function($) {
'use strict';
	
function MandatoryDataController($scope, profileDataService) {
	
	profileDataService.getGeneticistUsers().then(function(response) {
		var geneticistUsersDistinct = [];

		$.each(response.data, function(index){
			var exists = false;
			$.each(geneticistUsersDistinct, function(i){
				if (geneticistUsersDistinct[i].id === response.data[index].id){
					exists = true;
				}
			});

			if (!exists){
				geneticistUsersDistinct.push(response.data[index]);
			}
		});

		$scope.geneticistUsers = geneticistUsersDistinct;
	});
}

return MandatoryDataController;

});