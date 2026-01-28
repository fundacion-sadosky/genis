define([], function() {
'use strict';

function ProtoProfileDataService(playRoutes, $log) {

	this.updateProfileData = function(globalCode, profileData){
		return playRoutes.controllers.ProtoProfileData.update(globalCode).put(profileData);
	};

	this.isEditable = function(globalCode){
		return playRoutes.controllers.ProtoProfileData.isEditable(globalCode).get();
	};

	this.getGeneticist = function(lab) {
		return playRoutes.controllers.Geneticists.allGeneticist(lab).get();
	};

	this.getResource = function(resourceType, id){
		return playRoutes.controllers.ProtoProfileData.getResources(resourceType, id).get();
	};

	this.saveProfile = function(profileData) {
		$log.info('calling service: Profiles.create');
		return playRoutes.controllers.ProtoProfileData.create().post(profileData);
	};

	this.getProfileDataBySampleCode = function(profileId){
		return playRoutes.controllers.ProtoProfileData.getByCode(profileId).get();
	};
	
	this.isModal = function(){return true;};
	
	this.getResourcesURL = function(){return '/resources/proto/static';};
}

return ProtoProfileDataService;

});
