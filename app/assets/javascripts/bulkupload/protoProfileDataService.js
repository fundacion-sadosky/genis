define([], function() {
'use strict';

function ProtoProfileDataService(playRoutes, $log, $http) {

	this.updateProfileData = function(globalCode, profileData){
		return $http.put('/protoprofiledata/' + encodeURIComponent(globalCode), profileData);
		//return playRoutes.controllers.ProtoProfileData.update(globalCode).put(profileData);
	};

	this.isEditable = function(globalCode){
		return $http.get('/protoprofiledata-editable/' + encodeURIComponent(globalCode));
		//return playRoutes.controllers.ProtoProfileData.isEditable(globalCode).get();
	};

	this.getGeneticist = function(lab) {
		return $http.get('/geneticist/' + encodeURIComponent(lab));
		//return playRoutes.controllers.Geneticists.allGeneticist(lab).get();
	};

	this.getResource = function(resourceType, id){
		return $http.get('/resources/static/' + encodeURIComponent(resourceType) + '/' + encodeURIComponent(id));
		//return playRoutes.controllers.ProtoProfileData.getResources(resourceType, id).get();
	};

	this.saveProfile = function(profileData) {
		$log.info('calling service: Profiles.create');
		console.log('PROTO PRO FILE DATA');
		return $http.post('/protoprofiledata', profileData);
		//return playRoutes.controllers.ProtoProfileData.create().post(profileData);
	};

	this.getProfileDataBySampleCode = function(profileId){
		return $http.get('/protoprofiledata-complete/' + encodeURIComponent(profileId));
		//return playRoutes.controllers.ProtoProfileData.getByCode(profileId).get();
	};
	
	this.isModal = function(){return true;};
	
	this.getResourcesURL = function(){return '/resources/proto/static';};
}

return ProtoProfileDataService;

});
