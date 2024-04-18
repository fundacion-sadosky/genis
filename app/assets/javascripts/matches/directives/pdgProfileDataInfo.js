define([], function() {
'use strict';

function pdgProfileDataInfo(profiledataService) {

	return {
		restrict : 'E',
		scope: {
			profileId: "=",
			profileData: "="
		},
		templateUrl: '/assets/javascripts/matches/directives/pdg-profile-data-info.html',
		
		link : function(scope) {
			
			profiledataService.getProfileData(scope.profileId).then(
				function(response) {
					scope.profileData = response.data;
			});
			
		}
	};
	
}

return pdgProfileDataInfo;

});