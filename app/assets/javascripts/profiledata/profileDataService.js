/**
 * profiledata service, TODO: exposes user model to the rest of the app.
 */
define([], function() {
'use strict';

function ProfileDataService(playRoutes, $log, $http) {

	this.getCategories = function() {
		console.log('GET CATEGORIES');
		return $http.get('/categoryTree');
		//return playRoutes.controllers.Categories.categoryTree().get();
	};

    this.getSubCategories = function() {
		console.log('GET SUBCATEGORIES');
		return $http.get('/categories');
        //return playRoutes.controllers.Categories.list().get();
    };

    this.updateProfileData = function(globalCode, profileData){
		console.log('UPDATE PROFILE DATA');
		return $http.put('/profiledata/' + encodeURIComponent(globalCode), profileData);
		//return playRoutes.controllers.ProfileData.update(globalCode).put(profileData);
	};

	this.isEditable = function(globalCode){
		console.log('CHECK IF PROFILE DATA IS EDITABLE');
		return $http.get('/profiledata-editable/' + encodeURIComponent(globalCode));
		//return playRoutes.controllers.ProfileData.isEditable(globalCode).get();
	};

	this.getResource = function(resourceType, id){
		console.log('GET RESOURCES');
		return $http.get('/resources/static/' + resourceType + '/' + id);
		//return playRoutes.controllers.ProfileData.getResources(resourceType, id).get();
	};

	this.getCrimeTypes = function() {
		console.log('calling service: CrimeTypes.list()');
		return $http.get('/crimeTypes');
		//return playRoutes.controllers.CrimeTypes.list().get();
	};

	this.getBioMaterialTypes = function() {
		console.log('calling service: BioMaterialTypes.lis()');
		return $http.get('/bioMaterialTypes');
		//return playRoutes.controllers.BioMaterialTypes.list().get();
	};

	this.getGeneticist = function(lab) {
		console.log('calling all geneticist for '+lab);
		return $http.get('/geneticist/' + encodeURIComponent(lab));
		//return playRoutes.controllers.Geneticists.allGeneticist(lab).get();
	};

	this.getGeneticistUsers = function() {
		console.log('calling all geneticist users');
		return $http.get('/geneticist');
		//return playRoutes.controllers.Geneticists.getGeneticistUsers().get();
	};

	this.getLaboratories = function(){
		console.log('calling service: all labs');
		return $http.get('/laboratory');
		//return playRoutes.controllers.Laboratories.list().get();
	};

	this.saveProfile = function(profileData) {
		console.log('calling service: Profiles.create');
		return $http.post('/profiledata', profileData);
		//return playRoutes.controllers.ProfileData.create().post(profileData);
	};

	this.getFilesId = function(){
		console.log('calling for token for images');
		return $http.get('/getFilesId');
		//return playRoutes.controllers.Resources.getFilesId().get();
	};

	this.getProfileDataBySampleCode = function(profileId){
		console.log('GET PROFILE DATA BY SAMPLE CODE');
		return $http.get('/profiledata-complete/' + encodeURIComponent(profileId));
		//return playRoutes.controllers.ProfileData.getByCode(profileId).get();
	};

	this.getProfileData = function(profileId){
		console.log('getting ProfileData for ' + profileId);
		return $http.get('/profiledata', { params: { globalCode: profileId } });
		//return playRoutes.controllers.ProfileData.findByCode(profileId).get();
	};
	
	this.getProfilesData = function(globalCodes){
		console.log('getting ProfilesData for ');
		var codesString = globalCodes.join(',');
		return $http.get('/profilesdata?globalCodes=' + codesString);
		//return playRoutes.controllers.ProfileData.findByCodes(globalCodes).get();
	};
	
	this.getMotive = function(sampleCode){
		console.log('GET DELETE MOTIVE');
		return $http.get('/profiles-deleted/motive/' + encodeURIComponent(sampleCode));
		//return playRoutes.controllers.ProfileData.getDeleteMotive(sampleCode).get();
	};
	
	this.deleteProfile = function(sampleCode,motive){
		console.log('Deleting profile with sample code ' + sampleCode);
		return $http.put('/profiledata-deleted/' + encodeURIComponent(sampleCode), motive);
		//return playRoutes.controllers.ProfileData.deleteProfile(sampleCode).put(motive);
	};
	
	this.isModal = function(){return false;};
	
	this.getResourcesURL = function(){return '/resources/static';};
}

return ProfileDataService;

});
